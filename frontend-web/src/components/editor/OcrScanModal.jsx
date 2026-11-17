import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  X,
  Upload,
  Loader2,
  Plus,
  RotateCcw,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import flashcardApi from '../../api/flashcardApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

const MAX_OCR_BYTES = 10 * 1024 * 1024;
const ACCEPTED_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/bmp',
  'image/tiff',
];

function buildSnappedHighlighterLines(boxes, selectedIds) {
  if (!boxes.length || !selectedIds.length) return [];
  const idSet = new Set(selectedIds);
  const selected = boxes.filter((b) => idSet.has(b.id));
  if (!selected.length) return [];

  const groups = [[selected[0]]];
  for (let i = 1; i < selected.length; i++) {
    const prev = selected[i - 1];
    const curr = selected[i];
    const prevCy = (prev.y + prev.height / 2) * 100;
    const currCy = (curr.y + curr.height / 2) * 100;
    const maxH = Math.max(prev.height, curr.height) * 100;
    const angleDiff = Math.abs((curr.angle || 0) - (prev.angle || 0));

    // Same line if centers align vertically (or along same tilted baseline) and angle matches
    const sameLine =
      angleDiff < 12 &&
      (Math.abs(currCy - prevCy) <= maxH * 1.35 ||
        (Math.abs(curr.angle || 0) > 8 &&
          Math.abs((curr.x - (prev.x + prev.width)) * 100) < maxH * 3.0));

    if (sameLine) {
      groups[groups.length - 1].push(curr);
    } else {
      groups.push([curr]);
    }
  }

  return groups.map((group) => {
    const first = group[0];
    const last = group[group.length - 1];
    const avgAngle =
      group.reduce((sum, b) => sum + (b.angle || 0), 0) / group.length;
    const rad = (avgAngle * Math.PI) / 180;
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);

    const firstCx = (first.x + first.width / 2) * 100;
    const firstCy = (first.y + first.height / 2) * 100;
    const firstHw = (first.width * 100) / 2;

    const lastCx = (last.x + last.width / 2) * 100;
    const lastCy = (last.y + last.height / 2) * 100;
    const lastHw = (last.width * 100) / 2;

    const avgH =
      (group.reduce((sum, b) => sum + b.height * 100, 0) / group.length) * 1.1;

    return {
      x1: firstCx - firstHw * cos,
      y1: firstCy - firstHw * sin,
      x2: lastCx + lastHw * cos,
      y2: lastCy + lastHw * sin,
      strokeWidth: Math.max(2.2, Math.min(8.5, avgH)),
    };
  });
}

export default function OcrScanModal({
  open,
  onClose,
  defaultSourceLang = 'en',
  defaultTargetLang = 'vi',
  onAddExtractedCards,
}) {
  const fileInputRef = useRef(null);
  const imageContainerRef = useRef(null);

  const [previewUrl, setPreviewUrl] = useState('');
  const [scanning, setScanning] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [ocrData, setOcrData] = useState(null);

  // Active target input for "Vuốt để chọn" ('term' | 'definition')
  const [activeTarget, setActiveTarget] = useState('term');
  const [draftTerm, setDraftTerm] = useState('');
  const [draftDefinition, setDraftDefinition] = useState('');

  // Selected box IDs per field (highlighter strokes snap cleanly to these boxes)
  const [termBoxIds, setTermBoxIds] = useState([]);
  const [defBoxIds, setDefBoxIds] = useState([]);
  const [isSwiping, setIsSwiping] = useState(false);

  // Staged cards saved silently via "Lưu thẻ"
  const [stagedCards, setStagedCards] = useState([]);

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  useEffect(() => {
    if (!open) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    const handleGlobalUp = () => setIsSwiping(false);
    window.addEventListener('keydown', handleEsc);
    window.addEventListener('pointerup', handleGlobalUp);
    return () => {
      window.removeEventListener('keydown', handleEsc);
      window.removeEventListener('pointerup', handleGlobalUp);
    };
  }, [open, onClose]);

  const boxes = useMemo(() => ocrData?.boxes || [], [ocrData]);
  const autoCards = useMemo(() => ocrData?.cards || [], [ocrData]);

  const totalReadyCards = useMemo(() => {
    const list = [...stagedCards];
    if (draftTerm.trim() || draftDefinition.trim()) {
      list.push({
        key: 'current_draft',
        term: draftTerm.trim(),
        definition: draftDefinition.trim(),
      });
    }
    return list.filter((c) => c.term || c.definition);
  }, [stagedCards, draftTerm, draftDefinition]);

  if (!open) return null;

  const runOcrScan = async (file) => {
    setScanning(true);
    setErrorMsg('');
    setOcrData(null);
    setTermBoxIds([]);
    setDefBoxIds([]);
    try {
      const res = await flashcardApi.ocrExtract(file, {
        sourceLang: defaultSourceLang || 'en',
        targetLang: defaultTargetLang || 'vi',
      });
      const data = res?.data || {};
      setOcrData(data);

      if ((!data.boxes || data.boxes.length === 0) && data.cards?.length > 0) {
        setStagedCards(
          data.cards.map((c, i) => ({
            key: `auto_${Date.now()}_${i}`,
            term: c.term || '',
            definition: c.definition || '',
          }))
        );
      }

      if (!data.boxes?.length && !data.cards?.length && !data.rawText) {
        setErrorMsg(
          data.warnings?.[0] ||
            'Không nhận diện được chữ trong ảnh. Hãy thử ảnh rõ nét hơn.'
        );
      }
    } catch (err) {
      setErrorMsg(
        extractApiErrorMessage(
          err,
          'Không thể kết nối dịch vụ OCR. Hãy kiểm tra ai-ml-service (port 8000) đã chạy chưa.'
        )
      );
    } finally {
      setScanning(false);
    }
  };

  const handleFileSelect = (file) => {
    if (!file) return;
    setErrorMsg('');

    const extOk = /\.(jpe?g|png|webp|bmp|tiff?)$/i.test(file.name || '');
    if (!ACCEPTED_TYPES.includes(file.type) && !extOk) {
      setErrorMsg('Chỉ hỗ trợ ảnh JPG, PNG, WEBP, BMP hoặc TIFF.');
      return;
    }
    if (file.size > MAX_OCR_BYTES) {
      setErrorMsg('Ảnh quét OCR tối đa 10MB.');
      return;
    }

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(URL.createObjectURL(file));
    setDraftTerm('');
    setDraftDefinition('');
    setStagedCards([]);
    setActiveTarget('term');
    runOcrScan(file);
  };

  const applyBoxesToActiveField = (nextIds) => {
    const idSet = new Set(nextIds);
    const orderedText = boxes
      .filter((b) => idSet.has(b.id))
      .map((b) => b.text)
      .join(' ')
      .trim();

    if (activeTarget === 'term') {
      setTermBoxIds(nextIds);
      setDraftTerm(orderedText);
    } else {
      setDefBoxIds(nextIds);
      setDraftDefinition(orderedText);
    }
  };

  const addBoxToActiveSwipe = (boxId) => {
    const current = activeTarget === 'term' ? termBoxIds : defBoxIds;
    if (current.includes(boxId)) return;
    applyBoxesToActiveField([...current, boxId]);
  };

  const getNormalizedPointer = (e) => {
    const container = imageContainerRef.current;
    if (!container) return null;
    const rect = container.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return null;
    return {
      nx: Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)),
      ny: Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height)),
    };
  };

  const findBoxIdAtPoint = (nx, ny) => {
    if (!boxes.length) return null;
    const pad = 0.005;
    const hit = boxes.find((b) => {
      const cx = b.x + b.width / 2;
      const cy = b.y + b.height / 2;
      const angleDeg = b.angle || 0;
      if (Math.abs(angleDeg) < 0.5) {
        return (
          nx >= b.x - pad &&
          nx <= b.x + b.width + pad &&
          ny >= b.y - pad &&
          ny <= b.y + b.height + pad
        );
      }
      const rad = (-angleDeg * Math.PI) / 180;
      const dx = nx - cx;
      const dy = ny - cy;
      const rx = dx * Math.cos(rad) - dy * Math.sin(rad);
      const ry = dx * Math.sin(rad) + dy * Math.cos(rad);
      return (
        Math.abs(rx) <= b.width / 2 + pad &&
        Math.abs(ry) <= b.height / 2 + pad
      );
    });
    return hit ? hit.id : null;
  };

  const handlePointerDownOnImage = (e) => {
    if (!boxes.length) return;
    const pt = getNormalizedPointer(e);
    if (!pt) return;
    setIsSwiping(true);
    const boxId = findBoxIdAtPoint(pt.nx, pt.ny);
    if (boxId) {
      addBoxToActiveSwipe(boxId);
    }
  };

  const handlePointerMoveOnImage = (e) => {
    if (!isSwiping || !boxes.length) return;
    const pt = getNormalizedPointer(e);
    if (!pt) return;
    const boxId = findBoxIdAtPoint(pt.nx, pt.ny);
    if (boxId) {
      addBoxToActiveSwipe(boxId);
    }
  };

  const handleStageCurrentPair = () => {
    const t = draftTerm.trim();
    const d = draftDefinition.trim();
    if (!t && !d) return;
    setStagedCards((prev) => [
      ...prev,
      { key: `staged_${Date.now()}_${prev.length}`, term: t, definition: d },
    ]);
    setDraftTerm('');
    setDraftDefinition('');
    setTermBoxIds([]);
    setDefBoxIds([]);
    setActiveTarget('term');
  };

  const handleImportAutoCards = () => {
    if (!autoCards.length) return;
    setStagedCards((prev) => [
      ...prev,
      ...autoCards.map((c, idx) => ({
        key: `auto_${Date.now()}_${idx}`,
        term: c.term || '',
        definition: c.definition || '',
      })),
    ]);
  };

  const handleFinishAndAdd = () => {
    if (totalReadyCards.length === 0) return;
    onAddExtractedCards(
      totalReadyCards.map(({ term, definition }) => ({ term, definition }))
    );
    onClose();
  };

  const termBoxSet = new Set(termBoxIds);
  const defBoxSet = new Set(defBoxIds);

  return (
    <div className="fixed inset-0 z-50 h-screen w-screen bg-white dark:bg-[#0A082D] flex flex-col overflow-hidden">
      {/* Header: Title on left, Close (X) on top-right */}
      <header className="h-14 px-4 sm:px-8 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3 shrink-0 bg-white dark:bg-[#0A082D]">
        <h2 className="text-[18px] font-extrabold text-[#282E3E] dark:text-white">
          Vuốt để chọn
        </h2>

        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
        >
          <X className="w-5 h-5" />
        </button>
      </header>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/bmp,image/tiff"
        className="hidden"
        onChange={(e) => handleFileSelect(e.target.files?.[0])}
      />

      {/* Main Content — zero outer scrollbar */}
      {!previewUrl ? (
        <div className="flex-1 flex items-center justify-center p-6 overflow-hidden">
          <div
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              const f = e.dataTransfer?.files?.[0];
              if (f) handleFileSelect(f);
            }}
            onClick={() => fileInputRef.current?.click()}
            className="w-full max-w-xl rounded-2xl border-2 border-dashed border-[#D9DDE8] dark:border-[#3D4870] hover:border-[#4255FF] bg-[#F6F7FB] dark:bg-[#202140] p-12 text-center cursor-pointer transition space-y-3"
          >
            <div className="w-14 h-14 rounded-2xl bg-white dark:bg-[#171836] shadow-sm flex items-center justify-center text-[#4255FF] mx-auto">
              <Upload className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
              Tải ảnh tài liệu lên để quét OCR
            </h3>
            <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
              Kéo thả hoặc nhấn để chọn ảnh trang sách, ghi chú, màn hình học tập (JPG, PNG, WEBP ≤ 10MB).
            </p>
          </div>
        </div>
      ) : (
        <div className="flex-1 overflow-hidden px-4 sm:px-8 py-4">
          <div className="h-full max-w-[1360px] mx-auto flex flex-col">
            {errorMsg && (
              <div className="mb-3 p-3 rounded-xl bg-rose-50 dark:bg-rose-900/25 border border-rose-200 dark:border-rose-800/50 text-xs sm:text-sm font-semibold text-rose-700 dark:text-rose-300 flex items-center gap-2 shrink-0">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Split 2 Columns fitting cleanly within viewport height */}
            <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              {/* LEFT HALF: Image Canvas with Swipeable Boxes */}
              <div className="lg:col-span-6 h-full max-h-[calc(100vh-96px)] rounded-2xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] p-3 flex items-center justify-center overflow-hidden">
                {scanning ? (
                  <div className="flex flex-col items-center justify-center gap-3 text-sm font-bold text-[#4255FF]">
                    <Loader2 className="w-8 h-8 animate-spin" />
                    <span>Đang quét OCR...</span>
                  </div>
                ) : (
                  <div
                    ref={imageContainerRef}
                    onPointerDown={handlePointerDownOnImage}
                    onPointerMove={handlePointerMoveOnImage}
                    className="relative inline-block select-none touch-none max-w-full max-h-full"
                  >
                    <img
                      src={previewUrl}
                      alt="Ảnh quét OCR"
                      draggable={false}
                      className="max-h-[calc(100vh-124px)] max-w-full w-auto h-auto rounded-lg shadow-sm pointer-events-none select-none object-contain"
                    />

                    {/* Straight Snapped Green Highlighter Lines (immune to hand shake) */}
                    <svg
                      viewBox="0 0 100 100"
                      preserveAspectRatio="none"
                      className="absolute inset-0 w-full h-full pointer-events-none z-10 mix-blend-multiply dark:mix-blend-screen"
                    >
                      {[
                        ...buildSnappedHighlighterLines(boxes, termBoxIds),
                        ...buildSnappedHighlighterLines(boxes, defBoxIds),
                      ].map((ln, idx) => (
                        <line
                          key={idx}
                          x1={ln.x1}
                          y1={ln.y1}
                          x2={ln.x2}
                          y2={ln.y2}
                          stroke="#23B26D"
                          strokeOpacity={0.44}
                          strokeWidth={ln.strokeWidth}
                          strokeLinecap="round"
                        />
                      ))}
                    </svg>

                    {/* Word Bounding Boxes: crisp blue outline; hidden once swiped so only highlighter stroke remains */}
                    {boxes.map((box) => {
                      const isSwiped = termBoxSet.has(box.id) || defBoxSet.has(box.id);
                      return (
                        <div
                          key={box.id}
                          data-box-id={box.id}
                          title={box.text}
                          style={{
                            left: `${box.x * 100}%`,
                            top: `${box.y * 100}%`,
                            width: `${Math.max(box.width * 100, 1.0)}%`,
                            height: `${Math.max(box.height * 100, 1.4)}%`,
                            transform: box.angle ? `rotate(${box.angle}deg)` : undefined,
                            transformOrigin: 'center center',
                          }}
                          className={`absolute rounded-[3px] cursor-crosshair transition-opacity duration-150 ${
                            isSwiped
                              ? 'opacity-0'
                              : 'border border-[#4255FF]/80 bg-[#4255FF]/8 hover:bg-[#4255FF]/18'
                          }`}
                        />
                      );
                    })}
                  </div>
                )}
              </div>

              {/* RIGHT HALF: 3 Buttons (top) + Compact White Card with 2 SetEditor-sized Inputs */}
              <div className="lg:col-span-6 space-y-4">
                {/* 3 Action Buttons */}
                <div className="flex flex-wrap items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-white transition"
                  >
                    <RotateCcw className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
                    <span>Chọn ảnh</span>
                  </button>

                  <button
                    type="button"
                    disabled={!draftTerm.trim() && !draftDefinition.trim()}
                    onClick={handleStageCurrentPair}
                    className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#EDEFFF] hover:bg-[#DFE3FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] disabled:opacity-50 text-[13px] font-bold text-[#4255FF] dark:text-[#9AA7FF] transition"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Lưu thẻ</span>
                  </button>

                  <button
                    type="button"
                    disabled={totalReadyCards.length === 0}
                    onClick={handleFinishAndAdd}
                    className="inline-flex items-center gap-1.5 h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-50 text-white text-[13px] font-bold shadow-sm transition"
                  >
                    <span>
                      {totalReadyCards.length > 0
                        ? `Thêm thẻ (${totalReadyCards.length})`
                        : 'Thêm thẻ'}
                    </span>
                  </button>

                  {autoCards.length > 0 && stagedCards.length === 0 && (
                    <button
                      type="button"
                      onClick={handleImportAutoCards}
                      className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] text-[13px] font-bold text-[#4255FF] transition"
                    >
                      <Sparkles className="w-4 h-4" />
                      <span>Tách nhanh {autoCards.length} cặp</span>
                    </button>
                  )}
                </div>

                {/* Compact White Card with 2 Textboxes matching SetEditorPage size (h-12) & labels ABOVE */}
                <div className="rounded-2xl bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] p-4 space-y-3.5 shadow-[0_4px_16px_rgba(40,46,62,0.08)]">
                  {/* THUẬT NGỮ */}
                  <div onClick={() => setActiveTarget('term')}>
                    <p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1]">
                      Thuật ngữ
                    </p>
                    <div className="relative">
                      <input
                        type="text"
                        value={draftTerm}
                        onFocus={() => setActiveTarget('term')}
                        onChange={(e) => setDraftTerm(e.target.value)}
                        className={`w-full h-12 px-3.5 pr-9 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] border-2 text-[15px] font-medium text-[#282E3E] dark:text-white outline-none transition ${
                          activeTarget === 'term'
                            ? 'border-[#4255FF] bg-white dark:bg-[#171836]'
                            : 'border-transparent'
                        }`}
                      />
                      {draftTerm && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setDraftTerm('');
                            setTermBoxIds([]);
                          }}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-[#939BB4] hover:text-[#282E3E]"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* ĐỊNH NGHĨA */}
                  <div onClick={() => setActiveTarget('definition')}>
                    <p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1]">
                      Định nghĩa
                    </p>
                    <div className="relative">
                      <input
                        type="text"
                        value={draftDefinition}
                        onFocus={() => setActiveTarget('definition')}
                        onChange={(e) => setDraftDefinition(e.target.value)}
                        className={`w-full h-12 px-3.5 pr-9 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] border-2 text-[15px] font-medium text-[#282E3E] dark:text-white outline-none transition ${
                          activeTarget === 'definition'
                            ? 'border-[#4255FF] bg-white dark:bg-[#171836]'
                            : 'border-transparent'
                        }`}
                      />
                      {draftDefinition && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setDraftDefinition('');
                            setDefBoxIds([]);
                          }}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-[#939BB4] hover:text-[#282E3E]"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

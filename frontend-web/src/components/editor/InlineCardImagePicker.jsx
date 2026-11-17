import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Loader2,
  Search,
  Upload,
} from 'lucide-react';
import mediaApi from '../../api/mediaApi';
import { searchBuiltinIllustrations } from '../../data/illustrationLibrary';
import { extractApiErrorMessage } from '../../utils/userHelpers';

const PAGE_SIZE = 4;
const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
const ALLOWED_UPLOAD_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];

export default function InlineCardImagePicker({
  side = 'definition',
  card,
  onSelectImage,
  onClose,
}) {
  const fileInputRef = useRef(null);

  const initialQuery =
    side === 'term'
      ? card?.term || card?.definition || ''
      : card?.definition || card?.term || '';

  const [mode, setMode] = useState('gallery'); // 'gallery' | 'upload'
  const [query, setQuery] = useState(initialQuery);
  const [remoteItems, setRemoteItems] = useState([]);
  const [searching, setSearching] = useState(false);
  const [pageIdx, setPageIdx] = useState(0);
  const [errorMsg, setErrorMsg] = useState('');
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    setPageIdx(0);
    setErrorMsg('');
  }, [side, query]);

  const builtinItems = useMemo(() => searchBuiltinIllustrations(query), [query]);

  const combinedItems = useMemo(() => {
    if (!remoteItems.length) return builtinItems;
    const seen = new Set(builtinItems.map((i) => i.imageUrl));
    const extras = remoteItems.filter((r) => r?.imageUrl && !seen.has(r.imageUrl));
    return [...builtinItems, ...extras];
  }, [builtinItems, remoteItems]);

  const maxPage = Math.max(0, Math.ceil(combinedItems.length / PAGE_SIZE) - 1);
  const visibleItems = combinedItems.slice(pageIdx * PAGE_SIZE, pageIdx * PAGE_SIZE + PAGE_SIZE);

  const handleSearchRemote = async (e) => {
    e?.preventDefault();
    const q = query.trim();
    setPageIdx(0);
    if (!q) {
      setRemoteItems([]);
      return;
    }
    setSearching(true);
    try {
      const res = await mediaApi.searchStockImages(q, 12);
      setRemoteItems(res?.data?.items || []);
    } catch {
      setRemoteItems([]);
    } finally {
      setSearching(false);
    }
  };

  const handleUploadFile = async (file) => {
    if (!file) return;
    setErrorMsg('');
    if (!ALLOWED_UPLOAD_TYPES.includes(file.type)) {
      setErrorMsg('Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF.');
      return;
    }
    if (file.size > MAX_UPLOAD_BYTES) {
      setErrorMsg('Ảnh tải lên tối đa 5MB.');
      return;
    }

    setUploading(true);
    try {
      const res = await mediaApi.uploadCardImage(file);
      if (res?.data?.url) {
        onSelectImage(res.data.url, side);
        onClose();
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Tải ảnh lên thất bại.'));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div
      onClick={(e) => e.stopPropagation()}
      className="px-5 pt-4 pb-4 border-t border-[#EDEFF4] dark:border-[#2E3856] bg-white dark:bg-[#202140] rounded-b-2xl space-y-4"
    >
      {/* Header Row */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-[14px] font-extrabold text-[#282E3E] dark:text-white">
          {side === 'term' ? 'Thêm hình ảnh cho thuật ngữ' : 'Thêm hình ảnh cho định nghĩa'}
        </p>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => {
              setMode('gallery');
              setErrorMsg('');
            }}
            className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold transition ${
              mode === 'gallery'
                ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]'
                : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
            }`}
          >
            Thư viện ảnh
          </button>

          <button
            type="button"
            onClick={() => {
              setMode('upload');
              setErrorMsg('');
            }}
            className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold transition ${
              mode === 'upload'
                ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]'
                : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
            }`}
          >
            <Upload className="w-3.5 h-3.5" />
            Tải lên
          </button>
        </div>
      </div>

      {errorMsg && (
        <p className="text-xs font-semibold text-rose-600 dark:text-rose-400">
          {errorMsg}
        </p>
      )}

      {/* MODE 1: GALLERY / STOCK SEARCH */}
      {mode === 'gallery' && (
        <>
          <form onSubmit={handleSearchRemote} className="relative">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Tìm kiếm hình ảnh"
              className="w-full h-11 pl-4 pr-11 rounded-xl bg-white dark:bg-[#171836] border-2 border-[#4255FF]/70 focus:border-[#4255FF] text-[14px] font-medium text-[#282E3E] dark:text-white placeholder:text-[#939BB4] outline-none transition"
            />
            <button
              type="submit"
              aria-label="Tìm kiếm hình ảnh"
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#586380] hover:text-[#4255FF] dark:text-[#A8B1D1]"
            >
              {searching ? (
                <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
              ) : (
                <Search className="w-5 h-5" />
              )}
            </button>
          </form>

          <div className="relative flex items-center gap-2">
            <button
              type="button"
              disabled={pageIdx <= 0}
              onClick={() => setPageIdx((p) => Math.max(0, p - 1))}
              aria-label="Ảnh trước"
              className="w-9 h-9 rounded-full shrink-0 flex items-center justify-center transition bg-[#F6F7FB] dark:bg-[#2E3856] text-[#939BB4] enabled:bg-[#4255FF] enabled:text-white enabled:hover:bg-[#3545E0] disabled:opacity-50"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <div className="flex-1 grid grid-cols-2 sm:grid-cols-4 gap-3">
              {visibleItems.map((item) => (
                <button
                  key={item.id || item.imageUrl}
                  type="button"
                  onClick={() => {
                    onSelectImage(item.imageUrl, side);
                    onClose();
                  }}
                  title={item.title || item.attribution || 'Chọn hình ảnh'}
                  className="group relative aspect-[4/3] rounded-xl overflow-hidden border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] hover:ring-2 hover:ring-[#4255FF]/25 bg-[#F6F7FB] dark:bg-[#171836] transition"
                >
                  <img
                    src={item.previewUrl || item.imageUrl}
                    alt={item.title || item.attribution || 'Hình ảnh minh họa'}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                  />
                  {item.title && (
                    <span className="absolute inset-x-0 bottom-0 px-2 py-1 bg-black/55 text-[11px] font-bold text-white truncate opacity-0 group-hover:opacity-100 transition-opacity">
                      {item.title}
                    </span>
                  )}
                </button>
              ))}
            </div>

            <button
              type="button"
              disabled={pageIdx >= maxPage}
              onClick={() => setPageIdx((p) => Math.min(maxPage, p + 1))}
              aria-label="Ảnh tiếp theo"
              className="w-9 h-9 rounded-full shrink-0 flex items-center justify-center transition bg-[#F6F7FB] dark:bg-[#2E3856] text-[#939BB4] enabled:bg-[#4255FF] enabled:text-white enabled:hover:bg-[#3545E0] disabled:opacity-50"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </>
      )}

      {/* MODE 2: UPLOAD FROM DEVICE */}
      {mode === 'upload' && (
        <div
          onClick={() => !uploading && fileInputRef.current?.click()}
          className="rounded-xl border-2 border-dashed border-[#D9DDE8] dark:border-[#3D4870] hover:border-[#4255FF] bg-[#F6F7FB] dark:bg-[#171836] py-6 px-4 text-center cursor-pointer transition"
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            className="hidden"
            onChange={(e) => handleUploadFile(e.target.files?.[0])}
          />
          {uploading ? (
            <div className="inline-flex items-center gap-2 text-sm font-bold text-[#4255FF]">
              <Loader2 className="w-4 h-4 animate-spin" />
              Đang tải ảnh lên...
            </div>
          ) : (
            <div className="space-y-1">
              <Upload className="w-5 h-5 text-[#4255FF] mx-auto" />
              <p className="text-xs font-bold text-[#282E3E] dark:text-white">
                Nhấn để chọn ảnh từ máy tính (JPG, PNG, WEBP, GIF ≤ 5MB)
              </p>
            </div>
          )}
        </div>
      )}

      {/* Bottom Row: only "^ Đóng" on the right */}
      <div className="flex items-center justify-end pt-1">
        <button
          type="button"
          onClick={onClose}
          className="inline-flex items-center gap-1 text-xs font-bold text-[#282E3E] dark:text-[#E2E8F0] hover:text-[#4255FF] transition"
        >
          <ChevronUp className="w-4 h-4" />
          <span>Đóng</span>
        </button>
      </div>
    </div>
  );
}

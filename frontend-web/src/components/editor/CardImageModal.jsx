import React, { useEffect, useRef, useState } from 'react';
import {
  X,
  Sparkles,
  Search,
  Upload,
  Loader2,
  Image as ImageIcon,
  Check,
  AlertCircle,
  Link2,
} from 'lucide-react';
import mediaApi from '../../api/mediaApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

const AI_STYLES = [
  { id: 'illustration', label: 'Minh họa' },
  { id: 'cartoon', label: 'Hoạt hình' },
  { id: 'realistic', label: 'Ảnh thật' },
  { id: 'minimalist', label: 'Tối giản' },
  { id: '3d', label: '3D Render' },
];

const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
const ALLOWED_UPLOAD_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];

export default function CardImageModal({ open, card, onClose, onSelectImage }) {
  const fileInputRef = useRef(null);

  const [activeTab, setActiveTab] = useState('ai'); // 'ai' | 'stock' | 'upload'
  const [errorMsg, setErrorMsg] = useState('');

  // Tab 1: AI Generate
  const [aiTerm, setAiTerm] = useState('');
  const [aiDefinition, setAiDefinition] = useState('');
  const [aiStyle, setAiStyle] = useState('illustration');
  const [generatingAi, setGeneratingAi] = useState(false);
  const [aiResult, setAiResult] = useState(null);
  const [aiImgLoading, setAiImgLoading] = useState(false);

  // Tab 2: Stock Search
  const [stockQuery, setStockQuery] = useState('');
  const [searchingStock, setSearchingStock] = useState(false);
  const [stockItems, setStockItems] = useState([]);
  const [stockProvider, setStockProvider] = useState('');

  // Tab 3: Upload / URL
  const [uploading, setUploading] = useState(false);
  const [manualUrl, setManualUrl] = useState('');

  useEffect(() => {
    if (!open) return;
    setErrorMsg('');
    setAiTerm(card?.term || '');
    setAiDefinition(card?.definition || '');
    setStockQuery(card?.term || '');
    setManualUrl(card?.imageUrl || '');
    setAiResult(null);
    setStockItems([]);
    setStockProvider('');
  }, [open, card]);

  if (!open) return null;

  const handleGenerateAi = async (e) => {
    e?.preventDefault();
    if (!aiTerm.trim()) {
      setErrorMsg('Vui lòng nhập thuật ngữ (Term) để tạo ảnh minh họa AI.');
      return;
    }
    setErrorMsg('');
    setGeneratingAi(true);
    try {
      const res = await mediaApi.generateAiImage({
        term: aiTerm.trim(),
        definition: aiDefinition.trim(),
        style: aiStyle,
      });
      const data = res?.data;
      if (data?.imageUrl) {
        setAiResult(data);
        setAiImgLoading(true);
      } else {
        setErrorMsg('Không nhận được URL ảnh từ dịch vụ AI.');
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể tạo ảnh AI lúc này.'));
    } finally {
      setGeneratingAi(false);
    }
  };

  const handleSearchStock = async (e) => {
    e?.preventDefault();
    if (!stockQuery.trim()) {
      setErrorMsg('Vui lòng nhập từ khóa tìm ảnh.');
      return;
    }
    setErrorMsg('');
    setSearchingStock(true);
    try {
      const res = await mediaApi.searchStockImages(stockQuery.trim(), 12);
      const data = res?.data;
      setStockItems(data?.items || []);
      setStockProvider(data?.provider || '');
      if (!data?.items?.length) {
        setErrorMsg('Không tìm thấy ảnh phù hợp cho từ khóa này.');
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể tìm kiếm ảnh miễn phí.'));
    } finally {
      setSearchingStock(false);
    }
  };

  const handleUploadFile = async (file) => {
    if (!file) return;
    setErrorMsg('');
    if (!ALLOWED_UPLOAD_TYPES.includes(file.type)) {
      setErrorMsg('Chỉ hỗ trợ ảnh định dạng JPG, PNG, WEBP hoặc GIF.');
      return;
    }
    if (file.size > MAX_UPLOAD_BYTES) {
      setErrorMsg('Ảnh tải lên tối đa 5MB.');
      return;
    }

    setUploading(true);
    try {
      const res = await mediaApi.uploadCardImage(file);
      const uploadedUrl = res?.data?.url;
      if (uploadedUrl) {
        onSelectImage(uploadedUrl);
        onClose();
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Tải ảnh lên thất bại.'));
    } finally {
      setUploading(false);
    }
  };

  const handleApplyManualUrl = (e) => {
    e.preventDefault();
    const trimmed = manualUrl.trim();
    if (!trimmed) {
      setErrorMsg('Vui lòng nhập đường dẫn ảnh hợp lệ.');
      return;
    }
    onSelectImage(trimmed);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-3xl max-h-[90vh] flex flex-col bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#8E9BFF] flex items-center justify-center shrink-0">
              <ImageIcon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
                Chọn ảnh minh họa cho thẻ
              </h3>
              <p className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1] truncate max-w-md">
                {card?.term ? `Thuật ngữ: ${card.term}` : 'Tạo ảnh AI, tìm ảnh miễn phí hoặc tải ảnh của bạn'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="p-2 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tabs */}
        <div className="px-6 pt-3 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-2 overflow-x-auto">
          {[
            { id: 'ai', label: 'Tạo ảnh AI', icon: Sparkles },
            { id: 'stock', label: 'Tìm ảnh miễn phí', icon: Search },
            { id: 'upload', label: 'Tải lên / Dán URL', icon: Upload },
          ].map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => {
                setActiveTab(id);
                setErrorMsg('');
              }}
              className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-bold border-b-2 whitespace-nowrap transition ${
                activeTab === id
                  ? 'border-[#4255FF] text-[#4255FF] dark:text-[#8E9BFF]'
                  : 'border-transparent text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white'
              }`}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {errorMsg && (
            <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-900/25 border border-rose-200 dark:border-rose-800/50 text-xs font-semibold text-rose-700 dark:text-rose-300 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* TAB 1: AI MNEMONIC IMAGE */}
          {activeTab === 'ai' && (
            <form onSubmit={handleGenerateAi} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#282E3E] dark:text-[#E2E8F0] mb-1">
                    Thuật ngữ / Chủ đề ảnh *
                  </label>
                  <input
                    type="text"
                    value={aiTerm}
                    onChange={(e) => setAiTerm(e.target.value)}
                    placeholder="VD: photosynthesis, mountain..."
                    className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#282E3E] dark:text-[#E2E8F0] mb-1">
                    Ngữ cảnh / Định nghĩa (tùy chọn)
                  </label>
                  <input
                    type="text"
                    value={aiDefinition}
                    onChange={(e) => setAiDefinition(e.target.value)}
                    placeholder="Giúp AI vẽ sát nghĩa hơn"
                    className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#D9DDE8] dark:border-[#2E3856] text-sm text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                  />
                </div>
              </div>

              <div>
                <p className="text-xs font-bold text-[#282E3E] dark:text-[#E2E8F0] mb-2">
                  Phong cách minh họa
                </p>
                <div className="flex flex-wrap gap-2">
                  {AI_STYLES.map((st) => (
                    <button
                      key={st.id}
                      type="button"
                      onClick={() => setAiStyle(st.id)}
                      className={`px-3.5 py-1.5 rounded-full text-xs font-bold border transition ${
                        aiStyle === st.id
                          ? 'bg-[#4255FF] text-white border-[#4255FF]'
                          : 'bg-[#F6F7FB] dark:bg-[#171836] text-[#586380] dark:text-[#A8B1D1] border-[#D9DDE8] dark:border-[#2E3856] hover:border-[#4255FF]'
                      }`}
                    >
                      {st.label}
                    </button>
                  ))}
                </div>
              </div>

              <button
                type="submit"
                disabled={generatingAi}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-50 text-white text-sm font-bold transition"
              >
                {generatingAi ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Đang tạo ảnh AI...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    Tạo ảnh minh họa AI
                  </>
                )}
              </button>

              {aiResult?.imageUrl && (
                <div className="mt-4 p-4 rounded-2xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] space-y-3">
                  <div className="relative flex items-center justify-center min-h-[220px] rounded-xl overflow-hidden bg-white dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856]">
                    {aiImgLoading && (
                      <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-white/80 dark:bg-[#202140]/80 text-xs font-semibold text-[#586380] dark:text-[#A8B1D1]">
                        <Loader2 className="w-6 h-6 animate-spin text-[#4255FF]" />
                        Đang tải ảnh từ {aiResult.provider || 'AI'}...
                      </div>
                    )}
                    <img
                      src={aiResult.imageUrl}
                      alt={aiTerm}
                      onLoad={() => setAiImgLoading(false)}
                      onError={() => {
                        setAiImgLoading(false);
                        setErrorMsg('Không tải được ảnh xem trước từ máy chủ AI. Bạn có thể thử lại hoặc dùng ảnh miễn phí.');
                      }}
                      className="max-h-64 object-contain"
                    />
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1]">
                      Nguồn: {aiResult.provider || 'Pollinations AI'}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        onSelectImage(aiResult.imageUrl);
                        onClose();
                      }}
                      className="inline-flex items-center gap-1.5 px-5 py-2 rounded-full bg-[#23B26D] hover:bg-[#1d9a5d] text-white text-sm font-bold transition"
                    >
                      <Check className="w-4 h-4" />
                      Chọn ảnh này cho thẻ
                    </button>
                  </div>
                </div>
              )}
            </form>
          )}

          {/* TAB 2: STOCK SEARCH */}
          {activeTab === 'stock' && (
            <div className="space-y-4">
              <form onSubmit={handleSearchStock} className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="w-4 h-4 text-[#586380] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    type="text"
                    value={stockQuery}
                    onChange={(e) => setStockQuery(e.target.value)}
                    placeholder="Nhập từ khóa tìm ảnh (tiếng Anh cho kết quả tốt nhất)..."
                    className="w-full h-10 pl-10 pr-4 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                  />
                </div>
                <button
                  type="submit"
                  disabled={searchingStock}
                  className="px-5 h-10 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-50 text-white text-sm font-bold transition shrink-0"
                >
                  {searchingStock ? 'Đang tìm...' : 'Tìm ảnh'}
                </button>
              </form>

              {stockProvider && stockItems.length > 0 && (
                <p className="text-xs font-semibold text-[#586380] dark:text-[#A8B1D1]">
                  Hiển thị {stockItems.length} kết quả từ <span className="capitalize">{stockProvider}</span> — nhấn vào ảnh để chọn:
                </p>
              )}

              {searchingStock ? (
                <div className="py-12 flex items-center justify-center gap-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
                  <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
                  Đang tìm kiếm ảnh minh họa...
                </div>
              ) : stockItems.length > 0 ? (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                  {stockItems.map((item) => (
                    <button
                      key={item.id || item.imageUrl}
                      type="button"
                      onClick={() => {
                        onSelectImage(item.imageUrl);
                        onClose();
                      }}
                      className="group relative aspect-square rounded-xl overflow-hidden border-2 border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] bg-[#F6F7FB] dark:bg-[#171836] transition"
                    >
                      <img
                        src={item.previewUrl || item.imageUrl}
                        alt={item.attribution || stockQuery}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                      />
                      <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 to-transparent p-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <p className="text-[10px] font-bold text-white truncate">
                          {item.attribution || 'Chọn ảnh'}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="rounded-xl border-2 border-dashed border-[#D9DDE8] dark:border-[#2E3856] py-10 text-center text-sm text-[#586380] dark:text-[#A8B1D1]">
                  Nhập từ khóa và nhấn <strong>Tìm ảnh</strong> để duyệt thư viện ảnh minh họa miễn phí.
                </div>
              )}
            </div>
          )}

          {/* TAB 3: UPLOAD / URL */}
          {activeTab === 'upload' && (
            <div className="space-y-5">
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  const file = e.dataTransfer?.files?.[0];
                  if (file) handleUploadFile(file);
                }}
                onClick={() => !uploading && fileInputRef.current?.click()}
                className="rounded-2xl border-2 border-dashed border-[#D9DDE8] dark:border-[#3D4870] hover:border-[#4255FF] bg-[#F6F7FB] dark:bg-[#171836] p-8 flex flex-col items-center justify-center text-center cursor-pointer transition"
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  className="hidden"
                  onChange={(e) => handleUploadFile(e.target.files?.[0])}
                />
                {uploading ? (
                  <div className="flex flex-col items-center gap-2 text-sm font-bold text-[#4255FF]">
                    <Loader2 className="w-7 h-7 animate-spin" />
                    Đang tải ảnh lên máy chủ...
                  </div>
                ) : (
                  <>
                    <Upload className="w-8 h-8 text-[#4255FF] mb-2" />
                    <p className="text-sm font-bold text-[#282E3E] dark:text-white">
                      Chọn hoặc kéo thả ảnh từ máy tính
                    </p>
                    <p className="text-xs text-[#586380] dark:text-[#A8B1D1] mt-1">
                      JPG, PNG, WEBP, GIF — tối đa 5MB
                    </p>
                  </>
                )}
              </div>

              <form onSubmit={handleApplyManualUrl} className="space-y-2">
                <label className="block text-xs font-bold text-[#282E3E] dark:text-[#E2E8F0]">
                  Hoặc dán trực tiếp URL ảnh
                </label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Link2 className="w-4 h-4 text-[#586380] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <input
                      type="url"
                      value={manualUrl}
                      onChange={(e) => setManualUrl(e.target.value)}
                      placeholder="https://example.com/image.png"
                      className="w-full h-10 pl-10 pr-4 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#D9DDE8] dark:border-[#2E3856] text-sm text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                    />
                  </div>
                  <button
                    type="submit"
                    className="px-5 h-10 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shrink-0"
                  >
                    Áp dụng
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

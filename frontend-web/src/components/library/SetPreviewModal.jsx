import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { X, Layers, ArrowRight, Loader2 } from 'lucide-react';
import flashcardApi from '../../api/flashcardApi';
import { MOCK_CARDS_BY_SET } from '../../data/libraryExploreMockData';

export default function SetPreviewModal({ set, onClose }) {
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!set) return;
    let active = true;

    const loadPreview = async () => {
      // Mock sets with ID >= 1000 fallback directly to mock cards if not in DB
      setLoading(true);
      try {
        const res = await flashcardApi.getSetById(set.id);
        const fetched = res?.data?.cards || [];
        if (active) {
          setCards(fetched.length > 0 ? fetched : MOCK_CARDS_BY_SET.default);
        }
      } catch {
        if (active) {
          setCards(MOCK_CARDS_BY_SET.default);
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    loadPreview();
    return () => {
      active = false;
    };
  }, [set]);

  if (!set) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-2xl max-h-[85vh] flex flex-col bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white truncate">
              {set.title}
            </h3>
            <p className="text-xs font-semibold text-[#586380] dark:text-[#A8B1D1] flex items-center gap-1.5 mt-0.5">
              <Layers className="w-3.5 h-3.5 text-[#4255FF]" />
              {set.cardCount ?? cards.length} thuật ngữ • Tác giả: {set.owner?.username || 'Ẩn danh'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Cards List */}
        <div className="p-6 overflow-y-auto space-y-2.5 flex-1">
          {loading ? (
            <div className="py-12 flex items-center justify-center gap-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
              <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
              Đang tải xem trước...
            </div>
          ) : (
            cards.slice(0, 12).map((card, idx) => (
              <div
                key={card.id || idx}
                className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856]"
              >
                <div className="text-sm font-bold text-[#282E3E] dark:text-white sm:border-r sm:border-[#D9DDE8] dark:sm:border-[#2E3856] sm:pr-3">
                  {card.term}
                </div>
                <div className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                  {card.definition}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3 bg-[#F6F7FB]/60 dark:bg-[#171836]">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-full text-sm font-bold text-[#586380] hover:bg-[#EDEFF4] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
          >
            Đóng
          </button>
          <Link
            to={`/sets/${set.id}`}
            onClick={onClose}
            className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
          >
            Học bộ thẻ này
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    </div>
  );
}

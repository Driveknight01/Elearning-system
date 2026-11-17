import React from 'react';
import { Link } from 'react-router-dom';
import {
  Bookmark,
  Star,
  Eye,
  Copy,
  Trash2,
  Edit3,
  Globe,
  Link2,
  Lock,
  Sparkles,
  BadgeCheck,
  Layers,
} from 'lucide-react';
import UserAvatar from '../common/UserAvatar';

const VISIBILITY_BADGE = {
  PUBLIC: {
    label: 'Công khai',
    icon: Globe,
    cls: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300',
  },
  LINK_ONLY: {
    label: 'Qua link',
    icon: Link2,
    cls: 'bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300',
  },
  PRIVATE: {
    label: 'Riêng tư',
    icon: Lock,
    cls: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300',
  },
};

export default function FlashcardSetCard({
  set,
  isSaved = false,
  saving = false,
  onToggleSave,
  onClone,
  onDelete,
  showVisibility = false,
  showEdit = false,
  extraMeta = null,
}) {
  if (!set) return null;

  const visMeta = VISIBILITY_BADGE[set.visibility] || VISIBILITY_BADGE.PUBLIC;
  const VisIcon = visMeta.icon;
  const ratingValue = set.avgRating ? Number(set.avgRating).toFixed(1) : null;

  return (
    <div className="group bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] dark:hover:border-[#6B7CFF] p-4 flex flex-col justify-between transition-all shadow-sm hover:shadow-md">
      <div>
        {/* Top Row: Badges + Save button */}
        <div className="flex items-start justify-between gap-2 mb-2.5">
          <div className="flex flex-wrap items-center gap-1.5 min-w-0">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#A8B1D1]">
              <Layers className="w-3 h-3" />
              {set.cardCount ?? 0} thuật ngữ
            </span>

            {set.featured && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300">
                <Sparkles className="w-3 h-3" />
                Nổi bật
              </span>
            )}

            {showVisibility && (
              <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold ${visMeta.cls}`}>
                <VisIcon className="w-3 h-3" />
                {visMeta.label}
              </span>
            )}
          </div>

          {onToggleSave && (
            <button
              type="button"
              disabled={saving}
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                onToggleSave(set);
              }}
              title={isSaved ? 'Bỏ lưu bộ thẻ' : 'Lưu bộ thẻ'}
              aria-label={isSaved ? 'Bỏ lưu bộ thẻ' : 'Lưu bộ thẻ'}
              className={`p-1.5 rounded-lg transition-colors shrink-0 ${
                isSaved
                  ? 'text-[#4255FF] bg-[#EDEFFF] dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                  : 'text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
              }`}
            >
              <Bookmark className={`w-4 h-4 ${isSaved ? 'fill-current' : ''}`} />
            </button>
          )}
        </div>

        {/* Title & Description */}
        <Link to={`/sets/${set.id}`} className="block group/title">
          <h3 className="text-[16px] font-bold text-[#282E3E] dark:text-white group-hover/title:text-[#4255FF] dark:group-hover/title:text-[#8E9BFF] transition-colors line-clamp-1">
            {set.title}
          </h3>
          {set.description && (
            <p className="mt-1 text-[13px] text-[#586380] dark:text-[#A8B1D1] line-clamp-2 leading-snug">
              {set.description}
            </p>
          )}
        </Link>

        {/* Category + Extra Meta */}
        <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
          {set.category?.name && (
            <span
              className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-[#F6F7FB] text-[#586380] dark:bg-[#171836] dark:text-[#A8B1D1] border border-[#EDEFF4] dark:border-[#2E3856]"
              style={
                set.category.colorHex
                  ? { borderLeftColor: set.category.colorHex, borderLeftWidth: '3px' }
                  : undefined
              }
            >
              {set.category.name}
            </span>
          )}
          {extraMeta}
        </div>
      </div>

      {/* Footer: Owner & Stats / Actions */}
      <div className="mt-4 pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-2">
        <Link
          to={set.owner?.username ? `/users/${set.owner.username}` : '#'}
          className="flex items-center gap-2 min-w-0 hover:opacity-85 transition"
        >
          <UserAvatar
            src={set.owner?.avatarUrl}
            fullName={set.owner?.fullName}
            username={set.owner?.username}
            size="xs"
          />
          <span className="text-[12px] font-semibold text-[#586380] dark:text-[#A8B1D1] truncate">
            {set.owner?.fullName || set.owner?.username || 'Ẩn danh'}
          </span>
          {set.owner?.verifiedTeacher && (
            <BadgeCheck className="w-3.5 h-3.5 text-[#4255FF] shrink-0" title="Giáo viên đã xác minh" />
          )}
        </Link>

        <div className="flex items-center gap-2 shrink-0">
          {!showEdit && !onDelete && !onClone && (
            <div className="flex items-center gap-2.5 text-[12px] font-semibold text-[#586380] dark:text-[#A8B1D1]">
              {set.ratingCount > 0 && ratingValue && (
                <span className="inline-flex items-center gap-0.5 text-amber-600 dark:text-amber-400">
                  <Star className="w-3.5 h-3.5 fill-current" />
                  {ratingValue}
                </span>
              )}
              <span className="inline-flex items-center gap-1" title="Lượt xem">
                <Eye className="w-3.5 h-3.5" />
                {set.viewCount ?? 0}
              </span>
            </div>
          )}

          {onClone && (
            <button
              type="button"
              onClick={() => onClone(set)}
              title="Nhân bản bộ thẻ"
              className="p-1.5 rounded-lg text-[#586380] hover:bg-[#F6F7FB] hover:text-[#4255FF] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
            >
              <Copy className="w-3.5 h-3.5" />
            </button>
          )}

          {showEdit && (
            <Link
              to={`/sets/${set.id}/edit`}
              title="Chỉnh sửa bộ thẻ"
              className="p-1.5 rounded-lg text-[#586380] hover:bg-[#F6F7FB] hover:text-[#4255FF] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
            >
              <Edit3 className="w-3.5 h-3.5" />
            </Link>
          )}

          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(set)}
              title="Xóa bộ thẻ"
              className="p-1.5 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600 dark:text-[#A8B1D1] dark:hover:bg-[#431F25] dark:hover:text-red-400 transition"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

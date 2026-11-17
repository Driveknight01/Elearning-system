import React from 'react';
import { Link } from 'react-router-dom';
import {
  Bookmark,
  Users,
  BadgeCheck,
  Image as ImageIcon,
} from 'lucide-react';
import UserAvatar from '../common/UserAvatar';

export default function FlashcardSetCard({
  set,
  isSaved = false,
  saving = false,
  onToggleSave,
  onPreview,
}) {
  if (!set) return null;

  const isTeacher = set.owner?.verifiedTeacher || set.owner?.role === 'TEACHER';

  return (
    <div className="relative overflow-hidden bg-white dark:bg-[#202140] rounded-2xl border-2 border-[#EDEFF4] dark:border-[#2E3856] px-5 py-4 flex flex-col justify-between min-h-[176px] transition-all shadow-[0_1px_4px_rgba(40,46,62,0.04)] after:content-[''] after:absolute after:inset-x-0 after:bottom-0 after:h-[4px] after:bg-[#A8B1FF] after:opacity-0 hover:after:opacity-100 after:transition-opacity">
      <div>
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0 flex-1">
            {set.recentLearnersCount > 0 && (
              <div className="mb-2">
                <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]">
                  <Users className="w-3 h-3" />
                  {set.recentLearnersCount} người học gần đây
                </span>
              </div>
            )}

            <Link to={`/sets/${set.id}`} className="inline-flex items-center gap-1.5 group/title max-w-full">
              <h3 className="text-[15px] font-bold text-[#282E3E] dark:text-white group-hover/title:text-[#4255FF] dark:group-hover/title:text-[#8E9BFF] transition-colors truncate">
                {set.title}
              </h3>
              {set.hasImages && (
                <ImageIcon className="w-3.5 h-3.5 text-[#586380] dark:text-[#A8B1D1] shrink-0" />
              )}
            </Link>
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
              title={isSaved ? 'Bỏ lưu học phần' : 'Lưu học phần'}
              aria-label={isSaved ? 'Bỏ lưu học phần' : 'Lưu học phần'}
              className={`p-1 rounded-lg transition-colors shrink-0 ${
                isSaved
                  ? 'text-[#4255FF] bg-[#EDEFFF] dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                  : 'text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
              }`}
            >
              <Bookmark className={`w-4 h-4 ${isSaved ? 'fill-current' : ''}`} />
            </button>
          )}
        </div>

        {/* Term count pill (Quizlet clean style) */}
        <div className="mt-2">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
            {set.cardCount ?? 0} thuật ngữ
          </span>
        </div>
      </div>

      {/* Bottom: Author + 2 Quizlet Action Buttons */}
      <div className="mt-4 space-y-2.5">
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
          <span className="text-[13px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] truncate">
            {set.owner?.username || set.owner?.fullName || 'Ẩn danh'}
          </span>
          {isTeacher && (
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#EDEFFF]/80 text-[#586380] dark:bg-[#2E3856] dark:text-[#A8B1D1] shrink-0">
              Giáo viên
            </span>
          )}
          {set.owner?.verifiedTeacher && (
            <BadgeCheck className="w-4 h-4 text-emerald-500 shrink-0" title="Giáo viên đã xác minh" />
          )}
        </Link>

        <div className="grid grid-cols-2 gap-2.5">
          <button
            type="button"
            onClick={() => onPreview?.(set)}
            className="h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-[#586380] dark:text-[#A8B1D1] text-[13px] font-bold transition"
          >
            Xem trước
          </button>
          <Link
            to={`/sets/${set.id}`}
            className="h-9 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-[13px] font-bold flex items-center justify-center transition"
          >
            Học
          </Link>
        </div>
      </div>
    </div>
  );
}

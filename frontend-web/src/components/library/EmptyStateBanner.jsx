import React from 'react';
import { Link } from 'react-router-dom';

function DefaultIllustration({ variant }) {
  if (variant === 'folder') {
    return (
      <svg viewBox="0 0 120 96" className="w-28 h-24 mx-auto" fill="none" aria-hidden="true">
        <rect x="20" y="18" width="64" height="48" rx="8" fill="#A5F3FC" />
        <rect x="28" y="12" width="56" height="44" rx="8" fill="#818CF8" />
        <path d="M16 32C16 27.5817 19.5817 24 24 24H46L53 31H96C100.418 31 104 34.5817 104 39V76C104 80.4183 100.418 84 96 84H24C19.5817 84 16 80.4183 16 76V32Z" fill="#C7D2FE" />
        <path d="M12 72C26 68 38 60 46 50" stroke="#282E3E" strokeWidth="2.5" strokeLinecap="round" />
        <path d="M39 49L47 49L46 57" stroke="#282E3E" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  if (variant === 'group') {
    return (
      <svg viewBox="0 0 120 96" className="w-28 h-24 mx-auto" fill="none" aria-hidden="true">
        <rect x="22" y="16" width="82" height="62" rx="10" fill="#D946EF" opacity="0.75" />
        <rect x="16" y="20" width="84" height="62" rx="10" fill="#F8FAFC" stroke="#E2E8F0" strokeWidth="2" />
        <circle cx="28" cy="34" r="5" fill="#4255FF" />
        <rect x="38" y="31" width="16" height="6" rx="3" fill="#CBD5E1" />
        <path d="M62 34L66 37L73 30" stroke="#10B981" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="28" cy="51" r="5" fill="#4255FF" />
        <rect x="38" y="48" width="12" height="6" rx="3" fill="#CBD5E1" />
        <path d="M62 51L66 54L73 47" stroke="#10B981" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="28" cy="68" r="5" fill="#4255FF" />
        <rect x="38" y="65" width="20" height="6" rx="3" fill="#CBD5E1" />
        <path d="M76 68L80 71L87 64" stroke="#10B981" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  if (variant === 'test') {
    return (
      <svg viewBox="0 0 120 96" className="w-28 h-24 mx-auto" fill="none" aria-hidden="true">
        <rect x="34" y="16" width="52" height="64" rx="10" fill="#FB923C" />
        <path d="M44 33L48 37L55 29" stroke="#FFFFFF" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
        <rect x="60" y="31" width="16" height="5" rx="2.5" fill="#FED7AA" />
        <path d="M44 49L48 53L55 45" stroke="#FFFFFF" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
        <rect x="60" y="47" width="16" height="5" rx="2.5" fill="#FED7AA" />
        <rect x="44" y="63" width="22" height="5" rx="2.5" fill="#FED7AA" />
        <path d="M84 54L88 64L98 68L88 72L84 82L80 72L70 68L80 64L84 54Z" fill="#4255FF" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 120 96" className="w-32 h-24 mx-auto" fill="none" aria-hidden="true">
      <rect x="20" y="22" width="80" height="54" rx="10" fill="#FFFFFF" stroke="#E2E8F0" strokeWidth="2" />
      <circle cx="46" cy="40" r="9" fill="#FDE68A" />
      <circle cx="60" cy="40" r="9" fill="#4255FF" />
      <circle cx="74" cy="40" r="9" fill="#BFDBFE" />
      <rect x="32" y="54" width="38" height="6" rx="3" fill="#FB923C" />
      <rect x="32" y="64" width="52" height="6" rx="3" fill="#34D399" />
      <rect x="14" y="52" width="22" height="22" rx="6" fill="#BAE6FD" />
      <path d="M19 66L24 61L27 64L32 58" stroke="#0284C7" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="76" y="10" width="34" height="22" rx="11" fill="#E0E7FF" />
    </svg>
  );
}

export default function EmptyStateBanner({
  variant = 'activity',
  imageSrc = null,
  title,
  description,
  actionLabel,
  onAction,
  actionTo,
  secondaryLabel,
  onSecondaryAction,
}) {
  return (
    <div className="py-12 px-4 flex flex-col items-center text-center max-w-md mx-auto">
      {imageSrc ? (
        <img
          src={imageSrc}
          alt={title}
          className="w-72 sm:w-80 h-48 object-contain mb-5"
        />
      ) : (
        <div className="mb-5">
          <DefaultIllustration variant={variant} />
        </div>
      )}

      <h3 className="text-[20px] font-extrabold text-[#586380] dark:text-white leading-snug">
        {title}
      </h3>

      {description && (
        <p className="mt-2 text-sm text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
          {description}
        </p>
      )}

      <div className="mt-5 flex flex-wrap items-center justify-center gap-2.5">
        {actionLabel && actionTo && (
          <Link
            to={actionTo}
            className="px-6 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shadow-sm"
          >
            {actionLabel}
          </Link>
        )}

        {actionLabel && onAction && !actionTo && (
          <button
            type="button"
            onClick={onAction}
            className="px-6 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shadow-sm"
          >
            {actionLabel}
          </button>
        )}

        {secondaryLabel && onSecondaryAction && (
          <button
            type="button"
            onClick={onSecondaryAction}
            className="px-5 py-2.5 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#282E3E] dark:text-white text-sm font-bold transition"
          >
            {secondaryLabel}
          </button>
        )}
      </div>
    </div>
  );
}

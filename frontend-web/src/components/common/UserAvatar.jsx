import React, { useState } from 'react';
import { getInitials } from '../../utils/userHelpers';

const SIZE_CLASSES = {
  xs: 'w-7 h-7 text-xs',
  sm: 'w-9 h-9 text-sm',
  md: 'w-11 h-11 text-base',
  lg: 'w-20 h-20 text-2xl',
  xl: 'w-24 h-24 sm:w-28 sm:h-28 text-3xl',
};

export default function UserAvatar({
  src,
  fullName,
  username,
  size = 'sm',
  className = '',
}) {
  const [imgError, setImgError] = useState(false);
  const sizeClass = SIZE_CLASSES[size] || SIZE_CLASSES.sm;
  const initials = getInitials(fullName, username);

  if (src && !imgError) {
    return (
      <img
        src={src}
        alt={fullName || username || 'Avatar'}
        onError={() => setImgError(true)}
        className={`${sizeClass} rounded-full object-cover border border-gray-200 bg-gray-100 shrink-0 ${className}`}
      />
    );
  }

  return (
    <div
      aria-label={fullName || username || 'Avatar'}
      className={`${sizeClass} rounded-full bg-gradient-to-br from-quizlet-blue to-quizlet-darkBlue text-white font-extrabold flex items-center justify-center shrink-0 select-none ${className}`}
    >
      {initials}
    </div>
  );
}

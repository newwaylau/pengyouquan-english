import React from 'react';

/** 三眼乌鸦 - 闭眼状态（密码隐藏） */
export function EyeClosed() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* 眼睛轮廓 */}
      <path d="M12 5C7 5 2.5 8.5 1 12C2.5 15.5 7 19 12 19C17 19 21.5 15.5 23 12C21.5 8.5 17 5 12 5Z"
        stroke="#666" strokeWidth="1.5" fill="none" />
      {/* 三只乌鸦眼睛符号 - 闭眼斜线 */}
      <line x1="5" y1="6" x2="19" y2="18" stroke="#666" strokeWidth="2" strokeLinecap="round" />
      {/* 第三只眼 - 额头上的乌鸦眼 */}
      <ellipse cx="12" cy="9" rx="2.5" ry="4" stroke="#666" strokeWidth="1" fill="none" />
      <line x1="10" y1="7" x2="14" y2="11" stroke="#666" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  );
}

/** 三眼乌鸦 - 睁眼状态（密码可见） */
export function EyeOpen() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* 眼睛轮廓 */}
      <path d="M12 5C7 5 2.5 8.5 1 12C2.5 15.5 7 19 12 19C17 19 21.5 15.5 23 12C21.5 8.5 17 5 12 5Z"
        stroke="#1a6b3c" strokeWidth="1.5" fill="none" />
      {/* 三眼乌鸦的第三只眼 (额头) */}
      <ellipse cx="12" cy="9" rx="2.5" ry="4" stroke="#1a6b3c" strokeWidth="1" fill="none" />
      <circle cx="12" cy="9" r="1.2" fill="#1a6b3c" />
      {/* 瞳孔 - 两个常规眼睛 */}
      <circle cx="8" cy="12.5" r="1.8" fill="#1a6b3c" />
      <circle cx="16" cy="12.5" r="1.8" fill="#1a6b3c" />
      {/* 瞳孔高光 */}
      <circle cx="7.2" cy="12" r="0.6" fill="white" />
      <circle cx="15.2" cy="12" r="0.6" fill="white" />
      <circle cx="11.5" cy="8.5" r="0.5" fill="white" />
      {/* 第三只眼的瞳孔 */}
    </svg>
  );
}

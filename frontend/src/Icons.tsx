import React from 'react';

type Props = {
  size?: number;
  className?: string;
  style?: React.CSSProperties;
};

function icon(render: (s: number) => React.ReactNode): React.FC<Props> {
  return ({ size = 20, className, style }) => (
    <span className={className} style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', verticalAlign: 'middle', width: size, height: size, ...style }}>
      <svg width={size} height={size} viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
        {render(size)}
      </svg>
    </span>
  );
}

export const IconTarget = icon(() => (
  <>
    <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="10" cy="10" r="5" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="10" cy="10" r="2" fill="currentColor" />
  </>
));

export const IconClose = icon(() => (
  <path d="M5 5l10 10M15 5l-10 10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
));

export const IconSearch = icon(() => (
  <>
    <circle cx="9" cy="9" r="5.5" stroke="currentColor" strokeWidth="1.5" />
    <path d="M13 13l4 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconBook = icon(() => (
  <>
    <path d="M3 16V5a2 2 0 012-2h4a3 3 0 013 3v10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M17 16V5a2 2 0 00-2-2h-4a3 3 0 00-3 3v10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M3 16h14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M10 6v10" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
  </>
));

export const IconSettings = icon(() => (
  <>
    <circle cx="10" cy="10" r="2.5" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 2.5v1M10 16.5v1M2.5 10h1M16.5 10h1M4.1 4.1l.7.7M15.2 15.2l.7.7M4.1 15.9l.7-.7M15.2 4.8l.7-.7"
      stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconSun = icon(() => (
  <>
    <circle cx="10" cy="10" r="3.5" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 1v2M10 17v2M1 10h2M17 10h2M3.3 3.3l1.4 1.4M15.3 15.3l1.4 1.4M3.3 16.7l1.4-1.4M15.3 4.7l1.4-1.4"
      stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconMoon = icon(() => (
  <path d="M17.39 10.74A7 7 0 019.26 2.61 7 7 0 1017.39 10.74z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
));

export const IconEdit = icon(() => (
  <>
    <path d="M3 17h14M3 17l1-4 9-9a2 2 0 112.8 2.8L7 16l-4 1z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconPen = icon(() => (
  <path d="M3 17h14M17 3l-4 4-2-2 4-4a1 1 0 012 2zM3 17l2-6 4 4-6 2z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
));

export const IconFilm = icon(() => (
  <>
    <rect x="2" y="3" width="16" height="14" rx="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M7 3v14M13 3v14M2 8h5M2 12h5M13 8h5M13 12h5" stroke="currentColor" strokeWidth="1.2" />
  </>
));

export const IconMic = icon(() => (
  <>
    <rect x="7" y="1" width="6" height="10" rx="3" stroke="currentColor" strokeWidth="1.5" />
    <path d="M4 10a6 6 0 1012 0" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M10 16v3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconEye = icon(() => (
  <>
    <path d="M1 10s3-7 9-7 9 7 9 7-3 7-9 7-9-7-9-7z" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="10" cy="10" r="3" stroke="currentColor" strokeWidth="1.5" />
  </>
));

export const IconEyeOff = icon(() => (
  <>
    <path d="M1 10s3-7 9-7 9 7 9 7-3 7-9 7-9-7-9-7z" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="10" cy="10" r="3" stroke="currentColor" strokeWidth="1.5" />
    <path d="M3 3l14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconFlag = icon(() => (
  <>
    <path d="M4 17V3h8l1 3h4v8H9l-1-3H4z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M4 17v2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconCheck = icon(() => (
  <path d="M5 10l3 4 7-8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
));

export const IconCheckCircle = icon(() => (
  <>
    <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
    <path d="M6 10l3 3 5-6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconMeditation = icon(() => (
  <>
    <circle cx="10" cy="4" r="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M6 10l2-3 3 2 3-2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M7 13l1 5 2-2 2 2 1-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconCalendar = icon(() => (
  <>
    <rect x="2" y="3" width="16" height="15" rx="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M2 7h16" stroke="currentColor" strokeWidth="1.5" />
    <path d="M6 1v4M14 1v4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M7 11l2 2 4-4" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconCalendarUpcoming = icon(() => (
  <>
    <rect x="2" y="3" width="16" height="15" rx="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M2 7h16" stroke="currentColor" strokeWidth="1.5" />
    <path d="M6 1v4M14 1v4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M13 13h1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconClock = icon(() => (
  <>
    <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 5v5l3 2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconForbidden = icon(() => (
  <>
    <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
    <path d="M5 15L15 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconBookClosed = icon(() => (
  <>
    <rect x="3" y="2" width="14" height="16" rx="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M10 2v16" stroke="currentColor" strokeWidth="1.2" />
    <path d="M7 6h2M7 9h2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
  </>
));

export const IconBarChart = icon(() => (
  <>
    <rect x="2" y="14" width="3" height="4" rx="0.5" fill="currentColor" opacity="0.7" />
    <rect x="8" y="10" width="3" height="8" rx="0.5" fill="currentColor" opacity="0.7" />
    <rect x="14" y="5" width="3" height="13" rx="0.5" fill="currentColor" opacity="0.7" />
  </>
));

export const IconLineChart = icon(() => (
  <>
    <path d="M1 17l5-7 4 3 5-8 4 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M1 17h18" stroke="currentColor" strokeWidth="1" />
  </>
));

export const IconUser = icon(() => (
  <>
    <circle cx="10" cy="7" r="4" stroke="currentColor" strokeWidth="1.5" />
    <path d="M2 19c0-4 3.6-7 8-7s8 3 8 7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconUsers = icon(() => (
  <>
    <circle cx="7" cy="6" r="3" stroke="currentColor" strokeWidth="1.5" />
    <path d="M1 17c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M12 4.5a3 3 0 010 5.5c2 .8 3 2.5 3 4.5h4c0-3.3-2.5-6-5.7-6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconNotification = icon(() => (
  <>
    <path d="M16 8c0-3.3-2.7-6-6-6S4 4.7 4 8v2l-1 3h14l-1-3V8z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M8 17a2 2 0 004 0" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconNext = icon(() => (
  <>
    <path d="M3 4v12l7-6-7-6z" fill="currentColor" />
    <path d="M12 4v12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </>
));

export const IconSkipNext = icon(() => (
  <>
    <path d="M4 4v12l7-6-7-6z" fill="currentColor" />
    <path d="M13 4v12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </>
));

export const IconRefresh = icon(() => (
  <path d="M14 6a6 6 0 10-5 9.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
));

export const IconRocket = icon(() => (
  <>
    <path d="M10 19c-.5-1-1.5-3-3-6-1-2-2-4-2-6a5 5 0 0110 0c0 2-1 4-2 6-1.5 3-2.5 5-3 6z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <circle cx="10" cy="7" r="1.5" fill="currentColor" />
  </>
));

export const IconKey = icon(() => (
  <>
    <circle cx="6" cy="14" r="4" stroke="currentColor" strokeWidth="1.5" />
    <path d="M9 11l6-6 2 2-5 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M13 7l2 2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconTV = icon(() => (
  <>
    <rect x="2" y="4" width="16" height="11" rx="2" stroke="currentColor" strokeWidth="1.5" />
    <path d="M7 15l-1 3h8l-1-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </>
));

export const IconSpeaker = icon(() => (
  <>
    <path d="M8 4v12l-4-4H2V8h2l4-4z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M12 5a5 5 0 010 10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M13 2a8 8 0 010 16" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconFire = icon(() => (
  <>
    <path d="M12 19c3 0 5-2.5 5-6 0-3.5-2-6-3-8-.5-1-1-2-1-3 0 1-1 3-2 4-1.5 2-3 4-3 7 0 3.5 2 6 4 6z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M9 16a3 3 0 004-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconTheaterMasks = icon(() => (
  <>
    <circle cx="7" cy="9" r="5" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="13" cy="9" r="5" stroke="currentColor" strokeWidth="1.5" />
    <path d="M4 11c.5 1 1.5 2 3 2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    <path d="M16 11c-.5 1-1.5 2-3 2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    <path d="M7 6l-1 2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    <path d="M13 6l1 2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
  </>
));

export const IconCelebration = icon(() => (
  <>
    <circle cx="10" cy="10" r="4" stroke="currentColor" strokeWidth="1.5" />
    <path d="M6 6c-1-1-2-2-2-3M14 6c1-1 2-2 2-3M6 14c-1 1-2 2-2 3M14 14c1 1 2 2 2 3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    <circle cx="4" cy="4" r="0.8" fill="currentColor" />
    <circle cx="16" cy="16" r="0.8" fill="currentColor" />
  </>
));

export const IconConstruction = icon(() => (
  <>
    <path d="M6 4h8l3 4-3 4H6L3 8l3-4z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M10 8v8M7 16h6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </>
));

export const IconPhone = icon(() => (
  <path d="M5 2h10a1 1 0 011 1v14a1 1 0 01-1 1H5a1 1 0 01-1-1V3a1 1 0 011-1zM8 1h4M9 16h2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
));

export const IconCheckPlain = icon(() => (
  <path d="M4 10l4 5 8-9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
));

export const IconSave = icon(() => (
  <>
    <path d="M14 3H4a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V6l-3-3z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    <path d="M5 9h10v7H5V9z" stroke="currentColor" strokeWidth="1" />
    <path d="M7 9V4h6v5" stroke="currentColor" strokeWidth="1.5" />
  </>
));

export const IconArrowLeft = icon(() => (
  <path d="M8 4l-6 6 6 6M16 10H3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
));

/** 20x20 SVG icon wrapper — create consistent inline SVG icons with currentColor */
export function createIcon(render: (s: number) => React.ReactNode): React.FC<Props> {
  return icon(render);
}

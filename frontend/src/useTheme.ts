import { useState, useEffect, useCallback } from 'react';

type Theme = 'dark' | 'light';

function getSystemTheme(): Theme {
  return 'dark';
}

function getStoredTheme(): Theme | null {
  const t = localStorage.getItem('theme');
  if (t === 'light' || t === 'dark') return t;
  return null;
}

function resolveTheme(): Theme {
  return getStoredTheme() || getSystemTheme();
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(resolveTheme);

  // Sync to DOM + localStorage
  const applyTheme = useCallback((t: Theme) => {
    document.documentElement.setAttribute('data-theme', t);
    setThemeState(t);
  }, []);

  // Toggle theme
  const toggleTheme = useCallback(() => {
    const next = theme === 'dark' ? 'light' : 'dark';
    localStorage.setItem('theme', next);
    applyTheme(next);
  }, [theme, applyTheme]);

  // Listen for system preference changes (only when no explicit stored preference)
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: light)');
    const handler = () => {
      if (!getStoredTheme()) {
        applyTheme(getSystemTheme());
      }
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [applyTheme]);

  // Restore theme on mount (in case script didn't run or was blocked)
  useEffect(() => {
    applyTheme(resolveTheme());
  }, [applyTheme]);

  return { theme, toggleTheme, isDark: theme === 'dark', isLight: theme === 'light' };
}

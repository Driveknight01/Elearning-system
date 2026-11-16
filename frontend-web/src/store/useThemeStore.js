import { create } from 'zustand';

const THEME_STORAGE_KEY = 'smartflash_theme';
let transitionTimer = null;

function applyThemeClass(theme, animate = false) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;

  if (animate) {
    if (transitionTimer) {
      clearTimeout(transitionTimer);
    }
    root.classList.add('theme-transitioning');
    // Force style recalculation so the transition starts cleanly from current computed colors
    void root.offsetHeight;
  }

  if (theme === 'dark') {
    root.classList.add('dark');
    root.style.colorScheme = 'dark';
  } else {
    root.classList.remove('dark');
    root.style.colorScheme = 'light';
  }

  if (animate) {
    transitionTimer = setTimeout(() => {
      root.classList.remove('theme-transitioning');
      transitionTimer = null;
    }, 240);
  }
}

function getInitialTheme() {
  if (typeof window === 'undefined') return 'light';
  try {
    const saved = window.localStorage.getItem(THEME_STORAGE_KEY);
    if (saved === 'dark' || saved === 'light') {
      applyThemeClass(saved, false);
      return saved;
    }
  } catch {
    // Ignore storage access errors
  }
  applyThemeClass('light', false);
  return 'light';
}

export const useThemeStore = create((set) => ({
  theme: getInitialTheme(),

  toggleTheme: () =>
    set((state) => {
      const nextTheme = state.theme === 'dark' ? 'light' : 'dark';
      applyThemeClass(nextTheme, true);
      try {
        window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
      } catch {
        // Ignore storage errors
      }
      return { theme: nextTheme };
    }),

  setTheme: (nextTheme) =>
    set((state) => {
      const normalized = nextTheme === 'dark' ? 'dark' : 'light';
      if (normalized === state.theme) return state;
      applyThemeClass(normalized, true);
      try {
        window.localStorage.setItem(THEME_STORAGE_KEY, normalized);
      } catch {
        // Ignore storage errors
      }
      return { theme: normalized };
    }),
}));

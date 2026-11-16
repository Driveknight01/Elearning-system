import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  user: null,
  accessToken: localStorage.getItem('accessToken') || null,
  isAuthenticated: !!localStorage.getItem('accessToken'),

  login: (userData, token) => {
    localStorage.setItem('accessToken', token);
    set({ user: userData, accessToken: token, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    set({ user: null, accessToken: null, isAuthenticated: false });
  },

  setUser: (userData) => set({ user: userData }),
}));

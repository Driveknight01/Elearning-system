import { create } from 'zustand';
import axiosClient from '../api/axiosClient';

const readStoredUser = () => {
  try {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const useAuthStore = create((set, get) => ({
  user: readStoredUser(),
  accessToken: localStorage.getItem('accessToken') || null,
  refreshToken: localStorage.getItem('refreshToken') || null,
  isAuthenticated: !!localStorage.getItem('accessToken'),

  setAuth: ({ accessToken, refreshToken, user }) => {
    if (accessToken) {
      localStorage.setItem('accessToken', accessToken);
    }
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    }
    set({
      accessToken: accessToken || get().accessToken,
      refreshToken: refreshToken || get().refreshToken,
      user: user || get().user,
      isAuthenticated: !!(accessToken || get().accessToken),
    });
  },

  login: (userData, accessToken, refreshToken = null) => {
    get().setAuth({ accessToken, refreshToken, user: userData });
  },

  setUser: (userData) => {
    if (userData) {
      localStorage.setItem('user', JSON.stringify(userData));
    } else {
      localStorage.removeItem('user');
    }
    set({ user: userData });
  },

  fetchCurrentUser: async () => {
    if (!get().accessToken) return null;
    try {
      const res = await axiosClient.get('/users/me');
      if (res?.data) {
        get().setUser(res.data);
        return res.data;
      }
    } catch {
      // Handled by axiosClient interceptor if 401
    }
    return null;
  },

  logout: async () => {
    const currentRefreshToken = get().refreshToken;
    try {
      if (get().accessToken) {
        await axiosClient.post('/auth/logout', currentRefreshToken ? { refreshToken: currentRefreshToken } : {});
      }
    } catch {
      // Ignore network/token errors during logout cleanup
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
      });
    }
  },

  getRole: () => {
    if (!get().isAuthenticated) return 'GUEST';
    return get().user?.role || 'USER';
  },

  hasRole: (allowedRoles = []) => {
    if (!allowedRoles || allowedRoles.length === 0) return true;
    const currentRole = get().getRole();
    return allowedRoles.includes(currentRole);
  },
}));

import axiosClient from './axiosClient';

export const authApi = {
  register: (data) => axiosClient.post('/auth/register', data),
  login: (data) => axiosClient.post('/auth/login', data),
  googleLogin: (data) => axiosClient.post('/auth/google', data),
  forgotPassword: (email) => axiosClient.post('/auth/forgot-password', { email }),
  resetPassword: (data) => axiosClient.post('/auth/reset-password', data),
  refresh: (data) => axiosClient.post('/auth/refresh', data),
  logout: (refreshToken) => axiosClient.post('/auth/logout', refreshToken ? { refreshToken } : {}),
  getMe: () => axiosClient.get('/users/me'),
};

export default authApi;

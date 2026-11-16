import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Request interceptor: tự gắn JWT Bearer token nếu có
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: chuẩn hóa payload hoặc bắt lỗi 401
axiosClient.interceptors.response.use(
  (response) => {
    // Trả về thẳng data payload từ wrapper ApiResponse<T>
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      // Có thể xử lý refresh token ở Phase 2
      localStorage.removeItem('accessToken');
    }
    return Promise.reject(error.response?.data || error);
  }
);

export default axiosClient;

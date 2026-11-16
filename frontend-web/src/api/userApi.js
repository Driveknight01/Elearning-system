import axiosClient from './axiosClient';

export const userApi = {
  getMyProfile: () => axiosClient.get('/users/me'),
  updateMyProfile: (data) => axiosClient.put('/users/me', data),
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post('/users/me/avatar', formData);
  },
  changePassword: (data) => axiosClient.put('/users/me/password', data),
  updatePrivacy: (data) => axiosClient.put('/users/me/privacy', data),
  getPublicProfile: (username) =>
    axiosClient.get(`/users/profile/${encodeURIComponent(username)}`),
};

export default userApi;

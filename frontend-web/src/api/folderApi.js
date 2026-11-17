import axiosClient from './axiosClient';

export const folderApi = {
  getMyFolders: ({ page = 0, size = 50 } = {}) =>
    axiosClient.get('/folders/my', { params: { page, size } }),

  getFolderById: (id, token) =>
    axiosClient.get(`/folders/${id}`, {
      params: token ? { token } : undefined,
    }),

  getFolderByShareToken: (shareToken) =>
    axiosClient.get(`/folders/share/${encodeURIComponent(shareToken)}`),

  createFolder: (data) => axiosClient.post('/folders', data),

  updateFolder: (id, data) => axiosClient.put(`/folders/${id}`, data),

  deleteFolder: (id) => axiosClient.delete(`/folders/${id}`),

  addSetToFolder: (folderId, setId, token) =>
    axiosClient.post(`/folders/${folderId}/sets/${setId}`, null, {
      params: token ? { token } : undefined,
    }),

  removeSetFromFolder: (folderId, setId) =>
    axiosClient.delete(`/folders/${folderId}/sets/${setId}`),
};

export default folderApi;

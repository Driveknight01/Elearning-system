import axiosClient from './axiosClient';

export const flashcardApi = {
  explore: () => axiosClient.get('/flashcard-sets/explore'),

  search: ({ keyword, categoryId, sort = 'newest', page = 0, size = 12 } = {}) => {
    const params = { sort, page, size };
    if (keyword && keyword.trim()) {
      params.keyword = keyword.trim();
    }
    if (categoryId !== undefined && categoryId !== null && categoryId !== '') {
      params.categoryId = categoryId;
    }
    return axiosClient.get('/flashcard-sets/search', { params });
  },

  getMySets: ({ page = 0, size = 12 } = {}) =>
    axiosClient.get('/flashcard-sets/my', { params: { page, size } }),

  getSavedSets: ({ page = 0, size = 20 } = {}) =>
    axiosClient.get('/flashcard-sets/saved', { params: { page, size } }),

  getRecentSets: ({ page = 0, size = 20 } = {}) =>
    axiosClient.get('/flashcard-sets/recents', { params: { page, size } }),

  getSetById: (id, token) =>
    axiosClient.get(`/flashcard-sets/${id}`, {
      params: token ? { token } : undefined,
    }),

  saveSet: (id, token) =>
    axiosClient.post(`/flashcard-sets/${id}/save`, null, {
      params: token ? { token } : undefined,
    }),

  unsaveSet: (id, token) =>
    axiosClient.delete(`/flashcard-sets/${id}/save`, {
      params: token ? { token } : undefined,
    }),

  deleteSet: (id) => axiosClient.delete(`/flashcard-sets/${id}`),

  cloneSet: (id, data = null, token) =>
    axiosClient.post(`/flashcard-sets/${id}/clone`, data || {}, {
      params: token ? { token } : undefined,
    }),
};

export default flashcardApi;

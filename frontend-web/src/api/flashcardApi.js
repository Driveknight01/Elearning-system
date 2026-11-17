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

  getSetByShareToken: (shareToken) =>
    axiosClient.get(`/flashcard-sets/share/${encodeURIComponent(shareToken)}`),

  createSet: (payload) => axiosClient.post('/flashcard-sets', payload),

  updateSet: (id, payload) => axiosClient.put(`/flashcard-sets/${id}`, payload),

  ocrExtract: (file, { sourceLang, targetLang } = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (sourceLang) formData.append('sourceLang', sourceLang);
    if (targetLang) formData.append('targetLang', targetLang);
    return axiosClient.post('/flashcard-sets/ocr-extract', formData);
  },

  saveSet: (id, token) =>
    axiosClient.post(`/flashcard-sets/${id}/save`, null, {
      params: token ? { token } : undefined,
    }),

  unsaveSet: (id, token) =>
    axiosClient.delete(`/flashcard-sets/${id}/save`, {
      params: token ? { token } : undefined,
    }),

  getInteraction: (id, token) =>
    axiosClient.get(`/flashcard-sets/${id}/interaction`, {
      params: token ? { token } : undefined,
    }),

  saveResumeState: (id, payload, token) =>
    axiosClient.put(`/flashcard-sets/${id}/resume`, payload, {
      params: token ? { token } : undefined,
    }),

  rateSet: (id, payload, token) =>
    axiosClient.post(`/flashcard-sets/${id}/rate`, payload, {
      params: token ? { token } : undefined,
    }),

  getSetRatings: (id, { page = 0, size = 10, token } = {}) => {
    const params = { page, size };
    if (token) params.token = token;
    return axiosClient.get(`/flashcard-sets/${id}/ratings`, { params });
  },

  starCard: (id, cardId, token) =>
    axiosClient.post(`/flashcard-sets/${id}/cards/${cardId}/star`, null, {
      params: token ? { token } : undefined,
    }),

  unstarCard: (id, cardId) =>
    axiosClient.delete(`/flashcard-sets/${id}/cards/${cardId}/star`),

  getStarredCards: (id, token) =>
    axiosClient.get(`/flashcard-sets/${id}/starred-cards`, {
      params: token ? { token } : undefined,
    }),

  deleteSet: (id) => axiosClient.delete(`/flashcard-sets/${id}`),

  cloneSet: (id, data = null, token) =>
    axiosClient.post(`/flashcard-sets/${id}/clone`, data || {}, {
      params: token ? { token } : undefined,
    }),
};

export default flashcardApi;

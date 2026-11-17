import axiosClient from './axiosClient';

export const mediaApi = {
  uploadCardImage: (file, cardId) => {
    const formData = new FormData();
    formData.append('file', file);
    if (cardId !== undefined && cardId !== null) {
      formData.append('cardId', cardId);
    }
    return axiosClient.post('/media/upload', formData);
  },

  searchStockImages: (query, perPage = 12) =>
    axiosClient.get('/media/stock-search', {
      params: { query: query?.trim() || '', perPage },
    }),

  generateAiImage: ({ term, definition, style = 'illustration', cardId } = {}) =>
    axiosClient.post('/media/ai-generate', {
      term: term?.trim() || '',
      definition: definition?.trim() || undefined,
      style,
      ...(cardId !== undefined && cardId !== null ? { cardId } : {}),
    }),
};

export default mediaApi;

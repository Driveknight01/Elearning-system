import axiosClient from './axiosClient';

export const studyGroupApi = {
  getMyGroups: () => axiosClient.get('/study-groups/my'),

  createGroup: (data) => axiosClient.post('/study-groups', data),

  joinGroup: (inviteCode) =>
    axiosClient.post('/study-groups/join', { inviteCode }),

  getGroupDetail: (id) => axiosClient.get(`/study-groups/${id}`),

  deleteGroup: (id) => axiosClient.delete(`/study-groups/${id}`),

  removeOrLeaveMember: (groupId, userId) =>
    axiosClient.delete(`/study-groups/${groupId}/members/${userId}`),

  shareSetToGroup: (groupId, setId, token) =>
    axiosClient.post(`/study-groups/${groupId}/sets/${setId}`, null, {
      params: token ? { token } : undefined,
    }),

  removeSetFromGroup: (groupId, setId) =>
    axiosClient.delete(`/study-groups/${groupId}/sets/${setId}`),
};

export default studyGroupApi;

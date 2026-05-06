import api from './index.js';

export async function fetchComments(songId, params = {}) {
  const res = await api.get(`/songs/${songId}/comments`, { params });
  return res.data.data; // PageResponse<CommentResponse>
}

export async function createComment(songId, body) {
  const res = await api.post(`/songs/${songId}/comments`, body);
  return res.data.data;
}

export async function deleteComment(commentId) {
  await api.delete(`/comments/${commentId}`);
}

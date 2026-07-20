import request from './request'

// AI 运行智能体（会话管理 + 流式问答）
export const agentApi = {
  sessions: (page = 1, size = 20, keyword = '', createdBy = '') =>
    request.get('/agent/sessions', { params: { page, size, keyword, createdBy } }),
  createSession: (data = {}) =>
    request.post('/agent/sessions', { title: '新对话', mode: data.mode || null, unit_id: data.unit_id || null, created_by: data.created_by || 'system' }),
  messages: (id) => request.get(`/agent/sessions/${id}/messages`),
  rename: (id, title) => request.put(`/agent/sessions/${id}/title`, { title }),
  remove: (id) => request.delete(`/agent/sessions/${id}`),
  // SSE 流式问答：返回 fetch Response（调用方自行读取流）
  chatStream: (body) => fetch('/api/agent/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export default agentApi

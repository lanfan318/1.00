// ========================================
// 统一 API 客户端 —— 匹配 Spring Boot 后端（R 包装自动拆包）
// ========================================
import request from './request'

// ======= AI 智能体 =======
export const agentApi = {
  listSessions: (page = 1, size = 20) => request.get('/agent/sessions', { params: { page, size } }),
  getMessages: (sessionId) => request.get(`/agent/sessions/${sessionId}/messages`),
  // 入参: { title?, mode?, unit_id?, created_by? }
  createSession: (body = {}) => request.post('/agent/sessions', {
    title: body.title || '新对话',
    mode: body.mode ?? null,
    unit_id: body.unit_id ?? null,
    created_by: body.created_by || 'system'
  }),
  renameSession: (id, title) => request.put(`/agent/sessions/${id}/title`, { title }),
  removeSession: (id) => request.delete(`/agent/sessions/${id}`),
  // SSE 流式问答（用 fetch，不走 axios 拦截器）
  chatStream: (body) => fetch('/api/agent/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

// ======= 报警实时 =======
export const alarmApi = {
  getRealtime: (query) => request.get('/alarm/realtime', { params: query }),
  confirm: (id, operator) => request.post(`/alarm/realtime/${id}/confirm?operator=${operator}`),
  suppress: (id, reason, operator = 'system') => request.post(`/alarm/realtime/${id}/suppress`, { reason, operator }),
  levelGroups: () => request.get('/alarm/level/groups')
}

// ======= 报警配置 =======
export const alarmConfigApi = {
  page: (query) => request.get('/alarm/config', { params: query }),
  getById: (id) => request.get(`/alarm/config/${id}`),
  save: (data) => request.post('/alarm/config', data),
  update: (id, data) => request.put(`/alarm/config/${id}`, data),
  delete: (id) => request.delete(`/alarm/config/${id}`),
  updateStatus: (id, enabled) => request.put(`/alarm/config/${id}/status?enabled=${enabled}`)
}

// ======= 报警统计 =======
export const statsApi = {
  overview: (range = 'month') => request.get('/alarm/stats/overview', { params: { range } }),
  specialty: (range = 'month') => request.get('/alarm/stats/specialty', { params: { range } }),
  level: (range = 'month') => request.get('/alarm/stats/level', { params: { range } }),
  type: (range = 'month') => request.get('/alarm/stats/type', { params: { range } }),
  trend: (range = 'month') => request.get('/alarm/stats/trend', { params: { range } }),
  rank: (range = 'month') => request.get('/alarm/stats/rank', { params: { range } })
}

// ======= 设备管理 =======
export const equipmentApi = {
  getUnits: () => request.get('/equipment/units'),
  getSpecialties: () => request.get('/equipment/specialties'),
  getSystems: () => request.get('/equipment/systems'),
  getMeasurePoints: () => request.get('/equipment/measure-points')
}

// ======= 工况曲线 =======
export const curveApi = {
  getCurve: (unit, device, params) => request.get(`/equipment/${unit}/${device}/curve`, { params })
}

// ======= 操作指导 =======
export const guideApi = {
  getGuide: (alarmId) => request.get(`/alarm/${alarmId}/operation-guide`),
  addComment: (alarmId, comment, operator = 'system') => request.post(`/alarm/${alarmId}/operation-guide/comment?comment=${comment}&operator=${operator}`)
}

// ======= 历史导入 =======
export const historyApi = {
  importFile: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/history/import', fd)
  },
  listTasks: (page = 1, size = 20) => request.get('/history/tasks', { params: { page, size } })
}

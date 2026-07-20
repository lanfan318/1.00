import request from './request'

// 实时报警 / 分级分组
export const alarmApi = {
  // 分页查询实时报警
  page: (params = {}) =>
    request.get('/alarm/realtime', { params: { page: 1, size: 500, ...params } }),
  // 确认报警
  confirm: (id, operator = 'admin') =>
    request.post(`/alarm/realtime/${id}/confirm?operator=${encodeURIComponent(operator)}`),
  // 抑制报警
  suppress: (id, reason = '', operator = 'admin') =>
    request.post(`/alarm/realtime/${id}/suppress`, { reason, operator }),
  // 报警分级分组（一级/二级/智能预警）
  levelGroups: () => request.get('/alarm/level/groups')
}

export default alarmApi

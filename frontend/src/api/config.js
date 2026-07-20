import request from './request'

// 报警配置（规则管理）
export const configApi = {
  list: (params = {}) => request.get('/alarm/config', { params }),
  get: (id) => request.get(`/alarm/config/${id}`),
  save: (data) => request.post('/alarm/config', data),
  update: (id, data) => request.put(`/alarm/config/${id}`, data),
  remove: (id) => request.delete(`/alarm/config/${id}`),
  batchDelete: (ids) => request.post('/alarm/config/batch-delete', ids),
  enable: (id) => request.post(`/alarm/config/${id}/enable`),
  disable: (id) => request.post(`/alarm/config/${id}/disable`),
  // 文件导出/导入为独立地址（非 JSON 体）
  exportUrl: () => '/api/alarm/config/export',
  importUrl: () => '/api/alarm/config/import'
}

export default configApi

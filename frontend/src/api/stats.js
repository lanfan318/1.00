import request from './request'

// 报警统计报表
export const statsApi = {
  overview: (range = 'month') => request.get('/alarm/stats/overview', { params: { range } }),
  specialty: (range = 'month') => request.get('/alarm/stats/specialty', { params: { range } }),
  level: (range = 'month') => request.get('/alarm/stats/level', { params: { range } }),
  type: (range = 'month') => request.get('/alarm/stats/type', { params: { range } }),
  topFrequency: (range = 'month') => request.get('/alarm/stats/top-frequency', { params: { range } }),
  topDuration: (range = 'month') => request.get('/alarm/stats/top-duration', { params: { range } }),
  dailyTrend: (range = 'month') => request.get('/alarm/stats/daily-trend', { params: { range } })
}

export default statsApi

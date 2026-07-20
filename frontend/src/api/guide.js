import request from './request'

// 操作指导 / 运行意见（KG 转发落库）
export const guideApi = {
  get: (alarmId) => request.get(`/alarm/${alarmId}/operation-guide`),
  comment: (alarmId, comment, operator = 'admin') =>
    request.post(
      `/alarm/${alarmId}/operation-guide/comment?comment=${encodeURIComponent(comment)}&operator=${encodeURIComponent(operator)}`
    )
}

export default guideApi

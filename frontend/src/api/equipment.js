import request from './request'

// 设备维度 / 工况曲线 / 健康评分
export const equipmentApi = {
  units: () => request.get('/equipment/units'),
  specialties: () => request.get('/equipment/specialties'),
  systems: () => request.get('/equipment/systems'),
  measurePoints: () => request.get('/equipment/measure-points'),
  // 工况曲线：unit 为后端机组 id(Long)，device 为设备编码
  curve: (unit, device, type = 'sb', range = '24h') =>
    request.get(`/equipment/${unit}/${device}/curve`, { params: { type, range } }),
  health: (unit, device) => request.get(`/equipment/${unit}/${device}/health`)
}

export default equipmentApi

// 工业多变量时序建模算法服务（FastAPI）前端客户端
// 该服务是独立引擎，返回原始 JSON（非后端 R<T> 包装），故使用独立 axios 实例，不经过 request.js 拦截器。
import axios from 'axios'

const base = import.meta.env.VITE_ALGO_BASE || '/algo/api'
const client = axios.create({ baseURL: base, timeout: 120000 })

export const algoApi = {
  // 上传 CSV，返回 { file_id, file_name, rows, columns, time_start, time_end, ... }
  uploadCsv(file) {
    const fd = new FormData()
    fd.append('file', file)
    return client.post('/data/upload', fd).then(r => r.data)
  },
  // 数据概览
  profile(fileId) {
    return client.get(`/data/${fileId}/profile`).then(r => r.data)
  },
  // 数据清洗，返回 { clean_id, report }
  clean(fileId, payload = {}) {
    return client.post(`/data/${fileId}/clean`, payload).then(r => r.data)
  },
  // ARX 建模 + 闭环寻优，cleanId 走 query（与后端约定一致）
  arxModeling(cleanId, payload) {
    return client.post(`/modeling/arx?clean_id=${encodeURIComponent(cleanId)}`, payload).then(r => r.data)
  },
  // 残差预警检测
  residualCheck(cleanId, payload) {
    return client.post(`/alarm/residual-check?clean_id=${encodeURIComponent(cleanId)}`, payload).then(r => r.data)
  },
  // 生成仿真数据（无需上传）
  generateSimulation(payload) {
    return client.post('/simulation/generate', payload).then(r => r.data)
  },
  health() {
    return client.get('/health').then(r => r.data)
  }
}

export default algoApi

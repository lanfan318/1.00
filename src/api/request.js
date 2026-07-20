import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 响应拦截器
request.interceptors.response.use(
  (res) => {
    const data = res.data
    // 后端统一返回 { code: 0, msg: 'ok', data: ... }
    if (data.code !== undefined && data.code !== 0) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg))
    }
    // 自动解包 data 字段
    return data.data !== undefined ? data.data : data
  },
  (err) => {
    ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request

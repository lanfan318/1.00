import request from './request'

// 历史数据导入
export const historyApi = {
  tasks: () => request.get('/history/import/tasks'),
  // 文件上传使用 FormData，单独实现
  importFile: (file, taskName = '历史数据导入') => {
    const form = new FormData()
    form.append('file', file)
    form.append('taskName', taskName)
    return request.post('/history/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default historyApi

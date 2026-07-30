/** 报警状态文字转换 */
export const alarmStatusText = (st) =>
  st === 'unhandled' ? '未处理'
    : st === 'confirmed' ? '已确认'
    : st === 'suppressed' ? '已抑制'
    : st === 'resolved' ? '已处理'
    : '-'

/** 报警等级文字 */
export const alarmLevelText = (l) =>
  l === 1 ? '一级' : l === 2 ? '二级' : '智能预警'

/** 报警等级颜色 */
export const alarmLevelColor = (l) =>
  l === 1 ? '#ef4444' : l === 2 ? '#f59e0b' : '#06b6d4'

/** 健康度状态标签 */
export const healthLabel = (h) => {
  if (h >= 95) return { text: '运行良好', color: '#22c55e' }
  if (h >= 85) return { text: '需关注', color: '#f59e0b' }
  if (h >= 75) return { text: '需检修', color: '#f59e0b' }
  return { text: '已超限', color: '#ef4444' }
}

/** 健康度文字颜色 */
export const healthColor = (h) =>
  h >= 90 ? '#22c55e' : h >= 80 ? '#f59e0b' : '#ef4444'

/** element status converted to tag type */
export const alarmStatusTagType = (st) =>
  st === 'unhandled' ? 'danger'
    : st === 'confirmed' ? 'warning'
    : st === 'suppressed' ? 'info'
    : 'success'

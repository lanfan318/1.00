/** 时间格式化：完整日期 + 时间 */
export const fmtDateTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { hour12: false })
}

/** 时间格式化：仅时间（HH:mm:ss） */
export const fmtTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleTimeString('zh-CN', { hour12: false })
}

/** 机组名称 */
export const unitName = (units, id) => units.find(u => u.id === id)?.name || id || '-'

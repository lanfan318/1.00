import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 中央数据 store - 所有页面共享一个数据源
// 包含：units 机组 / devices 设备 / alarms 报警 / 关联关系
export const useDataStore = defineStore('data', () => {
  // ============ 机组 ============
  const units = ref([
    { id: 'U1', name: '#1 机组', type: '火电机组', capacity: 600,
      base: { load: 495, press: 16.72, temp: 541, coal: 298, nox: 42, so2: 18, dust: 5.5 } }
  ])

  const addUnit = (u) => {
    const id = 'U' + (units.value.length + 1)
    units.value.push({
      id, name: u.name, type: u.type, capacity: u.capacity,
      base: { load: 480 + Math.random() * 30, press: 16.5 + Math.random() * 0.4, temp: 538 + Math.random() * 8, coal: 296 + Math.random() * 8, nox: 38 + Math.random() * 15, so2: 16 + Math.random() * 5, dust: 4 + Math.random() * 3 }
    })
  }
  const delUnit = (id) => {
    units.value = units.value.filter(u => u.id !== id)
    devices.value = devices.value.filter(d => d.unit !== id)
  }

  // ============ 设备 ============
  const devices = ref([
    // U1 锅炉
    { id: 'U1-blower-a1', unit: 'U1', dept: '锅炉', name: 'A送风机', model: 'YF-2024A', health: 97.2,
      params: { 电流: [120, 150, 'A'], 温度: [45, 75, '℃'], 振动: [1.2, 5, 'mm/s'], 转速: [2980, 3200, 'rpm'] } },
    { id: 'U1-blower-b1', unit: 'U1', dept: '锅炉', name: 'B送风机', model: 'YF-2024A', health: 95.8,
      params: { 电流: [118, 150, 'A'], 温度: [43, 75, '℃'], 振动: [1.1, 5, 'mm/s'], 转速: [2975, 3200, 'rpm'] } },
    { id: 'U1-idf-a', unit: 'U1', dept: '锅炉', name: 'A引风机', model: 'YF-2024C', health: 82.3,
      params: { 电流: [142, 150, 'A'], 温度: [82.3, 75, '℃'], 振动: [3.8, 5, 'mm/s'], 转速: [1450, 1600, 'rpm'] } },
    { id: 'U1-idf-b', unit: 'U1', dept: '锅炉', name: 'B引风机', model: 'YF-2024C', health: 96.1,
      params: { 电流: [125, 150, 'A'], 温度: [52, 75, '℃'], 振动: [1.5, 5, 'mm/s'], 转速: [1445, 1600, 'rpm'] } },
    { id: 'U1-paf-a', unit: 'U1', dept: '锅炉', name: 'A一次风机', model: 'YF-2024D', health: 91.4,
      params: { 电流: [88, 110, 'A'], 温度: [48, 75, '℃'], 振动: [2.1, 5, 'mm/s'], 转速: [1480, 1550, 'rpm'] } },
    { id: 'U1-paf-b', unit: 'U1', dept: '锅炉', name: 'B一次风机', model: 'YF-2024D', health: 88.9,
      params: { 电流: [92, 110, 'A'], 温度: [51, 75, '℃'], 振动: [2.4, 5, 'mm/s'], 转速: [1475, 1550, 'rpm'] } },
    { id: 'U1-mill-a', unit: 'U1', dept: '锅炉', name: 'A磨煤机', model: 'MM-2024B', health: 85.0,
      params: { 电流: [95, 130, 'A'], 温度: [68, 80, '℃'], 振动: [4.7, 6, 'mm/s'], 转速: [24, 28, 'rpm'] } },
    { id: 'U1-mill-b', unit: 'U1', dept: '锅炉', name: 'B磨煤机', model: 'MM-2024B', health: 93.2,
      params: { 电流: [88, 130, 'A'], 温度: [55, 80, '℃'], 振动: [2.8, 6, 'mm/s'], 转速: [25, 28, 'rpm'] } },
    { id: 'U1-mill-c', unit: 'U1', dept: '锅炉', name: 'C磨煤机', model: 'MM-2024B', health: 94.7,
      params: { 电流: [85, 130, 'A'], 温度: [53, 80, '℃'], 振动: [2.5, 6, 'mm/s'], 转速: [25, 28, 'rpm'] } },
    { id: 'U1-mill-d', unit: 'U1', dept: '锅炉', name: 'D磨煤机', model: 'MM-2024B', health: 78.5,
      params: { 电流: [108, 130, 'A'], 温度: [73, 80, '℃'], 振动: [5.2, 6, 'mm/s'], 转速: [25, 28, 'rpm'] } },
    // U1 汽轮机
    { id: 'U1-pump-a', unit: 'U1', dept: '汽轮机', name: '给水泵A', model: 'ZB-2024', health: 98.4,
      params: { 电流: [180, 220, 'A'], 温度: [42, 70, '℃'], 振动: [0.8, 3, 'mm/s'], 转速: [2980, 3100, 'rpm'] } },
    { id: 'U1-pump-b', unit: 'U1', dept: '汽轮机', name: '给水泵B', model: 'ZB-2024', health: 97.7,
      params: { 电流: [178, 220, 'A'], 温度: [41, 70, '℃'], 振动: [0.7, 3, 'mm/s'], 转速: [2975, 3100, 'rpm'] } },
    { id: 'U1-oil', unit: 'U1', dept: '汽轮机', name: '润滑油系统', model: 'LY-2024', health: 94.2,
      params: { 油压: [0.25, 0.40, 'MPa'], 油温: [45, 70, '℃'], 流量: [850, 1200, 'L/min'] } },
    { id: 'U1-cond', unit: 'U1', dept: '汽轮机', name: '凝汽器', model: 'NQ-2024', health: 96.3,
      params: { 真空: [-95, -98, 'kPa'], 水位: [1.2, 2.0, 'm'], 温度: [32, 45, '℃'] } },
    { id: 'U1-hp', unit: 'U1', dept: '汽轮机', name: '高压加热器', model: 'GJ-2024', health: 89.5,
      params: { 端差: [4.8, 8, '℃'], 水位: [1.5, 2.0, 'm'] } },
    { id: 'U1-deox', unit: 'U1', dept: '汽轮机', name: '除氧器', model: 'CY-2024', health: 95.6,
      params: { 水位: [1.8, 2.5, 'm'], 温度: [104, 110, '℃'] } }
  ])

  const addDevice = (d) => {
    const id = d.unit + '-' + Date.now().toString(36)
    devices.value.push({
      id, unit: d.unit, dept: d.dept, name: d.name, model: d.model || '未知型号',
      health: 95,
      params: { 电流: [100, 150, 'A'], 温度: [45, 75, '℃'], 振动: [1.5, 5, 'mm/s'], 转速: [1500, 1600, 'rpm'] }
    })
  }
  const delDevice = (id) => {
    devices.value = devices.value.filter(d => d.id !== id)
  }
  const updateDeviceParam = (id, paramKey, newVal) => {
    const d = devices.value.find(x => x.id === id)
    if (!d || !d.params[paramKey]) return
    d.params[paramKey][0] = newVal
    const ok = newVal < d.params[paramKey][1]
    d.health = Math.min(100, Math.max(20, d.health * 0.4 + 60 * (ok ? 0.95 : 0.5)))
  }

  // ============ 报警 ============
  // 从 localStorage 读取，刷新页面也保留
  const stored = JSON.parse(localStorage.getItem('alarms') || 'null')
  const alarms = ref(stored || [
    { id: 'a1', l: 1, time: new Date(Date.now() - 16 * 60000), unit: 'U1', device: 'A引风机', dept: '锅炉', desc: 'A引风机轴承温度异常升高', type: '智能预警', point: 'A引风机轴承温度', val: '82.3℃', st: 'unhandled' },
    { id: 'a2', l: 1, time: new Date(Date.now() - 24 * 60000), unit: 'U1', device: '润滑油系统', dept: '汽机', desc: '润滑油压力低于安全阈值', type: '阈值报警', point: '润滑油压力', val: '0.12MPa', st: 'unhandled' },
    { id: 'a3', l: 2, time: new Date(Date.now() - 42 * 60000), unit: 'U1', device: '锅炉', dept: '锅炉', desc: '锅炉主汽温度偏高', type: '智能预警', point: '主汽温度', val: '552.8℃', st: 'confirmed' },
    { id: 'a4', l: 2, time: new Date(Date.now() - 58 * 60000), unit: 'U1', device: 'A磨煤机', dept: '锅炉', desc: 'A磨煤机振动幅值超标', type: '阈值报警', point: 'A磨煤机振动', val: '4.7mm/s', st: 'unhandled' },
    { id: 'a5', l: 2, time: new Date(Date.now() - 73 * 60000), unit: 'U1', device: '汽轮机', dept: '汽机', desc: '汽轮机轴向位移接近预警', type: '智能预警', point: '轴向位移', val: '0.68mm', st: 'resolved' },
    { id: 'a6', l: 2, time: new Date(Date.now() - 95 * 60000), unit: 'U1', device: 'D磨煤机', dept: '锅炉', desc: 'D磨煤机电流波动', type: '阈值报警', point: 'D磨煤机电流', val: '142A', st: 'resolved' },
    { id: 'a7', l: 2, time: new Date(Date.now() - 112 * 60000), unit: 'U1', device: '除氧器', dept: '辅网', desc: '除氧器水位偏低', type: '阈值报警', point: '除氧器水位', val: '0.82m', st: 'confirmed' },
    { id: 'a8', l: 3, time: new Date(Date.now() - 125 * 60000), unit: 'U1', device: '给水泵B', dept: '汽机', desc: '给水泵B效率下降预警', type: '智能预警', point: '给水泵B效率', val: '82.1%', st: 'resolved' },
    { id: 'a9', l: 3, time: new Date(Date.now() - 143 * 60000), unit: 'U1', device: '锅炉', dept: '锅炉', desc: 'NOx排放浓度上升', type: '智能预警', point: 'NOx浓度', val: '68.3mg/m³', st: 'resolved' },
    { id: 'a10', l: 3, time: new Date(Date.now() - 161 * 60000), unit: 'U1', device: '辅网', dept: '辅网', desc: '冷却塔入口水温偏高', type: '阈值报警', point: '冷却塔入口水温', val: '33.5℃', st: 'resolved' },
    { id: 'a11', l: 3, time: new Date(Date.now() - 178 * 60000), unit: 'U1', device: 'B一次风机', dept: '锅炉', desc: 'B一次风机效率偏低', type: '智能预警', point: 'B一次风机效率', val: '78.5%', st: 'resolved' },
    { id: 'a12', l: 3, time: new Date(Date.now() - 195 * 60000), unit: 'U1', device: '高压加热器', dept: '汽机', desc: '高压加热器端差增大', type: '智能预警', point: '高加上端差', val: '4.8℃', st: 'resolved' }
  ])

  const saveAlarms = () => {
    localStorage.setItem('alarms', JSON.stringify(alarms.value))
  }
  // 改变报警状态并持久化
  const setAlarmStatus = (id, st) => {
    const a = alarms.value.find(x => x.id === id)
    if (a) { a.st = st; saveAlarms() }
  }
  // 模拟设备故障触发新报警
  const triggerAlarm = (deviceId, level, desc, point, val) => {
    const d = devices.value.find(x => x.id === deviceId)
    if (!d) return
    const a = {
      id: 'a' + Date.now(),
      l: level, time: new Date(), unit: d.unit, device: d.name, dept: d.dept,
      desc, type: '智能预警', point, val, st: 'unhandled'
    }
    alarms.value.unshift(a)
    saveAlarms()
  }

  // ============ 报警规则配置 ============
  const alarmRules = ref([
    { id: 'r1', unit: 'U1', device: 'A引风机', point: '轴承温度', level: 1, cond: '>', val: 80, channels: ['站内', '短信', '钉钉'], enabled: true },
    { id: 'r2', unit: 'U1', device: 'A引风机', point: '振动幅值', level: 1, cond: '>', val: 5, channels: ['站内', '短信'], enabled: true },
    { id: 'r3', unit: 'U1', device: 'A磨煤机', point: '振动', level: 2, cond: '>', val: 4.5, channels: ['站内', '钉钉'], enabled: true },
    { id: 'r4', unit: 'U1', device: '锅炉', point: '主汽温度', level: 2, cond: '>', val: 545, channels: ['站内', '短信'], enabled: true },
    { id: 'r5', unit: 'U1', device: '锅炉', point: 'NOx浓度', level: 3, cond: 'trend', val: 0, channels: ['站内'], enabled: true },
    { id: 'r6', unit: 'U1', device: '润滑油系统', point: '润滑油压力', level: 1, cond: '<', val: 0.15, channels: ['站内', '短信', '钉钉'], enabled: true }
  ])
  const addRule = (r) => {
    r.id = 'r' + Date.now()
    alarmRules.value.push(r)
  }
  const delRule = (id) => {
    alarmRules.value = alarmRules.value.filter(r => r.id !== id)
  }

  // ============ 通知渠道定义 ============
  const channels = [
    { key: '站内', label: '站内消息', icon: '🔔', desc: '系统内消息中心' },
    { key: '短信', label: '短信通知', icon: '📱', desc: '绑定手机号' },
    { key: '邮件', label: '邮件', icon: '📧', desc: '绑定邮箱' },
    { key: '钉钉', label: '钉钉机器人', icon: '🔵', desc: '绑定群机器人Webhook' },
    { key: '企微', label: '企业微信', icon: '🟢', desc: '绑定企业微信' }
  ]

  // ============ 报警分级 - 哪些设备故障属于哪个等级 ============
  const alarmLevels = ref({
    1: {
      label: '一级报警',
      color: '#ef4444',
      desc: '紧急：可能造成设备损坏或安全威胁，需立即处置',
      avgTime: 1.2,
      deviceFaults: [
        { device: 'A引风机', point: '轴承温度', reason: '润滑失效/冷却不足' },
        { device: '润滑油系统', point: '油压低', reason: '油泵故障/管路泄漏' },
        { device: '锅炉', point: '蒸汽超压', reason: '安全阀异常/燃料过量' },
        { device: '给水泵', point: '轴温急升', reason: '轴承抱死' }
      ],
      rules: ['30分钟未处置自动升级至场站负责人', '同步推送短信+钉钉+邮件', '触发联动控制逻辑']
    },
    2: {
      label: '二级报警',
      color: '#f59e0b',
      desc: '关注：参数异常但暂无安全风险，需在当班内确认',
      avgTime: 4.5,
      deviceFaults: [
        { device: '磨煤机', point: '振动超标', reason: '磨辊磨损/煤质变差' },
        { device: '锅炉', point: '主汽温度高', reason: '减温水量不足' },
        { device: '凝汽器', point: '真空下降', reason: '冷却水温度高' },
        { device: '除氧器', point: '水位低', reason: '给水调节异常' }
      ],
      rules: ['2小时未确认自动升级为一级', '推送站内+短信', '生成巡检工单']
    },
    3: {
      label: '智能预警',
      color: '#06b6d4',
      desc: '预警：智能预测发现潜在风险趋势，建议关注',
      avgTime: 12,
      deviceFaults: [
        { device: '引风机', point: '效率下降', reason: '叶轮积灰' },
        { device: '给水泵', point: '效率下降', reason: '气蚀征兆' },
        { device: '高压加热器', point: '端差增大', reason: '传热管结垢趋势' }
      ],
      rules: ['24小时未关闭需交接班处理', '仅推送站内', '纳入趋势分析报告']
    }
  })

  // ============ 知识图谱 - 全部实体（按 category 分组） ============
  const kgData = {
    设备: [
      { name: 'A引风机', c: '#3b82f6', unit: 'U1', deviceId: 'U1-idf-a' },
      { name: 'A磨煤机', c: '#3b82f6', unit: 'U1', deviceId: 'U1-mill-a' },
      { name: '给水泵B', c: '#3b82f6', unit: 'U1', deviceId: 'U1-pump-b' },
      { name: 'B引风机', c: '#3b82f6', unit: 'U1', deviceId: 'U1-idf-b' },
      { name: '锅炉', c: '#3b82f6', unit: 'U1', deviceId: null }
    ],
    故障: [
      { name: '轴承温度高', c: '#ef4444' },
      { name: '振动超标', c: '#ef4444' },
      { name: '效率下降', c: '#ef4444' },
      { name: '主汽温度高', c: '#ef4444' },
      { name: '润滑油压低', c: '#ef4444' }
    ],
    原因: [
      { name: '润滑失效', c: '#f59e0b' },
      { name: '冷却不足', c: '#f59e0b' },
      { name: '磨辊磨损', c: '#f59e0b' },
      { name: '煤质变差', c: '#f59e0b' },
      { name: '气蚀', c: '#f59e0b' },
      { name: '减温水不足', c: '#f59e0b' },
      { name: '管路泄漏', c: '#f59e0b' }
    ],
    方案: [
      { name: '更换油脂', c: '#22c55e' },
      { name: '清洗滤网', c: '#22c55e' },
      { name: '更换磨辊', c: '#22c55e' },
      { name: '调整给煤量', c: '#22c55e' },
      { name: '检修叶轮', c: '#22c55e' },
      { name: '紧固螺栓', c: '#22c55e' }
    ]
  }
  // 图谱关系（按分类）
  const kgRelations = {
    锅炉故障: [
      { from: 'A引风机', to: '轴承温度高' }, { from: 'A引风机', to: '效率下降' },
      { from: 'A磨煤机', to: '振动超标' }, { from: '锅炉', to: '主汽温度高' },
      { from: '轴承温度高', to: '润滑失效' }, { from: '主汽温度高', to: '减温水不足' },
      { from: '振动超标', to: '磨辊磨损' }, { from: '振动超标', to: '煤质变差' },
      { from: '润滑失效', to: '更换油脂' }, { from: '磨辊磨损', to: '更换磨辊' },
      { from: '煤质变差', to: '调整给煤量' }
    ],
    汽轮机故障: [
      { from: '给水泵B', to: '效率下降' }, { from: 'B引风机', to: '轴承温度高' },
      { from: '效率下降', to: '气蚀' }, { from: '轴承温度高', to: '冷却不足' },
      { from: '气蚀', to: '检修叶轮' }, { from: '冷却不足', to: '清洗滤网' }
    ],
    辅机故障: [
      { from: '给水泵B', to: '效率下降' }, { from: '锅炉', to: '主汽温度高' },
      { from: '润滑油压低', to: '管路泄漏' }, { from: '管路泄漏', to: '紧固螺栓' }
    ],
    火警关联: [
      { from: 'A引风机', to: '轴承温度高' }, { from: '锅炉', to: '主汽温度高' },
      { from: '轴承温度高', to: '更换油脂' }, { from: '主汽温度高', to: '减温水不足' }
    ]
  }

  // ============ Computed ============
  const selectedUnitId = ref('U1')
  const selectedDevice = ref('U1-idf-a')
  const selectedUnit = computed(() => units.value.find(u => u.id === selectedUnitId.value))
  const unitDevices = (uid) => devices.value.filter(d => d.unit === uid)
  const deviceById = (id) => devices.value.find(d => d.id === id)
  const unitAlarms = (uid) => alarms.value.filter(a => a.unit === uid)

  // 全局统计
  const stats = computed(() => {
    const total = alarms.value.length
    const l1 = alarms.value.filter(a => a.l === 1).length
    const l2 = alarms.value.filter(a => a.l === 2).length
    const l3 = alarms.value.filter(a => a.l === 3).length
    const unhandled = alarms.value.filter(a => a.st === 'unhandled').length
    return { total, l1, l2, l3, unhandled, handled: total - unhandled }
  })

  return {
    units, addUnit, delUnit, selectedUnitId, selectedUnit,
    devices, addDevice, delDevice, updateDeviceParam, unitDevices, deviceById,
    selectedDevice,
    alarms, setAlarmStatus, triggerAlarm, unitAlarms, stats,
    alarmRules, addRule, delRule, channels, alarmLevels,
    kgData, kgRelations
  }
})

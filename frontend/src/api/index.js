import {
  agentApi, alarmApi, alarmConfigApi,
  statsApi, equipmentApi, curveApi, guideApi, historyApi
} from './modules'
export { algoApi } from './algorithm'

// ======= Mock 开关 =======
// 改为 false 可切换为真实 API（需启动 dev-back + 算法服务）
export const USE_MOCK = true

// ======= 统一导出 =======
export {
  agentApi,
  alarmApi,
  alarmConfigApi,
  statsApi,
  equipmentApi,
  curveApi,
  guideApi,
  historyApi
}

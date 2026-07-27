// ECharts 按需引入 — 只打包项目实际用到的图表与组件，显著减小构建体积
// 用法: import * as echarts from '@/utils/echarts'  (替代 import * as echarts from 'echarts')
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, GaugeChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  MarkLineComponent,
  PolarComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart, BarChart, PieChart, GaugeChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent, MarkLineComponent,
  PolarComponent,
  CanvasRenderer
])

export * from 'echarts/core'

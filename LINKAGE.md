# P3联调对接说明

## 服务
- 地址: http://localhost:8001
- 文档: http://localhost:8001/docs

## P1需代理的接口
POST /api/data/upload - CSV上传
GET /api/data/{id}/profile - 数据概览
POST /api/data/{id}/clean - 数据清洗
POST /api/modeling/arx - ARX建模
POST /api/alarm/residual-check - 残差预警
POST /api/alarm/batch-check - 批量检测
GET /api/data/{id}/residual-trend - 趋势数据
POST /api/simulation/generate - 仿真生成

## 启动顺序
1. P3 -> python -m uvicorn app.main:app --port 8001
2. P1 -> mvn spring-boot:run (8080)
3. P2 -> npm run dev (5173)

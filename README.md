# 算法服务模块 (P3)

## 项目简介
工业多变量时序建模与残差预警引擎。数据上传、清洗、ARX建模、闭环寻优、残差预警全链路。

**技术栈:** Python 3.10+ / FastAPI / Pandas / NumPy

## 快速启动
`ash
cd algorithm
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
`
- API: http://localhost:8001/docs
- Demo: streamlit run demo_app.py -> :8501

## API接口
| 接口 | 说明 |
|------|------|
| POST /api/data/upload | CSV上传 |
| GET /api/data/{id}/profile | 数据概览 |
| POST /api/data/{id}/clean | 数据清洗 |
| POST /api/modeling/arx | ARX建模+闭环寻优 |
| POST /api/alarm/residual-check | 残差预警3sigma |
| POST /api/simulation/generate | 仿真生成 |

## 对接说明
- P1: 代理API到 localhost:8001
- P2: 前端调P1网关/profile /clean /residual-trend
- P4: Agent调/residual-check /batch-check

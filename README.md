# 电厂智能预警与故障诊断 Agent 系统（前后端一体化）

火警（实时报警 / 智能预警 / 故障诊断）系统的**前端 + 后端 + 算法引擎整合工程**。
本目录将原本分散的：

- 前端（`1.00--`，Vue3）
- 后端（`1.00-dev-back`，Spring Boot）
- 算法服务（`1.00--算法`，FastAPI 多变量时序建模引擎）

合并为单一可编排项目。前端通过 API 层对接后端与算法服务，后端不可达时自动回退到本地 mock 数据，保证离线也能演示。

> 原始文件夹 `D:/Projects/Coding/zakuzakuzaku/clone/火警/1.00'` 与 `1.00--算法` 保持不变。

---

## 目录结构

```
ppa-integrated/
├── docker-compose.yml        # 根编排：postgres + redis + backend + algorithm + nginx
├── README.md
├── .gitignore
├── algorithm/                # ★ 新增：FastAPI 工业多变量时序建模算法服务（python:3.11-slim）
│   ├── Dockerfile            # uvicorn 监听 8001（app.main:app）
│   ├── requirements.txt      # fastapi / uvicorn / pandas / numpy / scipy / statsmodels / scikit-learn ...
│   ├── app/                  # FastAPI 入口 + services（清洗/建模/残差检测/仿真）
│   ├── data/                 # 算法调试脚本
│   └── demo_app.py           # Streamlit Demo（可选，独立运行）
├── frontend/                 # Vue3 + Vite + Element Plus + ECharts
│   ├── Dockerfile            # 多阶段：npm build -> nginx 托管
│   ├── nginx.conf            # SPA 托管 + /api -> backend:8080 + /algo/ -> algorithm:8001
│   ├── .env                  # VITE_API_BASE / VITE_USE_MOCK / VITE_ALGO_BASE
│   └── src/
│       ├── api/              # ★ 新增：统一 API 客户端与各模块接口
│       │   ├── request.js    # axios 实例（拆 R 包装、注入 token），用于后端
│       │   ├── algorithm.js  # ★ 新增：算法服务独立 axios（不经 R 包装）
│       │   ├── alarm.js / stats.js / equipment.js / agent.js
│       │   ├── config.js / guide.js / history.js / index.js
│       ├── views/AlgorithmView.vue  # ★ 新增：算法建模页（上传/概览/清洗/ARX/残差预警）
│       └── stores/data.js    # ★ 改动：init() 拉取后端报警，失败回退 mock
└── backend/                  # Spring Boot 3 + MyBatis-Plus
    ├── Dockerfile            # 多阶段：maven build -> jre 运行
    ├── sql/init-timescale.sql# 容器初始化预创建 TimescaleDB 扩展
    └── src/main/resources/
        └── application-docker.yml  # ★ 新增：docker profile（连 postgres/redis 服务名）
```

---

## 快速开始

### 方式一：Docker 一键起整套（推荐）

前置：安装 Docker Desktop（含 compose v2）。

```bash
cd ppa-integrated
docker compose up -d --build
```

启动完成后：

| 服务        | 地址                                 | 说明                          |
|-------------|--------------------------------------|-------------------------------|
| 前端页面    | http://localhost                     | Nginx 托管构建产物            |
| 后端 API    | http://localhost/api/...             | Nginx 反向代理 → backend:8080 |
| 算法服务    | http://localhost/algo/api/...        | Nginx 反向代理 → algorithm:8001 |
| 算法健康检查| http://localhost/algo/health         | 返回 {"status":"ok",...}      |
| 接口文档    | http://localhost:8080/doc.html       | Knife4j（Swagger）            |
| PostgreSQL  | localhost:5432  (ppa_db/ppa_user/ppa_pass_2024) | TimescaleDB          |
| Redis       | localhost:6379                       |                               |
| 算法直连    | localhost:8001                       | 容器端口映射，便于本地调试 / Streamlit Demo |

常用命令：

```bash
docker compose logs -f backend     # 跟踪后端日志
docker compose down                # 停止并移除容器（数据卷保留）
docker compose down -v             # 连同数据卷一起清除
```

### 方式二：本地开发（前后端分离）

**后端**（需 JDK 17 + Maven 3.9）：

```bash
cd backend
# 修改 src/main/resources/application-dev.yml 的数据库连接后：
./mvnw spring-boot:run            # 默认 8080，CORS 已放开
```

**前端**（需 Node 22+）：

```bash
cd frontend
npm install
npm run dev                       # 默认 5173，/api 经 vite proxy -> localhost:8080
```

前端打开 http://localhost:5173 。

---

## 前后端联调原理

- 开发：前端 `vite.config.js` 已配置 `server.proxy['/api'] -> http://localhost:8080`，
  以及 `server.proxy['/algo'] -> http://localhost:8001`（去掉 `/algo` 前缀）。
- 生产：Nginx 将 `/api/` 反向代理到 `backend:8080`、将 `/algo/` 反向代理到 `algorithm:8001`（去掉 `/algo` 前缀），
  其余请求作为 SPA 回退到 `index.html`。
- 后端所有接口以 `/api/**` 暴露，CORS 已全局放行，前端 `src/api/request.js` 统一去掉 `R<T>` 包装后返回 `data`。
- 算法服务以 `/api/**` 暴露（内部路径），经 Nginx 挂载在 `/algo/api/**`；其返回**原始 JSON**（无 `R<T>` 包装），
  故前端使用独立 axios 实例 `src/api/algorithm.js`，不经过 `request.js` 拦截器。

**数据来源策略（自动模式）**：前端 `App.vue` 启动即调用 `store.init()`，
优先向 `/api/alarm/realtime` 拉取实时报警；若后端不可达或返回空，则回退到 `stores/data.js` 中的本地 mock，
因此**没有后端也能完整演示**。可通过 `frontend/.env` 的 `VITE_USE_MOCK=true` 强制纯前端 demo。

---

## 已对接的后后端接口（前端 → 后端）

| 模块        | 前端 API 函数                  | 后端端点                                   | 集成状态        |
|-------------|--------------------------------|--------------------------------------------|-----------------|
| 实时报警    | `alarmApi.page`                | `GET /api/alarm/realtime`                  | ✅ 已打通+回退   |
| 报警确认    | `alarmApi.confirm`             | `POST /api/alarm/realtime/{id}/confirm`    | ✅ 已打通        |
| 报警抑制    | `alarmApi.suppress`            | `POST /api/alarm/realtime/{id}/suppress`   | ✅ 已打通        |
| 分级分组    | `alarmApi.levelGroups`         | `GET /api/alarm/level/groups`              | 🟡 API 就绪      |
| 统计报表    | `statsApi.*`                   | `GET /api/alarm/stats/**`                  | 🟡 API 就绪      |
| 设备维度    | `equipmentApi.*`               | `GET /api/equipment/**`                    | 🟡 API 就绪      |
| AI 智能体   | `agentApi.*`                   | `GET/POST /api/agent/**`                   | ✅ 独立服务已落地 |
| 报警配置    | `configApi.*`                  | `GET/POST/PUT/DELETE /api/alarm/config/**` | 🟡 API 就绪      |
| 操作指导    | `guideApi.*`                   | `GET/POST /api/alarm/{id}/operation-guide*`| 🟡 API 就绪      |
| 历史导入    | `historyApi.*`                 | `POST/GET /api/history/**`                 | 🟡 API 就绪      |

> ✅ 表示页面数据已真正切换为后端来源（实时报警列表 / 仪表盘统计）。
> 🟡 表示 API 客户端与类型映射已就绪，可在对应页面进一步接线（机组 `units`、设备 `devices` 的
> 富字段目前仍用 mock，因为后端维度表仅有下拉选项、无健康度/工况参数等展示字段）。

---

## 算法服务（FastAPI 建模引擎）

`algorithm/` 是独立的工业多变量时序建模引擎，负责**数据清洗、ARX 多变量建模、残差预警**等计算密集任务。
它**不是**后端 `agent.service.url`(9001) 所指的「AI 运行智能体」（那个是聊天/推理服务），二者职责不同。

### 在系统中的位置

- 容器名 `ppa-algorithm`，镜像基于 `python:3.11-slim`，由 `uvicorn app.main:app` 在 `8001` 暴露。
- 通过 Nginx 挂载到 `/algo/`，前端页面「算法建模」(`/algorithm`) 直接调用，无需关心后端。
- 自带内存数据缓存（`_data_cache`），无需数据库；`/health` 提供健康检查。

### 主要端点（外部经 `/algo` 访问）

| 方法 | 外部路径（Nginx）                 | 内部路径                       | 说明                         |
|------|-----------------------------------|--------------------------------|------------------------------|
| POST | `/algo/api/data/upload`           | `/api/data/upload`            | 上传 CSV，返回 `file_id`     |
| GET  | `/algo/api/data/{id}/profile`     | `/api/data/{id}/profile`      | 数据概览（变量统计）         |
| POST | `/algo/api/data/{id}/clean`       | `/api/data/{id}/clean`        | 清洗（去异常/缺失/重采样）   |
| POST | `/algo/api/modeling/arx`          | `/api/modeling/arx`           | ARX 建模 + 闭环寻优          |
| POST | `/algo/api/alarm/residual-check`  | `/api/alarm/residual-check`   | 残差预警检测                 |
| POST | `/algo/api/simulation/generate`   | `/api/simulation/generate`    | 生成仿真数据（无需上传）     |
| GET  | `/algo/health`                    | `/health`                     | 健康检查                     |

### 前端「算法建模」页

`frontend/src/views/AlgorithmView.vue` 复刻了 `demo_app.py`（Streamlit）的流程：
**上传/仿真 → 数据概览 → 清洗 → ARX 建模 → 残差预警**，全部走 `src/api/algorithm.js`。
数据经算法服务处理后仅保存在其进程内存中（演示用途），如需持久化可在后端增建对应表。

> 本地单跑算法服务（不依赖 Docker）：
> ```bash
> cd algorithm
> pip install -r requirements.txt
> uvicorn app.main:app --host 0.0.0.0 --port 8001
> # 可选：另开终端跑 Streamlit Demo（默认连 http://localhost:8001）
> # pip install streamlit && streamlit run demo_app.py
> ```

---

## 智能体问答服务（Agent Chat, FastAPI 9001）

`agent/` 是独立的 AI 运行智能体服务，为前端「运行智能体」(`/agent`) 提供**会话管理 + 流式问答 + 工具编排**能力。它与算法服务（FastAPI 建模引擎，`/algo`）是**两个不同服务**。

### 在系统中的位置

- 容器名 `ppa-agent`，基于 `python:3.11-slim`，`uvicorn app.main:app` 监听 `9001`。
- 通过 Nginx 挂载在 `/api/agent/`（`frontend/nginx.conf` 已为它单独配置，并开启 SSE 缓冲关闭）。
- 复用现有 `ppa_db` 实例，新建 `agent_sessions` / `agent_messages` / `agent_knowledge` 三张表；本地开发默认用 SQLite 兜底（`DATABASE_URL` 未配置时）。
- LLM 默认走 **mock 编排**（无 API Key 即可演示）；配置 `LLM_API_KEY` 后切换为 OpenAI-compatible 函数调用。

### 主要端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/agent/sessions` | 分页 / 搜索会话列表 |
| POST | `/api/agent/sessions` | 新建会话 |
| GET  | `/api/agent/sessions/{id}/messages` | 取会话历史 |
| PUT  | `/api/agent/sessions/{id}/title` | 重命名 |
| DELETE | `/api/agent/sessions/{id}` | 删除会话 |
| POST | `/api/agent/chat/stream` | **SSE 流式问答**（thinking→tool_call→tool_result→card→chart→content→suggest→done） |
| POST | `/api/agent/chat` | 非流式同步问答 |
| GET  | `/api/agent/knowledge/search` | 知识库检索 |
| GET  | `/health` | 健康检查（返回 llm=mock/configured） |

### 内置工具（Tool Dispatcher）

`query_device_status` / `query_alarms` / `query_trend` / `query_working_condition` / `query_knowledge` / `generate_report`。
工具优先调用 `BACKEND_URL`（`backend:8080`）拿实时数据，后端不可达时回退内置 mock，保证离线可演示。

### 前端接入

- `frontend/src/api/agent.js`：会话 CRUD + `chatStream`（fetch 读取 SSE）。
- `frontend/src/views/AgentView.vue`：会话侧栏、8 大功能模式、消息流（思考过程/卡片/图表/推荐问题）全部由后端驱动；发送走 SSE 实时渲染。
- 开发代理：`vite.config.js` 新增 `/api/agent` → `http://localhost:9001`（建在 `/api` 之前以优先匹配）。

### 本地单跑（不依赖 Docker）

```bash
cd agent
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 9001
# 配置真实 LLM：
# set LLM_API_KEY=sk-xxx   (可选 LLM_BASE_URL / LLM_MODEL)
```

---

## 已知限制 / 后续工作

1. **外部依赖**：后端 `application.yml` 中 `kg.service.url`(9000) 为独立知识图谱微服务，
   未部署时知识检索相关能力会降级（Agent 服务内已用本地兜底）。
   **AI 运行智能体（`agent.service.url`=9001）现已落地为独立 FastAPI 服务**（`agent/` 目录，见上文），
   由 `docker-compose.yml` 编排，Nginx 经 `/api/agent/` 暴露。
   注意：本集成的 **算法服务（FastAPI 建模引擎）是独立模块**，走 `/algo`，与 Agent 服务（9001）不是同一服务。
2. **机组与设备**：前端仪表盘依赖 `unit.base.*`（负荷/汽温等）与 `device.params`（电流/振动等），
   后端暂无对应接口，故这两块保留 mock。
3. **登录**：前端登录为本地 mock（生成假 token），后端未提供鉴权端点；`request.js` 已预留 token 注入。
4. **确认/抑制联动**：后端来源的报警在确认/抑制时会写回后端；`resolved`/重新打开目前仅更新本地状态。

---

## 环境变量（frontend/.env）

| 变量             | 默认值 | 说明                                          |
|------------------|--------|-----------------------------------------------|
| `VITE_API_BASE`  | `/api` | 后端基础路径（开发/生产统一相对路径）         |
| `VITE_USE_MOCK`  | `false`| `true`=强制纯前端 demo；`false`=自动模式回退  |
| `VITE_ALGO_BASE` | `/algo/api` | 算法服务基础路径（开发可改 `http://localhost:8001/api`） |

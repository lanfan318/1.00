# 工业多变量时序建模算法服务（FastAPI）镜像
# 构建：docker compose build algorithm
# 运行：uvicorn 在 0.0.0.0:8001 提供 /api/... 与 /health

FROM python:3.11-slim

WORKDIR /app

# 编译依赖（scipy / statsmodels 在 slim 上可能需要 gcc；优先用预编译 wheel，
# 装 build-essential 以防个别包回退源码编译）
RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc curl \
    && rm -rf /var/lib/apt/lists/*

# 先装依赖，利用 Docker 层缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app ./app
COPY data ./data
COPY demo_app.py .

EXPOSE 8001

# 注意：app 包需在 sys.path 上，uvicorn 默认把 cwd(/app) 加入，故 app.main 可导入
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]

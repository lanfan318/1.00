-- 容器首次启动时以超级用户身份执行（docker-entrypoint-initdb.d）
-- 预创建 TimescaleDB 扩展，使应用所用的 ppa_user 能够使用 create_hypertable
-- （Flyway 的 V1 迁移中会对 ts_measure_point 调用 create_hypertable）
CREATE EXTENSION IF NOT EXISTS timescaledb;

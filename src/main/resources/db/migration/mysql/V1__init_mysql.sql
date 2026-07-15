-- ============================================================================
-- V1__init_mysql.sql — MySQL 版本建表脚本（开发环境）
-- 注意：TimescaleDB hypertable 仅 PG 支持，MySQL 下 ts_measure_point 为普通表
-- ============================================================================

-- ============================================================================
-- 1. 维度表
-- ============================================================================

CREATE TABLE dim_unit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_code       VARCHAR(32)  NOT NULL,
    unit_name       VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_unit_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机组维度表';

CREATE TABLE dim_specialty (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    specialty_code  VARCHAR(32)  NOT NULL,
    specialty_name  VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_specialty_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业维度表';

CREATE TABLE dim_system (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    specialty_id    BIGINT       NOT NULL,
    system_code     VARCHAR(32)  NOT NULL,
    system_name     VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_system_specialty (specialty_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统维度表';

CREATE TABLE dim_measure_point (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    system_id       BIGINT,
    point_code      VARCHAR(100) NOT NULL,
    point_name      VARCHAR(200) NOT NULL,
    tag_no          VARCHAR(64),
    point_type      VARCHAR(16)  DEFAULT 'IN',
    unit            VARCHAR(32),
    range_low       DOUBLE,
    range_high      DOUBLE,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_point_code (point_code),
    INDEX idx_point_unit (unit_id),
    INDEX idx_point_system (system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测点维度表';

-- ============================================================================
-- 2. 报警配置表
-- ============================================================================

CREATE TABLE t_alarm_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(200) NOT NULL,
    rule_code       VARCHAR(64)  NOT NULL,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    alarm_type      TINYINT      NOT NULL,
    alarm_level     TINYINT      NOT NULL DEFAULT 1,
    enabled         TINYINT      NOT NULL DEFAULT 1,
    description     TEXT,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_config_code (rule_code),
    INDEX idx_config_unit (unit_id),
    INDEX idx_config_specialty (specialty_id),
    INDEX idx_config_type (alarm_type),
    INDEX idx_config_level (alarm_level),
    INDEX idx_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警配置主表';

CREATE TABLE t_alarm_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       BIGINT       NOT NULL,
    rule_type       TINYINT      NOT NULL,
    operator        VARCHAR(8),
    threshold       DECIMAL(18, 6),
    deadband        DECIMAL(18, 6),
    rate_period     INT,
    duration        INT          DEFAULT 0,
    logic_expression TEXT,
    sort_order      INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_rule_config (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警规则明细表';

-- ============================================================================
-- 3. 报警业务表
-- ============================================================================

CREATE TABLE t_alarm_realtime (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       BIGINT,
    alarm_code      VARCHAR(64)  NOT NULL,
    alarm_title     VARCHAR(300) NOT NULL,
    alarm_type      TINYINT      NOT NULL,
    alarm_level     TINYINT      NOT NULL DEFAULT 1,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    current_value   DECIMAL(18, 6),
    threshold_value DECIMAL(18, 6),
    occur_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed       TINYINT      NOT NULL DEFAULT 0,
    confirm_by      VARCHAR(64),
    confirm_time    DATETIME,
    suppressed      TINYINT      NOT NULL DEFAULT 0,
    suppress_reason TEXT,
    classify_level  TINYINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_realtime_occur (occur_time),
    INDEX idx_realtime_unit (unit_id),
    INDEX idx_realtime_specialty (specialty_id),
    INDEX idx_realtime_level (alarm_level),
    INDEX idx_realtime_confirmed (confirmed),
    INDEX idx_realtime_suppressed (suppressed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时报警表';

CREATE TABLE t_alarm_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       BIGINT,
    alarm_code      VARCHAR(64)  NOT NULL,
    alarm_title     VARCHAR(300) NOT NULL,
    alarm_type      TINYINT      NOT NULL,
    alarm_level     TINYINT      NOT NULL DEFAULT 1,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    peak_value      DECIMAL(18, 6),
    threshold_value DECIMAL(18, 6),
    occur_time      DATETIME     NOT NULL,
    recover_time    DATETIME,
    duration_seconds BIGINT      DEFAULT 0,
    confirm_by      VARCHAR(64),
    confirm_time    DATETIME,
    handle_status   TINYINT      DEFAULT 0,
    handle_remark   TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_history_occur (occur_time),
    INDEX idx_history_unit (unit_id),
    INDEX idx_history_specialty (specialty_id),
    INDEX idx_history_level (alarm_level),
    INDEX idx_history_type (alarm_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警历史表';

CREATE TABLE t_alarm_suppress (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT,
    config_id       BIGINT,
    measure_point_id BIGINT,
    suppress_type   TINYINT      NOT NULL DEFAULT 1,
    reason          TEXT,
    start_time      DATETIME     NOT NULL,
    end_time        DATETIME,
    operator        VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_suppress_alarm (alarm_id),
    INDEX idx_suppress_config (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警抑制表';

CREATE TABLE t_alarm_classify (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    classify_level  TINYINT      NOT NULL,
    classify_basis  TEXT,
    classify_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator        VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_classify_alarm (alarm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警分级表';

-- ============================================================================
-- 4. 业务表
-- ============================================================================

CREATE TABLE t_equipment_curve (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    measure_point_id BIGINT,
    curve_type      VARCHAR(8)   NOT NULL,
    sample_time     DATETIME     NOT NULL,
    sample_value    DECIMAL(18, 6),
    quality_flag    TINYINT      DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_curve_unit_device (unit_id, device_code),
    INDEX idx_curve_sample_time (sample_time),
    INDEX idx_curve_type (curve_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工况曲线表';

CREATE TABLE t_health_score (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    score           DECIMAL(5, 2),
    score_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detail          JSON,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_health_unit_device (unit_id, device_code),
    INDEX idx_health_score_time (score_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备健康评分表';

CREATE TABLE t_history_import_task (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name         VARCHAR(200) NOT NULL,
    original_file_name VARCHAR(500),
    file_size         BIGINT       DEFAULT 0,
    status            TINYINT      NOT NULL DEFAULT 0,
    total_records     BIGINT       DEFAULT 0,
    success_records   BIGINT       DEFAULT 0,
    fail_records      BIGINT       DEFAULT 0,
    error_detail      TEXT,
    start_time        DATETIME,
    end_time          DATETIME,
    operator          VARCHAR(64),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_import_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史数据导入任务表';

-- ============================================================================
-- 5. 协作表
-- ============================================================================

CREATE TABLE t_operation_guide (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    title           VARCHAR(300),
    content         TEXT,
    diagnosis       TEXT,
    suggestion      TEXT,
    risk_level      VARCHAR(8),
    comment         TEXT,
    comment_by      VARCHAR(64),
    source          TINYINT      DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_guide_alarm (alarm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作指导表';

CREATE TABLE t_reference_trace (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    guide_id        BIGINT       NOT NULL,
    source_type     VARCHAR(32),
    source_name     VARCHAR(300),
    source_url      VARCHAR(1000),
    snippet         TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_trace_guide (guide_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='引用溯源表';

CREATE TABLE t_kg_reasoning (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    reasoning_path  JSON,
    confidence      DOUBLE,
    raw_result      JSON,
    cost_ms         BIGINT       DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_kg_reasoning_alarm (alarm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱推理记录表';

CREATE TABLE t_chat_session (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(300),
    status          VARCHAR(16)  DEFAULT 'active',
    created_by      VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_session_status (status),
    INDEX idx_session_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 会话表';

CREATE TABLE t_chat_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    content         TEXT,
    raw_data        JSON,
    seq             INT          DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_message_session (session_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 消息表';

-- ============================================================================
-- 6. 时序数据表（MySQL 普通表，PG 下为 TimescaleDB hypertable）
-- ============================================================================

CREATE TABLE ts_measure_point (
    ts              DATETIME     NOT NULL,
    measure_point_id BIGINT      NOT NULL,
    unit_id         BIGINT       NOT NULL,
    value           DECIMAL(18, 6),
    quality_flag    TINYINT      DEFAULT 0,
    INDEX idx_ts_point (measure_point_id, ts),
    INDEX idx_ts_unit (unit_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测点时序数据（PG 下为 TimescaleDB hypertable）';

-- ============================================================================
-- 7. 种子数据
-- ============================================================================
INSERT INTO dim_specialty (specialty_code, specialty_name, sort_order) VALUES
    ('BOILER',     '锅炉', 1),
    ('TURBINE',    '汽机', 2),
    ('AUXILIARY',  '辅网', 3),
    ('ELECTRICAL', '电气', 4),
    ('CHEMICAL',   '化水', 5);

INSERT INTO dim_unit (unit_code, unit_name, sort_order) VALUES
    ('10001', '1号机组', 1),
    ('10002', '2号机组', 2);

-- ============================================================================
-- V2__ai_analysis.sql — AI 分析结果存储表
-- ============================================================================

CREATE TABLE t_ai_analysis_result (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT,
    unit_id         BIGINT,
    device_code     VARCHAR(64),
    analysis_type   VARCHAR(32)  NOT NULL,
    input_data      JSON,
    output_result   JSON,
    confidence      DOUBLE       DEFAULT 0,
    model_version   VARCHAR(32),
    cost_ms         BIGINT       DEFAULT 0,
    status          TINYINT      DEFAULT 1,
    error_msg       TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_ai_alarm (alarm_id),
    INDEX idx_ai_type (analysis_type),
    INDEX idx_ai_unit (unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 分析结果记录表';

-- 报警根因分析结果
CREATE TABLE t_root_cause_result (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    root_causes     JSON,
    related_alarms  JSON,
    causal_chain    TEXT,
    confidence      DOUBLE       DEFAULT 0,
    recommendation  TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_rc_alarm (alarm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警根因分析结果表';

-- 设备健康评分趋势表（内部算法计算）
CREATE TABLE t_health_score_trend (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    score           DECIMAL(5, 2),
    dimension_scores JSON,
    score_time      DATETIME     NOT NULL,
    algorithm       VARCHAR(32)  DEFAULT 'weighted',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_hst_unit_device (unit_id, device_code),
    INDEX idx_hst_time (score_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备健康评分趋势表';

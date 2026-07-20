CREATE TABLE t_ai_analysis_result (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT,
    unit_id         BIGINT,
    device_code     VARCHAR(64),
    analysis_type   VARCHAR(32)  NOT NULL,
    input_data      JSONB,
    output_result   JSONB,
    confidence      DOUBLE PRECISION DEFAULT 0,
    model_version   VARCHAR(32),
    cost_ms         BIGINT       DEFAULT 0,
    status          SMALLINT     DEFAULT 1,
    error_msg       TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_ai_analysis_result IS 'AI 分析结果记录表';
CREATE INDEX idx_ai_alarm ON t_ai_analysis_result(alarm_id) WHERE deleted = 0;
CREATE INDEX idx_ai_type ON t_ai_analysis_result(analysis_type) WHERE deleted = 0;
CREATE INDEX idx_ai_unit ON t_ai_analysis_result(unit_id) WHERE deleted = 0;
DO $$ BEGIN
    EXECUTE format('CREATE TRIGGER trg_t_ai_analysis_result_updated_at BEFORE UPDATE ON t_ai_analysis_result FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()');
END $$;

CREATE TABLE t_root_cause_result (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    root_causes     JSONB,
    related_alarms  JSONB,
    causal_chain    TEXT,
    confidence      DOUBLE PRECISION DEFAULT 0,
    recommendation  TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_root_cause_result IS '报警根因分析结果表';
CREATE INDEX idx_rc_alarm ON t_root_cause_result(alarm_id) WHERE deleted = 0;
DO $$ BEGIN
    EXECUTE format('CREATE TRIGGER trg_t_root_cause_result_updated_at BEFORE UPDATE ON t_root_cause_result FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()');
END $$;

CREATE TABLE t_health_score_trend (
    id              BIGSERIAL PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    score           NUMERIC(5, 2),
    dimension_scores JSONB,
    score_time      TIMESTAMPTZ  NOT NULL,
    algorithm       VARCHAR(32)  DEFAULT 'weighted',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_health_score_trend IS '设备健康评分趋势表';
CREATE INDEX idx_hst_unit_device ON t_health_score_trend(unit_id, device_code) WHERE deleted = 0;
CREATE INDEX idx_hst_time ON t_health_score_trend(score_time DESC) WHERE deleted = 0;
DO $$ BEGIN
    EXECUTE format('CREATE TRIGGER trg_t_health_score_trend_updated_at BEFORE UPDATE ON t_health_score_trend FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()');
END $$;

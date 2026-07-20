-- ============================================================================
-- V1__init.sql — 电厂智能预警与故障诊断 Agent 系统 完整建表脚本
-- Database: PostgreSQL 15 + TimescaleDB
-- ============================================================================

-- 启用 TimescaleDB 扩展
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================================
-- 1. 维度表
-- ============================================================================

-- 机组维度表
CREATE TABLE dim_unit (
    id              BIGSERIAL PRIMARY KEY,
    unit_code       VARCHAR(32)  NOT NULL,
    unit_name       VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE dim_unit IS '机组维度表';
COMMENT ON COLUMN dim_unit.unit_code IS '机组编码';
COMMENT ON COLUMN dim_unit.unit_name IS '机组名称';
COMMENT ON COLUMN dim_unit.sort_order IS '排序号';

-- 专业维度表（五大专业：锅炉/汽机/辅网/电气/化水）
CREATE TABLE dim_specialty (
    id              BIGSERIAL PRIMARY KEY,
    specialty_code  VARCHAR(32)  NOT NULL,
    specialty_name  VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE dim_specialty IS '专业维度表';
COMMENT ON COLUMN dim_specialty.specialty_code IS '专业编码';
COMMENT ON COLUMN dim_specialty.specialty_name IS '专业名称';
COMMENT ON COLUMN dim_specialty.sort_order IS '排序号';

-- 系统维度表
CREATE TABLE dim_system (
    id              BIGSERIAL PRIMARY KEY,
    specialty_id    BIGINT       NOT NULL,
    system_code     VARCHAR(32)  NOT NULL,
    system_name     VARCHAR(100) NOT NULL,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE dim_system IS '系统维度表';
COMMENT ON COLUMN dim_system.specialty_id IS '所属专业ID';
COMMENT ON COLUMN dim_system.system_code IS '系统编码';
COMMENT ON COLUMN dim_system.system_name IS '系统名称';
COMMENT ON COLUMN dim_system.sort_order IS '排序号';
CREATE INDEX idx_dim_system_specialty ON dim_system(specialty_id) WHERE deleted = 0;

-- 测点维度表
CREATE TABLE dim_measure_point (
    id              BIGSERIAL PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    system_id       BIGINT,
    point_code      VARCHAR(100) NOT NULL,
    point_name      VARCHAR(200) NOT NULL,
    tag_no          VARCHAR(64),
    point_type      VARCHAR(16)  DEFAULT 'IN',
    unit            VARCHAR(32),
    range_low       DOUBLE PRECISION,
    range_high      DOUBLE PRECISION,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE dim_measure_point IS '测点维度表';
COMMENT ON COLUMN dim_measure_point.unit_id IS '所属机组ID';
COMMENT ON COLUMN dim_measure_point.system_id IS '所属系统ID';
COMMENT ON COLUMN dim_measure_point.point_code IS '测点编码（机组号:位号:类型，例 10001:MALF114:IN）';
COMMENT ON COLUMN dim_measure_point.point_name IS '测点名称';
COMMENT ON COLUMN dim_measure_point.tag_no IS '位号';
COMMENT ON COLUMN dim_measure_point.point_type IS '类型（IN/OUT/MID）';
COMMENT ON COLUMN dim_measure_point.unit IS '单位';
COMMENT ON COLUMN dim_measure_point.range_low IS '量程下限';
COMMENT ON COLUMN dim_measure_point.range_high IS '量程上限';
COMMENT ON COLUMN dim_measure_point.sort_order IS '排序号';
CREATE UNIQUE INDEX idx_measure_point_code ON dim_measure_point(point_code) WHERE deleted = 0;
CREATE INDEX idx_measure_point_unit ON dim_measure_point(unit_id) WHERE deleted = 0;
CREATE INDEX idx_measure_point_system ON dim_measure_point(system_id) WHERE deleted = 0;

-- ============================================================================
-- 2. 报警配置表
-- ============================================================================

-- 报警配置主表（49 条规则）
CREATE TABLE t_alarm_config (
    id              BIGSERIAL PRIMARY KEY,
    rule_name       VARCHAR(200) NOT NULL,
    rule_code       VARCHAR(64)  NOT NULL,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    alarm_type      SMALLINT     NOT NULL,
    alarm_level     SMALLINT     NOT NULL DEFAULT 1,
    enabled         SMALLINT     NOT NULL DEFAULT 1,
    description     TEXT,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_config IS '报警配置主表';
COMMENT ON COLUMN t_alarm_config.rule_name IS '规则名称';
COMMENT ON COLUMN t_alarm_config.rule_code IS '规则编码';
COMMENT ON COLUMN t_alarm_config.unit_id IS '所属机组ID';
COMMENT ON COLUMN t_alarm_config.specialty_id IS '所属专业ID';
COMMENT ON COLUMN t_alarm_config.system_id IS '所属系统ID';
COMMENT ON COLUMN t_alarm_config.measure_point_id IS '关联测点ID';
COMMENT ON COLUMN t_alarm_config.alarm_type IS '报警类型: 1-预测预警 2-故障预警 3-阈值预警 4-视频预警 5-速率预警';
COMMENT ON COLUMN t_alarm_config.alarm_level IS '报警等级: 1-一级 2-二级 3-智能预警';
COMMENT ON COLUMN t_alarm_config.enabled IS '启用状态: 1-启用 0-禁用';
COMMENT ON COLUMN t_alarm_config.description IS '规则描述';
COMMENT ON COLUMN t_alarm_config.sort_order IS '排序号';
CREATE UNIQUE INDEX idx_alarm_config_code ON t_alarm_config(rule_code) WHERE deleted = 0;
CREATE INDEX idx_alarm_config_unit ON t_alarm_config(unit_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_config_specialty ON t_alarm_config(specialty_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_config_type ON t_alarm_config(alarm_type) WHERE deleted = 0;
CREATE INDEX idx_alarm_config_level ON t_alarm_config(alarm_level) WHERE deleted = 0;
CREATE INDEX idx_alarm_config_enabled ON t_alarm_config(enabled) WHERE deleted = 0;

-- 报警规则明细表（一对多子表）
CREATE TABLE t_alarm_rule (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT       NOT NULL,
    rule_type       SMALLINT     NOT NULL,
    operator        VARCHAR(8),
    threshold       NUMERIC(18, 6),
    deadband        NUMERIC(18, 6),
    rate_period     INT,
    duration        INT          DEFAULT 0,
    logic_expression TEXT,
    sort_order      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_rule IS '报警规则明细表';
COMMENT ON COLUMN t_alarm_rule.config_id IS '所属报警配置ID';
COMMENT ON COLUMN t_alarm_rule.rule_type IS '规则类型: 1-阈值 2-速率 3-偏差 4-逻辑';
COMMENT ON COLUMN t_alarm_rule.operator IS '比较运算符: GT/GTE/LT/LTE/EQ/NEQ';
COMMENT ON COLUMN t_alarm_rule.threshold IS '阈值';
COMMENT ON COLUMN t_alarm_rule.deadband IS '死区值';
COMMENT ON COLUMN t_alarm_rule.rate_period IS '速率周期（秒），速率预警时使用';
COMMENT ON COLUMN t_alarm_rule.duration IS '持续时长（秒），防抖用';
COMMENT ON COLUMN t_alarm_rule.logic_expression IS '逻辑表达式，逻辑预警时使用';
COMMENT ON COLUMN t_alarm_rule.sort_order IS '排序号';
CREATE INDEX idx_alarm_rule_config ON t_alarm_rule(config_id) WHERE deleted = 0;

-- ============================================================================
-- 3. 报警业务表
-- ============================================================================

-- 实时报警表
CREATE TABLE t_alarm_realtime (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT,
    alarm_code      VARCHAR(64)  NOT NULL,
    alarm_title     VARCHAR(300) NOT NULL,
    alarm_type      SMALLINT     NOT NULL,
    alarm_level     SMALLINT     NOT NULL DEFAULT 1,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    current_value   NUMERIC(18, 6),
    threshold_value NUMERIC(18, 6),
    occur_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    confirmed       SMALLINT     NOT NULL DEFAULT 0,
    confirm_by      VARCHAR(64),
    confirm_time    TIMESTAMPTZ,
    suppressed      SMALLINT     NOT NULL DEFAULT 0,
    suppress_reason TEXT,
    classify_level  SMALLINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_realtime IS '实时报警表';
COMMENT ON COLUMN t_alarm_realtime.config_id IS '关联报警配置ID';
COMMENT ON COLUMN t_alarm_realtime.alarm_code IS '报警编码';
COMMENT ON COLUMN t_alarm_realtime.alarm_title IS '报警标题';
COMMENT ON COLUMN t_alarm_realtime.alarm_type IS '报警类型';
COMMENT ON COLUMN t_alarm_realtime.alarm_level IS '报警等级';
COMMENT ON COLUMN t_alarm_realtime.unit_id IS '机组ID';
COMMENT ON COLUMN t_alarm_realtime.specialty_id IS '专业ID';
COMMENT ON COLUMN t_alarm_realtime.system_id IS '系统ID';
COMMENT ON COLUMN t_alarm_realtime.measure_point_id IS '测点ID';
COMMENT ON COLUMN t_alarm_realtime.current_value IS '当前值';
COMMENT ON COLUMN t_alarm_realtime.threshold_value IS '阈值';
COMMENT ON COLUMN t_alarm_realtime.occur_time IS '报警发生时间';
COMMENT ON COLUMN t_alarm_realtime.confirmed IS '确认状态: 0-未确认 1-已确认';
COMMENT ON COLUMN t_alarm_realtime.confirm_by IS '确认人';
COMMENT ON COLUMN t_alarm_realtime.confirm_time IS '确认时间';
COMMENT ON COLUMN t_alarm_realtime.suppressed IS '抑制状态: 0-正常 1-已抑制';
COMMENT ON COLUMN t_alarm_realtime.suppress_reason IS '抑制原因';
COMMENT ON COLUMN t_alarm_realtime.classify_level IS '分级标签';
CREATE INDEX idx_alarm_realtime_occur ON t_alarm_realtime(occur_time DESC) WHERE deleted = 0;
CREATE INDEX idx_alarm_realtime_unit ON t_alarm_realtime(unit_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_realtime_specialty ON t_alarm_realtime(specialty_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_realtime_level ON t_alarm_realtime(alarm_level) WHERE deleted = 0;
CREATE INDEX idx_alarm_realtime_confirmed ON t_alarm_realtime(confirmed) WHERE deleted = 0;
CREATE INDEX idx_alarm_realtime_suppressed ON t_alarm_realtime(suppressed) WHERE deleted = 0;

-- 报警历史表
CREATE TABLE t_alarm_history (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT,
    alarm_code      VARCHAR(64)  NOT NULL,
    alarm_title     VARCHAR(300) NOT NULL,
    alarm_type      SMALLINT     NOT NULL,
    alarm_level     SMALLINT     NOT NULL DEFAULT 1,
    unit_id         BIGINT,
    specialty_id    BIGINT,
    system_id       BIGINT,
    measure_point_id BIGINT,
    peak_value      NUMERIC(18, 6),
    threshold_value NUMERIC(18, 6),
    occur_time      TIMESTAMPTZ  NOT NULL,
    recover_time    TIMESTAMPTZ,
    duration_seconds BIGINT      DEFAULT 0,
    confirm_by      VARCHAR(64),
    confirm_time    TIMESTAMPTZ,
    handle_status   SMALLINT     DEFAULT 0,
    handle_remark   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_history IS '报警历史表';
COMMENT ON COLUMN t_alarm_history.config_id IS '关联报警配置ID';
COMMENT ON COLUMN t_alarm_history.alarm_code IS '报警编码';
COMMENT ON COLUMN t_alarm_history.alarm_title IS '报警标题';
COMMENT ON COLUMN t_alarm_history.alarm_type IS '报警类型';
COMMENT ON COLUMN t_alarm_history.alarm_level IS '报警等级';
COMMENT ON COLUMN t_alarm_history.unit_id IS '机组ID';
COMMENT ON COLUMN t_alarm_history.specialty_id IS '专业ID';
COMMENT ON COLUMN t_alarm_history.system_id IS '系统ID';
COMMENT ON COLUMN t_alarm_history.measure_point_id IS '测点ID';
COMMENT ON COLUMN t_alarm_history.peak_value IS '峰值';
COMMENT ON COLUMN t_alarm_history.threshold_value IS '阈值';
COMMENT ON COLUMN t_alarm_history.occur_time IS '发生时间';
COMMENT ON COLUMN t_alarm_history.recover_time IS '恢复时间';
COMMENT ON COLUMN t_alarm_history.duration_seconds IS '持续时长（秒）';
COMMENT ON COLUMN t_alarm_history.confirm_by IS '确认人';
COMMENT ON COLUMN t_alarm_history.confirm_time IS '确认时间';
COMMENT ON COLUMN t_alarm_history.handle_status IS '处理状态';
COMMENT ON COLUMN t_alarm_history.handle_remark IS '处理备注';
CREATE INDEX idx_alarm_history_occur ON t_alarm_history(occur_time DESC) WHERE deleted = 0;
CREATE INDEX idx_alarm_history_unit ON t_alarm_history(unit_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_history_specialty ON t_alarm_history(specialty_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_history_level ON t_alarm_history(alarm_level) WHERE deleted = 0;
CREATE INDEX idx_alarm_history_type ON t_alarm_history(alarm_type) WHERE deleted = 0;

-- 报警抑制表
CREATE TABLE t_alarm_suppress (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT,
    config_id       BIGINT,
    measure_point_id BIGINT,
    suppress_type   SMALLINT     NOT NULL DEFAULT 1,
    reason          TEXT,
    start_time      TIMESTAMPTZ  NOT NULL,
    end_time        TIMESTAMPTZ,
    operator        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_suppress IS '报警抑制表';
COMMENT ON COLUMN t_alarm_suppress.alarm_id IS '关联实时报警ID';
COMMENT ON COLUMN t_alarm_suppress.config_id IS '报警配置ID';
COMMENT ON COLUMN t_alarm_suppress.measure_point_id IS '测点ID';
COMMENT ON COLUMN t_alarm_suppress.suppress_type IS '抑制类型: 1-手动抑制 2-自动抑制 3-定时抑制';
COMMENT ON COLUMN t_alarm_suppress.reason IS '抑制原因';
COMMENT ON COLUMN t_alarm_suppress.start_time IS '抑制开始时间';
COMMENT ON COLUMN t_alarm_suppress.end_time IS '抑制结束时间';
COMMENT ON COLUMN t_alarm_suppress.operator IS '操作人';
CREATE INDEX idx_alarm_suppress_alarm ON t_alarm_suppress(alarm_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_suppress_config ON t_alarm_suppress(config_id) WHERE deleted = 0;
CREATE INDEX idx_alarm_suppress_time ON t_alarm_suppress(start_time, end_time) WHERE deleted = 0;

-- 报警分级表
CREATE TABLE t_alarm_classify (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    classify_level  SMALLINT     NOT NULL,
    classify_basis  TEXT,
    classify_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    operator        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_alarm_classify IS '报警分级表';
COMMENT ON COLUMN t_alarm_classify.alarm_id IS '关联实时报警ID';
COMMENT ON COLUMN t_alarm_classify.classify_level IS '分级等级: 1-一级 2-二级 3-智能预警';
COMMENT ON COLUMN t_alarm_classify.classify_basis IS '分级依据';
COMMENT ON COLUMN t_alarm_classify.classify_time IS '分级时间';
COMMENT ON COLUMN t_alarm_classify.operator IS '操作人';
CREATE INDEX idx_alarm_classify_alarm ON t_alarm_classify(alarm_id) WHERE deleted = 0;

-- ============================================================================
-- 4. 业务表
-- ============================================================================

-- 工况曲线表
CREATE TABLE t_equipment_curve (
    id              BIGSERIAL PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    measure_point_id BIGINT,
    curve_type      VARCHAR(8)   NOT NULL,
    sample_time     TIMESTAMPTZ  NOT NULL,
    sample_value    NUMERIC(18, 6),
    quality_flag    SMALLINT     DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_equipment_curve IS '工况曲线表';
COMMENT ON COLUMN t_equipment_curve.unit_id IS '机组ID';
COMMENT ON COLUMN t_equipment_curve.device_code IS '设备编码';
COMMENT ON COLUMN t_equipment_curve.measure_point_id IS '测点ID';
COMMENT ON COLUMN t_equipment_curve.curve_type IS '曲线类型: fd-风道 sb-设备';
COMMENT ON COLUMN t_equipment_curve.sample_time IS '采样时间';
COMMENT ON COLUMN t_equipment_curve.sample_value IS '采样值';
COMMENT ON COLUMN t_equipment_curve.quality_flag IS '质量标记: 0-正常 1-可疑 2-坏值';
CREATE INDEX idx_curve_unit_device ON t_equipment_curve(unit_id, device_code) WHERE deleted = 0;
CREATE INDEX idx_curve_sample_time ON t_equipment_curve(sample_time DESC) WHERE deleted = 0;
CREATE INDEX idx_curve_type ON t_equipment_curve(curve_type) WHERE deleted = 0;

-- 设备健康评分表
CREATE TABLE t_health_score (
    id              BIGSERIAL PRIMARY KEY,
    unit_id         BIGINT       NOT NULL,
    device_code     VARCHAR(64)  NOT NULL,
    score           NUMERIC(5, 2),
    score_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    detail          JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_health_score IS '设备健康评分表';
COMMENT ON COLUMN t_health_score.unit_id IS '机组ID';
COMMENT ON COLUMN t_health_score.device_code IS '设备编码';
COMMENT ON COLUMN t_health_score.score IS '健康评分 (0-100)';
COMMENT ON COLUMN t_health_score.score_time IS '评分时间';
COMMENT ON COLUMN t_health_score.detail IS '评分依据/详情 JSON';
CREATE INDEX idx_health_unit_device ON t_health_score(unit_id, device_code) WHERE deleted = 0;
CREATE INDEX idx_health_score_time ON t_health_score(score_time DESC) WHERE deleted = 0;

-- 历史数据导入任务表
CREATE TABLE t_history_import_task (
    id                BIGSERIAL PRIMARY KEY,
    task_name         VARCHAR(200) NOT NULL,
    original_file_name VARCHAR(500),
    file_size         BIGINT       DEFAULT 0,
    status            SMALLINT     NOT NULL DEFAULT 0,
    total_records     BIGINT       DEFAULT 0,
    success_records   BIGINT       DEFAULT 0,
    fail_records      BIGINT       DEFAULT 0,
    error_detail      TEXT,
    start_time        TIMESTAMPTZ,
    end_time          TIMESTAMPTZ,
    operator          VARCHAR(64),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_history_import_task IS '历史数据导入任务表';
COMMENT ON COLUMN t_history_import_task.task_name IS '任务名称';
COMMENT ON COLUMN t_history_import_task.original_file_name IS '原始文件名';
COMMENT ON COLUMN t_history_import_task.file_size IS '文件大小（字节）';
COMMENT ON COLUMN t_history_import_task.status IS '状态: 0-等待 1-处理中 2-成功 3-失败';
COMMENT ON COLUMN t_history_import_task.total_records IS '总记录数';
COMMENT ON COLUMN t_history_import_task.success_records IS '成功记录数';
COMMENT ON COLUMN t_history_import_task.fail_records IS '失败记录数';
COMMENT ON COLUMN t_history_import_task.error_detail IS '错误详情';
COMMENT ON COLUMN t_history_import_task.start_time IS '开始时间';
COMMENT ON COLUMN t_history_import_task.end_time IS '结束时间';
COMMENT ON COLUMN t_history_import_task.operator IS '操作人';
CREATE INDEX idx_import_task_status ON t_history_import_task(status) WHERE deleted = 0;

-- ============================================================================
-- 5. 协作表
-- ============================================================================

-- 操作指导表
CREATE TABLE t_operation_guide (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    title           VARCHAR(300),
    content         TEXT,
    diagnosis       TEXT,
    suggestion      TEXT,
    risk_level      VARCHAR(8),
    comment         TEXT,
    comment_by      VARCHAR(64),
    source          SMALLINT     DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_operation_guide IS '操作指导表';
COMMENT ON COLUMN t_operation_guide.alarm_id IS '关联报警ID';
COMMENT ON COLUMN t_operation_guide.title IS '指导标题';
COMMENT ON COLUMN t_operation_guide.content IS '指导内容（富文本）';
COMMENT ON COLUMN t_operation_guide.diagnosis IS '诊断结论';
COMMENT ON COLUMN t_operation_guide.suggestion IS '建议措施';
COMMENT ON COLUMN t_operation_guide.risk_level IS '风险等级: 高/中/低';
COMMENT ON COLUMN t_operation_guide.comment IS '运行意见';
COMMENT ON COLUMN t_operation_guide.comment_by IS '运行意见填写人';
COMMENT ON COLUMN t_operation_guide.source IS '来源: 1-知识图谱 2-Agent 3-人工';
CREATE INDEX idx_guide_alarm ON t_operation_guide(alarm_id) WHERE deleted = 0;

-- 引用溯源表
CREATE TABLE t_reference_trace (
    id              BIGSERIAL PRIMARY KEY,
    guide_id        BIGINT       NOT NULL,
    source_type     VARCHAR(32),
    source_name     VARCHAR(300),
    source_url      VARCHAR(1000),
    snippet         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_reference_trace IS '引用溯源表';
COMMENT ON COLUMN t_reference_trace.guide_id IS '关联操作指导ID';
COMMENT ON COLUMN t_reference_trace.source_type IS '引用来源类型: 规程/历史案例/专家经验/文献';
COMMENT ON COLUMN t_reference_trace.source_name IS '引用来源名称';
COMMENT ON COLUMN t_reference_trace.source_url IS '引用来源路径或URL';
COMMENT ON COLUMN t_reference_trace.snippet IS '引用片段';
CREATE INDEX idx_trace_guide ON t_reference_trace(guide_id) WHERE deleted = 0;

-- 知识图谱推理记录表
CREATE TABLE t_kg_reasoning (
    id              BIGSERIAL PRIMARY KEY,
    alarm_id        BIGINT       NOT NULL,
    reasoning_path  JSONB,
    confidence      DOUBLE PRECISION,
    raw_result      JSONB,
    cost_ms         BIGINT       DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_kg_reasoning IS '知识图谱推理记录表';
COMMENT ON COLUMN t_kg_reasoning.alarm_id IS '关联报警ID';
COMMENT ON COLUMN t_kg_reasoning.reasoning_path IS '推理路径（JSON）';
COMMENT ON COLUMN t_kg_reasoning.confidence IS '置信度';
COMMENT ON COLUMN t_kg_reasoning.raw_result IS 'KG 服务返回的原始结果（JSON）';
COMMENT ON COLUMN t_kg_reasoning.cost_ms IS '推理耗时（毫秒）';
CREATE INDEX idx_kg_reasoning_alarm ON t_kg_reasoning(alarm_id) WHERE deleted = 0;

-- Agent 会话表
CREATE TABLE t_chat_session (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(300),
    status          VARCHAR(16)  DEFAULT 'active',
    created_by      VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_chat_session IS 'Agent 会话表';
COMMENT ON COLUMN t_chat_session.title IS '会话标题';
COMMENT ON COLUMN t_chat_session.status IS '会话状态: active/closed';
COMMENT ON COLUMN t_chat_session.created_by IS '创建人';
CREATE INDEX idx_session_status ON t_chat_session(status) WHERE deleted = 0;
CREATE INDEX idx_session_created_by ON t_chat_session(created_by) WHERE deleted = 0;

-- Agent 消息表
CREATE TABLE t_chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    content         TEXT,
    raw_data        JSONB,
    seq             INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_chat_message IS 'Agent 消息表';
COMMENT ON COLUMN t_chat_message.session_id IS '所属会话ID';
COMMENT ON COLUMN t_chat_message.role IS '角色: user/assistant/system';
COMMENT ON COLUMN t_chat_message.content IS '消息内容';
COMMENT ON COLUMN t_chat_message.raw_data IS 'Agent 服务返回的原始数据（JSON）';
COMMENT ON COLUMN t_chat_message.seq IS '消息序号';
CREATE INDEX idx_message_session ON t_chat_message(session_id, seq) WHERE deleted = 0;

-- ============================================================================
-- 6. TimescaleDB 时序 Hypertable
-- ============================================================================

CREATE TABLE ts_measure_point (
    ts              TIMESTAMPTZ  NOT NULL,
    measure_point_id BIGINT      NOT NULL,
    unit_id         BIGINT       NOT NULL,
    value           NUMERIC(18, 6),
    quality_flag    SMALLINT     DEFAULT 0
);
COMMENT ON TABLE ts_measure_point IS '测点时序数据表（TimescaleDB hypertable）';
COMMENT ON COLUMN ts_measure_point.ts IS '采样时间（分区键）';
COMMENT ON COLUMN ts_measure_point.measure_point_id IS '测点ID';
COMMENT ON COLUMN ts_measure_point.unit_id IS '机组ID';
COMMENT ON COLUMN ts_measure_point.value IS '采样值';
COMMENT ON COLUMN ts_measure_point.quality_flag IS '质量标记: 0-正常 1-可疑 2-坏值';

-- 转换为 hypertable，按 7 天分区
SELECT create_hypertable('ts_measure_point', 'ts', chunk_time_interval => INTERVAL '7 days', if_not_exists => TRUE);

-- 时序表常用查询索引
CREATE INDEX idx_ts_measure_point_id ON ts_measure_point(measure_point_id, ts DESC);
CREATE INDEX idx_ts_unit ON ts_measure_point(unit_id, ts DESC);

-- ============================================================================
-- 7. 种子数据：五大专业初始化
-- ============================================================================
INSERT INTO dim_specialty (specialty_code, specialty_name, sort_order) VALUES
    ('BOILER',     '锅炉', 1),
    ('TURBINE',    '汽机', 2),
    ('AUXILIARY',  '辅网', 3),
    ('ELECTRICAL', '电气', 4),
    ('CHEMICAL',   '化水', 5);

-- 种子数据：默认机组
INSERT INTO dim_unit (unit_code, unit_name, sort_order) VALUES
    ('10001', '1号机组', 1),
    ('10002', '2号机组', 2);

-- ============================================================================
-- 8. updated_at 自动更新触发器
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为所有含 updated_at 的业务表创建触发器
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN
        SELECT unnest(ARRAY[
            'dim_unit','dim_specialty','dim_system','dim_measure_point',
            't_alarm_config','t_alarm_rule',
            't_alarm_realtime','t_alarm_history','t_alarm_suppress','t_alarm_classify',
            't_equipment_curve','t_health_score','t_history_import_task',
            't_operation_guide','t_reference_trace','t_kg_reasoning',
            't_chat_session','t_chat_message'
        ])
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at
             BEFORE UPDATE ON %I
             FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()',
            tbl, tbl
        );
    END LOOP;
END $$;

CREATE TABLE t_voice_session (
    id                BIGSERIAL PRIMARY KEY,
    session_name      VARCHAR(200),
    status            VARCHAR(16)  DEFAULT 'active',
    total_count       INT          DEFAULT 0,
    total_duration_ms BIGINT       DEFAULT 0,
    created_by        VARCHAR(64),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_voice_session IS '语音识别会话表';
CREATE INDEX idx_vs_status ON t_voice_session(status) WHERE deleted = 0;
CREATE INDEX idx_vs_created_by ON t_voice_session(created_by) WHERE deleted = 0;
DO $$ BEGIN EXECUTE format('CREATE TRIGGER trg_t_voice_session_updated_at BEFORE UPDATE ON t_voice_session FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()'); END $$;

CREATE TABLE t_voice_record (
    id                BIGSERIAL PRIMARY KEY,
    session_id        BIGINT,
    unit_id           BIGINT,
    audio_url         VARCHAR(500),
    audio_duration_ms BIGINT       DEFAULT 0,
    audio_size_bytes  BIGINT       DEFAULT 0,
    transcript_text   TEXT,
    confidence        DOUBLE PRECISION DEFAULT 0,
    intent            VARCHAR(32),
    intent_params     JSONB,
    engine_type       VARCHAR(32)  DEFAULT 'mock',
    engine_version    VARCHAR(32),
    cost_ms           BIGINT       DEFAULT 0,
    status            SMALLINT     DEFAULT 1,
    error_msg         TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE t_voice_record IS '语音识别记录表';
CREATE INDEX idx_vr_session ON t_voice_record(session_id) WHERE deleted = 0;
CREATE INDEX idx_vr_intent ON t_voice_record(intent) WHERE deleted = 0;
CREATE INDEX idx_vr_created ON t_voice_record(created_at DESC) WHERE deleted = 0;
DO $$ BEGIN EXECUTE format('CREATE TRIGGER trg_t_voice_record_updated_at BEFORE UPDATE ON t_voice_record FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()'); END $$;

-- ============================================================================
-- V3__voice.sql — 语音识别模块建表
-- ============================================================================

CREATE TABLE t_voice_session (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_name   VARCHAR(200),
    status          VARCHAR(16)  DEFAULT 'active',
    total_count     INT          DEFAULT 0,
    total_duration_ms BIGINT     DEFAULT 0,
    created_by      VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_vs_status (status),
    INDEX idx_vs_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音识别会话表';

CREATE TABLE t_voice_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT,
    unit_id         BIGINT,
    audio_url       VARCHAR(500),
    audio_duration_ms BIGINT     DEFAULT 0,
    audio_size_bytes BIGINT      DEFAULT 0,
    transcript_text TEXT,
    confidence      DOUBLE       DEFAULT 0,
    intent          VARCHAR(32),
    intent_params   JSON,
    engine_type     VARCHAR(32)  DEFAULT 'mock',
    engine_version  VARCHAR(32),
    cost_ms         BIGINT       DEFAULT 0,
    status          TINYINT      DEFAULT 1,
    error_msg       TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_vr_session (session_id),
    INDEX idx_vr_intent (intent),
    INDEX idx_vr_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音识别记录表';

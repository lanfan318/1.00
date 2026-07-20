"""SQLAlchemy 模型：会话 / 消息 / 知识库。兼容 PostgreSQL 与 SQLite（本地兜底）。"""
from datetime import datetime, timezone
from sqlalchemy import (
    Column, BigInteger, String, Text, Integer,
    Boolean, JSON, ForeignKey, DateTime, text, Index
)
from sqlalchemy.orm import declarative_base, relationship

Base = declarative_base()

# SQLite 仅对 INTEGER PRIMARY KEY 自增，BIGINT 不自增；用 variant 在 SQLite 上退化为 Integer。
PK = BigInteger().with_variant(Integer, "sqlite")


def _now():
    return datetime.now(timezone.utc)


class Session(Base):
    __tablename__ = "agent_sessions"

    id = Column(PK, primary_key=True, autoincrement=True)
    title = Column(String(200), nullable=False, default="新对话")
    mode = Column(String(50))                 # 设备工况/报警分析/...
    unit_id = Column(String(50))              # 机组上下文（可选）
    created_by = Column(String(100), nullable=False)
    created_at = Column(DateTime(timezone=True), default=_now)
    updated_at = Column(DateTime(timezone=True), default=_now, onupdate=_now)

    messages = relationship(
        "Message", back_populates="session",
        cascade="all, delete-orphan", order_by="Message.created_at"
    )


class Message(Base):
    __tablename__ = "agent_messages"

    id = Column(PK, primary_key=True, autoincrement=True)
    session_id = Column(BigInteger, ForeignKey("agent_sessions.id", ondelete="CASCADE"), nullable=False)
    role = Column(String(20), nullable=False)            # user/assistant/tool/system
    content = Column(Text)
    payload = Column(JSON, default=dict)                 # {text, card, chart, quick_questions, templates}
    thinking = Column(Text)                              # LLM 思考过程（可折叠）
    tool_calls = Column(JSON, default=list)              # [{name, args, result, latency_ms}]
    model = Column(String(100))
    prompt_tokens = Column(Integer)
    completion_tokens = Column(Integer)
    created_at = Column(DateTime(timezone=True), default=_now)

    session = relationship("Session", back_populates="messages")


class Knowledge(Base):
    __tablename__ = "agent_knowledge"

    id = Column(PK, primary_key=True, autoincrement=True)
    category = Column(String(50))        # 知识库/政策/运行规程/故障案例
    title = Column(String(300))
    content = Column(Text, nullable=False)
    tags = Column(JSON, default=list)    # SQLite/PG 均可用的标签数组
    source = Column(String(300))         # 来源文件或规范编号
    created_at = Column(DateTime(timezone=True), default=_now)


Index("idx_agent_messages_session", Message.session_id, Message.created_at)
Index("idx_agent_knowledge_category", Knowledge.category)

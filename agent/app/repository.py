"""会话 / 消息 / 知识库 的 CRUD 数据访问层。"""
from datetime import datetime, timezone
from sqlalchemy import select, func, or_, text
from app.models import Session, Message, Knowledge


def _json(payload):
    return payload or {}


def list_sessions(db, page=1, size=20, keyword=None, created_by=None):
    """分页查会话，附带首条消息摘要。"""
    stmt = select(Session)
    if keyword:
        stmt = stmt.where(Session.title.ilike(f"%{keyword}%"))
    if created_by:
        stmt = stmt.where(Session.created_by == created_by)
    total = db.scalar(select(func.count()).select_from(stmt.subquery()))
    rows = db.scalars(
        stmt.order_by(Session.updated_at.desc())
        .offset((page - 1) * size).limit(size)
    ).all()
    return total, rows


def get_session(db, session_id):
    return db.get(Session, session_id)


def create_session(db, title="新对话", mode=None, unit_id=None, created_by="system"):
    s = Session(title=title, mode=mode, unit_id=unit_id, created_by=created_by)
    db.add(s)
    db.commit()
    db.refresh(s)
    return s


def rename_session(db, session_id, title):
    s = db.get(Session, session_id)
    if s:
        s.title = title
        s.updated_at = datetime.now(timezone.utc)
        db.commit()
        db.refresh(s)
    return s


def delete_session(db, session_id):
    s = db.get(Session, session_id)
    if s:
        db.delete(s)
        db.commit()
    return s is not None


def list_messages(db, session_id):
    return db.scalars(
        select(Message).where(Message.session_id == session_id)
        .order_by(Message.created_at.asc())
    ).all()


def add_message(db, session_id, role, content=None, payload=None, thinking=None,
                tool_calls=None, model=None, prompt_tokens=None, completion_tokens=None):
    m = Message(
        session_id=session_id, role=role, content=content,
        payload=_json(payload), thinking=thinking,
        tool_calls=tool_calls or [], model=model,
        prompt_tokens=prompt_tokens, completion_tokens=completion_tokens,
    )
    db.add(m)
    # 更新会话的 updated_at
    s = db.get(Session, session_id)
    if s:
        s.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(m)
    return m


def search_knowledge(db, q, limit=5):
    """知识库检索：PostgreSQL 用全全文；SQLite 用 LIKE 兜底。"""
    stmt = select(Knowledge)
    if q:
        like = f"%{q}%"
        stmt = stmt.where(or_(
            Knowledge.title.ilike(like),
            Knowledge.content.ilike(like),
        ))
    return db.scalars(stmt.order_by(Knowledge.id.desc()).limit(limit)).all()

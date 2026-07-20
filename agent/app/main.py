"""智能体问答服务（FastAPI，端口 9001）。"""
import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Depends, HTTPException, Query, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

from app import models
from app.repository import (
    list_sessions, get_session, create_session, rename_session,
    delete_session, list_messages, add_message, search_knowledge,
)
from app.chat import chat_stream, _gen_title


class SessionIn(BaseModel):
    title: str = "新对话"
    mode: str = None
    unit_id: str = None
    created_by: str = "system"


class TitleIn(BaseModel):
    title: str = "新对话"


class ChatIn(BaseModel):
    session_id: int = None
    question: str = ""
    mode: str = None
    unit_id: str = None

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./agent.db")
engine = create_engine(DATABASE_URL, pool_pre_ping=True,
                       connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {})
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


@asynccontextmanager
async def lifespan(app: FastAPI):
    models.Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="智能体问答服务", version="0.2.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.get("/health")
def health_check():
    return {"status": "ok", "llm": "configured" if os.getenv("LLM_API_KEY") else "mock"}


# ---------------- 会话管理 ----------------
@app.get("/api/agent/sessions")
def get_sessions(page: int = 1, size: int = 20, keyword: Optional[str] = None,
                 created_by: Optional[str] = None, db: Session = Depends(get_db)):
    total, rows = list_sessions(db, page, size, keyword, created_by)
    items = [{
        "id": s.id, "title": s.title, "mode": s.mode, "unitId": s.unit_id,
        "createdAt": s.created_at.isoformat() if s.created_at else None,
        "updatedAt": s.updated_at.isoformat() if s.updated_at else None,
    } for s in rows]
    return {"total": total, "page": page, "size": size, "items": items}


@app.post("/api/agent/sessions")
def post_session(body: SessionIn, db: Session = Depends(get_db)):
    s = create_session(
        db, title=body.title,
        mode=body.mode, unit_id=body.unit_id,
        created_by=body.created_by,
    )
    return {"id": s.id, "title": s.title, "mode": s.mode, "unitId": s.unit_id}


@app.get("/api/agent/sessions/{session_id}/messages")
def get_messages(session_id: int, db: Session = Depends(get_db)):
    if not get_session(db, session_id):
        raise HTTPException(404, "session not found")
    msgs = list_messages(db, session_id)
    return [{
        "id": m.id, "role": m.role, "content": m.content,
        "payload": m.payload, "thinking": m.thinking,
        "toolCalls": m.tool_calls, "model": m.model,
        "createdAt": m.created_at.isoformat() if m.created_at else None,
    } for m in msgs]


@app.put("/api/agent/sessions/{session_id}/title")
def put_title(session_id: int, body: TitleIn, db: Session = Depends(get_db)):
    s = rename_session(db, session_id, body.title)
    if not s:
        raise HTTPException(404, "session not found")
    return {"id": s.id, "title": s.title}


@app.delete("/api/agent/sessions/{session_id}")
def del_session(session_id: int, db: Session = Depends(get_db)):
    ok = delete_session(db, session_id)
    if not ok:
        raise HTTPException(404, "session not found")
    return {"ok": True}


# ---------------- 智能问答 ----------------
@app.post("/api/agent/chat/stream")
async def chat_stream_endpoint(body: ChatIn, db: Session = Depends(get_db)):
    session_id = body.session_id
    question = (body.question or "").strip()
    mode = body.mode
    unit_id = body.unit_id

    if not session_id:
        s = create_session(db, title=_gen_title(question), mode=mode, unit_id=unit_id)
        session_id = s.id
    elif not get_session(db, session_id):
        raise HTTPException(404, "session not found")
    if not question:
        raise HTTPException(400, "question required")

    # 落库用户消息
    add_message(db, session_id, "user", content=question)
    # 首次提问自动生成标题
    s = get_session(db, session_id)
    if s and (not s.title or s.title == "新对话"):
        rename_session(db, session_id, _gen_title(question))

    return StreamingResponse(
        chat_stream(db, session_id, question, mode, unit_id),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.post("/api/agent/chat")
def chat_once(body: ChatIn, db: Session = Depends(get_db)):
    """非流式版本：收集所有事件，返回最终 payload。"""
    session_id = body.session_id
    question = (body.question or "").strip()
    if not session_id:
        s = create_session(db, title=_gen_title(question))
        session_id = s.id
    add_message(db, session_id, "user", content=question)
    final = {"text": "", "card": None, "chart": None, "quick_questions": []}
    for _ in chat_stream(db, session_id, question, body.mode, body.unit_id):
        pass  # chat_stream 内部已落库；这里仅用于简单同步调用
    s = get_session(db, session_id)
    last = s.messages[-1] if s and s.messages else None
    if last and last.payload:
        final.update(last.payload)
    return {"sessionId": session_id, **final}


# ---------------- 知识库检索 ----------------
@app.get("/api/agent/knowledge/search")
def knowledge_search(q: str = Query(..., min_length=1), limit: int = 5, db: Session = Depends(get_db)):
    rows = search_knowledge(db, q, limit)
    return [{"id": r.id, "title": r.title, "content": r.content,
             "category": r.category, "source": r.source} for r in rows]

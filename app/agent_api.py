"""Agent API - dev-back HighLevelAgentClient"""
import uuid, time, asyncio
from fastapi import APIRouter, Body
from fastapi.responses import StreamingResponse

router = APIRouter(prefix="/api/v1", tags=["agent"])
_sessions = {}

@router.post("/handshake")
def handshake():
    return {"token": str(uuid.uuid4()), "status": "ok"}

@router.post("/chat")
def agent_chat(query: str = Body(""), session_id: str = Body("")):
    if not session_id:
        session_id = str(uuid.uuid4())
    kw = ["压力", "温度", "报警", "故障"]
    if any(k in query for k in kw):
        reply = {
            "session_id": session_id, "status": "success",
            "diagnosis": {
                "smart_analysis": ["DCS偏低", "同类偏差明显", "阀门异常"],
                "possible_causes": [
                    {"cause": "传感器故障", "confidence": 0.92},
                    {"cause": "阀门卡涩", "confidence": 0.85},
                    {"cause": "管路泄漏", "confidence": 0.78},
                ],
                "operation_guide": ["核对参数", "对比同类", "排查阀门", "校验变送器"],
                "references": [
                    {"ref_id": "REF-1", "source": "操作规程第3章", "relevance": "high"},
                    {"ref_id": "REF-2", "source": "历史案例#0451", "relevance": "high"},
                ],
                "elapsed_ms": 28450
            }
        }
    else:
        reply = {"session_id": session_id, "status": "success", "reply": f"收到: {query}"}
    _sessions[session_id] = reply
    return reply

@router.post("/stream")
async def agent_stream(query: str = Body(""), session_id: str = Body("")):
    if not session_id:
        session_id = str(uuid.uuid4())
    async def gen():
        for s in ["检索知识库", "分析趋势", "推理根因", "生成报告"]:
            await asyncio.sleep(0.5)
            yield f'data: {{"step": "{s}", "sid": "{session_id}"}}\n\n'
        yield 'data: {"done": true}\n\n'
    return StreamingResponse(gen(), media_type="text/event-stream")

@router.get("/status")
def agent_status(session_id: str = Body("")):
    if session_id and session_id in _sessions:
        return {"status": "active", "session_id": session_id}
    return {"status": "idle", "session_id": session_id}

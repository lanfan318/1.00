"""
工具 / 函数调用层。

每个工具返回标准 dict：{"ok": bool, "data": ...}。
工具执行优先调用 Spring Boot 后端（BACKEND_URL），失败时回退本地 mock，
保证没有后端时对话仍能跑通（与整个工程“无后端也能演示”的约定一致）。
"""
import os
import json
import httpx
from sqlalchemy import select

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
TIMEOUT = float(os.getenv("TOOL_TIMEOUT", "3"))

# ---- mock 数据（后端不可达时兜底）-----------------------------------------
_MOCK_DEVICES = {
    "A引风机": {"健康度": "97.3", "出力": "正常", "负荷": "500.0 MW", "比功": "2854 Nm/kg",
              "体积流量": "157.9 m³/s", "效率": "51%", "电流平衡": "99.58%", "流量平衡": "98.99%",
              "残差预警": "无"},
    "B引风机": {"健康度": "96.1", "振动": "1.5 mm/s", "电流": "125 A", "状态": "运行平稳"},
    "A给水泵": {"健康度": "94.0", "流量": "620 t/h", "状态": "正常"},
}


def _get(path, params=None):
    try:
        with httpx.Client(timeout=TIMEOUT) as c:
            r = c.get(f"{BACKEND_URL}{path}", params=params)
            if r.status_code == 200:
                return r.json()
    except Exception:
        pass
    return None


def query_device_status(device: str):
    """查询设备实时状态：健康度、负荷、振动、温度等。"""
    data = _get(f"/api/equipment/status", params={"device": device})
    if data is None:
        # 兜底 mock
        key = next((k for k in _MOCK_DEVICES if k in (device or "")), None)
        data = _MOCK_DEVICES.get(key) or {"状态": f"{device} 暂无数据"}
        return {"ok": True, "source": "mock", "device": device, "data": data}
    return {"ok": True, "source": "backend", "device": device, "data": data}


def query_alarms(level=None, status=None, time_range=None):
    """查询报警列表（可按等级/状态/时间过滤）。"""
    params = {k: v for k, v in {"level": level, "status": status, "timeRange": time_range}.items() if v}
    data = _get("/api/alarm/realtime", params=params) or {"rows": []}
    return {"ok": True, "source": "backend" if data.get("rows") else "mock",
            "count": len(data.get("rows", [])), "data": data}


def query_trend(metric: str, device_id: str = None, time_range: str = "24h"):
    """查询某一指标的趋势时间序列（返回点序列供前端作图）。"""
    data = _get("/api/alarm/stats/trend", params={"metric": metric, "device": device_id, "range": time_range})
    if data is None:
        import math, random
        base = {"负荷": 500, "主汽温度": 540, "NOx": 320}.get(metric, 100)
        pts = [{"t": i, "v": round(base + 20 * math.sin(i / 3) + random.uniform(-5, 5), 2)} for i in range(24)]
        data = {"points": pts}
        return {"ok": True, "source": "mock", "metric": metric, "data": data}
    return {"ok": True, "source": "backend", "metric": metric, "data": data}


def query_working_condition(device_id: str):
    """查询设备工况图数据（失速线 / 等熵曲线 / 当前工况点）。"""
    data = _get("/api/equipment/condition", params={"device": device_id})
    if data is None:
        return {"ok": True, "source": "mock", "device": device_id,
                "data": {"chart": "引风机工况分析",
                         "current": {"流量": 157.9, "比功": 8496.8},
                         "stall_line": [13200, 13100, 12900, 12600, 12300, 12000, 11700, 11400, 11100]}}
    return {"ok": True, "source": "backend", "device": device_id, "data": data}


def query_knowledge(q: str, db=None, limit: int = 5):
    """知识库检索：优先用传进来的 db 会话做 PG 全文检索。"""
    if db is not None:
        from app.repository import search_knowledge
        rows = search_knowledge(db, q, limit)
        if rows:
            return {"ok": True, "source": "db",
                    "data": [{"title": r.title, "content": r.content, "category": r.category} for r in rows]}
    return {"ok": True, "source": "none", "data": []}


def generate_report(report_type: str, unit_id: str = None):
    """生成运行日志/周报/月报（结构化文本）。"""
    tpl = {
        "运行日志": "【本班运行日志】机组负荷稳定，主要辅机运行正常，无一级报警。",
        "周报": "【本周巡检总结】本周共发生报警 12 条，已处置 10 条，遗留 2 条二级。",
        "月报": "【月度运行报告】月度可用率 99.2%，主要缺陷已闭环。",
    }
    return {"ok": True, "report_type": report_type, "data": tpl.get(report_type, "报告已生成。")}


# 工具注册表：供 LLM 函数调用使用
TOOLS = [
    {
        "name": "query_device_status",
        "description": "查询设备实时状态，返回健康度、负荷、振动、温度等参数。",
        "parameters": {"device": "设备名称，如 A引风机、B引风机、A给水泵"}
    },
    {
        "name": "query_alarms",
        "description": "查询报警列表，可按等级(level)、状态(status)、时间(time_range)过滤。",
        "parameters": {"level": "报警等级（可选）", "status": "状态（可选）", "time_range": "时间范围（可选）"}
    },
    {
        "name": "query_trend",
        "description": "查询某指标的时间趋势序列，用于前端趋势图。",
        "parameters": {"metric": "指标名，如负荷、主汽温度、NOx", "device_id": "设备（可选）", "time_range": "时间范围（可选）"}
    },
    {
        "name": "query_working_condition",
        "description": "查询设备工况图数据（失速线/等熵曲线/当前工况点）。",
        "parameters": {"device_id": "设备名称"}
    },
    {
        "name": "query_knowledge",
        "description": "检索知识库/运行规程/故障案例。",
        "parameters": {"q": "检索关键词"}
    },
    {
        "name": "generate_report",
        "description": "生成运行日志/周报/月报。",
        "parameters": {"report_type": "运行日志/周报/月报"}
    },
]

_TOOL_FUNCS = {
    "query_device_status": query_device_status,
    "query_alarms": query_alarms,
    "query_trend": query_trend,
    "query_working_condition": query_working_condition,
    "query_knowledge": query_knowledge,
    "generate_report": generate_report,
}


def execute_tool(name: str, args: dict, db=None):
    """执行工具，返回 (result_dict, latency_ms)。"""
    import time
    fn = _TOOL_FUNCS.get(name)
    if not fn:
        return {"ok": False, "error": f"unknown tool: {name}"}, 0
    t0 = time.time()
    try:
        if name == "query_knowledge":
            result = fn(args.get("q", ""), db=db)
        else:
            result = fn(**{k: v for k, v in args.items() if k in fn.__code__.co_varnames})
    except Exception as e:
        result = {"ok": False, "error": str(e)}
    latency = int((time.time() - t0) * 1000)
    return result, latency

"""
LLM 推理编排 + SSE 流式输出。

- 配置了 LLM_API_KEY 时走 OpenAI-compatible 函数调用；
- 未配置时走本地 mock 编排（规则 + 工具），保证离线可演示。
输出为 SSE 事件流：thinking -> tool_call -> tool_result -> content -> card -> chart -> done。
"""
import os
import json
import uuid
from datetime import datetime, timezone

from app.models import Message
from app.repository import add_message
from app import tools as tool_module

LLM_API_KEY = os.getenv("LLM_API_KEY", "")
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1")
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-4o-mini")


def _sse(event, data: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


def _gen_title(question: str) -> str:
    t = question.strip().replace("\n", " ")
    return t[:12] + ("…" if len(t) > 12 else "")


def _rule_based(question: str, mode: str, unit_id: str, db):
    """离线 mock：根据关键词调用工具并构造结构化回答。"""
    events = []
    events.append(_sse("thinking", {"type": "thinking", "content": f"分析「{question}」：识别意图，调用相关工具获取实时数据。"}))

    # 设备/工况
    q = question
    if any(k in q for k in ("引风机", "给水泵", "磨煤机", "设备", "工况")):
        dev = next((k for k in ("A引风机", "B引风机", "A给水泵") if k in q), "A引风机")
        res, lat = tool_module.execute_tool("query_device_status", {"device": dev}, db)
        events.append(_sse("tool_call", {"type": "tool_call", "name": "query_device_status", "args": {"device": dev}}))
        events.append(_sse("tool_result", {"type": "tool_result", "name": "query_device_status", "result": res}))
        d = res["data"]
        health = d.get("健康度", "—")
        card = {
            "title": f"{dev} · 健康度 {health}",
            "stat": d.get("残差预警", "无残差预警"),
            "statColor": "#22c55e" if "无" in str(d.get("残差预警", "无")) else "#ef4444",
            "body": "".join(f'<div class="ai-mt"><span>{k}</span><strong>{v}</strong></div>' for k, v in d.items()),
            "ok": "整体运行良好" if "无" in str(d.get("残差预警", "无")) else "存在异常项，建议核查",
        }
        events.append(_sse("card", {"type": "card", "card": card}))
        chart = {"title": f"{dev}工况分析", "source": "Surface", "id": f"s_{uuid.uuid4().hex[:10]}", "toggle": True}
        events.append(_sse("chart", {"type": "chart", "chart": chart}))
        text = f"{dev}当前健康度 {health}，主要运行参数在合理范围内。" + ("未见残差预警。" if "无" in str(d.get('残差预警','无')) else "存在残差预警，请关注。")
        events.append(_sse("content", {"type": "content", "delta": text}))
        quick = [f"{dev}的主要运行参数有哪些？", f"如何理解{dev}的健康度{health}？", f"{dev}距失速告警线的裕度含义？"]
        events.append(_sse("suggest", {"type": "suggest", "quick_questions": quick}))
        return events, text, card, chart, quick

    if "报警" in q:
        res, lat = tool_module.execute_tool("query_alarms", {"time_range": "24h"}, db)
        events.append(_sse("tool_call", {"type": "tool_call", "name": "query_alarms", "args": {}}))
        events.append(_sse("tool_result", {"type": "tool_result", "name": "query_alarms", "result": res}))
        card = {
            "title": f"{unit_id or '机组'} · 当前报警",
            "stat": "待处理 4 条", "statColor": "#ef4444",
            "body": '<div class="ai-mt"><span>一级未处理</span><strong style="color:#ef4444">2 条</strong></div>'
                    '<div class="ai-mt"><span>二级未处理</span><strong style="color:#f59e0b">2 条</strong></div>'
                    '<div class="ai-mt"><span>智能预警</span><strong style="color:#06b6d4">5 条</strong></div>',
            "ok": "已生成处置建议",
        }
        events.append(_sse("card", {"type": "card", "card": card}))
        text = "当前机组有 4 条待处理报警，其中一级 2 条、二级 2 条、智能预警 5 条，建议优先处置一级报警。"
        events.append(_sse("content", {"type": "content", "delta": text}))
        quick = ["今日有哪些一级报警？", "为什么A引风机轴承温度偏高？", "最近一周哪些设备报警最多？"]
        events.append(_sse("suggest", {"type": "suggest", "quick_questions": quick}))
        return events, text, card, None, quick

    # 默认
    res, lat = tool_module.execute_tool("query_knowledge", {"q": q}, db)
    events.append(_sse("tool_call", {"type": "tool_call", "name": "query_knowledge", "args": {"q": q}}))
    events.append(_sse("tool_result", {"type": "tool_result", "name": "query_knowledge", "result": res}))
    text = f"收到您关于「{q}」的咨询。当前为离线演示模式，已基于知识库检索生成初步回答。如需更精准分析，请配置 LLM_API_KEY。"
    events.append(_sse("content", {"type": "content", "delta": text}))
    quick = ["A引风机工况情况", "B引风机工况情况", "今日报警统计"]
    events.append(_sse("suggest", {"type": "suggest", "quick_questions": quick}))
    return events, text, None, None, quick


def chat_stream(db, session_id, question: str, mode: str = None, unit_id: str = None):
    """生成器：逐条产出 SSE 事件字符串。"""
    yield _sse("thinking", {"type": "thinking", "content": "正在思考..."})

    if LLM_API_KEY:
        try:
            for ev in _llm_stream(db, session_id, question, mode, unit_id):
                yield ev
            return
        except Exception as e:
            yield _sse("thinking", {"type": "thinking", "content": f"LLM 调用失败，回退本地推理：{e}"})

    events, text, card, chart, quick = _rule_based(question, mode, unit_id, db)
    for ev in events:
        yield ev

    payload = {"text": text}
    if card: payload["card"] = card
    if chart: payload["chart"] = chart
    if quick: payload["quick_questions"] = quick

    add_message(db, session_id, "assistant", content=text, payload=payload,
                thinking="离线规则编排", model="mock")
    yield _sse("done", {"type": "done", "session_id": session_id})


def _llm_stream(db, session_id, question, mode, unit_id):
    """真实 LLM 路径（OpenAI-compatible 函数调用）。"""
    from openai import OpenAI
    client = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL)

    sys_prompt = (
        f"你是火电厂智能运行助手，熟悉设备、运行规程、报警处置。当前模式：{mode or '通用'}。"
        "可用工具：设备状态/报警/趋势/工况/知识库/报告。回答要简洁，面向运行人员。"
    )
    messages = [{"role": "system", "content": sys_prompt}, {"role": "user", "content": question}]

    tool_defs = [{
        "type": "function",
        "function": {
            "name": t["name"],
            "description": t["description"],
            "parameters": {"type": "object", "properties": {k: {"type": "string"} for k in t["parameters"]},
                           "required": []},
        },
    } for t in tool_module.TOOLS]

    resp = client.chat.completions.create(model=LLM_MODEL, messages=messages, tools=tool_defs, tool_choice="auto")

    msg = resp.choices[0].message
    tool_calls_meta = []
    if msg.tool_calls:
        messages.append({"role": "assistant", "content": None, "tool_calls": msg.tool_calls})
        for tc in msg.tool_calls:
            name = tc.function.name
            args = json.loads(tc.function.arguments or "{}")
            yield _sse("tool_call", {"type": "tool_call", "name": name, "args": args})
            result, lat = tool_module.execute_tool(name, args, db)
            yield _sse("tool_result", {"type": "tool_result", "name": name, "result": result})
            tool_calls_meta.append({"name": name, "args": args, "result": result, "latency_ms": lat})
            messages.append({"role": "tool", "content": json.dumps(result, ensure_ascii=False), "tool_call_id": tc.id})

    final = client.chat.completions.create(model=LLM_MODEL, messages=messages)
    answer = final.choices[0].message.content
    yield _sse("content", {"type": "content", "delta": answer})
    yield _sse("suggest", {"type": "suggest", "quick_questions": ["A引风机工况情况", "今日报警统计", "生成运行日志"]})

    usage = final.usage
    add_message(db, session_id, "assistant", content=answer,
                payload={"text": answer}, tool_calls=tool_calls_meta, model=LLM_MODEL,
                prompt_tokens=usage.prompt_tokens if usage else None,
                completion_tokens=usage.completion_tokens if usage else None)
    yield _sse("done", {"type": "done", "session_id": session_id})

import streamlit as st
import urllib.request, json, math, pandas as pd

st.set_page_config(page_title="算法服务Demo", layout="wide")
st.title("工业多变量时序建模 — 算法服务演示")
BASE = "http://localhost:8001"

def api_get(path):
    req = urllib.request.Request(f"{BASE}{path}", headers={"Accept": "application/json"})
    return json.loads(urllib.request.urlopen(req, timeout=60).read())

def api_post(path, body=None, files=None):
    if files:
        boundary = "----b"; data = b""
        for name, (fn, content, mime) in files.items():
            data += f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"; filename=\"{fn}\"\r\nContent-Type: {mime}\r\n\r\n".encode()
            data += (content if isinstance(content, bytes) else content.encode()) + b"\r\n"
        data += f"--{boundary}--\r\n".encode()
        req = urllib.request.Request(f"{BASE}{path}", data=data, headers={"Content-Type": f"multipart/form-data; boundary={boundary}"})
    else:
        data = json.dumps(body or {}).encode()
        req = urllib.request.Request(f"{BASE}{path}", data=data, headers={"Content-Type": "application/json"})
    return json.loads(urllib.request.urlopen(req, timeout=120).read())

def get_var_count(fid):
    """获取变量列数（不含time）"""
    try:
        p = api_get(f"/api/data/{fid}/profile")
        return p.get("variable_count", 0), [c for c in (api_get(f"/api/data/{fid}/profile").get("variables", []))]
    except:
        return 0, []

if "fid" not in st.session_state: st.session_state.fid = None
if "cid" not in st.session_state: st.session_state.cid = None

st.sidebar.header("数据来源")
mode = st.sidebar.radio("选择", ["生成仿真数据", "上传真实CSV"])

if mode == "生成仿真数据":
    if st.sidebar.button("生成仿真数据"):
        data = api_post("/api/simulation/generate", body={"duration_days": 7, "tags": ["FC101", "FC102"]})
        st.session_state.fid = data["file_id"]
        st.sidebar.success(f"生成成功! {data['rows']}行, {len(data['columns'])}列")
else:
    uploaded = st.sidebar.file_uploader("选择CSV", type="csv")
    if uploaded and st.sidebar.button("上传"):
        data = api_post("/api/data/upload", files={"file": (uploaded.name, uploaded.getvalue(), "text/csv")})
        st.session_state.fid = data["file_id"]
        st.sidebar.success(f"上传成功! {data['rows']}行, {len(data['columns'])}列")

if st.session_state.fid is None:
    st.info("请先在侧边栏生成仿真数据或上传CSV")
    st.stop()

fid = st.session_state.fid
vc, vars_info = get_var_count(fid)
st.sidebar.metric("当前数据ID", fid)

tab1, tab2, tab3, tab4 = st.tabs(["数据概览", "数据清洗", "ARX建模", "残差预警"])

with tab1:
    st.subheader("数据概览")
    profile = api_get(f"/api/data/{fid}/profile")
    c1, c2, c3 = st.columns(3)
    c1.metric("总行数", f"{profile['total_rows']:,}")
    c2.metric("测点数", profile["variable_count"])
    c3.metric("采样周期", f"{profile['sample_interval_seconds']}s")
    for v in profile["variables"]:
        cols = st.columns(6)
        cols[0].metric("均值", round(v["mean"], 4))
        cols[1].metric("标准差", round(v["std"], 4))
        cols[2].metric("最小值", round(v["min_val"], 4))
        cols[3].metric("最大值", round(v["max_val"], 4))
        cols[4].metric("缺失率", f"{v['missing_rate']*100:.1f}%")
        cols[5].metric("可建模", "是" if v["suitable_for_modeling"] else "否")

with tab2:
    st.subheader("数据清洗")
    interval = st.selectbox("重采样间隔", ["1min", "5min", "10min"])
    strategy = st.selectbox("缺失值策略", ["interpolate", "ffill"])
    if st.button("执行清洗", type="primary"):
        cr = api_post(f"/api/data/{fid}/clean", body={"resample_interval": interval, "missing_strategy": strategy})
        st.session_state.cid = cr["clean_id"]
        r = cr["report"]
        cols = st.columns(4)
        cols[0].metric("异常点", r.get("anomaly_count", 0))
        cols[1].metric("缺失(前)", r.get("missing_before", 0))
        cols[2].metric("缺失(后)", r.get("missing_after", 0))
        cols[3].metric("清洗后行数", r.get("rows_after_clean", 0))
        st.success(f"clean_id = {st.session_state.cid}")

with tab3:
    st.subheader("ARX建模 + 闭环寻优")

    if vc < 2:
        st.warning("⚠️ 当前数据仅有 **1 个测点**，无法进行多变量 ARX 建模。\n\nARX 需要至少 **1 个输入变量 + 1 个输出变量**。\n\n请切换到侧边栏的 **「生成仿真数据」** 来体验完整建模流程。")
    else:
        col_names = [v["name"] for v in vars_info] if vars_info else []
        pv_names = [n for n in col_names if n.endswith(".PV")]
        default_target = pv_names[0] if pv_names else (col_names[0] if col_names else "FC101.PV")
        target = st.text_input("目标变量", default_target)
        cid_use = st.session_state.cid if st.session_state.cid else fid
        if st.button("开始建模 (闭环寻优)", type="primary"):
            with st.spinner("随机搜索最优策略中..."):
                mr = api_post(f"/api/modeling/arx?clean_id={cid_use}", body={"target": target, "optimize_preprocessing": True})
                cols = st.columns(4)
                cols[0].metric("训练拟合度", f"{mr.get('train_fit', 0)}%")
                cols[1].metric("验证拟合度", f"{mr.get('test_fit', 0)}%")
                cols[2].metric("目标函数", round(mr.get("objective", 0), 4))
                cols[3].metric("选择输入数", len(mr.get("selected_inputs", [])))
                st.json(mr.get("selected_inputs", []))

with tab4:
    st.subheader("残差预警")
    col_names = [v["name"] for v in vars_info] if vars_info else ["ICS_HC_AS130.AV"]
    default_tag = col_names[0] if col_names else "ICS_HC_AS130.AV"
    tag = st.text_input("测点名称", default_tag)
    cv = st.number_input("当前值", value=5.50, step=0.01)
    if st.button("检测", type="primary"):
        cid_use = st.session_state.cid if st.session_state.cid else fid
        alarm = api_post(f"/api/alarm/residual-check?clean_id={cid_use}", body={"tag_name": tag, "current_value": cv})
        status = "🔴 报警" if alarm["is_alarm"] else "🟢 正常"
        st.markdown(f"### {status}")
        cols = st.columns(4)
        cols[0].metric("当前值", alarm["current_value"])
        cols[1].metric("预测值", round(alarm.get("predicted_value", 0), 4))
        cols[2].metric("残差", round(alarm.get("residual", 0), 4))
        cols[3].metric("级别", alarm.get("alarm_level", ""))
        if alarm["is_alarm"]:
            st.warning(alarm.get("alarm_message", ""))

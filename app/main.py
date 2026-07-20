"""FastAPI 算法服务入口"""
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
import pandas as pd, numpy as np
import tempfile, os, hashlib, time, json, math
from app.services.data_loader import DataLoader
from app.services.data_profiler import DataProfiler
from app.services.data_cleaner import DataCleaner
from app.services.segment_selector import SegmentSelector
from app.services.delay_estimator import DelayEstimator
from app.services.variable_selector import VariableSelector
from app.services.arx_modeler import ARXModeler
from app.services.optimizer import Optimizer
from app.services.residual_detector import ResidualDetector
from app.services.simulation import FOPDTSimulator
from app.agent_api import router as agent_router
from app.models.schemas import CleanRequest, ModelingRequest, SimulationRequest, ResidualAlarmRequest

app = FastAPI(title="工业多变量时序建模算法服务", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
_data_cache: dict = {}
app.include_router(agent_router)

def _clean_value(v):
    """确保单个值不含 NaN/Inf，可以安全 JSON 序列化"""
    if isinstance(v, (float, np.floating)):
        if math.isnan(v) or math.isinf(v):
            return None
        return float(v)
    if isinstance(v, (np.integer,)):
        return int(v)
    if isinstance(v, pd.Timestamp):
        return str(v)
    return v

def _clean_dict(d):
    """递归清洗 dict 里的所有 NaN"""
    if isinstance(d, dict):
        return {k: _clean_dict(v) for k, v in d.items()}
    elif isinstance(d, list):
        return [_clean_dict(v) for v in d]
    return _clean_value(d)

@app.post("/api/data/upload")
async def upload_csv(file: UploadFile = File(...)):
    content = await file.read()
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".csv") as tmp:
            tmp.write(content); tmp_path = tmp.name
        df, config_info, tags = DataLoader.load_csv(tmp_path)
        if len(df) == 0: raise HTTPException(400, "CSV为空")
        file_id = hashlib.md5(f"{file.filename}{time.time()}".encode()).hexdigest()[:12]
        _data_cache[file_id] = df
        return {"file_id": file_id, "file_name": file.filename, "config": config_info,
            "tags": tags, "rows": len(df),
            "columns": [c for c in df.columns if c != "time"],
            "time_start": str(df["time"].min()), "time_end": str(df["time"].max())}
    finally:
        if tmp_path and os.path.exists(tmp_path): os.unlink(tmp_path)

@app.get("/api/data/{file_id}/profile")
def get_profile(file_id: str):
    df = _data_cache.get(file_id)
    if df is None: raise HTTPException(404, "File not found")
    return DataProfiler.profile_dataframe(df, file_id)

@app.post("/api/data/{file_id}/clean")
def clean_data(file_id: str, req: CleanRequest):
    df = _data_cache.get(file_id)
    if df is None: raise HTTPException(404)
    cleaner = DataCleaner()
    df_clean, report = cleaner.clean(df, resample_interval=req.resample_interval,
        outlier_methods=req.outlier_methods, missing_strategy=req.missing_strategy)
    clean_id = f"{file_id}_clean"; _data_cache[clean_id] = df_clean
    return {"clean_id": clean_id, "report": report}

@app.get("/api/data/{clean_id}/segments")
def get_segments(clean_id: str, target: str, inputs: str = None):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    input_list = inputs.split(",") if inputs else [c for c in df.columns if c != "time" and c != target]
    selector = SegmentSelector()
    segments = selector.select_segments(df, target, input_list)
    return {"count": len(segments), "segments": [
        {"start": str(s.start_time), "end": str(s.end_time),
         "input": s.input_var, "quality_score": round(s.quality_score, 2),
         "n_points": s.n_points} for s in segments]}

@app.get("/api/data/{clean_id}/delay")
def estimate_delay(clean_id: str, target: str, inputs: str = None):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    input_list = inputs.split(",") if inputs else [c for c in df.columns if c != "time" and c != target]
    return {"delays": DelayEstimator.estimate_delay_matrix(df, input_list, [target])}

@app.post("/api/modeling/arx")
def arx_modeling(clean_id: str, req: ModelingRequest):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    if req.inputs is None: req.inputs = [c for c in df.columns if c != "time" and c != req.target]
    if req.optimize_preprocessing:
        opt = Optimizer(); result = opt.optimize(df, req.target, req.inputs)
        return _clean_dict({"target": result.target, "selected_inputs": result.selected_inputs,
            "delays": result.delays, "na": result.na, "nb": result.nb,
            "train_fit": result.train_fit, "test_fit": result.test_fit,
            "rmse": result.rmse, "r2": result.r2, "objective": result.objective,
            "trials": len(opt.trials_history), "best_config": result.config})
    else:
        delays = [1] * len(req.inputs)
        y_data = df[req.target].dropna().values
        U_data = np.column_stack([df[inp].ffill().bfill().values for inp in req.inputs])
        model = ARXModeler(na=2, nb=2); model.fit(y_data, U_data, delays)
        y_pred = model.predict(y_data, U_data, delays)
        return _clean_dict({"target": req.target, "selected_inputs": req.inputs,
            "na": 2, "nb": 2, "fit": round(model.compute_fit(y_data, y_pred), 2),
            "rmse": round(model.compute_rmse(y_data, y_pred), 6),
            "r2": round(model.compute_r2(y_data, y_pred), 4)})

@app.post("/api/alarm/residual-check")
def residual_check(clean_id: str, req: ResidualAlarmRequest):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    if req.tag_name not in df.columns: raise HTTPException(404)
    series = df[req.tag_name]; recent = series.dropna().iloc[-req.lookback_hours*60:]
    detector = ResidualDetector()
    return detector.check(req.current_value, recent, req.tag_name)

@app.post("/api/simulation/generate")
def generate_simulation(req: SimulationRequest):
    tags_config = [{"tag": tag, "K": 0.8 + i*0.3, "tau": 25.0 + i*15, "theta": 3.0 + i*5,
        "steps": [(1440 + i*720, 12 - i*3), (4320 + i*360, -8 + i*2)]}
        for i, tag in enumerate(req.tags)]
    sim = FOPDTSimulator()
    df = sim.generate_multivariable(duration_days=req.duration_days, tags_config=tags_config)
    if req.include_anomaly: df = sim.inject_anomalies(df)
    # 最终清洗所有 NaN
    for col in df.columns:
        if col != "time":
            df[col] = df[col].ffill().bfill().fillna(0)
    file_id = f"sim_{req.duration_days}d_{len(req.tags)}tags"
    _data_cache[file_id] = df
    # 手动构建预览，确保无 NaN
    preview = []
    for _, row in df.head(5).iterrows():
        item = {}
        for col in df.columns:
            item[col] = _clean_value(row[col])
        preview.append(item)
    return _clean_dict({"file_id": file_id, "rows": len(df), "columns": list(df.columns),
        "preview": preview})

@app.get("/api/data/{clean_id}/residual-trend")
def residual_trend(clean_id: str, tag_name: str, hours: int = 24):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    series = df[tag_name].dropna(); recent = series.iloc[-hours*60:]
    result_df = ResidualDetector.compute_residual_statistics(recent)
    result_df["time"] = df["time"].iloc[-len(result_df):].values.astype(str)
    return _clean_dict({"tag": tag_name, "data": json.loads(result_df.fillna(0).to_json(orient="records"))})

@app.post("/api/alarm/batch-check")
def batch_check(clean_id: str, tag_names: List[str]):
    df = _data_cache.get(clean_id)
    if df is None: raise HTTPException(404)
    detector = ResidualDetector(); results = []
    for tag in tag_names:
        if tag in df.columns:
            series = df[tag].dropna()
            if len(series) > 0:
                results.append(detector.check(float(series.iloc[-1]), series, tag))
    return {"alarms": results}

@app.get("/health")
def health():
    return {"status": "ok", "cached_files": len(_data_cache)}

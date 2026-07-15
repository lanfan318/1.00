"""Pydantic数据模型"""
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class VariableProfile(BaseModel):
    name: str
    tag: str = ""
    var_type: str = ""
    count: int
    mean: float
    std: float
    min_val: float
    max_val: float
    missing_rate: float
    anomaly_rate: float = 0.0
    sample_interval_seconds: float = 0.0
    has_step: bool = False
    suitable_for_modeling: bool = False

class DataProfileResponse(BaseModel):
    file_name: str
    time_start: str
    time_end: str
    total_rows: int
    variable_count: int
    variables: List[VariableProfile]
    sample_interval_seconds: float
    time_sorted: bool
    duplicate_timestamps: int

class CleanRequest(BaseModel):
    resample_interval: str = "1min"
    outlier_methods: List[str] = ["hampel", "rate_limit", "stuck"]
    missing_strategy: str = "interpolate"
    op_strategy: str = "ffill"
    pv_strategy: str = "linear"
    hampel_window: int = 11
    hampel_threshold: float = 3.0

class CleanResponse(BaseModel):
    rows_before: int
    rows_after: int
    missing_before: int
    missing_after: int
    anomaly_count: int
    removed_segments: List[Dict[str, str]]
    summary: str

class ModelingRequest(BaseModel):
    target: str
    inputs: Optional[List[str]] = None
    auto_delay: bool = True
    auto_variable_selection: bool = True
    optimize_preprocessing: bool = True

class DelayResult(BaseModel):
    input_var: str
    output_var: str
    delay_points: int
    delay_seconds: float
    correlation: float

class ModelingResponse(BaseModel):
    target: str
    selected_inputs: List[str]
    delays: List[DelayResult]
    na: int
    nb: int
    train_fit: float
    test_fit: float
    rmse: float
    r2: float
    best_config: Dict[str, Any]
    model_params: Optional[Dict[str, Any]] = None

class SimulationRequest(BaseModel):
    duration_days: int = 7
    sample_interval: str = "1min"
    tags: List[str] = ["FC101", "FC102"]
    include_step: bool = True
    include_noise: bool = True
    include_anomaly: bool = True
    include_delay: bool = True

class ResidualAlarmRequest(BaseModel):
    tag_name: str
    current_value: float
    lookback_hours: int = 24

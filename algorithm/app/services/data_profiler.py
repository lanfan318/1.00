"""数据概览分析服务"""
import pandas as pd
import numpy as np
from typing import List
from app.models.schemas import VariableProfile, DataProfileResponse
from app.services.data_loader import DataLoader

class DataProfiler:

    @staticmethod
    def profile_variable(series: pd.Series, name: str, sample_interval: float) -> VariableProfile:
        clean = series.dropna()

        if len(clean) == 0:
            return VariableProfile(
                name=name, count=0, mean=0, std=0, min_val=0, max_val=0,
                missing_rate=1.0, sample_interval_seconds=sample_interval
            )

        has_step = DataProfiler._detect_has_step(clean)
        suitable = (clean.std() > 1e-8) and (len(clean) / len(series) > 0.8)
        tag_info = DataLoader.parse_tag(name)

        return VariableProfile(
            name=name,
            tag=tag_info["tag"],
            var_type=tag_info["var_type"],
            count=len(clean),
            mean=round(float(clean.mean()), 6),
            std=round(float(clean.std()), 6),
            min_val=round(float(clean.min()), 6),
            max_val=round(float(clean.max()), 6),
            missing_rate=round(1 - len(clean) / len(series), 4),
            sample_interval_seconds=round(sample_interval, 2),
            has_step=has_step,
            suitable_for_modeling=suitable
        )

    @staticmethod
    def _detect_has_step(series: pd.Series, threshold_multiplier: float = 3.0) -> bool:
        if len(series) < 10:
            return False
        diffs = series.diff().dropna().abs()
        std = diffs.std()
        if std == 0:
            return False
        threshold = threshold_multiplier * std
        return (diffs > threshold).any()

    @staticmethod
    def profile_dataframe(df: pd.DataFrame, file_name: str = "") -> DataProfileResponse:
        var_cols = [c for c in df.columns if c != "time"]
        sample_interval = DataLoader.infer_sample_interval(df)

        variables = []
        for col in var_cols:
            profile = DataProfiler.profile_variable(df[col], col, sample_interval)
            variables.append(profile)

        time_sorted = df["time"].is_monotonic_increasing
        dup_count = df["time"].duplicated().sum()

        return DataProfileResponse(
            file_name=file_name,
            time_start=str(df["time"].min()),
            time_end=str(df["time"].max()),
            total_rows=len(df),
            variable_count=len(var_cols),
            variables=variables,
            sample_interval_seconds=round(sample_interval, 2),
            time_sorted=bool(time_sorted),
            duplicate_timestamps=int(dup_count)
        )

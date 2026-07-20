"""数据清洗引擎"""
import pandas as pd
import numpy as np
from typing import List, Dict, Tuple
from app.config import config as cfg

class DataCleaner:
    def __init__(self):
        self.removed_segments = []
        self.anomaly_count = 0
        self.missing_before = 0
        self.missing_after = 0

    def clean(self, df, resample_interval="1min", outlier_methods=None,
              missing_strategy="interpolate", op_strategy="ffill", pv_strategy="linear"):
        if outlier_methods is None: outlier_methods = ["hampel", "rate_limit", "stuck"]
        df = df.copy()
        var_cols = [c for c in df.columns if c != "time"]
        self.missing_before = int(df[var_cols].isna().sum().sum())
        df = df.drop_duplicates(subset="time").sort_values("time")
        df = self._resample(df, resample_interval)
        df = self._fill_missing(df, var_cols, missing_strategy)
        df = self._detect_and_repair_outliers(df, var_cols, outlier_methods)
        self.missing_after = int(df[var_cols].isna().sum().sum())
        return df, {"rows_after_clean": len(df), "missing_before": self.missing_before,
            "missing_after": self.missing_after, "anomaly_count": self.anomaly_count,
            "methods_used": outlier_methods}

    def _resample(self, df, interval):
        df = df.set_index("time")
        agg_dict = {col: "mean" for col in df.columns}
        resampled = df.resample(interval).agg(agg_dict)
        for col in resampled.columns:
            if col.endswith(".OP") or col.endswith(".SP"):
                resampled[col] = resampled[col].ffill()
        return resampled.reset_index()

    def _fill_missing(self, df, var_cols, strategy):
        for col in var_cols:
            if strategy == "interpolate":
                df[col] = df[col].interpolate(method="linear", limit=30)
            elif strategy == "ffill":
                df[col] = df[col].ffill().bfill()
        return df

    def _detect_and_repair_outliers(self, df, var_cols, methods):
        total_mask = pd.Series(False, index=df.index)
        for col in var_cols:
            mask = pd.Series(False, index=df.index)
            if "hampel" in methods: mask |= self._hampel_filter(df[col])
            if "rate_limit" in methods: mask |= self._rate_limit_filter(df[col])
            if "stuck" in methods: mask |= self._stuck_filter(df[col])
            if mask.any():
                total_mask |= mask
                valid = df[col].where(~mask)
                df[col] = valid.interpolate(method="linear").ffill().bfill()
        self.anomaly_count = int(total_mask.sum())
        return df

    def _hampel_filter(self, series):
        window, threshold = cfg.hampel_window, cfg.hampel_threshold
        clean = series.dropna()
        if len(clean) < window: return pd.Series(False, index=series.index)
        rolling_median = clean.rolling(window=window, center=True, min_periods=3).median()
        abs_dev = (clean - rolling_median).abs()
        mad = abs_dev.rolling(window=window, center=True, min_periods=3).median()
        outlier = abs_dev / (mad * 1.4826 + 1e-10) > threshold
        return outlier.reindex(series.index, fill_value=False)

    def _rate_limit_filter(self, series):
        if len(series) < 10: return pd.Series(False, index=series.index)
        diffs = series.diff().abs(); q99 = diffs.quantile(0.99)
        if q99 == 0: return pd.Series(False, index=series.index)
        return diffs > q99 * cfg.rate_limit_alpha

    def _stuck_filter(self, series):
        n = cfg.stuck_window
        if len(series) < n: return pd.Series(False, index=series.index)
        rolling_std = series.rolling(window=n, min_periods=n).std()
        stuck = rolling_std < cfg.stuck_epsilon
        result = pd.Series(False, index=series.index)
        if stuck.any():
            for idx in stuck[stuck].index:
                loc = series.index.get_loc(idx)
                result.iloc[max(0, loc-n+1):loc+1] = True
        return result

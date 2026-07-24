
"""数据清洗引擎 (Hampel + 孤立森林 + 卡死 + 边界)"""
import pandas as pd
import numpy as np
from typing import List, Tuple
from app.config import config as cfg

class DataCleaner:
    def __init__(self):
        self.anomaly_count = 0
        self.missing_before = 0
        self.missing_after = 0

    def clean(self, df, resample_interval="1min", outlier_methods=None,
              missing_strategy="interpolate", bounds=None):
        if outlier_methods is None:
            outlier_methods = ["hampel", "isolation_forest", "rate_limit", "stuck"]
        df = df.copy()
        var_cols = [c for c in df.columns if c != "time"]
        self.missing_before = int(df[var_cols].isna().sum().sum())
        df = df.drop_duplicates(subset="time").sort_values("time")
        df = self._resample(df, resample_interval)
        df = self._fill_missing(df, var_cols, missing_strategy)
        df = self._detect_outliers(df, var_cols, outlier_methods, bounds)
        self.missing_after = int(df[var_cols].isna().sum().sum())
        return df, {"rows_after_clean": len(df), "missing_before": self.missing_before,
                    "missing_after": self.missing_after, "anomaly_count": self.anomaly_count,
                    "methods_used": outlier_methods}

    def _resample(self, df, interval):
        df = df.set_index("time")
        resampled = df.resample(interval).mean()
        for col in resampled.columns:
            if col.endswith(".OP") or col.endswith(".SP"):
                resampled[col] = resampled[col].ffill()
        return resampled.reset_index()

    def _fill_missing(self, df, var_cols, strategy):
        for col in var_cols:
            if strategy == "interpolate":
                df[col] = df[col].interpolate(method="linear", limit=30)
            else:
                df[col] = df[col].ffill().bfill()
        return df

    def _detect_outliers(self, df, var_cols, methods, bounds):
        total_mask = pd.Series(False, index=df.index)
        for col in var_cols:
            mask = pd.Series(False, index=df.index)
            if "hampel" in methods:
                mask |= self._hampel(df[col])
            if "isolation_forest" in methods and len(df) > 50:
                mask |= self._iforest(df, col)
            if "rate_limit" in methods:
                mask |= self._rate_limit(df[col])
            if "stuck" in methods:
                mask |= self._stuck(df[col])
            if bounds and col in bounds:
                lo, hi = bounds[col]
                mask |= (df[col] < lo) | (df[col] > hi)
            if mask.any():
                total_mask |= mask
                df[col] = df[col].where(~mask).interpolate(method="linear").ffill().bfill()
        self.anomaly_count = int(total_mask.sum())
        return df

    def _hampel(self, series):
        w, th = cfg.hampel_window, cfg.hampel_threshold
        clean = series.dropna()
        if len(clean) < w: return pd.Series(False, index=series.index)
        med = clean.rolling(w, center=True, min_periods=3).median()
        mad = (clean - med).abs().rolling(w, center=True, min_periods=3).median()
        out = (clean - med).abs() / (mad * 1.4826 + 1e-10) > th
        return out.reindex(series.index, fill_value=False)

    def _iforest(self, df, col):
        try:
            from sklearn.ensemble import IsolationForest
            vals = df[[col]].ffill().bfill().values
            clf = IsolationForest(contamination=0.01, random_state=42, n_jobs=-1)
            preds = clf.fit_predict(vals)
            return pd.Series(preds == -1, index=df.index)
        except:
            return pd.Series(False, index=df.index)

    def _rate_limit(self, series):
        if len(series) < 10: return pd.Series(False, index=series.index)
        diffs = series.diff().abs()
        q99 = diffs.quantile(0.99)
        return diffs > q99 * cfg.rate_limit_alpha if q99 > 0 else pd.Series(False, index=series.index)

    def _stuck(self, series):
        n = cfg.stuck_window
        if len(series) < n: return pd.Series(False, index=series.index)
        std = series.rolling(n, min_periods=n).std()
        stuck = std < cfg.stuck_epsilon
        result = pd.Series(False, index=series.index)
        if stuck.any():
            for idx in stuck[stuck].index:
                loc = series.index.get_loc(idx)
                result.iloc[max(0, loc-n+1):loc+1] = True
        return result

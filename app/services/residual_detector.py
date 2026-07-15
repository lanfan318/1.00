"""残差预警检测器"""
import numpy as np, pandas as pd
from typing import Dict, Tuple
from dataclasses import dataclass

@dataclass
class AlarmResult:
    tag_name: str; current_value: float; predicted_value: float; residual: float
    baseline_mean: float; baseline_std: float; threshold_upper: float
    threshold_lower: float; is_alarm: bool; alarm_level: str; alarm_message: str

class ResidualDetector:
    def __init__(self, window_size=100, sigma_threshold=3.0):
        self.window_size = window_size; self.sigma_threshold = sigma_threshold

    def fit_baseline(self, series):
        clean = series.dropna()
        if len(clean) < self.window_size: raise ValueError(f"Not enough data: {len(clean)}")
        baseline = clean.iloc[-self.window_size:]
        return {"mean": float(baseline.mean()), "std": float(baseline.std()),
            "q05": float(baseline.quantile(0.05)), "q95": float(baseline.quantile(0.95)),
            "min_val": float(baseline.min()), "max_val": float(baseline.max()),
            "n_points": len(baseline)}

    def predict(self, series, baseline=None):
        clean = series.dropna()
        if baseline is None: baseline = self.fit_baseline(series)
        if len(clean) < 10:
            return baseline["mean"], baseline["mean"] + 3*baseline["std"], baseline["mean"] - 3*baseline["std"]
        recent = clean.iloc[-10:]
        trend = (recent.iloc[-1] - recent.iloc[0]) / len(recent)
        predicted = baseline["mean"] + trend * 5
        ts = self.sigma_threshold * baseline["std"]
        return predicted, predicted + ts, predicted - ts

    def check(self, current_value, series, tag_name="", baseline=None):
        if baseline is None: baseline = self.fit_baseline(series)
        predicted, upper, lower = self.predict(series, baseline)
        residual = current_value - predicted
        if abs(residual) > 5 * baseline["std"]:
            level, msg = "alarm", f"严重偏离:残差={residual:.4f}"
        elif abs(residual) > self.sigma_threshold * baseline["std"]:
            level, msg = "warning", f"异常偏离:残差={residual:.4f}"
        else:
            level, msg = "normal", "正常"
        return AlarmResult(tag_name=tag_name, current_value=round(current_value, 6),
            predicted_value=round(predicted, 6), residual=round(residual, 6),
            baseline_mean=round(baseline["mean"], 6), baseline_std=round(baseline["std"], 6),
            threshold_upper=round(upper, 6), threshold_lower=round(lower, 6),
            is_alarm=(level != "normal"), alarm_level=level, alarm_message=msg)

    @staticmethod
    def compute_residual_statistics(series, window_size=100):
        df = pd.DataFrame({"value": series.dropna()})
        df["predicted"] = df["value"].rolling(window=window_size, min_periods=10).mean()
        df["residual"] = df["value"] - df["predicted"]
        rolling_std = df["value"].rolling(window=window_size, min_periods=10).std()
        df["upper_3sigma"] = df["predicted"] + 3 * rolling_std
        df["lower_3sigma"] = df["predicted"] - 3 * rolling_std
        df["is_anomaly"] = (df["value"] > df["upper_3sigma"]) | (df["value"] < df["lower_3sigma"])
        return df

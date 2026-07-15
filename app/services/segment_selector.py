"""有效建模片段筛选"""
import pandas as pd
import numpy as np
from typing import List, Dict, Tuple
from dataclasses import dataclass

@dataclass
class Segment:
    start_time: pd.Timestamp
    end_time: pd.Timestamp
    input_var: str
    output_var: str
    quality_score: float
    snr_score: float
    dynamic_score: float
    n_points: int

class SegmentSelector:

    def __init__(self, pre_window: int = 30, post_window: int = 240):
        self.pre_window = pre_window
        self.post_window = post_window

    def detect_steps(self, series: pd.Series,
                     threshold_multiplier: float = 3.0,
                     min_step: float = 0.1) -> List[int]:
        clean = series.dropna()
        if len(clean) < 10:
            return []
        diffs = clean.diff().dropna()
        std_diff = diffs.std()
        threshold = max(threshold_multiplier * std_diff, min_step)
        step_indices = diffs[abs(diffs) > threshold].index.tolist()
        return [clean.index.get_loc(i) for i in step_indices]

    def select_segments(self, df: pd.DataFrame,
                        target: str,
                        input_candidates: List[str],
                        min_score: float = 60.0,
                        max_segments: int = 10
                        ) -> List[Segment]:
        segments = []
        for inp in input_candidates:
            if inp not in df.columns:
                continue
            input_series = df[inp]
            step_indices = self.detect_steps(input_series)
            for idx in step_indices:
                start = max(0, idx - self.pre_window)
                end = min(len(df), idx + self.post_window)
                if end - start < 30:
                    continue
                seg_df = df.iloc[start:end]
                score = self._compute_quality_score(seg_df, inp, target)
                if score >= min_score:
                    segments.append(Segment(
                        start_time=df.iloc[start]["time"],
                        end_time=df.iloc[end-1]["time"],
                        input_var=inp,
                        output_var=target,
                        quality_score=score,
                        snr_score=self._compute_snr(seg_df[target]),
                        dynamic_score=self._compute_dynamic_score(seg_df, inp, target),
                        n_points=end - start
                    ))
        segments.sort(key=lambda s: s.quality_score, reverse=True)
        return segments[:max_segments]

    def _compute_snr(self, series: pd.Series) -> float:
        clean = series.dropna()
        if len(clean) < 10:
            return 0
        smoothed = clean.rolling(window=5, center=True, min_periods=1).mean()
        signal_var = smoothed.var()
        noise_var = (clean - smoothed).var()
        if noise_var < 1e-10:
            return 100.0
        snr = signal_var / noise_var
        return min(100.0, float(snr * 10))

    def _compute_dynamic_score(self, df: pd.DataFrame,
                                input_col: str, output_col: str) -> float:
        u_score = min(1.0, df[input_col].std() / (df[input_col].mean() + 1e-10)) * 40
        y_score = min(1.0, df[output_col].std() / (df[output_col].mean() + 1e-10)) * 30
        u_clean = df[input_col].dropna()
        y_clean = df[output_col].dropna()
        common_idx = u_clean.index.intersection(y_clean.index)
        if len(common_idx) > 10:
            corr = abs(u_clean[common_idx].corr(y_clean[common_idx]))
        else:
            corr = 0
        corr_score = corr * 30
        return min(100.0, u_score + y_score + corr_score)

    def _compute_quality_score(self, df: pd.DataFrame,
                                input_col: str, output_col: str) -> float:
        snr = self._compute_snr(df[output_col])
        dynamic = self._compute_dynamic_score(df, input_col, output_col)
        missing_rate = df[output_col].isna().mean() + df[input_col].isna().mean()
        penalty = missing_rate * 100
        score = 0.3 * snr + 0.5 * dynamic + 0.2 * (100 - penalty)
        return max(0.0, min(100.0, score))

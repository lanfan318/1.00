"""时滞估计服务"""
import numpy as np
import pandas as pd
from typing import List, Dict, Tuple

class DelayEstimator:

    @staticmethod
    def cross_correlation_delay(u: pd.Series, y: pd.Series,
                                 max_delay: int = 60) -> Tuple[int, float]:
        u_clean = u.dropna().values
        y_clean = y.dropna().values
        if len(u_clean) < 2 * max_delay or len(y_clean) < 2 * max_delay:
            return 0, 0.0
        best_delay = 0
        best_corr = -1.0
        for d in range(max_delay + 1):
            u_shifted = u_clean[d:] if d > 0 else u_clean
            y_aligned = y_clean[:len(u_shifted)]
            if len(y_aligned) < 10:
                continue
            corr = abs(np.corrcoef(u_shifted, y_aligned)[0, 1])
            if corr > best_corr:
                best_corr = corr
                best_delay = d
        return best_delay, float(best_corr)

    @staticmethod
    def estimate_delay_matrix(df: pd.DataFrame,
                               inputs: List[str],
                               outputs: List[str],
                               max_delay: int = 60
                               ) -> List[Dict]:
        results = []
        for out in outputs:
            if out not in df.columns:
                continue
            for inp in inputs:
                if inp not in df.columns:
                    continue
                delay, corr = DelayEstimator.cross_correlation_delay(
                    df[inp], df[out], max_delay
                )
                results.append({
                    "input": inp,
                    "output": out,
                    "delay_points": delay,
                    "delay_seconds": delay * 60,
                    "correlation": round(corr, 4)
                })
        results.sort(key=lambda x: x["delay_points"])
        return results

    @staticmethod
    def compensate_delay(df: pd.DataFrame, delays: List[Dict]) -> pd.DataFrame:
        df_comp = df.copy()
        for d in delays:
            inp = d["input"]
            delay = d["delay_points"]
            if delay > 0 and inp in df_comp.columns:
                df_comp[f"{inp}_aligned"] = df_comp[inp].shift(-delay)
        return df_comp

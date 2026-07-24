
"""时滞估计: 互相关 + ARX网格搜索双重校验"""
import numpy as np
import pandas as pd
from typing import List, Dict, Tuple

class DelayEstimator:

    @staticmethod
    def cross_correlation_delay(u: pd.Series, y: pd.Series, max_delay: int = 60) -> Tuple[int, float]:
        uc, yc = u.dropna().values, y.dropna().values
        if len(uc) < 2*max_delay or len(yc) < 2*max_delay:
            return 0, 0.0
        best_d, best_c = 0, -1.0
        for d in range(max_delay + 1):
            us = uc[d:] if d > 0 else uc
            ya = yc[:len(us)]
            if len(ya) < 10: continue
            corr = abs(np.corrcoef(us, ya)[0, 1])
            if corr > best_c:
                best_c, best_d = corr, d
        return best_d, float(best_c)

    @staticmethod
    def arx_grid_search_delay(u: pd.Series, y: pd.Series, max_delay: int = 60) -> Tuple[int, float]:
        """ARX网格搜索: 以拟合度最大选择时滞"""
        from app.services.arx_modeler import ARXModeler
        uc, yc = u.dropna().values, y.dropna().values
        if len(uc) < max_delay + 20:
            return 0, 0.0
        best_d, best_fit = 0, -999.0
        for d in range(max_delay + 1):
            try:
                y_use = yc[d:]
                u_use = uc[:len(y_use)]
                if len(y_use) < 20: continue
                model = ARXModeler(na=1, nb=1)
                model.fit(y_use, u_use.reshape(-1, 1), [0])
                yp = model.predict(y_use, u_use.reshape(-1, 1), [0])
                fit = model.compute_fit(y_use, yp)
                if fit > best_fit:
                    best_fit, best_d = fit, d
            except:
                continue
        return best_d, float(max(-999, best_fit))

    @staticmethod
    def estimate_delay_matrix(df, inputs, outputs, max_delay=60) -> List[Dict]:
        results = []
        for out in outputs:
            if out not in df.columns: continue
            for inp in inputs:
                if inp not in df.columns: continue
                d1, c1 = DelayEstimator.cross_correlation_delay(df[inp], df[out], max_delay)
                d2, c2 = DelayEstimator.arx_grid_search_delay(df[inp], df[out], max_delay)
                # 双重校验: 两者一致则采信, 否则取互相关结果
                final_d = d1 if abs(d1 - d2) <= 5 else d1
                confidence = "high" if abs(d1 - d2) <= 5 else "medium"
                results.append({
                    "input": inp, "output": out,
                    "delay_points": final_d,
                    "delay_seconds": final_d * 60,
                    "ccf_delay": d1, "ccf_corr": round(c1, 4),
                    "arx_delay": d2, "arx_fit": round(c2, 2),
                    "confidence": confidence
                })
        results.sort(key=lambda x: x["delay_points"])
        return results

    @staticmethod
    def compensate_delay(df, delays):
        df_c = df.copy()
        for d in delays:
            inp, delay = d["input"], d["delay_points"]
            if delay > 0 and inp in df_c.columns:
                df_c[f"{inp}_aligned"] = df_c[inp].shift(-delay)
        return df_c

"""FOPDT仿真数据生成器"""
import numpy as np, pandas as pd
from typing import List, Dict, Tuple
from datetime import datetime, timedelta

class FOPDTSimulator:
    def __init__(self, seed=42):
        self.rng = np.random.RandomState(seed)

    def generate_single_loop(self, n_points, K=1.0, tau=30.0, theta=5.0,
                              Ts=1.0, noise_std=0.02, step_times=None,
                              step_magnitudes=None, initial_op=50.0, initial_pv=50.0):
        a = float(np.exp(-Ts / max(tau, 1e-6)))
        b = float(K * (1 - a))
        d_steps = max(1, int(theta / max(Ts, 1e-6)))
        OP = np.full(n_points, float(initial_op))
        SP = np.full(n_points, float(initial_pv))
        PV = np.full(n_points, float(initial_pv))
        if step_times and step_magnitudes:
            for t, mag in zip(step_times, step_magnitudes):
                idx = int(float(t) / Ts)
                if 0 <= idx < n_points:
                    OP[idx:] += float(mag)
                    SP[idx:] += float(mag) * 0.8
        OP += self.rng.normal(0, max(noise_std, 0.001) * 0.5, n_points)
        for t in range(1, n_points):
            op_idx = max(0, t - d_steps)
            PV[t] = float(a * PV[t-1] + b * OP[op_idx] + self.rng.normal(0, max(noise_std, 0.001)))
        # 确保没有 NaN/Inf
        OP = np.where(np.isfinite(OP), OP, float(initial_op))
        SP = np.where(np.isfinite(SP), SP, float(initial_pv))
        PV = np.where(np.isfinite(PV), PV, float(initial_pv))
        return OP, SP, PV

    def generate_multivariable(self, duration_days=7, Ts_min=1.0, tags_config=None):
        n_points = int(duration_days * 24 * 60 / Ts_min)
        if tags_config is None:
            tags_config = [
                {"tag": "FC101", "K": 1.0, "tau": 30.0, "theta": 4.0, "steps": [(1440, 12), (4320, -8)]},
                {"tag": "FC102", "K": 0.7, "tau": 45.0, "theta": 8.0, "steps": [(2880, -15), (5760, 10)]},
            ]
        ts = datetime(2026, 5, 13, 0, 0, 0)
        timestamps = [ts + timedelta(minutes=i * Ts_min) for i in range(n_points)]
        result = {"time": timestamps}
        for cfg in tags_config:
            tag = cfg["tag"]
            steps = cfg.get("steps", [])
            OP, SP, PV = self.generate_single_loop(
                n_points=n_points, K=float(cfg.get("K", 1.0)),
                tau=float(cfg.get("tau", 30.0)), theta=float(cfg.get("theta", 5.0)),
                Ts=Ts_min, noise_std=float(cfg.get("noise", 0.015)),
                step_times=[float(s[0]) for s in steps],
                step_magnitudes=[float(s[1]) for s in steps])
            result[f"{tag}.OP"] = OP
            result[f"{tag}.PV"] = PV
            result[f"{tag}.SP"] = SP
        df = pd.DataFrame(result)
        return df

    def inject_anomalies(self, df, columns=None, spike_rate=0.002, missing_rate=0.01):
        df = df.copy()
        if columns is None:
            columns = [c for c in df.columns if c != "time"]
        n = len(df)
        for col in columns:
            std_val = float(df[col].std())
            if not np.isfinite(std_val) or std_val <= 0:
                std_val = 1.0
            n_spikes = max(1, int(n * spike_rate))
            idxs = self.rng.choice(n, n_spikes, replace=False)
            df.loc[idxs, col] = df.loc[idxs, col].values + self.rng.normal(0, std_val * 5, n_spikes)
            n_miss = max(1, int(n * missing_rate))
            idxs2 = self.rng.choice(n, n_miss, replace=False)
            df.loc[idxs2, col] = np.nan
        for col in columns:
            df[col] = df[col].ffill().bfill().fillna(df[col].mean() if df[col].notna().any() else 0)
        return df

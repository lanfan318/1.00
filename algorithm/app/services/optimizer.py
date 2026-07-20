"""闭环寻优引擎"""
import numpy as np, pandas as pd, random, traceback
from typing import List, Dict, Optional
from dataclasses import dataclass, field
from app.services.arx_modeler import ARXModeler
from app.services.delay_estimator import DelayEstimator
from app.services.variable_selector import VariableSelector
from app.config import config as cfg

@dataclass
class OptimizationResult:
    target: str; selected_inputs: List[str]; delays: List[Dict]
    na: int; nb: int; train_fit: float; test_fit: float
    rmse: float; r2: float; whiteness: float; objective: float
    config: Dict = field(default_factory=dict)

class Optimizer:
    def __init__(self):
        self.best_result = None; self.best_objective = -float("inf")
        self.trials_history = []; self.errors = []

    def optimize(self, df, target, candidate_inputs=None, n_trials=None):
        if n_trials is None: n_trials = cfg.random_search_trials
        if candidate_inputs is None:
            candidate_inputs = [c for c in df.columns if c != "time" and c != target]
        self.best_result = None; self.best_objective = -float("inf")
        self.trials_history = []; self.errors = []
        no_improvement = 0

        for trial in range(n_trials):
            config = {
                "na": random.choice([1, 2]),
                "nb": random.choice([1, 2]),
                "max_delay": random.choice([10, 30, 60]),
                "max_features": random.choice([2, 3, 5, 8]),
                "test_ratio": random.uniform(0.2, 0.4)
            }
            try:
                result = self._evaluate_config(df, target, candidate_inputs, config)
                self.trials_history.append({"trial": trial, "objective": result.objective, "config": config})
                if result.objective > self.best_objective:
                    self.best_objective = result.objective
                    self.best_result = result
                    no_improvement = 0
                else:
                    no_improvement += 1
                if no_improvement >= cfg.early_stop_patience:
                    break
            except Exception as e:
                self.errors.append(f"Trial {trial}: {e}")
                continue

        if self.best_result is None:
            fallback = {"na": 1, "nb": 1, "max_delay": 10, "max_features": 3, "test_ratio": 0.3}
            try:
                self.best_result = self._evaluate_config(df, target, candidate_inputs, fallback)
            except Exception as e:
                raise ValueError(f"All {n_trials} failed. Last: {'; '.join(self.errors[-2:])}")

        return self.best_result

    def _evaluate_config(self, df, target, candidate_inputs, config):
        selected_inputs, _ = VariableSelector.select_variables(
            df, target, candidate_inputs, min(config["max_features"], len(candidate_inputs))
        )
        if len(selected_inputs) == 0:
            other_cols = [c for c in df.columns if c != "time" and c != target]
            corrs = []
            for c in other_cols:
                common = df[[c, target]].dropna()
                if len(common) > 10:
                    corrs.append((c, abs(common[c].corr(common[target]))))
            corrs.sort(key=lambda x: x[1], reverse=True)
            selected_inputs = [c for c, _ in corrs[:config["max_features"]]]
            if len(selected_inputs) == 0:
                raise ValueError("No correlated inputs")

        delay_results = DelayEstimator.estimate_delay_matrix(
            df, selected_inputs, [target], config["max_delay"]
        )
        delays = [max(0, min(d["delay_points"], config["max_delay"])) for d in delay_results]

        y_data = df[target].dropna().values.astype(float)
        U_list = [df[inp].ffill().bfill().fillna(0).values.astype(float) for inp in selected_inputs]
        U_data = np.column_stack(U_list)

        n = len(y_data)
        split_idx = max(20, int(n * (1 - config["test_ratio"])))
        y_train, U_train = y_data[:split_idx], U_data[:split_idx]
        y_test, U_test = y_data[split_idx:], U_data[split_idx:]

        na = min(config["na"], 2)
        nb = min(config["nb"], 2)
        model = ARXModeler(na=na, nb=nb)
        model.fit(y_train, U_train, delays)

        y_train_pred = model.predict(y_train, U_train, delays)
        y_test_pred = model.predict(y_test, U_test, delays)

        # ===== 关键修复: 对齐预测和真实值长度 =====
        min_train = min(len(y_train), len(y_train_pred))
        y_train_a = y_train[-min_train:]
        y_train_pa = y_train_pred[-min_train:]

        min_test = min(len(y_test), len(y_test_pred))
        y_test_a = y_test[-min_test:]
        y_test_pa = y_test_pred[-min_test:]

        train_fit = model.compute_fit(y_train_a, y_train_pa)
        test_fit = model.compute_fit(y_test_a, y_test_pa)
        rmse = model.compute_rmse(y_test_a, y_test_pa)
        r2 = model.compute_r2(y_test_a, y_test_pa)

        residuals = y_test_a - y_test_pa
        whiteness = model.whiteness_test(residuals)

        if not np.isfinite(test_fit): test_fit = 0.0
        if not np.isfinite(train_fit): train_fit = 0.0

        objective = (
            0.60 * max(0, test_fit) / 100.0 +
            0.15 * max(0, 1 - whiteness) +
            0.10 * min(1.0, max(0, test_fit) / (max(0, train_fit) + 1e-10)) +
            0.10 / max(1, len(selected_inputs)) +
            0.05 * min(1.0, 1 - df[target].isna().mean())
        )

        return OptimizationResult(
            target=target, selected_inputs=selected_inputs,
            delays=delay_results, na=na, nb=nb,
            train_fit=round(train_fit, 2), test_fit=round(test_fit, 2),
            rmse=round(float(rmse), 6), r2=round(float(r2), 4),
            whiteness=round(float(whiteness), 4),
            objective=round(float(objective), 4), config=config
        )

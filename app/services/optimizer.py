
"""贝叶斯闭环寻优 (Optuna)"""
import numpy as np
import pandas as pd
from typing import List, Dict, Optional, Any
from dataclasses import dataclass, field
from app.services.arx_modeler import ARXModeler
from app.services.subspace_id import SubspaceID
from app.services.delay_estimator import DelayEstimator
from app.services.variable_selector import VariableSelector
from app.config import config as cfg

@dataclass
class OptimizationResult:
    target: str
    best_algorithm: str
    selected_inputs: List[str]
    delays: List[Dict]
    train_fit: float
    test_fit: float
    rmse: float
    r2: float
    whiteness: float
    objective: float
    model_params: Dict[str, Any] = field(default_factory=dict)

class Optimizer:
    """支持 Optuna 和随机搜索双模式"""

    def __init__(self, use_optuna: bool = True):
        self.use_optuna = use_optuna
        self.best_result: Optional[OptimizationResult] = None
        self.trials_history: List[Dict] = []

    def optimize(self, df: pd.DataFrame, target: str,
                 candidate_inputs: List[str] = None,
                 n_trials: int = None) -> OptimizationResult:
        if n_trials is None:
            n_trials = cfg.random_search_trials
        if candidate_inputs is None:
            candidate_inputs = [c for c in df.columns if c != "time" and c != target]
        if len(candidate_inputs) == 0:
            raise ValueError("无候选输入变量")

        self.trials_history = []

        if self.use_optuna:
            try:
                import optuna
                optuna.logging.set_verbosity(optuna.logging.WARNING)
                return self._optuna_optimize(df, target, candidate_inputs, n_trials)
            except ImportError:
                print("Optuna 未安装，降级为随机搜索")

        return self._random_optimize(df, target, candidate_inputs, n_trials)

    def _optuna_optimize(self, df, target, inputs, n_trials):
        import optuna

        def objective(trial):
            na = trial.suggest_int("na", 1, 3)
            nb = trial.suggest_int("nb", 1, 3)
            max_delay = trial.suggest_int("max_delay", 10, 60)
            max_features = trial.suggest_int("max_features", 2, min(8, len(inputs)))
            test_ratio = trial.suggest_float("test_ratio", 0.2, 0.4)
            algo = trial.suggest_categorical("algorithm", ["ARX", "N4SID"])

            config = {"na": na, "nb": nb, "max_delay": max_delay,
                      "max_features": max_features, "test_ratio": test_ratio,
                      "algorithm": algo}
            try:
                result = self._evaluate_config(df, target, inputs, config)
                self.trials_history.append({"objective": result.objective, "config": config})
                return result.objective
            except Exception:
                return -999.0

        study = optuna.create_study(direction="maximize")
        study.optimize(objective, n_trials=n_trials, show_progress_bar=False)

        best_config = {
            "na": study.best_params["na"],
            "nb": study.best_params["nb"],
            "max_delay": study.best_params["max_delay"],
            "max_features": study.best_params["max_features"],
            "test_ratio": study.best_params["test_ratio"],
            "algorithm": study.best_params["algorithm"]
        }
        return self._evaluate_config(df, target, inputs, best_config)

    def _random_optimize(self, df, target, inputs, n_trials):
        import random
        self.best_result = None
        best_obj = -float("inf")
        no_imp = 0
        for trial in range(n_trials):
            config = {
                "na": random.choice([1, 2, 3]),
                "nb": random.choice([1, 2, 3]),
                "max_delay": random.choice([10, 30, 60]),
                "max_features": random.choice([2, 3, 5]),
                "test_ratio": random.uniform(0.2, 0.4),
                "algorithm": random.choice(["ARX", "N4SID"])
            }
            try:
                result = self._evaluate_config(df, target, inputs, config)
                self.trials_history.append({"trial": trial, "objective": result.objective, "config": config})
                if result.objective > best_obj:
                    best_obj = result.objective
                    self.best_result = result
                    no_imp = 0
                else:
                    no_imp += 1
                if no_imp >= cfg.early_stop_patience:
                    break
            except Exception:
                continue
        if self.best_result is None:
            fallback = {"na": 1, "nb": 1, "max_delay": 10, "max_features": 3,
                        "test_ratio": 0.3, "algorithm": "ARX"}
            self.best_result = self._evaluate_config(df, target, inputs, fallback)
        return self.best_result

    def _evaluate_config(self, df, target, inputs, config):
        selected, _ = VariableSelector.select_variables(df, target, inputs, config["max_features"])
        if len(selected) == 0:
            corrs = [(c, abs(df[[c, target]].dropna().pipe(lambda d: d[c].corr(d[target]))))
                     for c in inputs if c in df.columns]
            selected = [c for c, _ in sorted(corrs, key=lambda x: x[1], reverse=True)[:config["max_features"]]]

        delays_result = DelayEstimator.estimate_delay_matrix(df, selected, [target], config["max_delay"])
        delays = [max(0, min(d["delay_points"], config["max_delay"])) for d in delays_result]

        y_data = df[target].dropna().values.astype(float)
        U_data = np.column_stack([df[c].ffill().bfill().fillna(0).values.astype(float) for c in selected])

        n = len(y_data)
        split = max(20, int(n * (1 - config["test_ratio"])))
        y_tr, U_tr = y_data[:split], U_data[:split]
        y_te, U_te = y_data[split:], U_data[split:]

        if config["algorithm"] == "N4SID":
            model = SubspaceID(order=min(config["na"], 3))
            model.fit(y_tr, U_tr)
            y_tr_pred = model.predict(y_tr, U_tr)
            y_te_pred = model.predict(y_te, U_te)
            algo = "N4SID"
        else:
            model = ARXModeler(na=config["na"], nb=config["nb"])
            model.fit(y_tr, U_tr, delays)
            y_tr_pred = model.predict(y_tr, U_tr, delays)
            y_te_pred = model.predict(y_te, U_te, delays)
            algo = "ARX"

        m = min(len(y_te), len(y_te_pred))
        train_fit = model.compute_fit(y_tr[-m:], y_tr_pred[-m:]) if hasattr(model, 'compute_fit') else float(np.clip(100*(1-np.linalg.norm(y_tr[-m:]-y_tr_pred[-m:])/(np.linalg.norm(y_tr[-m:]-np.mean(y_tr[-m:]))+1e-10)), 0, 100))
        test_fit = model.compute_fit(y_te[:m], y_te_pred[:m]) if hasattr(model, 'compute_fit') else float(np.clip(100*(1-np.linalg.norm(y_te[:m]-y_te_pred[:m])/(np.linalg.norm(y_te[:m]-np.mean(y_te[:m]))+1e-10)), 0, 100))
        rmse = float(np.sqrt(np.mean((y_te[:m] - y_te_pred[:m]) ** 2)))
        r2 = float(1 - np.sum((y_te[:m]-y_te_pred[:m])**2) / (np.sum((y_te[:m]-np.mean(y_te[:m]))**2) + 1e-10))
        whiteness = float(np.mean([abs(np.corrcoef(y_te[:m-k], y_te[k:m])[0,1]) for k in range(1, min(11, m//2))])) if m > 11 else 0.0

        if not np.isfinite(test_fit): test_fit = 0.0
        if not np.isfinite(train_fit): train_fit = 0.0

        obj = 0.60 * max(0, test_fit) / 100.0 + 0.15 * max(0, 1 - whiteness) + \
              0.15 / max(1, len(selected)) + 0.10 * max(0, min(1, 1 - df[target].isna().mean()))

        return OptimizationResult(
            target=target, best_algorithm=algo, selected_inputs=selected,
            delays=delays_result,
            train_fit=round(train_fit, 2), test_fit=round(test_fit, 2),
            rmse=round(rmse, 6), r2=round(r2, 4), whiteness=round(whiteness, 4),
            objective=round(obj, 4),
            model_params={"na": config["na"], "nb": config["nb"], "algorithm": algo}
        )

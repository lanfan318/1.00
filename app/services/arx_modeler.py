"""ARX建模引擎"""
import numpy as np
import pandas as pd
from typing import List, Dict, Tuple, Optional
from scipy import linalg

class ARXModeler:

    def __init__(self, na: int = 2, nb: int = 2):
        self.na = na
        self.nb = nb
        self.coefficients: Optional[np.ndarray] = None
        self.fitted = False

    def build_matrix(self, y: np.ndarray, U: np.ndarray,
                     delays: List[int]) -> Tuple[np.ndarray, np.ndarray]:
        na = self.na
        nb = self.nb
        max_lag = max([na] + [delays[i] + nb for i in range(len(delays))])
        N = len(y)
        X_rows = []
        Y_rows = []
        for t in range(max_lag, N):
            row = []
            for i in range(1, na + 1):
                row.append(y[t - i] if t - i >= 0 else 0.0)
            for j in range(U.shape[1]):
                nk = delays[j]
                for k in range(nb):
                    idx = t - nk - k
                    row.append(U[idx, j] if idx >= 0 else 0.0)
            X_rows.append(row)
            Y_rows.append(y[t])
        return np.array(X_rows), np.array(Y_rows)

    def fit(self, y: np.ndarray, U: np.ndarray, delays: List[int]):
        X, Y = self.build_matrix(y, U, delays)
        if len(X) < 10:
            raise ValueError(f"Not enough data: {len(X)} samples")
        self.coefficients = linalg.lstsq(X, Y)[0]
        self.fitted = True
        return self

    def predict(self, y: np.ndarray, U: np.ndarray, delays: List[int]) -> np.ndarray:
        if not self.fitted or self.coefficients is None:
            raise ValueError("Model not fitted")
        X, _ = self.build_matrix(y, U, delays)
        return X @ self.coefficients

    def compute_fit(self, y_true: np.ndarray, y_pred: np.ndarray) -> float:
        numerator = np.linalg.norm(y_true - y_pred)
        denominator = np.linalg.norm(y_true - np.mean(y_true))
        if denominator < 1e-10:
            return 100.0
        fit = 100 * (1 - numerator / denominator)
        return float(max(0.0, min(100.0, fit)))

    @staticmethod
    def compute_rmse(y_true: np.ndarray, y_pred: np.ndarray) -> float:
        return float(np.sqrt(np.mean((y_true - y_pred) ** 2)))

    @staticmethod
    def compute_r2(y_true: np.ndarray, y_pred: np.ndarray) -> float:
        ss_res = np.sum((y_true - y_pred) ** 2)
        ss_tot = np.sum((y_true - np.mean(y_true)) ** 2)
        return float(1 - ss_res / (ss_tot + 1e-10))

    @staticmethod
    def whiteness_test(residuals: np.ndarray, max_lag: int = 10) -> float:
        if len(residuals) < max_lag + 1:
            return 0.0
        acf_vals = []
        for lag in range(1, max_lag + 1):
            if len(residuals) > lag:
                corr = np.corrcoef(residuals[:-lag], residuals[lag:])[0, 1]
                acf_vals.append(abs(corr))
        return float(np.mean(acf_vals)) if acf_vals else 0.0

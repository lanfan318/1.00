
"""变量选择: mRMR + Granger因果 + 时滞LASSO + VIF"""
import numpy as np
import pandas as pd
from typing import List, Tuple
from app.config import config as cfg

class VariableSelector:

    @staticmethod
    def mrmr_select(df: pd.DataFrame, X_cols: List[str], y_col: str, K: int = 8) -> List[str]:
        """mRMR: 最小冗余最大相关"""
        data = df[X_cols + [y_col]].dropna()
        if len(data) < 20 or len(X_cols) == 0:
            return X_cols[:K]

        y = data[y_col].values
        # 相关性矩阵
        corr_matrix = data.corr().abs().values
        y_idx = len(X_cols)
        n = len(X_cols)

        # 相关性得分: F(x_i, y)
        relevance = np.array([abs(np.corrcoef(data[c].values, y)[0, 1]) for c in X_cols])

        selected = []
        remaining = list(range(n))

        # 第一个特征: 与目标最相关
        first = int(np.argmax(relevance))
        selected.append(first)
        remaining.remove(first)

        for _ in range(K - 1):
            if not remaining:
                break
            scores = []
            for i in remaining:
                rel = relevance[i]
                red = np.mean([corr_matrix[i, j] for j in selected]) if selected else 0
                scores.append(rel - red)
            best = remaining[np.argmax(scores)]
            selected.append(best)
            remaining.remove(best)

        return [X_cols[i] for i in selected]

    @staticmethod
    def granger_test(df: pd.DataFrame, cause: str, effect: str, max_lag: int = 5, alpha: float = 0.05) -> bool:
        """Granger因果检验 (简化版)"""
        data = df[[cause, effect]].dropna()
        if len(data) < max_lag + 10:
            return False
        y = data[effect].values
        X = data[cause].values
        N = len(y)
        # 受限模型 (仅自回归)
        X_restricted = np.column_stack([y[max_lag-i-1:N-i-1] for i in range(max_lag)])
        y_restricted = y[max_lag:]
        beta_r = np.linalg.lstsq(X_restricted, y_restricted, rcond=None)[0]
        ssr_r = np.sum((y_restricted - X_restricted @ beta_r) ** 2)
        # 非受限模型 (加入 X)
        X_unrestricted = np.column_stack([X_restricted] + [X[max_lag-i-1:N-i-1] for i in range(max_lag)])
        beta_u = np.linalg.lstsq(X_unrestricted, y_restricted, rcond=None)[0]
        ssr_u = np.sum((y_restricted - X_unrestricted @ beta_u) ** 2)
        if ssr_u < 1e-10:
            return False
        F = ((ssr_r - ssr_u) / max_lag) / (ssr_u / (N - 2 * max_lag - 1))
        return F > 2.0  # 简化阈值

    @staticmethod
    def filter_low_variance(df, columns):
        return [c for c in columns if c in df.columns and df[c].var() > cfg.var_threshold]

    @staticmethod
    def filter_high_correlation(df, columns, target):
        kept = set(columns)
        for i, c1 in enumerate(columns):
            if c1 not in kept: continue
            for c2 in columns[i+1:]:
                if c2 not in kept: continue
                common = df[[c1, c2]].dropna()
                if len(common) < 10: continue
                corr = abs(common[c1].corr(common[c2]))
                if corr > cfg.corr_threshold:
                    corr1 = abs(df[c1].corr(df[target])) if target in df.columns else 0
                    corr2 = abs(df[c2].corr(df[target])) if target in df.columns else 0
                    kept.discard(c2 if corr1 >= corr2 else c1)
        return list(kept)

    @staticmethod
    def vif_filter(df, columns):
        kept = list(columns)
        changed = True
        while changed and len(kept) > 1:
            changed = False
            vif_max, vif_max_col = 0, None
            for col in kept:
                y = df[col].dropna()
                X = df[[c for c in kept if c != col]].dropna()
                idx = y.index.intersection(X.index)
                if len(idx) < 10: continue
                y, X = y[idx], X.loc[idx]
                try:
                    Xc = np.column_stack([np.ones(len(X)), X.values])
                    beta = np.linalg.lstsq(Xc, y.values, rcond=None)[0]
                    yp = Xc @ beta
                    r2 = 1 - np.sum((y.values-yp)**2) / (np.sum((y.values-y.mean())**2) + 1e-10)
                    vif = 1.0 / (1.0 - r2 + 1e-10)
                    if vif > vif_max:
                        vif_max, vif_max_col = vif, col
                except: continue
            if vif_max > cfg.vif_threshold and vif_max_col:
                kept.remove(vif_max_col)
                changed = True
        return kept

    @staticmethod
    def lasso_select(df, X_cols, y_col, max_features=8):
        data = df[X_cols + [y_col]].dropna()
        if len(data) < 20: return X_cols[:max_features]
        try:
            from sklearn.linear_model import LassoCV
            X, y = data[X_cols].values, data[y_col].values
            lasso = LassoCV(cv=min(5, len(data)//10), max_iter=5000, random_state=42)
            lasso.fit(X, y)
            imp = np.abs(lasso.coef_)
            return [X_cols[i] for i in np.argsort(imp)[::-1] if imp[i] > 1e-6][:max_features]
        except:
            return X_cols[:max_features]

    @staticmethod
    def select_variables(df, target, candidate_inputs, max_features=8):
        report = {"initial": len(candidate_inputs)}
        valid = [c for c in candidate_inputs if c in df.columns and df[c].isna().mean() < 0.2]
        report["after_missing"] = len(valid)

        kept = VariableSelector.filter_low_variance(df, valid)
        report["after_variance"] = len(kept)

        kept = VariableSelector.filter_high_correlation(df, kept, target)
        report["after_corr"] = len(kept)

        # mRMR
        kept = VariableSelector.mrmr_select(df, kept, target, min(max_features, len(kept)))
        report["after_mrmr"] = len(kept)

        # Granger
        granger_kept = [c for c in kept if VariableSelector.granger_test(df, c, target)]
        if len(granger_kept) >= 2:
            kept = granger_kept
        report["after_granger"] = len(kept)

        # VIF
        kept = VariableSelector.vif_filter(df, kept)
        report["after_vif"] = len(kept)

        # LASSO
        kept = VariableSelector.lasso_select(df, kept, target, max_features)
        report["after_lasso"] = len(kept)

        report["selected"] = kept
        return kept, report

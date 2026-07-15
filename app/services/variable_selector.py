"""变量选择与降维"""
import numpy as np
import pandas as pd
from typing import List, Tuple
from sklearn.linear_model import LassoCV
from app.config import config as cfg

class VariableSelector:

    @staticmethod
    def filter_low_variance(df: pd.DataFrame, columns: List[str]) -> List[str]:
        kept = []
        for col in columns:
            if col in df.columns and df[col].var() > cfg.var_threshold:
                kept.append(col)
        return kept

    @staticmethod
    def filter_high_correlation(df: pd.DataFrame, columns: List[str],
                                 target: str) -> List[str]:
        kept = set(columns)
        for i, col1 in enumerate(columns):
            if col1 not in kept:
                continue
            for col2 in columns[i+1:]:
                if col2 not in kept:
                    continue
                common = df[[col1, col2]].dropna()
                if len(common) < 10:
                    continue
                corr = abs(common[col1].corr(common[col2]))
                if corr > cfg.corr_threshold:
                    corr1 = abs(df[col1].corr(df[target])) if target in df.columns else 0
                    corr2 = abs(df[col2].corr(df[target])) if target in df.columns else 0
                    if corr1 >= corr2:
                        kept.discard(col2)
                    else:
                        kept.discard(col1)
        return list(kept)

    @staticmethod
    def vif_filter(df: pd.DataFrame, columns: List[str]) -> List[str]:
        kept = list(columns)
        changed = True
        while changed and len(kept) > 1:
            changed = False
            vif_max = 0
            vif_max_col = None
            for i, col in enumerate(kept):
                y = df[col].dropna()
                X = df[[c for c in kept if c != col]].dropna()
                common_idx = y.index.intersection(X.index)
                if len(common_idx) < 10:
                    continue
                y = y[common_idx]
                X = X.loc[common_idx]
                try:
                    X_with_const = np.column_stack([np.ones(len(X)), X.values])
                    beta = np.linalg.lstsq(X_with_const, y.values, rcond=None)[0]
                    y_pred = X_with_const @ beta
                    ss_res = np.sum((y.values - y_pred) ** 2)
                    ss_tot = np.sum((y.values - y.mean()) ** 2)
                    r2 = 1 - ss_res / (ss_tot + 1e-10)
                    vif = 1.0 / (1.0 - r2 + 1e-10)
                    if vif > vif_max:
                        vif_max = vif
                        vif_max_col = col
                except:
                    continue
            if vif_max > cfg.vif_threshold and vif_max_col:
                kept.remove(vif_max_col)
                changed = True
        return kept

    @staticmethod
    def lasso_select(df: pd.DataFrame, X_cols: List[str],
                      y_col: str, max_features: int = 8) -> List[str]:
        data = df[X_cols + [y_col]].dropna()
        if len(data) < 20 or len(X_cols) == 0:
            return X_cols[:max_features]
        X = data[X_cols].values
        y = data[y_col].values
        try:
            lasso = LassoCV(cv=min(5, len(data)//10), max_iter=5000, random_state=42)
            lasso.fit(X, y)
            importance = np.abs(lasso.coef_)
            sorted_idx = np.argsort(importance)[::-1]
            selected = [X_cols[i] for i in sorted_idx if importance[i] > 1e-6]
            return selected[:max_features]
        except:
            return X_cols[:max_features]

    @staticmethod
    def select_variables(df: pd.DataFrame,
                          target: str,
                          candidate_inputs: List[str],
                          max_features: int = 8) -> Tuple[List[str], dict]:
        report = {"initial_count": len(candidate_inputs)}
        valid_inputs = [c for c in candidate_inputs
                       if c in df.columns and df[c].isna().mean() < 0.2]
        report["after_missing_filter"] = len(valid_inputs)
        kept = VariableSelector.filter_low_variance(df, valid_inputs)
        report["after_variance_filter"] = len(kept)
        kept = VariableSelector.filter_high_correlation(df, kept, target)
        report["after_corr_filter"] = len(kept)
        kept = VariableSelector.vif_filter(df, kept)
        report["after_vif_filter"] = len(kept)
        kept = VariableSelector.lasso_select(df, kept, target, max_features)
        report["after_lasso"] = len(kept)
        report["selected_variables"] = kept
        return kept, report

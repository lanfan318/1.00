
"""卡尔曼滤波降噪"""
import numpy as np
from typing import Tuple

class KalmanFilter:
    """
    一维卡尔曼滤波器 (用于工业信号降噪)
    状态方程: x[t] = x[t-1] + w  (随机游走)
    观测方程: z[t] = x[t] + v
    """

    def __init__(self, process_noise: float = 0.01, measurement_noise: float = 1.0):
        self.Q = process_noise
        self.R = measurement_noise
        self.x = 0.0
        self.P = 1.0

    def update(self, z: float) -> Tuple[float, float]:
        """单步更新, 返回 (滤波值, 滤波方差)"""
        # 预测
        x_pred = self.x
        P_pred = self.P + self.Q
        # 更新
        K = P_pred / (P_pred + self.R + 1e-10)
        self.x = x_pred + K * (z - x_pred)
        self.P = (1 - K) * P_pred
        return self.x, self.P

    def smooth(self, signal: np.ndarray) -> np.ndarray:
        """对整个信号做前向滤波 + 后向平滑"""
        n = len(signal)
        # 前向滤波
        fwd = np.zeros(n)
        self.x = signal[0] if n > 0 else 0.0
        self.P = 1.0
        for i in range(n):
            fwd[i], _ = self.update(float(signal[i]))
        # 后向平滑 (Rauch-Tung-Striebel)
        bwd = fwd.copy()
        for i in range(n - 2, -1, -1):
            bwd[i] = bwd[i] + (self.Q / (self.P + self.Q)) * (bwd[i+1] - bwd[i])
        return bwd

    @staticmethod
    def estimate_noise(series: np.ndarray) -> Tuple[float, float]:
        """从数据估计过程噪声和观测噪声"""
        clean = series[~np.isnan(series)]
        if len(clean) < 10:
            return 0.01, 1.0
        diffs = np.diff(clean)
        Q = float(np.var(diffs) * 0.1)
        # 观测噪声: 移动窗口残差方差
        window = min(20, len(clean) // 5)
        smoothed = np.convolve(clean, np.ones(window)/window, mode='same')
        R = float(np.var(clean - smoothed))
        return max(Q, 1e-6), max(R, 1e-6)

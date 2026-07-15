"""全局配置"""
from dataclasses import dataclass, field
from typing import List

@dataclass
class AlgorithmConfig:
    default_sample_interval: str = "1min"
    hampel_window: int = 11
    hampel_threshold: float = 3.0
    stuck_window: int = 30
    stuck_epsilon: float = 1e-6
    rate_limit_alpha: float = 5.0
    pre_window_points: int = 30
    post_window_points: int = 240
    min_quality_score: float = 60.0
    max_delay_points: int = 60
    vif_threshold: float = 10.0
    corr_threshold: float = 0.98
    var_threshold: float = 1e-8
    na_candidates: List[int] = field(default_factory=lambda: [1,2,3,5])
    nb_candidates: List[int] = field(default_factory=lambda: [1,2,3,5])
    test_ratio: float = 0.3
    random_search_trials: int = 50
    early_stop_patience: int = 10
    residual_window: int = 100
    residual_threshold_sigma: float = 3.0

config = AlgorithmConfig()

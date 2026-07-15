"""CSV数据加载器：长表->宽表转换"""
import pandas as pd
import numpy as np
from pathlib import Path
from typing import Tuple, List, Optional
import re

class DataLoader:

    @staticmethod
    def parse_config_line(line: str) -> dict:
        parts = line.replace("#config,", "").split(",")
        return {
            "device": parts[0].strip(),
            "unit_id": parts[1].strip(),
            "part": parts[2].strip(),
            "time_start": parts[3].strip(),
            "time_end": parts[4].strip()
        }

    @staticmethod
    def parse_tag(tag: str) -> dict:
        parts = tag.rsplit(".", 1)
        if len(parts) == 2:
            return {"tag": parts[0], "var_type": parts[1]}
        return {"tag": tag, "var_type": "UNKNOWN"}

    @staticmethod
    def load_csv(filepath: str) -> Tuple[pd.DataFrame, dict, List[str]]:
        config_info = {}

        with open(filepath, 'r', encoding='utf-8') as f:
            first_line = f.readline().strip()

        if first_line.startswith("#config"):
            config_info = DataLoader.parse_config_line(first_line)

        # comment='#' 自动跳过 #config 行, time,nodeId,value 作为表头
        df = pd.read_csv(
            filepath,
            comment='#',
            dtype={"nodeId": str},
            low_memory=False
        )

        print(f"Loaded {len(df)} rows, columns: {list(df.columns)}")

        df["time"] = pd.to_datetime(df["time"], errors="coerce")
        df = df.dropna(subset=["time"])
        print(f"After time parse: {len(df)} rows")

        if len(df) == 0:
            raise ValueError("CSV解析后为空")

        unique_tags = df["nodeId"].unique().tolist()
        print(f"Tags ({len(unique_tags)}): {unique_tags[:5]}")

        wide_df = df.pivot_table(
            index="time",
            columns="nodeId",
            values="value",
            aggfunc="first"
        ).reset_index()

        wide_df = wide_df.sort_values("time").reset_index(drop=True)
        print(f"Wide: {len(wide_df)} rows x {len(wide_df.columns)} cols")
        return wide_df, config_info, unique_tags

    @staticmethod
    def load_multiple_csvs(filepaths: List[str]) -> Tuple[pd.DataFrame, dict, List[str]]:
        all_dfs = []
        all_tags = set()
        config_info = {}

        for fp in filepaths:
            df, cfg, tags = DataLoader.load_csv(fp)
            all_dfs.append(df)
            all_tags.update(tags)
            if not config_info:
                config_info = cfg

        merged = pd.concat(all_dfs, ignore_index=True)
        merged = merged.sort_values("time").drop_duplicates(subset="time").reset_index(drop=True)
        return merged, config_info, list(all_tags)

    @staticmethod
    def infer_sample_interval(df: pd.DataFrame) -> float:
        if "time" not in df.columns:
            return 0
        diffs = df["time"].diff().dropna()
        if len(diffs) == 0:
            return 0
        return diffs.dt.total_seconds().median()

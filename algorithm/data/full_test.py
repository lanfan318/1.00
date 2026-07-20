import urllib.request
import json

# 1. 上传
fn = "ICS_HC_AS130_20260513000000~20260520000000_10001_part_00001.csv"
boundary = "----boundary12345"
data = b""
data += f"--{boundary}\r\n".encode()
data += f'Content-Disposition: form-data; name=\"file\"; filename=\"{fn}\"\r\n'.encode()
data += b"Content-Type: text/csv\r\n\r\n"
with open(fn, "rb") as f:
    data += f.read()
data += f"\r\n--{boundary}--\r\n".encode()

req = urllib.request.Request("http://localhost:8001/api/data/upload", data=data)
req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
resp = urllib.request.urlopen(req)
result = json.loads(resp.read().decode())
fid = result["file_id"]
print(f"1. 上传成功: {fid}, 行数={result['rows']}")

# 2. 清洗
clean_body = json.dumps({
    "resample_interval": "1min",
    "outlier_methods": ["hampel", "rate_limit", "stuck"],
    "missing_strategy": "interpolate"
}).encode()
req2 = urllib.request.Request(f"http://localhost:8001/api/data/{fid}/clean", data=clean_body)
req2.add_header("Content-Type", "application/json")
resp2 = urllib.request.urlopen(req2)
clean_result = json.loads(resp2.read().decode())
cid = clean_result["clean_id"]
print(f"2. 清洗完成: {cid}")
print(f"   异常点: {clean_result['report']['anomaly_count']}")
print(f"   缺失before: {clean_result['report']['missing_before']}, after: {clean_result['report']['missing_after']}")

# 3. 残差预警
alarm_body = json.dumps({
    "tag_name": "ICS_HC_AS130.AV",
    "current_value": 5.50,
    "lookback_hours": 24
}).encode()
req3 = urllib.request.Request(f"http://localhost:8001/api/alarm/residual-check?clean_id={cid}", data=alarm_body)
req3.add_header("Content-Type", "application/json")
resp3 = urllib.request.urlopen(req3)
alarm_result = json.loads(resp3.read().decode())
print(f"3. 残差预警: 报警={alarm_result['is_alarm']}, 级别={alarm_result['alarm_level']}")
print(f"   当前值={alarm_result['current_value']}, 预测值={alarm_result['predicted_value']}")
print(f"   残差={alarm_result['residual']}, 基线均值={alarm_result['baseline_mean']}, 基线标准差={alarm_result['baseline_std']}")

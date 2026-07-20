import urllib.request
import json

fn = 'ICS_HC_AS130_20260513000000~20260520000000_10001_part_00001.csv'
boundary = '----boundary12345'
data = b''
data += f'--{boundary}\r\n'.encode()
data += f'Content-Disposition: form-data; name=\"file\"; filename=\"{fn}\"\r\n'.encode()
data += b'Content-Type: text/csv\r\n\r\n'
with open(fn, 'rb') as f:
    data += f.read()
data += f'\r\n--{boundary}--\r\n'.encode()

req = urllib.request.Request('http://localhost:8001/api/data/upload', data=data)
req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
resp = urllib.request.urlopen(req)
result = json.loads(resp.read().decode())
fid = result['file_id']
print(f'上传成功, file_id = {fid}')
print(f'行数: {result["rows"]}, 测点: {result["tags"]}')

resp2 = urllib.request.urlopen(f'http://localhost:8001/api/data/{fid}/profile')
print('=== 数据概览 ===')
print(resp2.read().decode())

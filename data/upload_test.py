import urllib.request

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
try:
    resp = urllib.request.urlopen(req)
    print(resp.read().decode())
except Exception as e:
    print(f'Error: {e}')
    if hasattr(e, 'read'):
        print(e.read().decode())

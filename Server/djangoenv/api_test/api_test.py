import requests  # 오류가 있으면 "pip install requests"

HOST = 'http://127.0.0.1:8000'

# 토큰 인증
res = requests.post(HOST + '/api-token-auth/', {
    'username': 'admin',
    'password': 'admin',
})
res.raise_for_status()
token = res.json()['token']
print(f"토큰: {token}")

# 인증이 필요한 요청에 아래의 headers를 붙임
headers = {'Authorization': 'Token ' + token, 'Accept': 'application/json'}

# Post Create
data = {
    'title': '제목 by code',
    'text': 'API내용 by code',
    'author': 1,  # admin 사용자 ID
    'created_date': '2024-06-03T18:34:00+09:00',
    'published_date': '2024-06-03T18:34:00+09:00'
}

# 이미지 파일 (Windows 경로로 수정)
file = {'image': open('C:\\Users\\asus\\Desktop\\sheild.png', 'rb')}

# API 요청 (URL 수정: api -> api_root)
res = requests.post(HOST + '/api_root/Post/', data=data, files=file, headers=headers)
print(f"응답 상태: {res}")
print(f"응답 내용: {res.json()}")

# 파일 닫기
file['image'].close()

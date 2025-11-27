"""
Token 발급 스크립트
사용법: python get_token.py
"""
import requests
import sys

HOST = 'http://127.0.0.1:8000'

# 사용자 정보 (필요시 수정)
username = 'admin'
password = 'admin'

if len(sys.argv) > 1:
    username = sys.argv[1]
if len(sys.argv) > 2:
    password = sys.argv[2]

try:
    # 토큰 인증
    res = requests.post(HOST + '/api-token-auth/', {
        'username': username,
        'password': password,
    })
    res.raise_for_status()
    token = res.json()['token']
    print(f"토큰: {token}")
    print(f"\n사용할 헤더:")
    print(f"Authorization: Token {token}")
except requests.exceptions.RequestException as e:
    print(f"오류 발생: {e}")
    print("서버가 실행 중인지 확인하세요: python manage.py runserver")



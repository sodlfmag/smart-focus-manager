"""
Status API 테스트 스크립트
로컬 서버에서 Status API의 GET/POST 기능을 테스트합니다.
"""
import requests
import json

BASE_URL = "http://127.0.0.1:8000/api/status/"

def test_get_status():
    """GET 요청으로 현재 상태 확인"""
    print("=== GET /api/status/ 테스트 ===")
    response = requests.get(BASE_URL)
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}")
    assert response.status_code == 200
    assert 'is_running' in response.json()
    print("✓ GET 테스트 통과\n")

def test_post_start():
    """POST 요청으로 START 명령 전송"""
    print("=== POST /api/status/ (START) 테스트 ===")
    response = requests.post(BASE_URL, json={'command': 'START'})
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}")
    assert response.status_code == 200
    assert response.json()['is_running'] == True
    print("✓ START 테스트 통과\n")

def test_post_stop():
    """POST 요청으로 STOP 명령 전송"""
    print("=== POST /api/status/ (STOP) 테스트 ===")
    response = requests.post(BASE_URL, json={'command': 'STOP'})
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}")
    assert response.status_code == 200
    assert response.json()['is_running'] == False
    print("✓ STOP 테스트 통과\n")

def test_post_invalid():
    """POST 요청으로 잘못된 명령 전송"""
    print("=== POST /api/status/ (INVALID) 테스트 ===")
    response = requests.post(BASE_URL, json={'command': 'INVALID'})
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}")
    assert response.status_code == 400
    assert response.json()['status'] == 'error'
    print("✓ INVALID 명령 에러 처리 테스트 통과\n")

if __name__ == "__main__":
    print("Status API 테스트 시작\n")
    try:
        test_get_status()
        test_post_start()
        test_get_status()  # START 후 상태 확인
        test_post_stop()
        test_get_status()  # STOP 후 상태 확인
        test_post_invalid()
        print("모든 테스트 통과!")
    except Exception as e:
        print(f"테스트 실패: {e}")
        exit(1)


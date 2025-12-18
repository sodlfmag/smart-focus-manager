"""
PythonAnywhere 서버의 Status API 테스트 스크립트
실제 배포된 서버에서 API가 정상 작동하는지 확인합니다.
"""
import requests
import json
import time

BASE_URL = "https://sodlfmag.pythonanywhere.com/api/status/"

def test_get_status():
    """GET 요청으로 현재 상태 확인"""
    print("=" * 50)
    print("테스트 1: GET /api/status/ - 현재 상태 조회")
    print("=" * 50)
    try:
        response = requests.get(BASE_URL, timeout=10)
        print(f"Status Code: {response.status_code}")
        print(f"Response Headers: {dict(response.headers)}")
        print(f"Response Body: {response.json()}")
        
        if response.status_code == 200:
            data = response.json()
            assert 'is_running' in data, "응답에 'is_running' 키가 없습니다"
            assert 'updated_at' in data, "응답에 'updated_at' 키가 없습니다"
            print("\n✓ GET 테스트 통과: 서버에서 상태를 정상적으로 반환합니다.")
            return data.get('is_running')
        else:
            print(f"\n✗ GET 테스트 실패: Status Code {response.status_code}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"\n✗ GET 테스트 실패: {e}")
        return None

def test_post_start():
    """POST 요청으로 START 명령 전송"""
    print("\n" + "=" * 50)
    print("테스트 2: POST /api/status/ - START 명령")
    print("=" * 50)
    try:
        response = requests.post(
            BASE_URL,
            json={'command': 'START'},
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response Body: {response.json()}")
        
        if response.status_code == 200:
            data = response.json()
            assert data.get('is_running') == True, "is_running이 True가 아닙니다"
            print("\n✓ START 테스트 통과: 서버 상태가 True로 변경되었습니다.")
            return True
        else:
            print(f"\n✗ START 테스트 실패: Status Code {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"\n✗ START 테스트 실패: {e}")
        return False

def test_post_stop():
    """POST 요청으로 STOP 명령 전송"""
    print("\n" + "=" * 50)
    print("테스트 3: POST /api/status/ - STOP 명령")
    print("=" * 50)
    try:
        response = requests.post(
            BASE_URL,
            json={'command': 'STOP'},
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response Body: {response.json()}")
        
        if response.status_code == 200:
            data = response.json()
            assert data.get('is_running') == False, "is_running이 False가 아닙니다"
            print("\n✓ STOP 테스트 통과: 서버 상태가 False로 변경되었습니다.")
            return True
        else:
            print(f"\n✗ STOP 테스트 실패: Status Code {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"\n✗ STOP 테스트 실패: {e}")
        return False

def test_post_invalid():
    """POST 요청으로 잘못된 명령 전송"""
    print("\n" + "=" * 50)
    print("테스트 4: POST /api/status/ - 잘못된 명령 (에러 처리 확인)")
    print("=" * 50)
    try:
        response = requests.post(
            BASE_URL,
            json={'command': 'INVALID'},
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response Body: {response.json()}")
        
        if response.status_code == 400:
            data = response.json()
            assert data.get('status') == 'error', "에러 응답이 올바르지 않습니다"
            print("\n✓ INVALID 명령 테스트 통과: 에러가 올바르게 처리되었습니다.")
            return True
        else:
            print(f"\n✗ INVALID 명령 테스트 실패: 예상 Status Code 400, 실제 {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"\n✗ INVALID 명령 테스트 실패: {e}")
        return False

def main():
    """메인 테스트 함수"""
    print("\n" + "=" * 50)
    print("PythonAnywhere Status API 테스트 시작")
    print(f"서버 URL: {BASE_URL}")
    print("=" * 50 + "\n")
    
    results = []
    
    # 테스트 1: GET 요청
    initial_status = test_get_status()
    results.append(("GET Status", initial_status is not None))
    
    # 테스트 2: POST START
    if test_post_start():
        results.append(("POST START", True))
        time.sleep(1)  # 상태 변경 후 잠시 대기
        
        # START 후 상태 확인
        print("\n" + "-" * 50)
        print("START 명령 후 상태 확인")
        print("-" * 50)
        status_after_start = test_get_status()
    else:
        results.append(("POST START", False))
    
    # 테스트 3: POST STOP
    if test_post_stop():
        results.append(("POST STOP", True))
        time.sleep(1)  # 상태 변경 후 잠시 대기
        
        # STOP 후 상태 확인
        print("\n" + "-" * 50)
        print("STOP 명령 후 상태 확인")
        print("-" * 50)
        status_after_stop = test_get_status()
    else:
        results.append(("POST STOP", False))
    
    # 테스트 4: 잘못된 명령
    results.append(("POST INVALID", test_post_invalid()))
    
    # 최종 결과 요약
    print("\n" + "=" * 50)
    print("테스트 결과 요약")
    print("=" * 50)
    for test_name, passed in results:
        status = "✓ 통과" if passed else "✗ 실패"
        print(f"{test_name}: {status}")
    
    all_passed = all(result[1] for result in results)
    print("\n" + "=" * 50)
    if all_passed:
        print("모든 테스트 통과! ✓")
    else:
        print("일부 테스트 실패 ✗")
    print("=" * 50)

if __name__ == "__main__":
    main()





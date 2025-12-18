"""
서버의 시스템 제어 상태를 확인하는 스크립트
"""
import requests

HOST = 'https://sodlfmag.pythonanywhere.com'

def check_status():
    """서버의 is_running 상태 확인"""
    try:
        url = f'{HOST}/api/status/'
        response = requests.get(url, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            is_running = data.get('is_running', False)
            updated_at = data.get('updated_at', 'N/A')
            
            print("=" * 60)
            print("서버 시스템 제어 상태")
            print("=" * 60)
            print(f"is_running: {is_running}")
            print(f"updated_at: {updated_at}")
            print("=" * 60)
            
            if is_running:
                print("\n[OK] 시스템이 실행 중입니다.")
                print("  -> launcher.py가 detect.py를 실행해야 합니다.")
            else:
                print("\n[STOP] 시스템이 중지되어 있습니다.")
                print("  -> 앱에서 시스템 제어 스위치를 켜주세요.")
            
            return is_running
        else:
            print(f"오류: 서버가 {response.status_code} 상태 코드를 반환했습니다.")
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    check_status()


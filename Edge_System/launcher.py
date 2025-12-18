"""
launcher.py - Edge 시스템의 생명주기 관리자
서버의 명령을 주기적으로 확인(Polling)하여 detect.py의 생명주기를 관리합니다.
"""
import requests
import subprocess
import time
import sys
from pathlib import Path

# 서버 설정
HOST = 'https://sodlfmag.pythonanywhere.com'
POLLING_INTERVAL = 3  # 3초마다 상태 확인

# detect.py 경로
EDGE_DIR = Path(__file__).parent
DETECT_SCRIPT = EDGE_DIR / 'detect.py'

def get_server_status():
    """서버에서 현재 is_running 상태를 조회"""
    try:
        response = requests.get(f'{HOST}/api/status/', timeout=5)
        if response.status_code == 200:
            data = response.json()
            is_running = data.get('is_running', False)
            print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Server status: is_running={is_running}")
            return is_running
        else:
            print(f"Error: Server returned status code {response.status_code}")
            return False
    except Exception as e:
        print(f"Error getting server status: {e}")
        return False

def is_process_running(process):
    """프로세스가 실행 중인지 확인"""
    if process is None:
        return False
    return process.poll() is None

def main():
    """메인 루프: 서버 상태를 확인하고 detect.py를 실행/종료"""
    detect_process = None
    
    print("Launcher started. Polling server every 3 seconds...")
    print(f"Detect script: {DETECT_SCRIPT}")
    
    try:
        while True:
            # 서버에서 상태 확인
            is_running = get_server_status()
            
            # 상태에 따라 detect.py 실행/종료
            if is_running:
                # 서버가 실행 중이라고 하면 detect.py가 실행되어야 함
                if not is_process_running(detect_process):
                    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Starting detect.py...")
                    # detect.py 실행 (웹캠 사용: source=0, Person(0)과 Cell Phone(67)만 감지)
                    detect_process = subprocess.Popen(
                        [sys.executable, str(DETECT_SCRIPT), '--source', '0', '--nosave', '--classes', '0', '67'],
                        cwd=str(EDGE_DIR)
                    )
                    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] detect.py started with PID: {detect_process.pid}")
                # else: 이미 실행 중이면 아무것도 하지 않음
            else:
                # 서버가 중지되었다고 하면 detect.py를 종료해야 함
                if is_process_running(detect_process):
                    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Stopping detect.py...")
                    detect_process.terminate()
                    try:
                        detect_process.wait(timeout=5)
                        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] detect.py stopped successfully")
                    except subprocess.TimeoutExpired:
                        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] Force killing detect.py...")
                        detect_process.kill()
                        detect_process.wait()
                    detect_process = None
            
            # 3초 대기
            time.sleep(POLLING_INTERVAL)
            
    except KeyboardInterrupt:
        print("\nLauncher interrupted. Stopping detect.py if running...")
        if is_process_running(detect_process):
            detect_process.terminate()
            try:
                detect_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                detect_process.kill()
        print("Launcher stopped.")

if __name__ == "__main__":
    main()


"""
Socket Server for capturing Client Request Protocol
VSCode에서 Client Request Protocol 검증용 (24페이지)
"""
import socket
import os
import sys
from datetime import datetime

# 출력 버퍼링 비활성화 (로그 즉시 표시)
sys.stdout.reconfigure(line_buffering=True) if hasattr(sys.stdout, 'reconfigure') else None

# 서버 설정
HOST = '0.0.0.0'  # 모든 인터페이스에서 수신
PORT = 8000

def save_request(request_data, client_address):
    """수신된 요청을 파일로 저장"""
    # request 디렉토리 생성
    request_dir = 'request'
    if not os.path.exists(request_dir):
        os.makedirs(request_dir)
        print(f"[DEBUG] request 디렉토리 생성: {os.path.abspath(request_dir)}")
    
    # 타임스탬프로 파일명 생성
    timestamp = datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
    filename = os.path.join(request_dir, f'{timestamp}.bin')
    
    # 요청 데이터 저장
    with open(filename, 'wb') as f:
        f.write(request_data)
    
    abs_path = os.path.abspath(filename)
    print(f"\n✅ 요청 저장됨: {abs_path}")
    print(f"   클라이언트: {client_address}, 크기: {len(request_data)} bytes")
    return filename

def start_socket_server():
    """Socket 서버 시작"""
    # 포트 충돌 확인
    import socket as sock_test
    test_socket = sock_test.socket(sock_test.AF_INET, sock_test.SOCK_STREAM)
    try:
        test_socket.bind((HOST, PORT))
        test_socket.close()
    except OSError as e:
        if e.errno == 10048 or "Address already in use" in str(e):
            print(f"\n❌ 오류: 포트 {PORT}가 이미 사용 중입니다!")
            print("   Django 서버를 먼저 종료하세요 (Ctrl+C)")
            print("   그런 다음 이 Socket 서버를 실행하세요.")
            sys.exit(1)
        else:
            raise
    
    # Socket 생성
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    try:
        # 서버 바인딩
        server_socket.bind((HOST, PORT))
        server_socket.listen(5)
        
        print(f"Socket 서버 시작: {HOST}:{PORT}")
        print(f"요청 대기 중... (종료: Ctrl+C)")
        print(f"Android 앱은 다음 주소로 요청을 보내야 합니다:")
        print(f"  - 에뮬레이터: http://10.0.2.2:8000")
        print(f"  - 실제 기기: http://[PC의 IP주소]:8000")
        print("-" * 50)
        
        connection_count = 0
        while True:
            # 클라이언트 연결 대기
            print("\n[대기 중] 클라이언트 연결을 기다리는 중...")
            try:
                client_socket, client_address = server_socket.accept()
                connection_count += 1
                print(f"\n{'='*60}", flush=True)
                print(f"[연결 #{connection_count}] 클라이언트 연결됨!", flush=True)
                print(f"주소: {client_address}", flush=True)
                print(f"{'='*60}", flush=True)
                
                try:
                    # 요청 데이터 수신
                    request_data = b''
                    client_socket.settimeout(10.0)  # 타임아웃 설정
                    
                    print("[DEBUG] 데이터 수신 시작...", flush=True)
                    
                    # 데이터 수신
                    chunk_count = 0
                    while True:
                        try:
                            chunk = client_socket.recv(4096)
                            chunk_count += 1
                            
                            if not chunk:
                                print(f"[DEBUG] chunk {chunk_count}: 연결이 닫혔습니다 (chunk가 비어있음)")
                                break
                            
                            request_data += chunk
                            print(f"[DEBUG] chunk {chunk_count}: {len(chunk)} bytes 수신 (총: {len(request_data)} bytes)", flush=True)
                            
                            # HTTP 헤더가 완전히 수신되었는지 확인 (\r\n\r\n)
                            if b'\r\n\r\n' in request_data:
                                print("[DEBUG] HTTP 헤더 수신 완료")
                                
                                # Content-Length 헤더 확인 (본문이 있는 경우)
                                try:
                                    request_str = request_data.decode('utf-8', errors='ignore')
                                    lines = request_str.split('\r\n')
                                    content_length = 0
                                    for line in lines:
                                        if line.lower().startswith('content-length:'):
                                            content_length = int(line.split(':')[1].strip())
                                            print(f"[DEBUG] Content-Length 발견: {content_length} bytes")
                                            break
                                    
                                    if content_length > 0:
                                        # 본문 데이터가 더 필요한 경우
                                        body_start = request_data.find(b'\r\n\r\n') + 4
                                        body_received = len(request_data) - body_start
                                        print(f"[DEBUG] 본문 수신 상태: {body_received}/{content_length} bytes")
                                        if body_received < content_length:
                                            # 본문이 더 필요한 경우 추가 수신
                                            remaining = content_length - body_received
                                            print(f"[DEBUG] 본문 추가 수신 시도: {remaining} bytes")
                                            additional_data = client_socket.recv(remaining + 100)  # 여유 공간
                                            if additional_data:
                                                request_data += additional_data
                                                print(f"[DEBUG] 본문 추가 수신 완료: {len(additional_data)} bytes")
                                except Exception as e:
                                    print(f"[DEBUG] Content-Length 파싱 중 오류: {e}")
                                
                                # 일반적인 GET 요청은 본문이 없으므로 종료
                                break
                                
                        except socket.timeout:
                            print("[DEBUG] 타임아웃 발생 - 수신된 데이터 처리...")
                            if len(request_data) > 0:
                                break
                            else:
                                print("[WARNING] 타임아웃 동안 데이터가 수신되지 않았습니다")
                                break
                        except Exception as e:
                            print(f"[DEBUG] recv() 중 오류: {e}")
                            break
                    
                    if request_data:
                        # 요청을 파일로 저장
                        filename = save_request(request_data, client_address)
                        
                        # 요청 내용 출력 (디버깅용)
                        try:
                            request_str = request_data.decode('utf-8', errors='ignore')
                            print(f"\n수신된 요청 내용:")
                            print("-" * 50)
                            print(request_str[:500])  # 처음 500자만 출력
                            if len(request_str) > 500:
                                print("... (더 많은 내용)")
                            print("-" * 50)
                        except:
                            print("요청을 텍스트로 변환할 수 없습니다 (바이너리 데이터)")
                        
                        # 간단한 HTTP 응답 (Android 앱이 계속 기다리지 않도록)
                        response = b'HTTP/1.1 200 OK\r\n'
                        response += b'Content-Type: text/plain\r\n'
                        response += b'Content-Length: 13\r\n'
                        response += b'\r\n'
                        response += b'Request saved'
                        
                        print("[DEBUG] HTTP 응답 전송 중...")
                        client_socket.sendall(response)
                        print("[DEBUG] HTTP 응답 전송 완료")
                    else:
                        print("[WARNING] 요청 데이터가 비어있습니다")
                        
                except Exception as e:
                    print(f"[ERROR] 요청 처리 중 오류: {e}")
                    import traceback
                    traceback.print_exc()
                finally:
                    client_socket.close()
                    print(f"[종료] 클라이언트 연결 종료: {client_address}")
                    print(f"{'='*60}\n")
                    
            except Exception as e:
                print(f"[ERROR] accept() 중 오류: {e}")
                import traceback
                traceback.print_exc()
                
    except KeyboardInterrupt:
        print("\n\n서버 종료 중...")
    except Exception as e:
        print(f"[ERROR] 서버 오류: {e}")
        import traceback
        traceback.print_exc()
    finally:
        server_socket.close()
        print("Socket 서버 종료")

if __name__ == '__main__':
    start_socket_server()

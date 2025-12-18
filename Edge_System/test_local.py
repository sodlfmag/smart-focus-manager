"""
로컬 환경에서 YOLOv5 침입 감지 시스템 테스트 스크립트
사용법: python test_local.py [이미지경로]
"""
import sys
import os
from pathlib import Path

# detect.py의 run 함수를 직접 호출
from detect import run

if __name__ == '__main__':
    # 기본 이미지 경로 (YOLOv5의 샘플 이미지)
    default_image = Path(__file__).parent / 'data' / 'images' / 'bus.jpg'
    
    if len(sys.argv) > 1:
        image_path = sys.argv[1]
    else:
        image_path = str(default_image)
    
    if not os.path.exists(image_path):
        print(f"오류: 이미지 파일을 찾을 수 없습니다: {image_path}")
        print(f"사용법: python test_local.py [이미지경로]")
        sys.exit(1)
    
    print(f"테스트 이미지: {image_path}")
    print("Django 서버가 http://127.0.0.1:8000 에서 실행 중인지 확인하세요.")
    print("검출된 객체가 새로 나타나면 서버로 전송됩니다.\n")
    
    # detect.py 실행
    run(
        source=image_path,
        view_img=True,
        save_txt=False,
        save_conf=False,
        nosave=False,
    )


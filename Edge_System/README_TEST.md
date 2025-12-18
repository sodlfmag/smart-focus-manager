# YOLOv5 침입 감지 시스템 테스트 가이드

## Cam 없이 테스트하는 방법

### 1. 정적 이미지로 테스트
```bash
# 가상환경 활성화
.\venv\Scripts\Activate.ps1

# 이미지 파일로 테스트
python detect.py --source data/images/bus.jpg --view-img

# 또는 여러 이미지가 있는 디렉토리로 테스트
python detect.py --source data/images/ --view-img
```

### 2. Cam이 있는 환경에서 테스트 (내일)
```bash
# 웹캠 사용 (기본 카메라)
python detect.py --source 0 --view-img

# 특정 카메라 사용
python detect.py --source 1 --view-img
```

## Django 서버 설정

1. Django 서버가 실행 중인지 확인
2. admin 사용자 생성 및 토큰 확인
3. `changedetection.py`의 HOST, username, password 확인

## 주의사항

- 첫 실행 시 모델 파일(yolov5s.pt)이 자동으로 다운로드됩니다
- 검출된 객체가 새로 나타날 때만 서버로 전송됩니다 (상태 변화 감지)
- 이미지는 `runs/detect/exp*/intruder_image/` 경로에 저장됩니다


# detect.py 검증 결과

## 1. 클래스 필터링 확인

### 수정 사항
- **launcher.py**에서 `detect.py` 실행 시 `--classes 0 67` 파라미터 추가
- Person(클래스 0)과 Cell Phone(클래스 67)만 감지하도록 설정

### 코드 검토
- `non_max_suppression` 함수의 `classes` 파라미터가 올바르게 전달됨
- `detected` 배열에서 Person과 Cell Phone만 1로 설정됨 (line 246-258)

**검증 완료**: Person과 Cell Phone만 필터링하여 감지합니다.

## 2. changedetection.py 연동 확인

### 연동 로직
- `cd = ChangeDetection(names)` - ChangeDetection 인스턴스 생성 (line 184)
- `cd.add(names, detected, save_dir, im0)` - 감지 결과를 ChangeDetection에 전달
- `detected` 배열은 Person(0)과 Cell Phone(67)만 1로 설정됨

**검증 완료**: detect.py와 changedetection.py가 올바르게 연동됩니다.

## 결론

detect.py가 Person과 Cell Phone만 감지하도록 설정되었으며, changedetection.py와의 연동도 정상적으로 작동합니다.






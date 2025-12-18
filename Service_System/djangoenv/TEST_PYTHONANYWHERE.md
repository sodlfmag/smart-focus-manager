# PythonAnywhere Status API 테스트 가이드

## 방법 1: PowerShell 스크립트 사용 (추천)

가장 간단한 방법입니다. PowerShell에서 다음 명령을 실행하세요:

```powershell
cd c:\dev\smart_focus_manager\Server\djangoenv
.\test_api_powershell.ps1
```

이 스크립트는 다음을 자동으로 테스트합니다:
1. GET 요청으로 현재 상태 확인
2. POST START 명령으로 상태를 True로 변경
3. POST STOP 명령으로 상태를 False로 변경
4. 잘못된 명령으로 에러 처리 확인

## 방법 2: Python 스크립트 사용

Python 환경이 설정되어 있다면:

```powershell
cd c:\dev\smart_focus_manager\Server\djangoenv
python test_pythonanywhere_api.py
```

## 방법 3: 수동 테스트 (PowerShell 명령어)

### GET 요청 (현재 상태 확인)
```powershell
Invoke-RestMethod -Uri "https://sodlfmag.pythonanywhere.com/api/status/" -Method Get
```

### POST START 명령
```powershell
$body = @{command='START'} | ConvertTo-Json
Invoke-RestMethod -Uri "https://sodlfmag.pythonanywhere.com/api/status/" -Method Post -Body $body -ContentType "application/json"
```

### POST STOP 명령
```powershell
$body = @{command='STOP'} | ConvertTo-Json
Invoke-RestMethod -Uri "https://sodlfmag.pythonanywhere.com/api/status/" -Method Post -Body $body -ContentType "application/json"
```

## 방법 4: 웹 브라우저에서 확인

웹 브라우저에서 다음 URL을 열면 GET 요청 결과를 볼 수 있습니다:
- https://sodlfmag.pythonanywhere.com/api/status/

하지만 POST 요청은 브라우저에서 직접 테스트할 수 없으므로, 위의 PowerShell 또는 Python 스크립트를 사용해야 합니다.

## 예상 결과

### GET 요청 응답
```json
{
  "is_running": false,
  "updated_at": "2025-12-08T05:56:16.187358+00:00"
}
```

### POST START 응답
```json
{
  "status": "success",
  "message": "System started",
  "is_running": true
}
```

### POST STOP 응답
```json
{
  "status": "success",
  "message": "System stopped",
  "is_running": false
}
```

### 잘못된 명령 응답 (400 에러)
```json
{
  "status": "error",
  "message": "Invalid command. Use START or STOP"
}
```






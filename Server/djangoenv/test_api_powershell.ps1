# PythonAnywhere Status API 테스트 스크립트 (PowerShell)
# 실제 배포된 서버에서 API가 정상 작동하는지 확인합니다.

$baseUrl = "https://sodlfmag.pythonanywhere.com/api/status/"

Write-Host "=================================================="
Write-Host "PythonAnywhere Status API 테스트 시작"
Write-Host "서버 URL: $baseUrl"
Write-Host "=================================================="
Write-Host ""

# 테스트 1: GET 요청
Write-Host "테스트 1: GET /api/status/ - 현재 상태 조회"
Write-Host "--------------------------------------------------"
try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Get -TimeoutSec 10
    Write-Host "응답:"
    $response | ConvertTo-Json -Depth 10
    if ($response.is_running -ne $null) {
        Write-Host "`n✓ GET 테스트 통과: 서버에서 상태를 정상적으로 반환합니다." -ForegroundColor Green
        $initialStatus = $response.is_running
    } else {
        Write-Host "`n✗ GET 테스트 실패: 응답에 'is_running' 키가 없습니다." -ForegroundColor Red
    }
} catch {
    Write-Host "`n✗ GET 테스트 실패: $_" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 1

# 테스트 2: POST START
Write-Host "테스트 2: POST /api/status/ - START 명령"
Write-Host "--------------------------------------------------"
try {
    $body = @{command='START'} | ConvertTo-Json
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $body -ContentType "application/json" -TimeoutSec 10
    Write-Host "응답:"
    $response | ConvertTo-Json -Depth 10
    if ($response.is_running -eq $true) {
        Write-Host "`n✓ START 테스트 통과: 서버 상태가 True로 변경되었습니다." -ForegroundColor Green
    } else {
        Write-Host "`n✗ START 테스트 실패: is_running이 True가 아닙니다." -ForegroundColor Red
    }
} catch {
    Write-Host "`n✗ START 테스트 실패: $_" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 2

# START 후 상태 확인
Write-Host "START 명령 후 상태 확인"
Write-Host "--------------------------------------------------"
try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Get -TimeoutSec 10
    Write-Host "현재 상태: is_running = $($response.is_running)"
} catch {
    Write-Host "상태 확인 실패: $_" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 1

# 테스트 3: POST STOP
Write-Host "테스트 3: POST /api/status/ - STOP 명령"
Write-Host "--------------------------------------------------"
try {
    $body = @{command='STOP'} | ConvertTo-Json
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $body -ContentType "application/json" -TimeoutSec 10
    Write-Host "응답:"
    $response | ConvertTo-Json -Depth 10
    if ($response.is_running -eq $false) {
        Write-Host "`n✓ STOP 테스트 통과: 서버 상태가 False로 변경되었습니다." -ForegroundColor Green
    } else {
        Write-Host "`n✗ STOP 테스트 실패: is_running이 False가 아닙니다." -ForegroundColor Red
    }
} catch {
    Write-Host "`n✗ STOP 테스트 실패: $_" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 2

# STOP 후 상태 확인
Write-Host "STOP 명령 후 상태 확인"
Write-Host "--------------------------------------------------"
try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Get -TimeoutSec 10
    Write-Host "현재 상태: is_running = $($response.is_running)"
} catch {
    Write-Host "상태 확인 실패: $_" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 1

# 테스트 4: 잘못된 명령
Write-Host "테스트 4: POST /api/status/ - 잘못된 명령 (에러 처리 확인)"
Write-Host "--------------------------------------------------"
try {
    $body = @{command='INVALID'} | ConvertTo-Json
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $body -ContentType "application/json" -TimeoutSec 10
    Write-Host "응답:"
    $response | ConvertTo-Json -Depth 10
    Write-Host "`n✗ INVALID 명령 테스트 실패: 400 에러가 발생해야 합니다." -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400) {
        Write-Host "`n✓ INVALID 명령 테스트 통과: 에러가 올바르게 처리되었습니다. (Status: 400)" -ForegroundColor Green
    } else {
        Write-Host "`n✗ INVALID 명령 테스트 실패: 예상 Status Code 400, 실제 $statusCode" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=================================================="
Write-Host "테스트 완료"
Write-Host "=================================================="



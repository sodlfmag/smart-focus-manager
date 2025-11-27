# Django 서버 시작 스크립트
# 사용법: .\start_server.ps1

Write-Host "=== Django 서버 시작 ===" -ForegroundColor Cyan

# 현재 디렉토리 확인
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# 가상 환경 활성화
Write-Host "가상 환경 활성화 중..." -ForegroundColor Yellow
if (Test-Path "venv\Scripts\Activate.ps1") {
    & "venv\Scripts\Activate.ps1"
    Write-Host "가상 환경 활성화 완료" -ForegroundColor Green
} else {
    Write-Host "오류: 가상 환경을 찾을 수 없습니다!" -ForegroundColor Red
    exit 1
}

# Django 서버 실행
Write-Host ""
Write-Host "Django 서버 실행 중..." -ForegroundColor Yellow
Write-Host "서버를 종료하려면 Ctrl+C를 누르세요." -ForegroundColor Cyan
Write-Host ""

python manage.py runserver


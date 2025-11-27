"""
Pythonanywhere 배포를 위한 설정 파일
이 파일은 Pythonanywhere에서만 사용됩니다.
로컬에서는 settings.py의 기본 설정을 사용합니다.
"""
import os

# Pythonanywhere 환경 변수 설정
os.environ.setdefault('DEBUG', 'False')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'mysite.settings')


"""
Pythonanywhere WSGI 설정 파일 예시
이 파일의 내용을 Pythonanywhere Web 탭의 WSGI configuration file에 복사하세요.
"""
# ============================================
# 아래 내용을 Pythonanywhere Web 탭의 WSGI configuration file에 붙여넣으세요
# ============================================

import os
import sys

# 프로젝트 경로 설정 (yourusername을 실제 사용자명으로 변경)
path = '/home/yourusername/common2/PhotoBlogServer/djangoenv'
if path not in sys.path:
    sys.path.append(path)

# Django 설정 모듈 지정
os.environ['DJANGO_SETTINGS_MODULE'] = 'mysite.settings'
os.environ['DEBUG'] = 'False'  # 프로덕션 환경

# Django WSGI 애플리케이션 로드
from django.core.wsgi import get_wsgi_application
application = get_wsgi_application()

# ============================================
# 참고사항:
# 1. 'yourusername'을 실제 Pythonanywhere 사용자명으로 변경하세요
# 2. 프로젝트 경로가 다르면 path 변수를 수정하세요
# 3. Web 탭에서 Reload 버튼을 눌러 변경사항을 적용하세요
# ============================================


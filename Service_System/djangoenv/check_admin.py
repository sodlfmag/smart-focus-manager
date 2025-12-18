"""
Django admin 계정 확인 및 생성 스크립트
사용법: python manage.py shell < check_admin.py
또는: python check_admin.py (Django 설정 후)
"""
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'mysite.settings')
django.setup()

from django.contrib.auth.models import User

username = 'admin'
password = 'password'

# admin 계정 확인
if User.objects.filter(username=username).exists():
    user = User.objects.get(username=username)
    print(f"✓ Admin 계정 '{username}'이 이미 존재합니다.")
    # 비밀번호 업데이트
    user.set_password(password)
    user.is_staff = True
    user.is_superuser = True
    user.save()
    print(f"✓ 비밀번호를 '{password}'로 업데이트했습니다.")
else:
    # admin 계정 생성
    User.objects.create_superuser(username=username, password=password, email='admin@example.com')
    print(f"✓ Admin 계정 '{username}'을 생성했습니다. (비밀번호: {password})")


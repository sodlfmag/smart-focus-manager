from django.contrib import admin
from .models import Post, SystemControl
admin.site.register(Post) #관리자 페이지에서 'Post' 모델 확인
admin.site.register(SystemControl) #관리자 페이지에서 'SystemControl' 모델 확인
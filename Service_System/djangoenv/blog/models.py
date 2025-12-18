from django.conf import settings
from django.db import models
from django.utils import timezone


class Post(models.Model):
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE
    )
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(
        default=timezone.now
    )
    published_date = models.DateTimeField(
        blank=True, null=True
    )
    image = models.ImageField(
        upload_to='blog_image/%Y/%m/%d/',
        blank=True,
        null=True
    )

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title


class SystemControl(models.Model):
    """시스템 원격 제어 상태를 관리하는 Singleton 모델"""
    is_running = models.BooleanField(default=False)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "System Control"
        verbose_name_plural = "System Controls"

    def __str__(self):
        return f"System Control - Running: {self.is_running}"

    @classmethod
    def get_instance(cls):
        """Singleton 인스턴스 반환 (없으면 생성)"""
        instance, created = cls.objects.get_or_create(pk=1)
        return instance
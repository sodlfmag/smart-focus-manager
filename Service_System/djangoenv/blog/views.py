from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone
from .models import Post, SystemControl
from .forms import PostForm
from rest_framework import viewsets
from rest_framework.decorators import action, api_view
from rest_framework.response import Response
from rest_framework import status
from .serializers import PostSerializer
from django.db.models import Count, Max
from collections import defaultdict
from datetime import datetime, timedelta

def post_list(request):
    posts = Post.objects.all().order_by('-created_date')
    
    # 오늘 날짜 기준 통계 계산
    today = timezone.now().date()
    today_start = timezone.make_aware(datetime.combine(today, datetime.min.time()))
    today_end = today_start + timedelta(days=1)
    
    today_posts = Post.objects.filter(
        created_date__gte=today_start,
        created_date__lt=today_end
    )
    
    # 오늘 통계
    today_focus_count = today_posts.filter(title='Focus').count()
    today_distracted_count = today_posts.filter(title='Distracted').count()
    today_away_count = today_posts.filter(title='Away').count()
    today_total = today_posts.count()
    
    # 집중시간 비율 계산
    today_focus_percent = 0
    if today_total > 0:
        today_focus_percent = int((today_focus_count * 100) / today_total)
    
    # 전체 통계
    all_focus_count = Post.objects.filter(title='Focus').count()
    all_distracted_count = Post.objects.filter(title='Distracted').count()
    all_away_count = Post.objects.filter(title='Away').count()
    all_total = Post.objects.count()
    
    # 전체 집중시간 비율
    all_focus_percent = 0
    if all_total > 0:
        all_focus_percent = int((all_focus_count * 100) / all_total)
    
    context = {
        'posts': posts,
        'today_stats': {
            'focus_count': today_focus_count,
            'distracted_count': today_distracted_count,
            'away_count': today_away_count,
            'total': today_total,
            'focus_percent': today_focus_percent,
        },
        'all_stats': {
            'focus_count': all_focus_count,
            'distracted_count': all_distracted_count,
            'away_count': all_away_count,
            'total': all_total,
            'focus_percent': all_focus_percent,
        }
    }
    
    return render(request, 'blog/post_list.html', context)

def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('blog:post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})

def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        form = PostForm(request.POST, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('blog:post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})


def js_test(request):
    return render(request, 'blog/js_test.html', {})

class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all().order_by('-created_date')  # 최신순 정렬
    serializer_class = PostSerializer
    
    @action(detail=False, methods=['get'])
    def stats(self, request):
        """
        검출 객체별 통계 API
        각 객체 클래스별 검출 횟수와 최근 검출 시간을 반환합니다.
        URL: /api_root/Post/stats/
        """
        # 모든 Post에서 title 필드를 기준으로 그룹화
        stats = Post.objects.values('title').annotate(
            count=Count('id'),
            last_detected=Max('created_date')
        ).order_by('-count')
        
        # 결과를 딕셔너리 형태로 정리
        result = {
            'total_detections': Post.objects.count(),
            'object_stats': []
        }
        
        for stat in stats:
            result['object_stats'].append({
                'object_name': stat['title'],
                'detection_count': stat['count'],
                'last_detected': stat['last_detected'].isoformat() if stat['last_detected'] else None
            })
        
        return Response(result)


@api_view(['GET', 'POST'])
def status_view(request):
    """
    시스템 상태 조회 및 변경 API
    GET: 현재 is_running 상태 반환
    POST: command 파라미터로 START/STOP 처리
    """
    system_control = SystemControl.get_instance()
    
    if request.method == 'GET':
        return Response({
            'is_running': system_control.is_running,
            'updated_at': system_control.updated_at.isoformat()
        })
    
    elif request.method == 'POST':
        command = request.data.get('command', '').upper()
        
        if command == 'START':
            system_control.is_running = True
            system_control.save()
            return Response({
                'status': 'success',
                'message': 'System started',
                'is_running': True
            }, status=status.HTTP_200_OK)
        
        elif command == 'STOP':
            system_control.is_running = False
            system_control.save()
            return Response({
                'status': 'success',
                'message': 'System stopped',
                'is_running': False
            }, status=status.HTTP_200_OK)
        
        else:
            return Response({
                'status': 'error',
                'message': 'Invalid command. Use START or STOP'
            }, status=status.HTTP_400_BAD_REQUEST)
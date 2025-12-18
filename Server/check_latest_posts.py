"""
서버에 저장된 최신 Post 데이터를 조회하는 스크립트
데이터가 제대로 저장되고 있는지 확인하기 위한 디버깅 도구
"""
import requests
import json
from datetime import datetime

# 서버 설정
HOST = 'https://sodlfmag.pythonanywhere.com'
TOKEN = 'e79ef213eae997b907ae570486118e9486e51662'

def get_latest_posts(limit=20):
    """서버에서 최신 Post 데이터 조회"""
    try:
        url = f'{HOST}/api_root/Post/'
        headers = {
            'Authorization': f'Token {TOKEN}',
            'Accept': 'application/json'
        }
        
        # 최신순으로 정렬하기 위해 쿼리 파라미터 추가 (Django REST Framework는 기본적으로 최신순)
        params = {}
        if limit:
            params['limit'] = limit
        
        response = requests.get(url, headers=headers, params=params, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            
            # results 배열이 있는 경우 (페이지네이션)
            if 'results' in data:
                posts = data['results']
                print(f"총 {data.get('count', len(posts))}개의 Post가 있습니다.")
                print(f"현재 페이지: {len(posts)}개 표시\n")
            else:
                # 직접 배열인 경우
                posts = data if isinstance(data, list) else [data]
                print(f"총 {len(posts)}개의 Post를 조회했습니다.\n")
            
            print("=" * 80)
            print("최신 Post 데이터 (최신순)")
            print("=" * 80)
            
            for i, post in enumerate(posts, 1):
                post_id = post.get('id', 'N/A')
                title = post.get('title', 'N/A')
                text = post.get('text', 'N/A')
                created_date = post.get('created_date', 'N/A')
                published_date = post.get('published_date', 'N/A')
                author = post.get('author', 'N/A')
                image = post.get('image', 'N/A')
                
                print(f"\n[{i}] Post ID: {post_id}")
                print(f"    Title: {title}")
                print(f"    Text: {text}")
                print(f"    Created Date: {created_date}")
                print(f"    Published Date: {published_date}")
                print(f"    Author: {author}")
                print(f"    Image: {image[:80] if image and image != 'N/A' else 'N/A'}...")
            
            # 오늘 날짜의 데이터만 필터링
            today = datetime.now().date()
            today_posts = []
            for post in posts:
                created_date_str = post.get('created_date', '')
                if created_date_str:
                    try:
                        # ISO 형식 파싱 (예: "2025-12-18T10:30:00+09:00" 또는 "2025-12-18T10:30:00Z")
                        if 'T' in created_date_str:
                            date_part = created_date_str.split('T')[0]
                            post_date = datetime.strptime(date_part, '%Y-%m-%d').date()
                            if post_date == today:
                                today_posts.append(post)
                    except Exception as e:
                        print(f"날짜 파싱 오류: {created_date_str} - {e}")
            
            print("\n" + "=" * 80)
            print(f"오늘({today}) 날짜의 Post 데이터: {len(today_posts)}개")
            print("=" * 80)
            
            if today_posts:
                for i, post in enumerate(today_posts, 1):
                    print(f"\n[{i}] {post.get('title', 'N/A')} - {post.get('created_date', 'N/A')}")
            else:
                print("오늘 날짜의 데이터가 없습니다.")
            
            # 통계 정보
            print("\n" + "=" * 80)
            print("통계 정보")
            print("=" * 80)
            
            focus_count = sum(1 for p in posts if p.get('title') == 'Focus')
            distracted_count = sum(1 for p in posts if p.get('title') == 'Distracted')
            away_count = sum(1 for p in posts if p.get('title') == 'Away')
            
            print(f"전체: {len(posts)}개")
            print(f"  - Focus: {focus_count}개")
            print(f"  - Distracted: {distracted_count}개")
            print(f"  - Away: {away_count}개")
            
            today_focus = sum(1 for p in today_posts if p.get('title') == 'Focus')
            today_distracted = sum(1 for p in today_posts if p.get('title') == 'Distracted')
            today_away = sum(1 for p in today_posts if p.get('title') == 'Away')
            
            print(f"\n오늘({today}): {len(today_posts)}개")
            print(f"  - Focus: {today_focus}개")
            print(f"  - Distracted: {today_distracted}개")
            print(f"  - Away: {today_away}개")
            
            return posts
            
        else:
            print(f"오류: 서버가 {response.status_code} 상태 코드를 반환했습니다.")
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    print("서버에서 최신 Post 데이터를 조회합니다...\n")
    posts = get_latest_posts(limit=50)
    
    if posts:
        print("\n✓ 데이터 조회 성공!")
    else:
        print("\n✗ 데이터 조회 실패!")




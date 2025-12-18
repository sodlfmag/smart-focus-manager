"""
서버에 저장된 모든 Post 데이터를 조회하는 스크립트 (페이지네이션 확인)
"""
import requests
import json
from datetime import datetime

# 서버 설정
HOST = 'https://sodlfmag.pythonanywhere.com'
TOKEN = 'e79ef213eae997b907ae570486118e9486e51662'

def get_all_posts():
    """서버에서 모든 Post 데이터 조회 (페이지네이션 포함)"""
    try:
        url = f'{HOST}/api_root/Post/'
        headers = {
            'Authorization': f'Token {TOKEN}',
            'Accept': 'application/json'
        }
        
        all_posts = []
        next_url = url
        
        while next_url:
            response = requests.get(next_url, headers=headers, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                # 페이지네이션 확인
                if 'results' in data:
                    # 페이지네이션된 응답
                    posts = data['results']
                    all_posts.extend(posts)
                    
                    print(f"현재 페이지: {len(posts)}개, 누적: {len(all_posts)}개")
                    
                    # 다음 페이지 확인
                    next_url = data.get('next')
                    if next_url:
                        print(f"다음 페이지 있음: {next_url}")
                    else:
                        print("마지막 페이지 도달")
                        break
                else:
                    # 직접 배열
                    posts = data if isinstance(data, list) else [data]
                    all_posts.extend(posts)
                    break
            else:
                print(f"오류: 서버가 {response.status_code} 상태 코드를 반환했습니다.")
                print(f"응답: {response.text}")
                break
        
        print(f"\n총 {len(all_posts)}개의 Post를 조회했습니다.\n")
        
        # 날짜별로 정렬 (최신순)
        all_posts.sort(key=lambda x: x.get('created_date', ''), reverse=True)
        
        # 최신 10개 출력
        print("=" * 80)
        print("최신 Post 데이터 (최신순, 상위 10개)")
        print("=" * 80)
        
        for i, post in enumerate(all_posts[:10], 1):
            post_id = post.get('id', 'N/A')
            title = post.get('title', 'N/A')
            created_date = post.get('created_date', 'N/A')
            
            print(f"\n[{i}] Post ID: {post_id}")
            print(f"    Title: {title}")
            print(f"    Created Date: {created_date}")
        
        # 오늘 날짜의 데이터 필터링
        today = datetime.now().date()
        today_posts = []
        for post in all_posts:
            created_date_str = post.get('created_date', '')
            if created_date_str:
                try:
                    if 'T' in created_date_str:
                        date_part = created_date_str.split('T')[0]
                        post_date = datetime.strptime(date_part, '%Y-%m-%d').date()
                        if post_date == today:
                            today_posts.append(post)
                except Exception as e:
                    pass
        
        print("\n" + "=" * 80)
        print(f"오늘({today}) 날짜의 Post 데이터: {len(today_posts)}개")
        print("=" * 80)
        
        if today_posts:
            for i, post in enumerate(today_posts, 1):
                print(f"\n[{i}] {post.get('title', 'N/A')} - {post.get('created_date', 'N/A')}")
        else:
            print("오늘 날짜의 데이터가 없습니다.")
        
        # 날짜별 통계
        print("\n" + "=" * 80)
        print("날짜별 통계")
        print("=" * 80)
        
        date_stats = {}
        for post in all_posts:
            created_date_str = post.get('created_date', '')
            if created_date_str:
                try:
                    if 'T' in created_date_str:
                        date_part = created_date_str.split('T')[0]
                        if date_part not in date_stats:
                            date_stats[date_part] = {'Focus': 0, 'Distracted': 0, 'Away': 0, 'total': 0}
                        title = post.get('title', '')
                        date_stats[date_part][title] = date_stats[date_part].get(title, 0) + 1
                        date_stats[date_part]['total'] += 1
                except:
                    pass
        
        for date_str in sorted(date_stats.keys(), reverse=True)[:10]:
            stats = date_stats[date_str]
            print(f"\n{date_str}: 총 {stats['total']}개")
            print(f"  - Focus: {stats.get('Focus', 0)}개")
            print(f"  - Distracted: {stats.get('Distracted', 0)}개")
            print(f"  - Away: {stats.get('Away', 0)}개")
        
        return all_posts
        
    except Exception as e:
        print(f"오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    print("서버에서 모든 Post 데이터를 조회합니다 (페이지네이션 포함)...\n")
    posts = get_all_posts()
    
    if posts:
        print("\n[OK] 데이터 조회 성공!")
    else:
        print("\n[FAIL] 데이터 조회 실패!")




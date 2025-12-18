# PythonAnywhere 배포 가이드

## Static 파일 배포 (CSS, JavaScript)

PythonAnywhere에서 CSS와 JavaScript 파일이 제대로 로드되려면 다음 단계를 따라야 합니다:

### 1. 최신 코드 가져오기
```bash
cd ~/smart_focus_manager/Server/djangoenv
git pull
```

### 2. Static 파일 수집
```bash
python manage.py collectstatic --noinput
```

이 명령어는 모든 앱의 static 파일들을 `STATIC_ROOT` 디렉토리로 모읍니다.

### 3. PythonAnywhere 웹 앱 설정 확인

PythonAnywhere 대시보드에서:
1. **Web** 탭으로 이동
2. **Static files** 섹션 확인
3. 다음 경로가 추가되어 있는지 확인:
   - **URL**: `/static/`
   - **Directory**: `/home/sodlfmag/smart_focus_manager/Server/djangoenv/static/`

   (사용자명이 다르면 경로를 수정하세요)

### 4. 웹 앱 재시작

**Web** 탭에서 **Reload** 버튼 클릭

### 5. 확인

브라우저에서 페이지를 새로고침하고 개발자 도구(F12)를 열어:
- **Network** 탭에서 CSS 파일(`blog.css`)과 JS 파일(`modal.js`)이 200 상태로 로드되는지 확인
- 만약 404 에러가 나면 Static files 경로가 잘못 설정된 것입니다

## 문제 해결

### CSS가 여전히 로드되지 않는 경우:

1. **Static 파일 경로 확인**:
   ```bash
   ls -la ~/smart_focus_manager/Server/djangoenv/static/
   ```
   `static/css/blog.css`와 `static/js/modal.js` 파일이 있어야 합니다.

2. **PythonAnywhere Static files 설정 재확인**:
   - URL: `/static/`
   - Directory: 절대 경로 (예: `/home/sodlfmag/smart_focus_manager/Server/djangoenv/static/`)

3. **브라우저 캐시 지우기**:
   - Ctrl+Shift+R (강력 새로고침)
   - 또는 개발자 도구에서 "Disable cache" 체크




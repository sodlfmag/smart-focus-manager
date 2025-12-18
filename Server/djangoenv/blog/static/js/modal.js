// 모달 기능 구현
console.log('modal.js 스크립트 로드됨');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOMContentLoaded 이벤트 발생');
    
    const modal = document.getElementById('postModal');
    if (!modal) {
        console.error('postModal을 찾을 수 없습니다!');
        return;
    }
    console.log('postModal 찾음');
    
    const modalOverlay = modal.querySelector('.modal-overlay');
    const modalClose = modal.querySelector('.modal-close');
    const modalTitle = document.getElementById('modalTitle');
    const modalStatus = document.getElementById('modalStatus');
    const modalImage = document.getElementById('modalImage');
    const modalImageContainer = document.getElementById('modalImageContainer');
    const modalDate = document.getElementById('modalDate');
    const modalText = document.getElementById('modalText');
    const modalDeleteBtn = document.getElementById('modalDeleteBtn');
    const postCards = document.querySelectorAll('.post-card');
    
    let currentPostId = null;

    // 삭제 버튼 존재 확인
    if (!modalDeleteBtn) {
        console.error('삭제 버튼을 찾을 수 없습니다.');
    } else {
        console.log('삭제 버튼이 성공적으로 로드되었습니다.');
    }

    // 상태별 한글 표시
    const statusLabels = {
        'focus': '집중',
        'distracted': '딴짓',
        'away': '부재'
    };

    // 모달 열기
    function openModal(postCard) {
        const postId = postCard.getAttribute('data-post-id');
        const postTitle = postCard.getAttribute('data-post-title');
        const postText = postCard.getAttribute('data-post-text');
        const postDate = postCard.getAttribute('data-post-date');
        const postImage = postCard.getAttribute('data-post-image');

        // 현재 포스트 ID 저장
        currentPostId = postId;

        // 모달 내용 설정
        modalTitle.textContent = postTitle;
        
        // 상태 배지 설정
        const statusClass = postTitle.toLowerCase();
        modalStatus.textContent = statusLabels[statusClass] || postTitle;
        modalStatus.className = 'modal-status-badge status-' + statusClass;

        // 날짜 포맷팅
        const dateObj = new Date(postDate);
        const formattedDate = dateObj.toLocaleString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
        modalDate.textContent = formattedDate;

        // 텍스트 설정
        modalText.textContent = postText;

        // 이미지 설정
        if (postImage) {
            modalImage.src = postImage;
            modalImage.alt = postTitle;
            modalImageContainer.style.display = 'block';
        } else {
            modalImageContainer.style.display = 'none';
        }

        // 모달 표시
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    // 모달 닫기
    function closeModal() {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    // 포스트 카드 클릭 이벤트
    postCards.forEach(function(card) {
        card.addEventListener('click', function() {
            openModal(card);
        });
        
        // 호버 효과를 위한 커서 변경
        card.style.cursor = 'pointer';
    });

    // 닫기 버튼 클릭
    modalClose.addEventListener('click', closeModal);

    // 오버레이 클릭 시 닫기 (모달 콘텐츠 클릭은 무시)
    modalOverlay.addEventListener('click', closeModal);
    
    // 모달 콘텐츠 클릭 시 이벤트 전파 방지 (모달이 닫히지 않도록)
    // 단, 삭제 버튼 클릭은 제외
    const modalContent = modal.querySelector('.modal-content');
    if (modalContent) {
        modalContent.addEventListener('click', function(event) {
            // 삭제 버튼이나 그 자식 요소가 아닌 경우에만 전파 방지
            if (!event.target.closest('#modalDeleteBtn')) {
                event.stopPropagation();
            }
        });
    }

    // ESC 키로 닫기
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && modal.classList.contains('active')) {
            closeModal();
        }
    });

    // 삭제 버튼 클릭 이벤트 - 이벤트 위임 사용
    // 모달 자체에 이벤트 리스너를 등록하여 동적으로 처리
    modal.addEventListener('click', function(event) {
        // 삭제 버튼 클릭인지 확인
        if (event.target && event.target.id === 'modalDeleteBtn') {
            event.preventDefault();
            event.stopPropagation();
            
            console.log('삭제 버튼 클릭됨, currentPostId:', currentPostId);
            
            if (!currentPostId) {
                alert('삭제할 항목을 찾을 수 없습니다.');
                console.error('currentPostId가 null입니다.');
                return;
            }

            // 삭제 확인
            if (!confirm('정말로 이 활동 기록을 삭제하시겠습니까?')) {
                console.log('삭제 취소됨');
                return;
            }

            console.log('삭제 API 호출 시작, postId:', currentPostId);
            // 삭제 API 호출
            deletePost(currentPostId);
        }
    });
    
    // 삭제 버튼 직접 이벤트 리스너도 추가 (이중 보험)
    if (modalDeleteBtn) {
        modalDeleteBtn.addEventListener('click', function(event) {
            event.preventDefault();
            event.stopPropagation();
            
            console.log('삭제 버튼 직접 클릭 이벤트 발생, currentPostId:', currentPostId);
            
            if (!currentPostId) {
                alert('삭제할 항목을 찾을 수 없습니다.');
                console.error('currentPostId가 null입니다.');
                return;
            }

            // 삭제 확인
            if (!confirm('정말로 이 활동 기록을 삭제하시겠습니까?')) {
                console.log('삭제 취소됨');
                return;
            }

            console.log('삭제 API 호출 시작, postId:', currentPostId);
            // 삭제 API 호출
            deletePost(currentPostId);
        });
        console.log('삭제 버튼 직접 이벤트 리스너 등록 완료');
    } else {
        console.error('삭제 버튼이 없어 이벤트 리스너를 등록할 수 없습니다.');
    }

    // 포스트 삭제 함수
    function deletePost(postId) {
        const apiUrl = `/api_root/Post/${postId}/`;
        const csrfToken = getCookie('csrftoken');
        
        console.log('삭제 요청 시작:', {
            url: apiUrl,
            method: 'DELETE',
            csrfToken: csrfToken ? '존재함' : '없음'
        });
        
        const headers = {
            'Content-Type': 'application/json',
        };
        
        // CSRF 토큰이 있으면 추가
        if (csrfToken) {
            headers['X-CSRFToken'] = csrfToken;
        }
        
        fetch(apiUrl, {
            method: 'DELETE',
            headers: headers,
            credentials: 'same-origin'
        })
        .then(response => {
            console.log('삭제 응답 받음:', {
                status: response.status,
                statusText: response.statusText,
                ok: response.ok
            });
            
            if (response.ok || response.status === 204) {
                // 삭제 성공
                console.log('삭제 성공');
                alert('활동 기록이 삭제되었습니다.');
                closeModal();
                // 페이지 새로고침하여 목록 업데이트
                window.location.reload();
            } else {
                // 삭제 실패
                console.error('삭제 실패, 상태 코드:', response.status);
                return response.json().then(data => {
                    console.error('삭제 실패 응답 데이터:', data);
                    alert('삭제에 실패했습니다: ' + (data.detail || '알 수 없는 오류'));
                }).catch(() => {
                    alert('삭제에 실패했습니다. 상태 코드: ' + response.status);
                });
            }
        })
        .catch(error => {
            console.error('삭제 중 네트워크 오류 발생:', error);
            alert('삭제 중 오류가 발생했습니다: ' + error.message);
        });
    }

    // CSRF 토큰 가져오기 함수
    function getCookie(name) {
        let cookieValue = null;
        if (document.cookie && document.cookie !== '') {
            const cookies = document.cookie.split(';');
            for (let i = 0; i < cookies.length; i++) {
                const cookie = cookies[i].trim();
                if (cookie.substring(0, name.length + 1) === (name + '=')) {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    }
});




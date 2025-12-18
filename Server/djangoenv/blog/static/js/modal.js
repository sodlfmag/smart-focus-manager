// 모달 기능 구현
document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('postModal');
    const modalOverlay = modal.querySelector('.modal-overlay');
    const modalClose = modal.querySelector('.modal-close');
    const modalTitle = document.getElementById('modalTitle');
    const modalStatus = document.getElementById('modalStatus');
    const modalImage = document.getElementById('modalImage');
    const modalImageContainer = document.getElementById('modalImageContainer');
    const modalDate = document.getElementById('modalDate');
    const modalText = document.getElementById('modalText');
    const postCards = document.querySelectorAll('.post-card');

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

    // 오버레이 클릭 시 닫기
    modalOverlay.addEventListener('click', closeModal);

    // ESC 키로 닫기
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && modal.classList.contains('active')) {
            closeModal();
        }
    });
});


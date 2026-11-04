const LOGIN_ID_PATTERN = /^[a-zA-Z0-9]{4,20}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function showCheckResult(resultId, ok, message) {
    const result = document.getElementById(resultId);
    result.className = 'admin-check-result ' + (ok ? 'ok' : 'error');
    result.textContent = message;
}

function clearCheckResult(resultId) {
    const result = document.getElementById(resultId);
    result.className = 'admin-check-result';
    result.textContent = '';
}

async function checkAvailability(url, resultId, availableMessage, unavailableMessage) {
    try {
        const response = await fetch(url);
        if (!response.ok) {
            showCheckResult(resultId, false, '확인 중 오류가 발생했어요');
            return;
        }
        const data = await response.json();
        showCheckResult(resultId, data.available, data.available ? availableMessage : unavailableMessage);
    } catch (e) {
        showCheckResult(resultId, false, '확인 중 오류가 발생했어요');
    }
}

function checkLoginId() {
    const loginId = document.getElementById('loginId').value.trim();

    if (!LOGIN_ID_PATTERN.test(loginId)) {
        showCheckResult('loginIdResult', false, '영문과 숫자 4~20자로 입력해주세요');
        return;
    }
    checkAvailability(
        '/admin/users/check-login-id?loginId=' + encodeURIComponent(loginId),
        'loginIdResult', '사용 가능한 아이디예요', '이미 사용 중인 아이디예요');
}

function checkEmail() {
    const input = document.getElementById('email');
    const email = input.value.trim();

    if (email === '') {
        showCheckResult('emailResult', false, '이메일을 입력해주세요');
        return;
    }
    if (!EMAIL_PATTERN.test(email)) {
        showCheckResult('emailResult', false, '올바른 이메일 형식이 아니에요');
        return;
    }
    // 수정 화면에서 원래 이메일 그대로면 서버에 묻지 않음
    if (email === input.dataset.original) {
        showCheckResult('emailResult', true, '현재 사용 중인 이메일이에요');
        return;
    }
    checkAvailability(
        '/admin/users/check-email?email=' + encodeURIComponent(email),
        'emailResult', '사용 가능한 이메일이에요', '이미 사용 중인 이메일이에요');
}

// 값을 고치면 이전 확인 결과는 지움
document.addEventListener('DOMContentLoaded', () => {
    const loginId = document.getElementById('loginId');
    if (loginId) loginId.addEventListener('input', () => clearCheckResult('loginIdResult'));

    const email = document.getElementById('email');
    if (email) email.addEventListener('input', () => clearCheckResult('emailResult'));
});
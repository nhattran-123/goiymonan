document.addEventListener("DOMContentLoaded", function () {
    const loginForm = document.getElementById('loginForm');
    const msgContainer = document.getElementById('msg-container');
    const btnLogin = document.getElementById('btnLogin');

    loginForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        // UI loading
        btnLogin.innerText = "Đang xử lý...";
        btnLogin.disabled = true;
        msgContainer.innerHTML = '';

        // lấy dữ liệu
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            // gọi API
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            const result = await response.json();

            // xử lý kết quả backend trả về
            if (result.status === "SUCCESS") {

                showMsg('success', 'Đăng nhập thành công! Đang chuyển hướng...');

                setTimeout(() => {
                    window.location.href = result.redirect;
                }, 1000);

            } else {
                showMsg('error', 'Email hoặc mật khẩu không chính xác');
                btnLogin.innerText = "Đăng nhập";
                btnLogin.disabled = false;
            }

        } catch (error) {
            console.error("Lỗi kết nối:", error);

            showMsg('error', 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');

            btnLogin.innerText = "Đăng nhập";
            btnLogin.disabled = false;
        }
    });

    function showMsg(type, content) {
        const className = type === 'success'
            ? 'msg success-msg'
            : 'error-msg';

        msgContainer.innerHTML = `<div class="${className}">${content}</div>`;
    }
});
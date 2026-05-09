$(document).ready(function() {
    // 1. Khởi tạo Select2
    $('.searchable-dropdown').select2({
        placeholder: "Gõ để tìm kiếm...",
        allowClear: true
    });

    // SỬA: Sử dụng API tập trung để load dữ liệu cho Step 4
    async function loadData() {
        try {
            const res = await fetch('/api/auth/register-data');
            const result = await res.json();

            // Xóa dữ liệu cũ trước khi append để tránh lặp
            $('#allergy-select').empty();
            $('#disease-select').empty();

             result.listIngredient.forEach(i => $('#allergy-select').append(new Option(i.ingredientName, i.ingredientId)));
            result.listDisease.forEach(d => $('#disease-select').append(new Option(d.diseaseName, d.diseaseId)));
        } catch (err) {
            console.error("Lỗi khi tải dữ liệu dropdown:", err);
        }
    }

    loadData();

    // 2. Xử lý Submit Form cuối cùng
    $('#regForm').on('submit', async function(e) {
        e.preventDefault();
        const btn = $('#btnSubmit');
        btn.text('Đang xử lý...');
        btn.prop('disabled', true);

        const formData = new FormData(this);
        const data = Object.fromEntries(formData.entries());
        
        // SỬA: Đồng bộ tên thuộc tính khớp với UserDTO (camelCase)
        data.allergyIds = $('#allergy-select').val() || [];
        data.diseaseIds = $('#disease-select').val() || [];
        
        // SỬA: Lấy giá trị Goal Type từ Radio button
        data.goalType = $("input[name='goalType']:checked").val();
        
        // SỬA: Đảm bảo các chỉ số là kiểu số (Number)
        data.age = parseInt(data.age);
        data.height = parseFloat(data.height);
        data.weight = parseFloat(data.weight);
        data.desiredWeight = parseFloat(data.desiredWeight);
        data.desiredHeight = parseFloat(data.desiredHeight);

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            // SỬA: Kiểm tra phản hồi dạng chuỗi "SUCCESS" từ AuthController
            const resultText = await res.text();

            if (resultText === "SUCCESS") {
                // Tự động chuyển hướng sang trang đăng nhập với thông báo thành công
                window.location.href = "/login?registered=true";
            } else if (resultText === "EMAIL_EXISTS") {
                $('#server-error').text("Email này đã được sử dụng. Vui lòng thử email khác.").show();
                btn.text('Hoàn tất').prop('disabled', false);
            } else {
                $('#server-error').text("Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.").show();
                btn.text('Hoàn tất').prop('disabled', false);
            }
        } catch (err) {
            console.error("Lỗi kết nối:", err);
            $('#server-error').text("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.").show();
            btn.text('Hoàn tất').prop('disabled', false);
        }
    });
});

// 3. Logic Validate từng bước (Giữ nguyên cấu trúc, sửa ID trường Step 3)
async function validateAndNext(step) {
    let isValid = true;
    const fields = {
        1: ['fullName', 'email', 'password', 'confirmPassword'],
        2: ['age', 'height', 'weight'],
        3: ['targetWeight'] // Khớp với id="targetWeight" trong HTML
    };

    fields[step].forEach(id => {
        const el = document.getElementById(id);
        if (!el || !el.value.trim()) {
            if(el) el.classList.add('error-border');
            isValid = false;
        } else {
            el.classList.remove('error-border');
        }
    });

    if (isValid && step === 1) {
        const pass = document.getElementById('password').value;
        const cPass = document.getElementById('confirmPassword').value;
        if (pass !== cPass) {
            document.getElementById('passError').style.display = 'block';
            isValid = false;
        } else {
            document.getElementById('passError').style.display = 'none';
        }

        if (isValid) {
            const email = document.getElementById('email').value;
            const check = await fetch(`/api/auth/check-email?email=${encodeURIComponent(email)}`);
            const res = await check.json();
            if (res.exists) {
                document.getElementById('emailError').style.display = 'block';
                isValid = false;
            } else {
                document.getElementById('emailError').style.display = 'none';
            }
        }
    }

    if (isValid) changeStep(step, step + 1);
}

function changeStep(current, next) {
    $(`#step${current}`).hide().removeClass('active');
    $(`#step${next}`).fadeIn().addClass('active');
    
    const width = next === 1 ? '400px' : '480px';
    $('#main-container').css('width', width);
}
$(document).ready(function() {
    $('.searchable-dropdown').select2({
        placeholder: "Bắt đầu gõ để tìm...",
        allowClear: true,
        language: { noResults: () => "Không tìm thấy dữ liệu!" }
    });

    async function loadData() {
        try {
            const res = await fetch('/api/auth/register-data');
            const result = await res.json();
            $('#allergy-select').empty();
            $('#disease-select').empty();
            result.listIngredient.forEach(i => $('#allergy-select').append(new Option(i.name, i.id)));
            result.listDisease.forEach(d => $('#disease-select').append(new Option(d.name, d.id)));
        } catch (err) {
            console.error('Lỗi khi tải dữ liệu dropdown:', err);
        }
    }

    loadData();

    $('#regForm').on('submit', async function(e) {
        e.preventDefault();
        const btn = $('#btnSubmit');
        btn.text('Đang xử lý...').prop('disabled', true);

        const formData = new FormData(this);
        const data = Object.fromEntries(formData.entries());
        
        data.allergyIds = $('#allergy-select').val() || [];
        data.diseaseIds = $('#disease-select').val() || [];
        
        // SỬA: Lấy giá trị Goal Type từ Radio button
        data.goalType = $("input[name='goal']:checked").val();
        
       data.age = parseInt(data.age || 0, 10);
        data.height = parseFloat(data.height || 0);
        data.weight = parseFloat(data.weight || 0);
        data.desiredHeight = parseFloat(data.desired_height || 0);
        data.desiredWeight = parseFloat(data.desired_weight || 0);

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            
            const resultText = await res.text();
             if (resultText === 'SUCCESS') {
                window.location.href = '/login?registered=true';
            } else {
                $('#server-error').text('Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.').show();
                btn.text('Đăng ký →').prop('disabled', false);
            }
        } catch (err) {
            console.error('Lỗi kết nối:', err);
            $('#server-error').text('Không thể kết nối đến máy chủ. Vui lòng thử lại sau.').show();
            btn.text('Đăng ký →').prop('disabled', false);
        }
    });
});

// 3. Logic Validate từng bước (Giữ nguyên cấu trúc, sửa ID trường Step 3)
async function nextStep(currentStep) {
    let isValid = true;
     let fieldsToCheck = [];

    if (currentStep === 1) fieldsToCheck = ['fullName', 'email', 'password', 'confirmPassword'];
    else if (currentStep === 2) fieldsToCheck = ['age', 'height', 'weight'];
    else if (currentStep === 3) fieldsToCheck = ['targetHeight', 'targetWeight'];

    fieldsToCheck.forEach(id => {
        const el = document.getElementById(id);
        if (el.value.trim() === '') {
            el.classList.add('error-border');
            isValid = false;
        } else {
            el.classList.remove('error-border');
        }
    });

     if (isValid && currentStep === 1) {
        const pass = document.getElementById('password').value;
        const cPass = document.getElementById('confirmPassword').value;
        if (pass !== cPass) {
            document.getElementById('confirmPassword').classList.add('error-border');
            document.getElementById('passError').style.display = 'block';
            isValid = false;
        } else {
            document.getElementById('passError').style.display = 'none';
        }

        if (isValid) {
            try {
                const emailVal = document.getElementById('email').value;
                const check = await fetch(`/api/auth/check-email?email=${encodeURIComponent(emailVal)}`);
                const data = await check.json();
                if (data.exists) {
                    document.getElementById('email').classList.add('error-border');
                    document.getElementById('emailError').style.display = 'block';
                    isValid = false;
                } else {
                    document.getElementById('emailError').style.display = 'none';
                }
            } catch (e) {
                console.error(e);
            }
        }
    }

    if (isValid) changeStep(currentStep, currentStep + 1);
}

function changeStep(current, next) {
    $(`#step${current}`).hide().removeClass('active');
    $(`#step${next}`).fadeIn().addClass('active');
    $('#main-container').css('width', next === 1 ? '400px' : '480px');
}
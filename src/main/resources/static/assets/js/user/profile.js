$(document).ready(function() {
    // 1. Khởi tạo Select2 với giao diện thẻ (Tags)
    $('.searchable-dropdown').select2({
        placeholder: "Gõ để tìm kiếm (ví dụ: Tiểu đường, Tôm, ...)",
        allowClear: true,
        closeOnSelect: false, 
        language: { noResults: () => "Không tìm thấy dữ liệu" }
    });

    // Hàm chuyển đổi chế độ Xem/Sửa
    function toggleEditMode(isEditing) {
        const form = $('#profileForm');
        form.find('input, select').prop('disabled', !isEditing);
        
        if (isEditing) {
            $('#edit-btn').addClass('d-none');
            $('#action-buttons').removeClass('d-none');
        } else {
            $('#edit-btn').removeClass('d-none');
            $('#action-buttons').addClass('d-none');
        }
    }

    function isInvalidNumber(value) {
        return Number.isNaN(value) || value <= 0;
    }

    // 2. Tải dữ liệu ban đầu
    async function initProfile() {
        try {
            const [profileRes, diseaseRes, ingredientRes] = await Promise.all([
                fetch('/api/user/profile'),
                fetch('/api/user/diseases'), // Cập nhật endpoint theo Controller của bạn
                fetch('/api/user/ingredients') // Cập nhật endpoint theo Controller của bạn
            ]);

            const profile = await profileRes.json();
            const diseases = await diseaseRes.json();
            const ingredients = await ingredientRes.json();

            // Đổ dữ liệu cơ bản (Đã sửa tên biến theo UserDTO)
            $('#gender').val(profile.gender).trigger('change');
            $('#age').val(profile.age);
            $('#weight').val(profile.weight);
            $('#height').val(profile.height);
            $('#desired_weight').val(profile.desiredWeight); // camelCase
            $('#desired_height').val(profile.desiredHeight); // camelCase

            // Render danh sách Bệnh lý
            const diseaseSelect = $('#disease_ids');
            diseaseSelect.empty();
            diseases.forEach(d => {
                // Kiểm tra theo diseaseIds trong UserDTO
                const isSelected = profile.diseaseIds && profile.diseaseIds.includes(d.diseaseId);
                const option = new Option(d.diseaseName, d.diseaseId, isSelected, isSelected);
                diseaseSelect.append(option);
            });
            diseaseSelect.trigger('change');

            // Render danh sách Dị ứng
            const allergySelect = $('#allergy_ids');
            allergySelect.empty();
            ingredients.forEach(i => {
                // Kiểm tra theo allergyIds trong UserDTO
                const isSelected = profile.allergyIds && profile.allergyIds.includes(i.ingredientId);
                const option = new Option(i.ingredientName, i.ingredientId, isSelected, isSelected);
                allergySelect.append(option);
            });
            allergySelect.trigger('change');

            toggleEditMode(false);

        } catch (error) {
            console.error("Lỗi tải dữ liệu hồ sơ:", error);
            alert("Không thể tải dữ liệu hồ sơ. Vui lòng thử lại.");
        }
    }

    initProfile();

    $('#edit-btn').on('click', function() {
        toggleEditMode(true);
    });

    $('#cancel-btn').on('click', function() {
        initProfile(); 
    });

    // 3. Xử lý lưu hồ sơ
    $('#profileForm').on('submit', async function(e) {
        e.preventDefault();

        // Tên các thuộc tính ở đây PHẢI khớp chính xác với UserDTO.java
        const formData = {
            gender: $('#gender').val(),
             age: parseInt($('#age').val(), 10),
            weight: parseFloat($('#weight').val()),
            height: parseFloat($('#height').val()),
           desiredWeight: parseFloat($('#desired_weight').val()),
            desiredHeight: parseFloat($('#desired_height').val()),
            diseaseIds: ($('#disease_ids').val() || []).map(Number),
            allergyIds: ($('#allergy_ids').val() || []).map(Number)
        };
        if (isInvalidNumber(formData.age) || formData.age > 120 ||
            isInvalidNumber(formData.weight) ||
            isInvalidNumber(formData.height) ||
            isInvalidNumber(formData.desiredWeight) ||
            isInvalidNumber(formData.desiredHeight)) {
            alert('Vui lòng nhập đầy đủ và hợp lệ cho tuổi, cân nặng, chiều cao.');
            return;
        }

        try {
            const response = await fetch('/api/user/update-profile', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                alert("Hồ sơ sức khỏe của bạn đã được cập nhật thành công!");
                initProfile(); 
            } else {
                alert("Có lỗi xảy ra khi lưu. Vui lòng thử lại.");
            }
        } catch (error) {
            console.error("Lỗi gửi form:", error);
        }
    });
});
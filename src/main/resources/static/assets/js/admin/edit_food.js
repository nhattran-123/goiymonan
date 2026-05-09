$(document).ready(function() {
    let allIngredients = [];
    const urlParams = new URLSearchParams(window.location.search);
    const foodId = urlParams.get('id'); // Lấy ID từ ?id=...

    if (!foodId) {
        alert("Không tìm thấy ID món ăn!");
        window.location.href = "/admin/manage_food";
        return;
    }

    // 1. Tải danh sách tất cả nguyên liệu và Dữ liệu món ăn cũ
    async function initData() {
        try {
            // Load danh sách nguyên liệu cho Select2
            const ingResponse = await fetch('/api/admin/ingredients');
            allIngredients = await ingResponse.json();
            
            // Cập nhật template row
            const $templateSelect = $('.template-row .ing-select');
            allIngredients.forEach(ing => {
                $templateSelect.append(`<option value="${ing.id}" data-cal="${ing.calories}" data-pro="${ing.protein}" data-fat="${ing.fat}" data-carb="${ing.carbohydrate}">${ing.name} (${ing.category})</option>`);
            });

            // Load dữ liệu món ăn hiện tại
            const foodResponse = await fetch(`/api/admin/foods/${foodId}`);
            const foodData = await foodResponse.json();
            
            fillFoodData(foodData);
        } catch (error) {
            console.error("Lỗi khởi tạo:", error);
        }
    }

    initData();

    // 2. Đổ dữ liệu vào Form
    function fillFoodData(data) {
       $('#foodId').val(data.foodId ?? data.id ?? '');
        $('#foodName').val(data.foodName ?? data.name ?? '');
        $('#description').val(data.description);
        $('#recipe').val(data.recipe);
         $('#foodType').val(data.foodType ?? '');
        $('#imagePreview').attr('src', data.imageUrl || '/assets/images/default-food.png');

        // Đổ danh sách nguyên liệu đã có
       const ingredients = Array.isArray(data.ingredients) ? data.ingredients : [];
        ingredients.forEach(item => {
            addRowWithData(item.ingredientId, item.quantity, item.unit);
        });
        
        calculateMacros();
    }

    function addRowWithData(id, qty, unit) {
        let $newRow = $('.template-row').clone().removeClass('template-row').show();
        $newRow.find('input, select').removeAttr('disabled');
        $('#ingredient-container').append($newRow);
        
        const $select = $newRow.find('.ing-select');
        $select.select2({ width: '100%' }).val(id).trigger('change');
        $newRow.find('.ing-qty').val(qty);
        $newRow.find('.ing-unit').val(unit);
    }

    // 3. Logic tính toán Macros (Giống add_food)
    function calculateMacros() {
        let totalCal = 0, totalPro = 0, totalFat = 0, totalCarb = 0;
        $('.ingredient-row:not(.template-row)').each(function() {
            let $selectedOption = $(this).find('.ing-select option:selected');
            let qty = parseFloat($(this).find('.ing-qty').val()) || 0;
            if($selectedOption.val()) {
                let ratio = qty / 100;
                totalCal += ratio * parseFloat($selectedOption.data('cal') || 0);
                totalPro += ratio * parseFloat($selectedOption.data('pro') || 0);
                totalFat += ratio * parseFloat($selectedOption.data('fat') || 0);
                totalCarb += ratio * parseFloat($selectedOption.data('carb') || 0);
            }
        });
        $('#totalCal').val(totalCal.toFixed(1));
        $('#totalPro').val(totalPro.toFixed(1));
        $('#totalFat').val(totalFat.toFixed(1));
        $('#totalCarb').val(totalCarb.toFixed(1));
    }

    // 4. Các sự kiện tương tác UI
    $('#btnAddRow').click(function() {
        addRowWithData('', '', 'g');
    });

    $('#imageFileInput').on('change', function() {
        if (this.files.length > 0) {
            $('#fileNameDisplay').text(this.files[0].name).css('color', '#10b981');
            // Preview ảnh mới
            const reader = new FileReader();
            reader.onload = e => $('#imagePreview').attr('src', e.target.result);
            reader.readAsDataURL(this.files[0]);
        }
    });

    $('#ingredient-container').on('change', '.ing-select', calculateMacros);
    $('#ingredient-container').on('input', '.ing-qty', calculateMacros);
    $('#ingredient-container').on('click', '.btn-remove-row', function() {
        $(this).closest('.ingredient-row').remove();
        calculateMacros();
    });

    // 5. Submit cập nhật
    $('#editFoodForm').on('submit', async function(e) {
        e.preventDefault();
        
        let formData = new FormData(this);
        // Thêm danh sách nguyên liệu động
        $('.ingredient-row:not(.template-row)').each(function() {
            formData.append('ingredientIds', $(this).find('.ing-select').val());
            formData.append('quantities', $(this).find('.ing-qty').val());
            formData.append('units', $(this).find('.ing-unit').val());
        });

        try {
            const res = await fetch(`/api/admin/foods/${foodId}`, {
                method: 'PUT',
                body: formData
            });

            if (res.ok) {
                alert("Cập nhật món ăn thành công!");
                window.location.href = "/admin/manage_food";
            } else {
                $('#errorMessage').show();
            }
        } catch (err) {
            console.error(err);
        }
    });
});
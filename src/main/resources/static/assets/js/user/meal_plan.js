$(document).ready(function() {
    let selectedDate = new URLSearchParams(window.location.search).get('date') || new Date().toISOString().split('T')[0];
    let allFoodsData = []; // Danh sách món ăn để chọn trong Modal

    // Khởi tạo
    $('#date-input').val(selectedDate);
    initPage();

    async function initPage() {
        await fetchMealPlan();
        await fetchAllFoods(); // Load sẵn dữ liệu cho Modal
    }

    // 1. Lấy dữ liệu thực đơn từ API
    async function fetchMealPlan() {
        try {
            const response = await fetch(`/api/user/meal-plan?date=${selectedDate}`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.json();

            // Cập nhật giao diện
            document.getElementById('total-day-calo').innerText = data.totalCalories.toFixed(1);
            document.getElementById('edit-notice').innerText = data.canEdit ? "Bạn có thể chỉnh sửa thực đơn ngày này." : "Ngày đã qua chỉ có thể xem.";
            
             renderSlider(Array.isArray(data.weekSlider) ? data.weekSlider : []);
            renderMealSections(Array.isArray(data.mealSections) ? data.mealSections : [], !!data.canEdit);
        } catch (err) {
            console.error("Lỗi tải thực đơn:", err);
             $("#week-slider-container").html(`<p class="empty-text">Không tải được dữ liệu tuần.</p>`);
            $("#meal-sections-container").html(`<p class="empty-text">Không tải được thực đơn. Vui lòng tải lại trang.</p>`);
        }
    }

    // 2. Render Slider tuần
    function renderSlider(weekSlider) {
        const container = $('#week-slider-container');
        container.empty();
        weekSlider.forEach(d => {
            const activeClass = d.selected ? 'active' : '';
            const dotClass = d.hasMeals ? 'has-meal' : '';
            container.append(`
                <a href="javascript:void(0)" onclick="changeDate('${d.date}')" class="day-pill ${activeClass}">
                    <span class="dow">${d.dow}</span>
                    <strong>${d.day}</strong>
                    <span class="dot ${dotClass}"></span>
                </a>
            `);
        });
    }

    // 3. Render danh sách các bữa ăn (Sáng, Trưa, Tối...)
    function renderMealSections(sections, canEdit) {
        const container = $('#meal-sections-container');
        container.empty();

        sections.forEach(section => {
            let foodsHtml = '';
            if (!section.foods || section.foods.length === 0) {
                foodsHtml = `<p class="empty-text">Chưa có món nào.</p>`;
            } else {
                section.foods.forEach(food => {
                    foodsHtml += `
                        <div class="food-pill">
                            <a href="/food-detail?id=${food.foodId}" class="food-link">
                                <img src="/assets/images/${food.imageUrl}" alt="${food.foodName}">
                                <div>
                                    <h4>${food.foodName}</h4>
                                    <p>${food.calories.toFixed(1)} calo</p>
                                </div>
                            </a>
                            ${canEdit ? `<button class="remove-btn" onclick="removeFood(${food.detailId})">x</button>` : ''}
                        </div>
                    `;
                });
            }

            const adjustBtn = canEdit ? `<button type="button" class="text-btn" onclick="openAdjustModal(${section.mealTypeId}, '${section.mealName}', '${section.foodIdsString}')">Điều chỉnh</button>` : '';

            container.append(`
                <article class="card meal-card-item">
                    <div class="meal-row-head">
                        <div class="meal-title"><i class="fa-regular fa-sun"></i> ${section.mealName}</div>
                        <div class="meal-meta">
                            ${section.usedCalories.toFixed(1)} / ${section.targetCalories.toFixed(1)} calo
                            ${adjustBtn}
                        </div>
                    </div>
                    ${foodsHtml}
                </article>
            `);
        });
    }

    // 4. Xử lý Modal & Select2
    window.openAdjustModal = function(id, name, currentFoodIds) {
        $('#adjustMealTypeId').val(id);
        $('#adjustTitle').text('Điều chỉnh cho ' + name);
        
        // Reset và gán giá trị cũ cho Select2
        const selectedArray = currentFoodIds ? currentFoodIds.split(',') : [];
        $('#adjustFoodIds').val(selectedArray).trigger('change');
        
        $('#adjustModal').addClass('show');
    };

    async function fetchAllFoods() {
        const res = await fetch('/api/user/all-foods-simple');
        allFoodsData = await res.json();
        const select = $('#adjustFoodIds');
        allFoodsData.forEach(f => {
            select.append(new Option(`${f.name} (${f.calories} calo)`, f.id));
        });
        select.select2({ width: '100%', placeholder: "Chọn món ăn..." });
    }

    // 5. Thao tác điều hướng & Xóa
    window.changeDate = function(newDate) {
         if (!newDate) return;
        selectedDate = newDate;
        $('#date-input').val(newDate);
        const url = new URL(window.location.href);
        url.searchParams.set('date', newDate);
        window.history.replaceState({}, '', url);
        fetchMealPlan();
    };

    $('#btn-view-date').click(() => changeDate($('#date-input').val()));

    window.removeFood = async function(detailId) {
        if (!confirm("Xóa món này?")) return;
        const res = await fetch(`/api/user/meal-plan/remove?detailId=${detailId}`, { method: 'DELETE' });
        if (res.ok) fetchMealPlan();
    };

    // 6. Submit Modal
    $('#adjustMealForm').submit(async function(e) {
        e.preventDefault();
        const foodIds = $('#adjustFoodIds').val();
        const mealTypeId = $('#adjustMealTypeId').val();
        const submitBtn = $(this).find('button[type="submit"]');
        submitBtn.prop('disabled', true);

       try {
            const res = await fetch('/api/user/meal-plan/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ date: selectedDate, mealTypeId, foodIds })
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(errorText || `HTTP ${res.status}`);
            }


            $('#adjustModal').removeClass('show');
            fetchMealPlan();
            } catch (err) {
            console.error('Lưu thực đơn thất bại:', err);
            alert('Không lưu được thay đổi. Vui lòng thử lại.');
        } finally {
            submitBtn.prop('disabled', false);
        }
    });

    $('.close-modal').click(() => $('.modal').removeClass('show'));
});
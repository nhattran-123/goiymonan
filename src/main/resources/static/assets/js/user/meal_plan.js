$(document).ready(function() {
    // 1. KHỞI TẠO BIẾN
    let selectedDate = new URLSearchParams(window.location.search).get('date') || new Date().toISOString().split('T')[0];
    let currentMenuId = null;
    let allSafeFoods = []; 
    let pendingFoods = { 1: [], 2: [], 3: [], 4: [] };
    let userTDEE = 2000;

    const mealTypes = [
        { id: 1, name: 'Bữa sáng', icon: 'fa-sun' },
        { id: 2, name: 'Bữa trưa', icon: 'fa-cloud-sun' },
        { id: 3, name: 'Bữa tối', icon: 'fa-moon' },
        { id: 4, name: 'Bữa phụ', icon: 'fa-cookie-bite' }
    ];

    $('#date-input').val(selectedDate);
    fetchMealPlan();
    renderWeekSlider();

    // 2. LẤY DỮ LIỆU TỪ SERVER
    async function fetchMealPlan() {
        try {
            // Lấy TDEE từ summary (mục tiêu calo)
            const summaryResponse = await fetch('/api/user/home-summary');
            const summaryData = await summaryResponse.json();
            userTDEE = summaryData.targetCalories || 2000;
            $('#user-tdee').text(Math.round(userTDEE));

            // Lấy dữ liệu thực đơn của NGÀY ĐANG CHỌN (selectedDate)
            const menuResponse = await fetch(`/api/user/meal-plan?date=${selectedDate}`);
            const menuData = await menuResponse.json();
            currentMenuId = menuData.menuId;

            // CẬP NHẬT: Lấy total_calories từ đúng ngày đang xem trong DB
            $('#total-day-calo').text(Math.round(menuData.totalConsumedCalories || 0));

            renderLayout(menuData.sections || []);
        } catch (err) {
            console.error("Lỗi tải dữ liệu:", err);
            renderLayout([]);
        }
    }

    function renderLayout(serverSections) {
        const container = $('#meal-sections-container');
        container.empty();
        let totalPlannedCalo = 0;

        mealTypes.forEach(type => {
            const sectionData = serverSections.find(s => s.mealTypeId === type.id) || { foods: [], confirmed: false };
            const isLocked = sectionData.confirmed; 
            
            const displayFoods = [
                ...sectionData.foods.map(f => ({ ...f, isPending: false })),
                ...pendingFoods[type.id].map(f => ({ ...f, isPending: true }))
            ];

            displayFoods.forEach(f => totalPlannedCalo += f.calories);

            // Render danh sách món ăn
            let foodsHtml = displayFoods.length > 0 ? displayFoods.map(f => `
                <div class="food-pill ${f.isPending ? 'pending-item' : ''}">
                    <img src="/images/${f.imageUrl || 'default.png'}" alt="">
                    <span>${f.foodName} (${f.calories} calo)</span>
                    ${!isLocked ? `
                        <button class="btn-remove" onclick="${f.isPending ? `removePending(${type.id}, ${f.foodId})` : `removeFood(${f.detailId})`}">×</button>
                    ` : '<i class="fa-solid fa-check" style="color: #2ecc71; margin-left: 5px;"></i>'}
                </div>
            `).join('') : '<p class="empty-text">Chưa có món ăn nào trong kế hoạch.</p>';

            // Render Card của từng bữa
            container.append(`
                <div class="meal-card ${isLocked ? 'is-confirmed' : ''}">
                    <div class="meal-header"><i class="fa-solid ${type.icon}"></i> ${type.name}</div>
                    <div class="meal-body">${foodsHtml}</div>
                    <div class="meal-footer">
                        ${!isLocked ? `
                            <button class="btn-add-circle" onclick="openAddModal(${type.id})" title="Thêm món">
                                <i class="fa-solid fa-plus"></i>
                            </button>
                            ${pendingFoods[type.id].length > 0 ? `<button class="btn-save-meal" onclick="saveSingleMeal(${type.id})">Lưu bữa này</button>` : ''}
                            ${sectionData.foods.length > 0 && pendingFoods[type.id].length === 0 ? `<button class="btn-check" onclick="confirmMeal(${type.id})">Đã ăn</button>` : ''}
                        ` : `<span class="lock-msg"><i class="fa-solid fa-circle-check"></i> Đã hoàn thành</span>`}
                    </div>
                </div>
            `);
        });

        updatePlanningUI(totalPlannedCalo);
    }

    function updatePlanningUI(total) {
        $('#planned-calo').text(Math.round(total));
        const percent = Math.min((total / userTDEE) * 100, 100);
        $('#tdee-progress-fill').css('width', percent + '%');
        
        if (total > userTDEE) {
            $('#tdee-progress-fill').css('background', '#e74c3c');
            $('#tdee-warning').show();
        } else {
            $('#tdee-progress-fill').css('background', '#2ecc71');
            $('#tdee-warning').hide();
        }
    }

    // 3. XỬ LÝ MODAL & CHỌN MÓN
    window.openAddModal = async function(mealTypeId) {
        $('#adjustMealTypeId').val(mealTypeId);
        $('#adjustTitle').text(`Thêm món cho ${mealTypes.find(t => t.id === mealTypeId).name}`);
        $('#adjustModal').fadeIn();

        if (allSafeFoods.length === 0) {
            const response = await fetch('/api/user/suggested-foods');
            allSafeFoods = await response.json();
        }
        renderFoodList(mealTypeId === 1 ? 0 : 1);
    };

    function renderFoodList(typeId) {
        $('.tab-btn').removeClass('active');
        $(`.tab-btn[data-type="${typeId}"]`).addClass('active');
        const container = $('#food-list-container').empty();
        const filtered = allSafeFoods.filter(f => f.foodType == typeId);
        filtered.forEach(food => {
            container.append(`
                <div class="food-choice-item" onclick="addToPending(${food.foodId})">
                    <img src="/images/${food.imageUrl || 'default.png'}">
                    <div class="detail"><strong>${food.foodName}</strong><span>${food.calories} calo</span></div>
                    <div class="add-icon"><i class="fa-solid fa-plus"></i></div>
                </div>
            `);
        });
    }

    window.addToPending = function(foodId) {
        const typeId = $('#adjustMealTypeId').val();
        const food = allSafeFoods.find(f => f.foodId === foodId);
        if (food) {
            pendingFoods[typeId].push(food);
            $('#adjustModal').fadeOut();
            fetchMealPlan();
        }
    };

    window.removePending = function(mealTypeId, foodId) {
        pendingFoods[mealTypeId] = pendingFoods[mealTypeId].filter(f => f.foodId !== foodId);
        fetchMealPlan();
    };

    // 4. LƯU & XÁC NHẬN
    window.saveSingleMeal = async function(mealTypeId) {
        if(!currentMenuId) return;
        const items = pendingFoods[mealTypeId].map(f => ({ foodId: f.foodId }));

        try {
            const response = await fetch('/api/user/meal-plan/save-batch', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    menuId: currentMenuId,
                    mealTypeId: mealTypeId,
                    items: items
                })
            });

            if (response.ok) {
                alert("Đã lưu kế hoạch!");
                pendingFoods[mealTypeId] = [];
                await fetchMealPlan();
            }
        } catch (error) {
            console.error("Lỗi:", error);
        }
    };

    window.confirmMeal = async function(mealTypeId) {
        if(!confirm("Xác nhận đã ăn bữa này?")) return;

        try {
            const response = await fetch('/api/user/meal-plan/confirm', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ 
                    menuId: currentMenuId, 
                    mealTypeId: mealTypeId,
                    selectedDate: selectedDate 
                })
            });

            if (response.ok) {
                const res = await response.json();
                alert(res.message || "Đã cập nhật Calo nạp vào!");
                await fetchMealPlan(); 
            }
        } catch (err) {
            console.error("Lỗi xác nhận:", err);
        }
    };

    // 5. SLIDER & TIỆN ÍCH
    function renderWeekSlider() {
        const track = $('#week-slider-container').empty();
        const baseDate = new Date(selectedDate);
        for (let i = -3; i <= 3; i++) {
            const d = new Date(baseDate);
            d.setDate(baseDate.getDate() + i);
            const dateStr = d.toISOString().split('T')[0];
            const dayName = d.toLocaleDateString('vi-VN', { weekday: 'short' });
            const dayNum = d.getDate();
            const monthNum = d.getMonth() + 1;
            track.append(`
                <div class="day-item ${dateStr === selectedDate ? 'active' : ''}" 
                 onclick="changeDate('${dateStr}')"
                 style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 8px 0; min-width: 60px;">
                
                <span style="font-size: 0.8em; opacity: 0.9;">${dayName}</span>
                
                <strong style="font-size: 1.2em; margin: 2px 0;">${dayNum}</strong>
                
                <span style="font-size: 0.75em; font-weight: 500; color: ${dateStr === selectedDate ? '#fff' : '#888'};">
                    Tháng ${monthNum}
                </span>
                </div>
            `);
        }
    }

    window.changeDate = date => window.location.href = `/meal_plan?date=${date}`;
    $('.close-modal').click(() => $('#adjustModal').fadeOut());
    $(document).on('click', '.tab-btn', function() { renderFoodList($(this).data('type')); });
    $('#prev-week').on('click', function() {
        let d = new Date(selectedDate);
        d.setDate(d.getDate() - 1); // Lùi 1 ngày
        changeDate(d.toISOString().split('T')[0]);
    });

    $('#next-week').on('click', function() {
        let d = new Date(selectedDate);
        d.setDate(d.getDate() + 1); // Tiến 1 ngày
        changeDate(d.toISOString().split('T')[0]);
    });
});
document.addEventListener("DOMContentLoaded", function() {
    fetchHomeSummary();
});

async function fetchHomeSummary() {
    try {
        const response = await fetch('/api/user/home-summary');
        const data = await response.json();

        // 1. Cập nhật thông tin cơ bản (Giữ nguyên logic cũ của Thành)
        if (document.getElementById('user-fullname')) document.getElementById('user-fullname').innerText = data.name || "Người dùng";
        if (document.getElementById('bmi-val')) document.getElementById('bmi-val').innerText = data.bmi ? data.bmi.toFixed(1) : "0.0";
        if (document.getElementById('bmr-val')) document.getElementById('bmr-val').innerText = Math.round(data.bmr || 0).toLocaleString();
        if (document.getElementById('goal-label')) document.getElementById('goal-label').innerText = data.goalType || "Chưa xác định";
        
        // 2. Cập nhật BMI Badge (Sửa màu nhạt theo ý Thành bằng Inline Style)
        const bmiBadge = document.getElementById('bmi-badge');
        if (bmiBadge) {
            const bmi = data.bmi;
            let statusText = data.bmiStatus || "Bình thường";
            let bgColor = "rgba(16, 185, 129, 0.15)"; // Mặc định xanh lá nhạt
            let textColor = "#10b981";

            if (bmi < 18.5) { bgColor = "rgba(59, 130, 246, 0.15)"; textColor = "#3b82f6"; statusText = "Cân nặng thấp"; }
            else if (bmi >= 25 && bmi < 30) { bgColor = "rgba(245, 158, 11, 0.15)"; textColor = "#f59e0b"; statusText = "Thừa cân"; }
            else if (bmi >= 30) { bgColor = "rgba(239, 68, 68, 0.15)"; textColor = "#ef4444"; statusText = "Béo phì"; }

            bmiBadge.innerText = statusText;
            bmiBadge.style.background = bgColor;
            bmiBadge.style.color = textColor;
            bmiBadge.style.display = "flex";
            bmiBadge.style.justifyContent = "center";
            bmiBadge.style.alignItems = "center";
        }

        // 3. Calo Progress (Giữ nguyên)
        const todayCalo = data.todayCalories || 0;
        const targetCalo = data.targetCalories || 2000;
        const remainCalo = data.remainCalories || 0;
        if (document.getElementById('target-calo-box')) document.getElementById('target-calo-box').innerText = Math.round(targetCalo).toLocaleString();
        if (document.getElementById('today-calo-box')) document.getElementById('today-calo-box').firstChild.textContent = Math.round(todayCalo).toLocaleString() + " ";
        if (document.getElementById('remain-calo-box')) document.getElementById('remain-calo-box').innerText = Math.round(remainCalo).toLocaleString();
        const percent = Math.min((todayCalo / targetCalo) * 100, 100);
        if (document.getElementById('calo-progress-fill')) document.getElementById('calo-progress-fill').style.width = percent + "%";
        window.currentMenuId = data.menuId;

        // 4. Render Bữa ăn (Tự định nghĩa layout 2x2 bằng JS)
        renderMeals(data.todayMeals);

        // 5. Render Gợi ý
        renderSuggestions((data.homeSuggestions || []).slice(0, 6));

    } catch (error) {
        console.error("Lỗi tải trang chủ:", error);
    }
}

function renderMeals(meals) {
    const container = document.getElementById('meals-container');
    if (!container) return;

    container.style.display = "grid";
    container.style.gridTemplateColumns = "1fr 1fr";
    container.style.gap = "25px"; 

    const mealTypes = [
        { id: 1, name: 'Bữa sáng', icon: 'fa-sun' },
        { id: 2, name: 'Bữa trưa', icon: 'fa-cloud-sun' },
        { id: 3, name: 'Bữa tối', icon: 'fa-moon' },
        { id: 4, name: 'Bữa phụ', icon: 'fa-cookie-bite' }
    ];

    container.innerHTML = mealTypes.map(type => {
        const mealData = meals ? meals.find(m => m.mealTypeId === type.id) : null;
        const isConfirmed = mealData && (mealData.confirmed === true || mealData.isConfirmed === true);
        const foods = mealData && mealData.foods ? mealData.foods : []; 
        const caloText = mealData ? Math.round(mealData.totalCalories) : 0;

        // TỰ ĐỘNG ĐỔI MÀU THEO TRẠNG THÁI
        const cardBg = isConfirmed ? "#f0fdf4" : "#ffffff"; // Xanh nhạt nếu đã ăn, trắng nếu chưa
        const cardBorder = isConfirmed ? "#bbf7d0" : "#f1f5f9"; // Viền xanh nếu đã ăn

        return `
            <div class="meal-card" style="
                min-height: 220px;
                background: ${cardBg}; 
                border: 2px solid ${cardBorder}; 
                border-radius: 20px;
                padding: 22px;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.07);
                transition: all 0.3s ease; /* Hiệu ứng chuyển màu mượt mà */
            ">
                <div>
                    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; width: 100%;">
                        <h4 style="margin:0; font-size: 18px; color: #1e293b; display: flex; align-items: center; gap: 12px; font-weight: 700; flex: 1;">
                            <i class="fa-solid ${type.icon}" style="color: #10b981; font-size: 20px; width: 25px;"></i>
                            <span style="white-space: nowrap;">${type.name}</span>
                        </h4>
                        <div style="text-align: right; margin-left: 10px;">
                            <span style="font-weight: 800; color: #10b981; font-size: 18px; white-space: nowrap;">
                                ${caloText} <small style="font-size: 13px; font-weight: 500; color: #94a3b8;">kcal</small>
                            </span>
                        </div>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 12px;">
                        ${foods.length > 0 ? foods.map(food => `
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <img src="/images/${food.imageUrl}" 
                                     style="width: 30px; height: 30px; object-fit: cover; border-radius: 50%; border: 1px solid #e2e8f0;"
                                     onerror="this.src='https://via.placeholder.com/30?text=F'">
                                <span style="font-size: 14px; color: #475569; font-weight: 500;">${food.foodName}</span>
                            </div>
                        `).join('') : '<span style="color: #cbd5e1; font-style: italic; font-size: 14px;">Chưa có món ăn</span>'}
                    </div>
                </div>

                <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 25px; padding-top: 15px; border-top: 1.5px solid ${isConfirmed ? '#dcfce7' : '#f8fafc'};">
                    <span style="font-size: 13px; color: #64748b; font-weight: 600;">
                        <i class="fa-solid fa-utensils" style="margin-right: 5px;"></i> ${foods.length} món
                    </span>
                    
                </div>
            </div>
        `;
    }).join('');
}
async function confirmMealHome(mealTypeId) {
    if(!confirm("Xác nhận bạn đã ăn bữa này?")) return;
    const response = await fetch('/api/user/meal-plan/confirm-quick', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ mealTypeId: mealTypeId })
    });
    if (response.ok) fetchHomeSummary();
}

function renderSuggestions(foods) {
    const container = document.getElementById('suggestions-container');
    if (!container) return;
    container.innerHTML = foods.map(food => `
        <a href="/food-detail?id=${food.foodId}" style="text-decoration:none; color:inherit;">
            <div class="food-card" style="height: 250px;">
                <img src="/images/${food.imageUrl}" class="food-img" style="height: 140px; width: 100%; object-fit: cover;" onerror="this.src='https://via.placeholder.com/300x200?text=Food'">
                <div class="status-badge ${food.allergyConflictCount === 0 ? 'bg-safe' : 'bg-warn'}">
                    <i class="fa-solid ${food.allergyConflictCount === 0 ? 'fa-check' : 'fa-shield-halved'}"></i> 
                    ${food.allergyConflictCount === 0 ? 'An toàn' : 'Dị ứng'}
                </div>
                <div class="food-content" style="padding: 12px;">
                    <h4 style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 5px;">${food.foodName}</h4>
                    <p style="font-weight: 600; color: #10b981; margin: 0;">${Math.round(food.calories)} calo</p>
                    <div class="food-footer" style="margin-top: 8px; border-top: 1px solid #eee; padding-top: 8px;">
                        <i class="fa-regular fa-star" style="color: #fbbf24;"></i> <span>${Math.round(food.suitabilityScore)}% phù hợp</span>
                    </div>
                </div>
            </div>
        </a>
    `).join('');
}
document.addEventListener("DOMContentLoaded", function() {
    fetchHomeSummary();
});

async function fetchHomeSummary() {
    try {
        const response = await fetch('/api/user/home-summary');
        const data = await response.json();

        // 1. Thông tin cá nhân & Chỉ số
        if (document.getElementById('user-fullname')) {
            document.getElementById('user-fullname').innerText = data.name || "Người dùng";
        }
        if (document.getElementById('bmi-val')) {
            document.getElementById('bmi-val').innerText = data.bmi ? data.bmi.toFixed(1) : "0.0";
        }
        if (document.getElementById('bmr-val')) {
            document.getElementById('bmr-val').innerText = Math.round(data.bmr || 0).toLocaleString();
        }
        if (document.getElementById('goal-label')) {
            document.getElementById('goal-label').innerText = data.goalType || "Chưa xác định";
        }
        
        // Cập nhật Badge BMI
        const bmiBadge = document.getElementById('bmi-badge');
        if (bmiBadge) {
            bmiBadge.innerText = data.bmiStatus || "Đang tải...";
            bmiBadge.style.color = "#ffffff";
            bmiBadge.style.fontWeight = "600";

            if (typeof data.bmi === "number") {
                if (data.bmi < 18.5) bmiBadge.style.background = "#3b82f6"; // Gầy
                else if (data.bmi < 25) bmiBadge.style.background = "#10b981"; // Bình thường
                else bmiBadge.style.background = "#ef4444"; // Thừa cân
            } else {
                bmiBadge.style.background = "#9ca3af";
            }
        }

        // 2. Xử lý Calo Progress Bar
        const todayCalo = data.todayCalories || 0;
        const targetCalo = data.targetCalories || 2000;
        const remainCalo = data.remainCalories || 0;
        
        // SỬA LỖI TẠI ĐÂY: Cập nhật riêng biệt để không làm mất thẻ con
        if (document.getElementById('target-calo-box')) {
            document.getElementById('target-calo-box').innerText = Math.round(targetCalo).toLocaleString();
        }
        
        // Cập nhật số calo đã ăn (chỉ thay đổi text đầu tiên của thẻ h2 để giữ lại thẻ span)
        const todayBox = document.getElementById('today-calo-box');
        if (todayBox) {
            todayBox.firstChild.textContent = Math.round(todayCalo).toLocaleString() + " ";
        }

        if (document.getElementById('remain-calo-box')) {
            document.getElementById('remain-calo-box').innerText = Math.round(remainCalo).toLocaleString();
        }

        const percent = Math.min((todayCalo / targetCalo) * 100, 100);
        if (document.getElementById('calo-progress-fill')) {
            document.getElementById('calo-progress-fill').style.width = percent + "%";
        }

        // 3. Render Bữa ăn hôm nay
        renderMeals(data.todayMeals);

        // 4. Render Gợi ý món ăn (Chỉ lấy 6 món đầu tiên)
        const limitedSuggestions = (data.homeSuggestions || []).slice(0, 6);
        renderSuggestions(limitedSuggestions);

    } catch (error) {
        console.error("Lỗi tải trang chủ:", error);
    }
}

function renderMeals(meals) {
    const container = document.getElementById('meals-container');
    if (!container) return;

    if (!meals || meals.length === 0) {
        container.innerHTML = '<p style="color:#999; font-style:italic; grid-column: 1/-1;">Hôm nay bạn chưa ghi nhận bữa ăn nào.</p>';
        return;
    }

    container.innerHTML = meals.map((meal, index) => `
        <div class="meal-card">
            <div class="meal-time">${index + 1}</div>
            <div class="meal-info">
                <h4>${meal.mealName}</h4>
                <p>${Math.round(meal.totalCalories)} calo - ${meal.totalFoods} món</p>
            </div>
        </div>
    `).join('');
}

function renderSuggestions(foods) {
    const container = document.getElementById('suggestions-container');
    if (!container) return;

    if (!foods || foods.length === 0) {
        container.innerHTML = '<p style="color:#999; grid-column: 1/-1;">Đang tìm món phù hợp cho bạn...</p>';
        return;
    }

    container.innerHTML = foods.map(food => `
        <a href="/food_detail?id=${food.foodId}" style="text-decoration:none; color:inherit;">
            <div class="food-card">
                <img src="/images/${food.imageUrl}" alt="${food.foodName}" class="food-img" 
                     onerror="this.onerror=null; this.src='https://via.placeholder.com/300x200?text=No+Image'">
                <div class="status-badge ${food.allergyConflictCount === 0 ? 'bg-safe' : 'bg-warn'}">
                    <i class="fa-solid ${food.allergyConflictCount === 0 ? 'fa-check' : 'fa-shield-halved'}"></i> 
                    ${food.allergyConflictCount === 0 ? 'An toàn' : 'Có dị ứng'}
                </div>
                <div class="food-content">
                    <h4>${food.foodName}</h4>
                    <p>${Math.round(food.calories)} calo</p>
                    <div class="food-footer">
                        <i class="fa-regular fa-star"></i>
                        <span>${Math.round(food.suitabilityScore)}% phù hợp</span>
                    </div>
                </div>
            </div>
        </a>
    `).join('');
} 
document.addEventListener("DOMContentLoaded", function() {
    if (window.Chart && window.ChartDataLabels) {
        Chart.register(ChartDataLabels);
    }
    loadDashboardData();
});
let loginChartInstance;
let registerChartInstance;

async function loadDashboardData() {
    try {
        const response = await fetch('/api/admin/dashboard-summary');
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
       const data = await response.json();

         document.getElementById('admin-name').innerText = data?.name || 'Admin';

        // 5. Render Top món ăn
        updateStatCard('total-users', 'user-growth-val', 'user-growth-container', data?.totalUsers, data?.userGrowth);
        updateStatCard('total-foods', 'food-growth-val', 'food-growth-container', data?.totalFoods, data?.foodGrowth);
        updateStatCard('total-menus', 'menu-growth-val', 'menu-growth-container', data?.totalMenus, data?.menuGrowth);
        document.getElementById('today-activities').innerText = formatNumber(data?.todayActivities);

       
        renderTopFoods(data?.topFoods || {});
        renderPopularGoals(data?.popularGoals || {});

        // Vẽ chart sau cùng, nếu chart lỗi vẫn không ảnh hưởng các block dữ liệu khác
        renderLoginChart(data?.chartLabels || [], data?.chartData || []);
        renderRegisterChart(data?.userChartData || []);

    } catch (error) {
        console.error("Lỗi tải dashboard:", error);
    }
}
function formatNumber(value) {
    return Number.isFinite(Number(value)) ? Number(value).toLocaleString() : '0';
}


function updateStatCard(idVal, idGrowth, idContainer, value, growth) {
    const safeValue = Number(value) || 0;
    const safeGrowth = Number.isFinite(Number(growth)) ? Number(growth) : 0;

    document.getElementById(idVal).innerText = safeValue.toLocaleString();
    const growthElem = document.getElementById(idGrowth);
    growthElem.innerText = (safeGrowth >= 0 ? "+" : "") + safeGrowth.toFixed(1) + "%";
    document.getElementById(idContainer).style.color = safeGrowth >= 0 ? "#10b981" : "#dc2626";
}

function renderLoginChart(labels, dataValues) {
    if (typeof Chart === "undefined") return;
    const canvas = document.getElementById('loginChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.4)');
    gradient.addColorStop(1, 'rgba(16, 185, 129, 0.0)');

    if (loginChartInstance) {
        loginChartInstance.destroy();
    }

     if (!labels.length || !dataValues.length) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        return;
    }

    loginChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels ,
            datasets: [{
                label: 'Lượt đăng nhập',
                data: dataValues,
                borderColor: '#10b981',
                backgroundColor: gradient,
                borderWidth: 3,
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { display: false, beginAtZero: true } }
        }
    });
}

function renderRegisterChart(rawRegisterData) {
     if (typeof Chart === "undefined") return;
    const currentMonth = new Date().getMonth() + 1;
    const labels = Array.from({length: currentMonth}, (_, i) => `T${i + 1}`);
    const dataValues = rawRegisterData.slice(0, currentMonth);

     const canvas = document.getElementById('registerChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (registerChartInstance) {
        registerChartInstance.destroy();
    }
    if (!dataValues.length) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        return;
    }

    registerChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Người dùng mới',
                data: dataValues,
                backgroundColor: '#0ea5e9',
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } }
        }
    });
}

function renderTopFoods(foods) {
    const container = document.getElementById('top-foods-list');
    if (Object.keys(foods).length === 0) {
        container.innerHTML = '<div class="food-item" style="justify-content: center; color: #9ca3af;">Chưa có dữ liệu</div>';
        return;
    }
    container.innerHTML = Object.entries(foods).map(([name, count]) => `
        <div class="food-item">
            <span>${name}</span>
            <span class="badge">${count} Lượt</span>
        </div>
    `).join('');
}

function renderPopularGoals(goals) {
    const container = document.getElementById('popular-goals-list');
    if ( Object.keys(goals).length === 0) {
        container.innerHTML = '<div style="text-align: center; color: #9ca3af;">Chưa có dữ liệu</div>';
        return;
    }
   container.innerHTML = Object.entries(goals).map(([name, percent]) => {
        const safePercent = Number.isFinite(Number(percent)) ? Number(percent) : 0;
        return `
        <div class="progress-item">
            <div class="progress-label">
                <span>${name}</span>
                <strong>${safePercent.toFixed(0)}%</strong>
            </div>
            <div class="progress-bg">
                 <div class="progress-fill" style="width: ${safePercent}%;"></div>
            </div>
         </div>`;
    }).join('');
}
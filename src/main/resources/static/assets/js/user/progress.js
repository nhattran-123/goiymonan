document.addEventListener("DOMContentLoaded", function() {
    // Biến toàn cục để quản lý các Instance của biểu đồ
    let weightChartInstance = null;
    let heightChartInstance = null;

    // 1. Khởi tạo dữ liệu khi vừa vào trang
    fetchProgressData();

    // 2. Logic Modal (Cập nhật chỉ số)
    const modal = document.getElementById("updateModalOverlay");
    const openBtn = document.getElementById("openUpdateModalBtn");
    const closeBtn = document.getElementById("closeUpdateModalBtn");
    const updateForm = document.getElementById('updateStatsForm');

    if (openBtn) openBtn.addEventListener("click", () => modal.classList.add("active"));
    if (closeBtn) closeBtn.addEventListener("click", () => modal.classList.remove("active"));
    
    window.addEventListener("click", (e) => { 
        if (e.target === modal) modal.classList.remove("active"); 
    });

    // 3. Gọi API lấy dữ liệu tiến trình
    async function fetchProgressData() {
        try {
            const response = await fetch('/api/user/progress-summary');
            if (!response.ok) throw new Error("Không thể lấy dữ liệu");
            
            const data = await response.json();

            // Render số liệu tổng quát và lịch sử món ăn
            renderStats(data);
            renderHistory(data.recentHistory);

            // --- VẼ BIỂU ĐỒ BIẾN ĐỘNG ---
            // Hủy chart cũ nếu đã tồn tại trước khi vẽ mới
            if (weightChartInstance) weightChartInstance.destroy();
            if (heightChartInstance) heightChartInstance.destroy();

            weightChartInstance = createLineChart('weightChart', data.weightHistory, 'Cân nặng (kg)', '#10b981');
            heightChartInstance = createLineChart('heightChart', data.heightHistory, 'Chiều cao (cm)', '#3b82f6');

        } catch (error) {
            console.error("Lỗi tải tiến trình:", error);
        }
    }

    // Hàm bổ trợ vẽ biểu đồ đường (Line Chart)
    function createLineChart(canvasId, historyData, label, color) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !historyData || historyData.length === 0) return null;

        // Lọc bỏ dữ liệu null và sắp xếp thời gian cũ -> mới
        const validData = historyData
            .filter(item => item.recordedAt !== null)
            .sort((a, b) => new Date(a.recordedAt) - new Date(b.recordedAt));

        if (validData.length === 0) return null;

        const labels = validData.map(item => new Date(item.recordedAt).toLocaleDateString('vi-VN'));
        const values = validData.map(item => item.value);

        return new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: label,
                    data: values,
                    borderColor: color,
                    backgroundColor: color + '22',
                    borderWidth: 3,
                    tension: 0.4,
                    fill: true,
                    pointRadius: 4,
                    pointBackgroundColor: color,
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { 
                        beginAtZero: false,
                        grid: { color: '#f1f5f9' }
                    },
                    x: {
                        grid: { display: false }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#1e293b',
                        padding: 10,
                        titleFont: { size: 14 },
                        bodyFont: { size: 14 }
                    }
                }
            }
        });
    }

    // 4. Render các thẻ con số (Calo, Cân nặng, BMI...)
    function renderStats(data) {
        if (!data) return;

        const safeSetText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.innerText = value;
        };

        safeSetText('today-calo', `${Math.round(data.todayCalories || 0)} kcal`);
        safeSetText('current-weight-display', `${(data.currentWeight || 0).toFixed(1)} kg`);
        safeSetText('total-days', data.totalDaysFollowed || 1);
        safeSetText('goal-label', data.goalLabel || "Đang theo dõi");
        safeSetText('bmi-display', data.bmi ? data.bmi.toFixed(2) : "0.00");
        
        // Cập nhật giá trị vào form modal
        const inputH = document.getElementById('inputHeight');
        const inputW = document.getElementById('inputWeight');
        if (inputH) inputH.value = data.currentHeight || "";
        if (inputW) inputW.value = data.currentWeight || "";
    }

    // 5. Render Lịch sử ăn uống gần đây
    function renderHistory(history) {
        const container = document.getElementById('history-list');
        if (!container) return;

        if (!history || history.length === 0) {
            container.innerHTML = `<div style="text-align: center; padding: 30px; color: #94a3b8;"><p>Chưa có lịch sử ăn uống gần đây.</p></div>`;
            return;
        }

        container.innerHTML = history.map(item => {
            const dateObj = new Date(item.eatenAt);
            return `
                <div class="history-item" style="display: flex; align-items: center; justify-content: space-between; padding: 15px; border-bottom: 1px solid #f1f5f9;">
                    <div style="display: flex; align-items: center; gap: 15px;">
                        <img src="/images/${item.imageUrl || 'default-food.png'}" style="width: 48px; height: 48px; object-fit: cover; border-radius: 12px;">
                        <div>
                            <div style="font-weight: 700; color: #1e293b;">${item.foodName}</div>
                            <div style="font-size: 12px; color: #94a3b8;">${dateObj.toLocaleTimeString('vi-VN')} • ${dateObj.toLocaleDateString('vi-VN')}</div>
                        </div>
                    </div>
                    <div><span style="font-weight: 800; color: #10b981;">+${Math.round(item.calories || 0)}</span> kcal</div>
                </div>`;
        }).join('');
    }

    // 6. Xử lý gửi form cập nhật chỉ số
    if (updateForm) {
        updateForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const payload = {
                height: parseFloat(document.getElementById('inputHeight').value),
                weight: parseFloat(document.getElementById('inputWeight').value)
            };

            try {
                const res = await fetch('/api/user/update-stats', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    modal.classList.remove("active");
                    fetchProgressData(); // Reload lại để biểu đồ và số liệu nhảy ngay lập tức
                    alert("Cập nhật chỉ số thành công! 🎉");
                } else {
                    alert("Có lỗi xảy ra khi cập nhật.");
                }
            } catch (err) {
                console.error("Lỗi submit:", err);
                alert("Lỗi kết nối máy chủ.");
            }
        });
    }
});
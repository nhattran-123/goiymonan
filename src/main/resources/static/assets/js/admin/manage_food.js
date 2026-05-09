document.addEventListener("DOMContentLoaded", function() {
     window.currentPage = 1;
    window.pageSize = 10;
    window.maxVisiblePages = 5;
    fetchFoods();

    // Lắng nghe sự kiện tìm kiếm
    document.getElementById('foodSearch').addEventListener('input', function(e) {
        filterFoods(e.target.value);
    });
});

let allFoods = []; // Lưu trữ danh sách gốc để tìm kiếm nhanh
let filteredFoods = [];

// 1. Lấy danh sách món ăn từ API
async function fetchFoods() {
    try {
        const response = await fetch('/api/admin/foods');
        allFoods = await response.json();
        filteredFoods = [...allFoods];
        renderCurrentPage();
    } catch (error) {
        console.error("Lỗi tải danh sách món ăn:", error);
    }
}

// 2. Render dữ liệu vào bảng
function renderFoodTable(foods) {
    const tbody = document.getElementById('food-tbody');
    if (foods.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #9ca3af;">Chưa có dữ liệu món ăn.</td></tr>`;
        return;
    }

    tbody.innerHTML = foods.map(f => `
        <tr style="border-bottom: 1px dashed #eee;">
            <td style="padding: 10px 15px;">
                <img src="/images/${f.imageUrl}" 
                     alt="${f.foodName}" 
                     class="food-img-td"
                     onerror="this.onerror=null; this.src='https://via.placeholder.com/150?text=No+Image'">
            </td>
            <td style="font-weight: 500;">${f.foodName}</td>
            <td>${f.calories}</td>
            <td>${f.protein}g</td>
            <td>${f.fat}g</td>
            <td>${f.carbohydrate}g</td>
            <td>
                <a href="/admin/edit_food?id=${f.foodId}" class="action-btn" style="color: #6b7280; margin-right: 10px;"><i class="fa-regular fa-pen-to-square"></i></a>
                <a href="javascript:void(0);" class="action-btn" style="color: #ef4444;" onclick="showDeleteModal('${f.foodId}', '${f.foodName}')">
                    <i class="fa-regular fa-trash-can"></i>
                </a>
            </td>
        </tr>
    `).join('');
}
function renderCurrentPage() {
    const totalPages = Math.max(1, Math.ceil(filteredFoods.length / pageSize));
    if (currentPage > totalPages) currentPage = totalPages;
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    renderFoodTable(filteredFoods.slice(start, end));
    renderPagination(totalPages);
}

function renderPagination(totalPages) {
    const paginationEl = document.getElementById('food-pagination');
    if (!paginationEl) return;

    if (filteredFoods.length === 0 || totalPages <= 1) {
        paginationEl.innerHTML = '';
        return;
    }

    const half = Math.floor(maxVisiblePages / 2);
    let startPage = Math.max(1, currentPage - half);
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
    startPage = Math.max(1, endPage - maxVisiblePages + 1);

    let html = `
        <button class="page-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">&laquo;</button>
    `;

    for (let p = startPage; p <= endPage; p++) {
        html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" onclick="goToPage(${p})">${p}</button>`;
    }

    html += `
        <button class="page-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">&raquo;</button>
    `;
    paginationEl.innerHTML = html;
}

function goToPage(page) {
    const totalPages = Math.max(1, Math.ceil(filteredFoods.length / pageSize));
    currentPage = Math.min(Math.max(page, 1), totalPages);
    renderCurrentPage();
}

window.goToPage = goToPage;

// 3. Xử lý tìm kiếm (Client-side filter)
function filterFoods(keyword) {
    filteredFoods = allFoods.filter(f =>  
        f.foodName.toLowerCase().includes(keyword.toLowerCase())
    );
   currentPage = 1;
    renderCurrentPage();
}

// 4. Logic Modal Xóa
let currentDeleteId = null;

function showDeleteModal(id, name) {
    currentDeleteId = id;
    document.getElementById('deleteModal').style.display = 'flex';
    document.getElementById('deleteFoodName').innerText = name;
}

function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    currentDeleteId = null;
}

document.getElementById('confirmDeleteBtn').onclick = async function() {
    if (!currentDeleteId) return;

    try {
        const response = await fetch(`/api/admin/foods/${currentDeleteId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('success', 'Đã xóa món ăn thành công!');
            fetchFoods(); // Tải lại bảng
        } else {
            showAlert('error', 'Có lỗi xảy ra, không thể xóa món ăn này!');
        }
    } catch (error) {
        console.error("Lỗi xóa món ăn:", error);
        showAlert('error', 'Lỗi kết nối đến máy chủ.');
    } finally {
        closeDeleteModal();
    }
};

// 5. Hàm hiển thị thông báo
function showAlert(type, message) {
    const container = document.getElementById('alert-container');
    const isSuccess = type === 'success';
    container.innerHTML = `
        <div style="background: ${isSuccess ? '#d1fae5' : '#fee2e2'}; 
                    color: ${isSuccess ? '#059669' : '#dc2626'}; 
                    padding: 15px; border-radius: 8px; margin-bottom: 20px;">
            <i class="fa-solid ${isSuccess ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i> ${message}
        </div>
    `;
    // Tự ẩn sau 3 giây
    setTimeout(() => container.innerHTML = '', 3000);
}
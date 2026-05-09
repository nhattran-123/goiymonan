document.addEventListener("DOMContentLoaded", function() {
    initTabs();
    initSearch();
    bindForms();
    
    fetchDiseases();
    fetchCompatibility();
    loadDropdownData();
    activateCompatibilityTabByQuery();
});
const PAGE_SIZE = 10;
let diseaseData = [];
let compatibilityData = [];
let diseasePage = 1;
let compatibilityPage = 1;

// 1. Chuyển Tab
function initTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', function () {
            switchTab(this.getAttribute('data-tab'));
        });
    });
}

function switchTab(sectionId) {
    document.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.querySelector(`.tab-btn[data-tab='${sectionId}']`)?.classList.add('active');
    document.getElementById(sectionId).style.display = 'block';
}

function activateCompatibilityTabByQuery() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('tab') === 'compatibility' || params.get('error') === 'duplicate_rating') {
        switchTab('compatibility-section');
    }
}

function initSearch() {
    const input = document.getElementById('diseaseSearch');
    input?.addEventListener('input', function () {
        const keyword = this.value.trim().toLowerCase();
        document.querySelectorAll('#disease-tbody tr').forEach(row => {
            const text = row.innerText.toLowerCase();
            row.style.display = text.includes(keyword) ? '' : 'none';
        });
    });
}

// 2. Xử lý Bệnh lý (Disease)
async function fetchDiseases() {
    const response = await fetch('/api/admin/diseases');
     diseaseData = await response.json();
    diseasePage = 1;
    renderDiseases();
}

function renderDiseases() {
    const tbody = document.getElementById('disease-tbody');
       const start = (diseasePage - 1) * PAGE_SIZE;
    const currentPageData = diseaseData.slice(start, start + PAGE_SIZE);
    tbody.innerHTML = currentPageData.map(d => {
        const name = escapeHtml(d.diseaseName || '');
        const desc = escapeHtml(d.diseaseDescription || '');

        return `
        <tr style="border-bottom: 1px solid #f3f4f6;">
            <td style="padding: 15px; font-weight: 500;">${name}</td>
            <td style="color: #6b7280;">${desc}</td>
            <td style="text-align: center;"><span class="badge-disease">${d.foodCount || 0}</span></td>
            <td style="text-align: center;">
                 <button onclick="editDisease(${d.diseaseId}, ${JSON.stringify(d.diseaseName || '')}, ${JSON.stringify(d.diseaseDescription || '')})" style="border:none; background:none; cursor:pointer; color:#6b7280; margin-right:10px;"><i class="fa-regular fa-pen-to-square"></i></button>
                <button onclick="deleteDisease(${d.diseaseId})" style="border:none; background:none; cursor:pointer; color:#ef4444;"><i class="fa-regular fa-trash-can"></i></button>
            </td>
       </tr>`;
    }).join('');
    renderPagination('disease-pagination', diseaseData.length, diseasePage, (page) => {
        diseasePage = page;
        renderDiseases();
    });
}

// 3. Xử lý Tương thích (Compatibility)
async function fetchCompatibility() {
    const response = await fetch('/api/admin/compatibility');
     compatibilityData = await response.json();
    compatibilityPage = 1;
    renderCompatibility();
}

function renderCompatibility() {
    const tbody = document.getElementById('compatibility-tbody');
    const start = (compatibilityPage - 1) * PAGE_SIZE;
    const currentPageData = compatibilityData.slice(start, start + PAGE_SIZE);
    tbody.innerHTML = currentPageData.map(item => `
        <tr style="border-bottom: 1px solid #f3f4f6;">
            <td style="padding: 15px; font-weight: 500;">${escapeHtml(item.foodName)}</td>
            <td><span style="background: #e0f2fe; color: #0284c7; padding: 4px 10px; border-radius: 20px; font-size: 13px;">${escapeHtml(item.diseaseName)}</span></td>
            <td><span style="color: #f59e0b; font-size: 18px;">${'★'.repeat(item.rating)}${'☆'.repeat(5 - item.rating)}</span></td>
            <td style="text-align: center;">
                <button onclick="deleteRating(${item.id})" style="border:none; background:none; cursor:pointer; color:#ef4444;"><i class="fas fa-trash"></i> Xóa</button>
            </td>
        </tr>
    `).join('');

     renderPagination('compatibility-pagination', compatibilityData.length, compatibilityPage, (page) => {
        compatibilityPage = page;
        renderCompatibility();
    });
}

function renderPagination(containerId, totalItems, currentPage, onPageClick) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '';
    for (let page = 1; page <= totalPages; page++) {
        html += `<button type="button" class="page-btn ${page === currentPage ? 'active' : ''}" data-page="${page}" style="padding:6px 10px;border:1px solid #d1d5db;background:${page === currentPage ? '#10b981' : '#fff'};color:${page === currentPage ? '#fff' : '#374151'};border-radius:6px;cursor:pointer;">${page}</button>`;
    }
    container.innerHTML = html;
    container.querySelectorAll('.page-btn').forEach(btn => {
        btn.addEventListener('click', () => onPageClick(Number(btn.dataset.page)));
    });
}


// 4. Modal Logic
async function loadDropdownData() {
    const [foods, diseases] = await Promise.all([
        fetch('/api/admin/foods').then(r => r.json()),
        fetch('/api/admin/diseases').then(r => r.json())
    ]);

    document.getElementById('selectFood').innerHTML = foods.map(f => `<option value="${f.foodId}">${escapeHtml(f.foodName)}</option>`).join('');
    document.getElementById('selectDisease').innerHTML = diseases.map(d => `<option value="${d.diseaseId}">${escapeHtml(d.diseaseName)}</option>`).join('');
}
function bindForms() {
    document.getElementById('diseaseForm').onsubmit = async function (e) {
        e.preventDefault();
        const id = document.getElementById('diseaseId').value;
        const method = id ? 'PUT' : 'POST';
        const url = id ? `/api/admin/diseases/${id}` : '/api/admin/diseases';

        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                diseaseName: document.getElementById('diseaseName').value,
                diseaseDescription: document.getElementById('diseaseDesc').value
            })
        });

        if (response.ok) {
            closeModal('diseaseModal');
            fetchDiseases();
            loadDropdownData();
        }
    };

    document.getElementById('ratingForm').onsubmit = async function (e) {
        e.preventDefault();
        const response = await fetch('/api/admin/compatibility', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                foodId: Number(document.getElementById('selectFood').value),
                diseaseId: Number(document.getElementById('selectDisease').value),
                rating: Number(document.getElementById('selectRating').value)
            })
        });

        const result = await response.json();
        if (!response.ok) {
            alert(result.message || 'Không thể thêm đánh giá.');
            return;
        }

        closeModal('ratingModal');
        this.reset();
        fetchCompatibility();
    };
}

function openModal(modalId) { document.getElementById(modalId).style.display = 'flex'; }
function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
    if (modalId === 'diseaseModal') {
        document.getElementById('diseaseForm').reset();
        document.getElementById('diseaseId').value = '';
        document.getElementById('modalTitle').innerText = 'Thêm bệnh lý mới';
    }
}
function openRatingModal() { openModal('ratingModal'); }

function editDisease(id, name, desc) {
    document.getElementById('modalTitle').innerText = 'Sửa bệnh lý';
    document.getElementById('diseaseId').value = id;
    document.getElementById('diseaseName').value = name;
    document.getElementById('diseaseDesc').value = desc;
    openModal('diseaseModal');
}

async function deleteDisease(id) {
    if (!confirm('Bạn có chắc chắn muốn xóa không?')) return;
    const res = await fetch(`/api/admin/diseases/${id}`, { method: 'DELETE' });
    if (res.ok) {
        fetchDiseases();
        fetchCompatibility();
        loadDropdownData();
    }
}

async function deleteRating(id) {
    if (!confirm('Xóa liên kết này?')) return;
    const res = await fetch(`/api/admin/compatibility/${id}`, { method: 'DELETE' });
    if (res.ok) fetchCompatibility();
}

    function escapeHtml(str = '') {
    return str.replace(/[&<>'"]/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[m]));
}
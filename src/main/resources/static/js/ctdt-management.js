// ============ CTDT GLOBAL VARIABLES ============
    window.CTDT_APP = window.CTDT_APP || {
        hocPhanList: [],
        nhomHocPhanList: [],
        currentCTDTId: null,
        hocPhanCounter: 0,
        nhomHocPhanCounter: 0,
        currentNganh: 'all',
        plans: [],
        activePlanId: null,
        allCTDTData: [],
        nganhList: []
    };

    var CTDT_APP = window.CTDT_APP;

// ============ KHỞI TẠO TABS CHO CTDT ============
function initCTTDTabs() {
    console.log('Initializing CTDT tabs');

    // Load danh sách ngành từ departments (để tạo tab cho ngành chưa có data)
    // VÀ từ tabNganhList trong DB (để hiển thị tab cho ngành đã có data import vào)
    $.when(
        $.ajax({ url: '/profile/api/admin/load-departments', type: 'GET' }),
        $.ajax({ url: '/profile/api/admin/ctdt/filters', type: 'GET' })
    ).done(function(deptResult, filterResult) {
        const deptData = deptResult[0];
        const filterData = filterResult[0];

        const deptTabs = deptData.distinctDepts || [];
        const dbTabs = filterData.tabNganhList || [];

        // Merge: ưu tiên danh sách departments, bổ sung thêm các tab đã có data trong DB
        const allTabs = [...new Set([...deptTabs, ...dbTabs])].sort((a, b) => a.localeCompare(b, 'vi'));

        console.log('Tabs from departments:', deptTabs);
        console.log('Tabs from DB (tabNganh):', dbTabs);
        console.log('Merged tabs:', allTabs);

        CTDT_APP.nganhList = allTabs;
        renderCTTDTabs();

        const savedNganh = localStorage.getItem('ctdt_current_nganh');
        if (savedNganh && savedNganh !== 'all' && CTDT_APP.nganhList.includes(savedNganh)) {
            setTimeout(() => switchCTDTTab(savedNganh), 200);
        } else {
            setTimeout(() => switchCTDTTab('all'), 200);
        }
    }).fail(function() {
        console.error('Error loading tabs');
        showToast('Lỗi khi tải danh sách ngành', 'error');
    });
}

// Render tabs
function renderCTTDTabs() {
    let tabsHtml = '';

    // Thêm các tab ngành (không lặp nút trong vòng lặp)
    CTDT_APP.nganhList.forEach((nganh) => {
        const safeNganh = nganh.replace(/[^a-zA-Z0-9]/g, '_');
        tabsHtml += `
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="${safeNganh}-tab" data-bs-toggle="tab"
                        data-bs-target="#${safeNganh}" type="button" role="tab"
                        onclick="switchCTDTTab('${nganh}')">
                    <i class="fas fa-book me-1"></i>${nganh}
                    <span class="badge bg-secondary ms-1" id="count-${safeNganh}">0</span>
                </button>
            </li>
        `;
    });

    $('#ctdtTabs').append(tabsHtml);

    // Thêm các tab content — mỗi tab ngành có đầy đủ bộ lọc + bảng
    let contentHtml = '';
    CTDT_APP.nganhList.forEach((nganh) => {
        const safeNganh = nganh.replace(/[^a-zA-Z0-9]/g, '_');
        contentHtml += `
            <div class="tab-pane fade" id="${safeNganh}" role="tabpanel">
                <div class="ctdt-content" data-nganh="${nganh}">
                    <!-- Bộ lọc (ngành đã cố định theo tab) -->
                    <div class="row g-3 align-items-center mb-3">
                        <div class="col-md-3">
                            <select class="form-select form-select-sm tab-filter-hocky" data-nganh="${nganh}">
                                <option value="">- Tìm theo học kỳ -</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <select class="form-select form-select-sm tab-filter-chuyennganh" data-nganh="${nganh}">
                                <option value="">- Tìm theo chuyên ngành -</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <select class="form-select form-select-sm tab-filter-loai" data-nganh="${nganh}">
                                <option value="">- Loại học phần -</option>
                                <option value="BB">Bắt buộc (BB)</option>
                                <option value="TC">Tự chọn (TC)</option>
                            </select>
                        </div>
                        <div class="col-md-1 text-center">
                            <button class="btn btn-light btn-sm border w-100" onclick="searchCTDT()" title="Tìm kiếm">
                                <i class="fas fa-search"></i>
                            </button>
                        </div>
                        <div class="col-md-2">
                            <button class="btn btn-outline-secondary btn-sm w-100" onclick="resetCTDTSearch()" style="font-size: 12px;">
                                Reset tìm kiếm
                            </button>
                        </div>
                        <div class="col-md-1 text-end">
                            <span class="small text-muted">
                                Hiển thị: <span class="tab-count-display" data-nganh="${nganh}">0</span>
                            </span>
                        </div>
                    </div>
                    <!-- Bảng CTĐT -->
                    <div class="table-responsive">
                        <table class="table table-bordered text-center align-middle shadow-sm" style="font-size: 13px;">
                            <thead class="admin-table-header">
                            <tr>
                                <th width="50">HK</th>
                                <th width="100">Mã HP</th>
                                <th width="50">TT</th>
                                <th>Tên học phần</th>
                                <th width="80">Tín chỉ</th>
                                <th width="70">BB/TC</th>
                                <th width="90">Nhóm TC</th>
                                <th>Học phần tiên quyết</th>
                                <th>Ngành</th>
                                <th>Chuyên ngành</th>
                                <th width="120">Tác vụ</th>
                            </tr>
                            </thead>
                            <tbody id="ctdtTableBody-${safeNganh}" class="ctdt-tab-body" data-nganh="${nganh}">
                            <tr><td colspan="11" class="text-center py-4 text-muted">
                                <i class="fas fa-arrow-up me-1"></i>Chuyển sang tab này để tải dữ liệu
                            </td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    });

    $('#ctdtTabContent').append(contentHtml);
}

// ============ HELPER: trả về selector tbody đang active ============
function getActiveTableBody() {
    const nganh = CTDT_APP.currentNganh;
    if (nganh === 'all') return '#ctdtTableBody';
    const safe = nganh.replace(/[^a-zA-Z0-9]/g, '_');
    return `#ctdtTableBody-${safe}`;
}

// ============ HELPER: cập nhật số đếm của tab đang active ============
function updateActiveCount(count) {
    const nganh = CTDT_APP.currentNganh;
    if (nganh === 'all') {
        $('#ctdtCount').text(count);
    } else {
        $(`.tab-count-display[data-nganh="${nganh}"]`).text(count);
    }
}

// ============ HELPER: populate filter dropdowns cho một tab ngành ============
function populateTabFilters(nganh, filters) {
    const safe = nganh.replace(/[^a-zA-Z0-9]/g, '_');
    const pane = $(`#${safe}`);

    // Học kỳ
    let hkHtml = '<option value="">- Tìm theo học kỳ -</option>';
    (filters.hocKyList || []).forEach(hk => {
        hkHtml += `<option value="${hk}">Học kỳ ${hk}</option>`;
    });
    pane.find('.tab-filter-hocky').html(hkHtml);

    // Chuyên ngành — lọc chỉ những chuyên ngành thuộc ngành này
    $.ajax({
        url: '/profile/api/admin/get-majors',
        type: 'GET',
        data: { departmentName: nganh },
        success: function(majors) {
            let cnHtml = '<option value="">- Tìm theo chuyên ngành -</option>';
            (majors || []).forEach(cn => {
                cnHtml += `<option value="${cn}">${cn}</option>`;
            });
            pane.find('.tab-filter-chuyennganh').html(cnHtml);
        }
    });
}

// Chuyển tab
function switchCTDTTab(nganh) {
    console.log('Switching to tab:', nganh);

    CTDT_APP.currentNganh = nganh;

    // Lưu vào localStorage
    localStorage.setItem('ctdt_current_nganh', nganh);

    // Cập nhật active class
    $('#ctdtTabs .nav-link').removeClass('active');
    $('.tab-pane').removeClass('show active');

    if (nganh === 'all') {
        $('#all-tab').addClass('active');
        $('#all').addClass('show active');
        loadCTDTData('all');
    } else {
        const safeNganh = nganh.replace(/[^a-zA-Z0-9]/g, '_');
        $(`#${safeNganh}-tab`).addClass('active');
        $(`#${safeNganh}`).addClass('show active');
        loadCTDTData(nganh);
    }
}

// Cập nhật số lượng cho tabs
function updateTabCounts() {
    // Load số lượng mỗi tab từ server để chính xác
    $.ajax({
        url: '/profile/api/admin/ctdt/filters',
        type: 'GET',
        success: function(filters) {
            // filters.tabNganhList không dùng để count, cần query riêng
            // Đơn giản: chỉ update tab đang active
            if (CTDT_APP.allCTDTData) {
                updateActiveCount(CTDT_APP.allCTDTData.length);
            }
        }
    });
}

// Mở modal tạo kế hoạch mới
function openCreatePlanModal() {
    console.log('Opening create plan modal');

    // Tạo modal HTML
    const modalHtml = `
        <div class="modal fade" id="createPlanModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header py-2" style="background: var(--vlu-green); color: white;">
                        <h6 class="modal-title font-weight-bold">
                            <i class="fas fa-plus-circle me-2"></i>Tạo kế hoạch đào tạo mới
                        </h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div class="mb-3">
                            <label class="form-label small fw-bold">Chọn ngành <span class="text-danger">*</span></label>
                            <select id="planNganh" class="form-select form-select-sm" required>
                                <option value="">-- Chọn ngành --</option>
                                ${CTDT_APP.nganhList.map(nganh => `<option value="${nganh}">${nganh}</option>`).join('')}
                            </select>
                            <div class="form-text">Kế hoạch đào tạo sẽ được tạo cho ngành này</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-bold">Tên kế hoạch (tùy chọn)</label>
                            <input type="text" id="planName" class="form-control form-control-sm"
                                   placeholder="VD: CTĐT Công nghệ thông tin 2024">
                            <div class="form-text">Nếu để trống sẽ tự động lấy tên ngành</div>
                        </div>
                        <div class="d-flex justify-content-center gap-2 mt-4">
                            <button type="button" class="btn btn-vlu-blue btn-sm px-4" onclick="createNewPlan()">
                                <i class="fas fa-check me-1"></i> Tạo mới
                            </button>
                            <button type="button" class="btn btn-danger btn-sm px-4" data-bs-dismiss="modal">
                                Hủy
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Thêm modal vào container
    $('#modalContainer').html(modalHtml);

    // Khởi tạo và hiển thị modal
    const modalElement = document.getElementById('createPlanModal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();

        // Xóa modal khỏi DOM khi đóng
        modalElement.addEventListener('hidden.bs.modal', function () {
            $(this).remove();
        });
    } else {
        console.error('Modal element not found');
        showToast('Không thể tạo modal', 'error');
    }
}

// Tạo kế hoạch mới
function createNewPlan() {
    const nganh = $('#planNganh').val();
    const planName = $('#planName').val().trim() || nganh;

    if (!nganh) {
        showToast('Vui lòng chọn ngành', 'error');
        return;
    }

    // Kiểm tra xem đã có kế hoạch cho ngành này chưa
    const existingPlan = CTDT_APP.allCTDTData.filter(ctdt => ctdt.nganh === nganh);

    if (existingPlan.length > 0) {
        if (!confirm(`Ngành "${nganh}" đã có ${existingPlan.length} học phần. Bạn có muốn tạo kế hoạch mới (sẽ xóa dữ liệu cũ)?`)) {
            return;
        }

        // Xóa dữ liệu cũ
        deletePlanData(nganh);
    }

    // Đóng modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('createPlanModal'));
    if (modal) {
        modal.hide();
    }

    showToast(`Đã tạo kế hoạch "${planName}"`, 'success');

    // Chuyển sang tab ngành vừa tạo
    setTimeout(() => {
        switchCTDTTab(nganh);
        resetCTDTForm();
    }, 300);
}

// ============ INITIALIZATION ============
// Initialize CTDT when tab is loaded
$(document).ready(function() {
    console.log('CTDT Management JS loaded');

    $(document).on('ctdtTabLoaded', function() {
        console.log('CTDT tab loaded event received');
        initCTTDTabs();
    });
});

// Xóa dữ liệu cũ của tab ngành
function deletePlanData(nganh) {
    $.ajax({
        url: `/profile/api/admin/ctdt/delete-by-tab/${encodeURIComponent(nganh)}`,
        type: 'DELETE',
        async: false,
        success: function(msg) {
            console.log('Deleted tab data:', msg);
        },
        error: function(xhr) {
            console.error('Error deleting tab data:', xhr.responseText);
        }
    });
    // Cập nhật lại dữ liệu local
    CTDT_APP.allCTDTData = CTDT_APP.allCTDTData.filter(ctdt => ctdt.tabNganh !== nganh);
}

// Reset form thêm học phần
function resetCTDTForm() {
    CTDT_APP.hocPhanList = [];
    CTDT_APP.nhomHocPhanList = [];
    CTDT_APP.hocPhanCounter = 0;
    CTDT_APP.nhomHocPhanCounter = 0;

    $('#hocPhanListContainer').html('<p class="text-muted text-center mb-0">Chưa có học phần nào được thêm</p>');
    $('#nhomHocPhanContainer').empty();
}

// ============ CTDT MODAL HTML ============
    function getCTDTModalHTML() {
        return `
            <div class="modal fade" id="ctdtModal" tabindex="-1" data-bs-backdrop="static" style="display: none;">
                <div class="modal-dialog modal-dialog-centered modal-xl">
                    <div class="modal-content">
                        <div class="modal-header py-2" style="background: var(--vlu-red); color: white;">
                            <h6 class="modal-title font-weight-bold" id="ctdtModalTitle">Thêm chương trình đào tạo</h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <!-- Phần chọn học kỳ và thông tin chung -->
                            <div class="card mb-4">
                                <div class="card-header py-2 bg-light">
                                    <h6 class="mb-0"><i class="fas fa-cog me-2"></i>Thông tin chung</h6>
                                </div>
                                <div class="card-body">
                                    <div class="row">
                                        <div class="col-md-4 mb-3">
                                            <label class="form-label small">Học kỳ *</label>
                                            <select id="ctdtHocKy" class="form-select form-select-sm">
                                                <option value="">-- Chọn học kỳ --</option>
                                                <option value="1">Học kỳ 1</option>
                                                <option value="2">Học kỳ 2</option>
                                                <option value="3">Học kỳ 3</option>
                                                <option value="4">Học kỳ 4</option>
                                                <option value="5">Học kỳ 5</option>
                                                <option value="6">Học kỳ 6</option>
                                                <option value="7">Học kỳ 7</option>
                                                <option value="8">Học kỳ 8</option>
                                                <option value="9">Học kỳ 9</option>
                                                <option value="10">Học kỳ 10</option>
                                                <option value="11">Học kỳ 11</option>
                                                <option value="12">Học kỳ 12</option>
                                            </select>
                                        </div>
                                        <div class="col-md-4 mb-3">
                                            <label class="form-label small">Nganh</label>
                                            <select id="ctdtNganh" class="form-select form-select-sm" onchange="updateChuyenNganh()">
                                                <option value="">-- Chọn nganh --</option>
                                            </select>
                                        </div>
                                        <div class="col-md-4 mb-3">
                                            <label class="form-label small">Chuyên ngành</label>
                                            <select id="ctdtChuyenNganh" class="form-select form-select-sm">
                                                <option value="">-- Chọn chuyên ngành --</option>
                                            </select>
                                        </div>
                                    </div>

                                    <div class="row">
                                        <div class="col-md-6 mb-3">
                                            <label class="form-label small">Loại học phần *</label>
                                            <select id="ctdtLoai" class="form-select form-select-sm" onchange="toggleNhomTC()">
                                                <option value="BB">Bắt buộc (BB)</option>
                                                <option value="TC">Tự chọn (TC)</option>
                                            </select>
                                        </div>
                                        <div class="col-md-6 mb-3" id="nhomTCContainer" style="display: none;">
                                            <label class="form-label small">Nhóm tự chọn</label>
                                            <input type="text" id="ctdtNhomTC" class="form-control form-control-sm"
                                                   placeholder="Ví dụ: 2/5 (học 2 trong 5 môn)">
                                        </div>
                                    </div>

                                    <!-- Nút thêm nhóm học phần -->
                                    <div class="text-end">
                                        <button type="button" class="btn btn-outline-primary btn-sm" onclick="addNhomHocPhan()">
                                            <i class="fas fa-plus me-1"></i> Thêm nhóm học phần
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <!-- Danh sách các nhóm học phần -->
                            <div id="nhomHocPhanContainer">
                                <!-- Các nhóm học phần sẽ được thêm vào đây -->
                            </div>

                            <!-- Nút thêm học phần -->
                            <div class="text-center mb-4">
                                <button type="button" class="btn btn-vlu-blue btn-sm" onclick="addHocPhan()">
                                    <i class="fas fa-plus-circle me-1"></i> Thêm học phần mới
                                </button>
                            </div>

                            <!-- Danh sách học phần đã thêm -->
                            <div class="card">
                                <div class="card-header py-2 bg-light">
                                    <h6 class="mb-0"><i class="fas fa-list me-2"></i>Danh sách học phần</h6>
                                </div>
                                <div class="card-body">
                                    <div id="hocPhanListContainer">
                                        <p class="text-muted text-center mb-0">Chưa có học phần nào được thêm</p>
                                    </div>
                                </div>
                            </div>

                            <!-- Nút lưu -->
                            <div class="d-flex justify-content-center gap-2 mt-4">
                                <button type="button" class="btn btn-vlu-blue btn-sm px-4" onclick="saveAllCTDT()">
                                    <i class="fas fa-save me-1"></i> Lưu tất cả
                                </button>
                                <button type="button" class="btn btn-danger btn-sm px-4" data-bs-dismiss="modal">
                                    Đóng
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

// ============ CTĐT FUNCTIONS ============
    function loadCTDTData(nganh = 'all') {
        console.log('loadCTDTData called with nganh:', nganh);

        if (APP.currentTab !== 'admin-ctdt') return;

        const tbodySelector = getActiveTableBody();
        showLoading(tbodySelector);

        // Truyền tabNganh để filters trả về đúng học kỳ/chuyên ngành của tab đó
        const filterParams = nganh === 'all' ? '' : `?tabNganh=${encodeURIComponent(nganh)}`;

        $.ajax({
            url: `/profile/api/admin/ctdt/filters${filterParams}`,
            type: 'GET',
            success: function(filters) {
                if (nganh === 'all') {
                    // Populate filters cho tab "Tất cả"
                    let hocKyHtml = '<option value="">- Tìm theo học kỳ -</option>';
                    (filters.hocKyList || []).forEach(hk => {
                        hocKyHtml += `<option value="${hk}">Học kỳ ${hk}</option>`;
                    });
                    $('#filterHocKy').html(hocKyHtml);

                    let nganhHtml = '<option value="">- Tìm theo ngành -</option>';
                    (filters.nganhList || []).forEach(ng => {
                        nganhHtml += `<option value="${ng}">${ng}</option>`;
                    });
                    $('#filterNganh').html(nganhHtml);

                    let cnHtml = '<option value="">- Tìm theo chuyên ngành -</option>';
                    (filters.chuyenNganhList || []).forEach(cn => {
                        cnHtml += `<option value="${cn}">${cn}</option>`;
                    });
                    $('#filterChuyenNganh').html(cnHtml);
                } else {
                    // Populate filters cho tab ngành cụ thể
                    populateTabFilters(nganh, filters);
                }

                loadAllCTDT(nganh);
            },
            error: function() {
                hideLoading(tbodySelector);
                loadAllCTDT(nganh);
            }
        });
    }

    // Load CTDT — dùng tabNganh để filter, không phụ thuộc field nganh của học phần
    function loadAllCTDT(nganh = 'all') {
        console.log('loadAllCTDT called with nganh:', nganh);

        const tbodySelector = getActiveTableBody();

        // Dùng by-tab để lấy đúng học phần thuộc tab, kể cả học phần không có field nganh
        const url = nganh === 'all'
            ? '/profile/api/admin/ctdt/all'
            : `/profile/api/admin/ctdt/by-tab/${encodeURIComponent(nganh)}`;

        $.ajax({
            url: url,
            type: 'GET',
            success: function(ctdtList) {
                console.log('CTDT data loaded:', ctdtList.length, 'records for tab:', nganh);
                CTDT_APP.allCTDTData = ctdtList || [];
                renderCTDTTable(ctdtList);
                updateTabCounts();
                hideLoading(tbodySelector);
            },
            error: function(xhr) {
                console.error('Error loading CTDT:', xhr);
                showToast('Lỗi khi tải danh sách CTĐT', 'error');
                hideLoading(tbodySelector);
                renderCTDTTable([]);
            }
        });
    }

    // Render bảng CTDT
    function renderCTDTTable(ctdtList) {
        let html = '';
        let totalCreditsAllSemesters = 0;

        if (!ctdtList || ctdtList.length === 0) {
            html = `
                <tr>
                    <td colspan="11" class="text-center py-5 text-muted">
                        <i class="fas fa-database me-2"></i>
                        Chưa có dữ liệu chương trình đào tạo
                    </td>
                </tr>
            `;
        } else {
            // NHÓM THEO HỌC KỲ
            const groupedByHocKy = {};
            ctdtList.forEach(ctdt => {
                const hocKy = ctdt.hocKy || 0;
                if (!groupedByHocKy[hocKy]) {
                    groupedByHocKy[hocKy] = [];
                }
                groupedByHocKy[hocKy].push(ctdt);
            });

            // SẮP XẾP HỌC KỲ
            const sortedHocKys = Object.keys(groupedByHocKy).sort((a, b) => a - b);

            let globalIndex = 1;

            sortedHocKys.forEach(hocKy => {
                let hocPhanList = groupedByHocKy[hocKy];

                // ============ SẮP XẾP THEO YÊU CẦU ============
                // Phân loại học phần
                const nhom1 = []; // BB - nganh null, chuyenNganh null
                const nhom2 = []; // BB - nganh != null, chuyenNganh null
                const nhom3 = []; // BB - nganh != null, chuyenNganh != null
                const nhom4 = []; // TC - tất cả

                hocPhanList.forEach(ctdt => {
                    const nganh = ctdt.nganh ? ctdt.nganh.trim() : '';
                    const chuyenNganh = ctdt.chuyenNganh ? ctdt.chuyenNganh.trim() : '';
                    const loai = ctdt.loai || 'BB';

                    if (loai === 'BB') {
                        if (nganh === '' && chuyenNganh === '') {
                            nhom1.push(ctdt);
                        } else if (nganh !== '' && chuyenNganh === '') {
                            nhom2.push(ctdt);
                        } else if (nganh !== '' && chuyenNganh !== '') {
                            nhom3.push(ctdt);
                        } else {
                            // Fallback - thêm vào nhóm 1
                            nhom1.push(ctdt);
                        }
                    } else if (loai === 'TC') {
                        nhom4.push(ctdt);
                    }
                });

                // Sắp xếp trong từng nhóm theo thứ tự nganh -> chuyên ngành -> tên
                const sortFunction = (a, b) => {
                    // So sánh nganh
                    const nganhA = a.nganh ? a.nganh.trim() : '';
                    const nganhB = b.nganh ? b.nganh.trim() : '';
                    if (nganhA !== nganhB) {
                        return nganhA.localeCompare(nganhB, 'vi');
                    }

                    // So sánh chuyên ngành
                    const cnA = a.chuyenNganh ? a.chuyenNganh.trim() : '';
                    const cnB = b.chuyenNganh ? b.chuyenNganh.trim() : '';
                    if (cnA !== cnB) {
                        return cnA.localeCompare(cnB, 'vi');
                    }

                    // So sánh tên học phần
                    const tenA = a.tenHocPhan ? a.tenHocPhan.trim() : '';
                    const tenB = b.tenHocPhan ? b.tenHocPhan.trim() : '';
                    return tenA.localeCompare(tenB, 'vi');
                };

                nhom1.sort(sortFunction);
                nhom2.sort(sortFunction);
                nhom3.sort(sortFunction);
                nhom4.sort(sortFunction);

                // Gộp lại thành danh sách đã sắp xếp
                hocPhanList = [...nhom1, ...nhom2, ...nhom3, ...nhom4];
                // ============ END SẮP XẾP ============

                const rowSpan = hocPhanList.length;

                // TÍNH TỔNG TÍN CHỈ THEO LOGIC MỚI
                let totalTinChi = 0;
                let totalTinChiBB = 0;
                let totalTinChiTC = 0;

                // Nhóm các môn tự chọn theo nhómTC
                const tcGroups = {};

                hocPhanList.forEach(ctdt => {
                    const tinChi = ctdt.tinChi || 0;

                    if (ctdt.loai === 'BB') {
                        totalTinChiBB += tinChi;
                    } else if (ctdt.loai === 'TC' && ctdt.nhomTC) {
                        const nhomTC = ctdt.nhomTC.trim();
                        if (!tcGroups[nhomTC]) {
                            tcGroups[nhomTC] = {
                                subjects: [],
                                totalCredits: 0
                            };
                        }
                        tcGroups[nhomTC].subjects.push(ctdt);
                        tcGroups[nhomTC].totalCredits += tinChi;
                    } else if (ctdt.loai === 'TC' && !ctdt.nhomTC) {
                        totalTinChiTC += tinChi;
                    }
                });

                // Xử lý các nhóm tự chọn
                Object.keys(tcGroups).forEach(nhomTC => {
                    const group = tcGroups[nhomTC];

                    try {
                        const parts = nhomTC.split('/');
                        if (parts.length === 2) {
                            const x = parseInt(parts[0].trim());
                            const y = parseInt(parts[1].trim());

                            if (!isNaN(x) && !isNaN(y) && y > 0) {
                                const ratio = x / y;
                                let groupConvertedCredits = 0;
                                group.subjects.forEach(subject => {
                                    const subjectCredits = subject.tinChi || 0;
                                    groupConvertedCredits += subjectCredits * ratio;
                                });

                                totalTinChiTC += groupConvertedCredits;
                            } else {
                                totalTinChiTC += group.totalCredits;
                            }
                        } else {
                            totalTinChiTC += group.totalCredits;
                        }
                    } catch (error) {
                        totalTinChiTC += group.totalCredits;
                    }
                });

                totalTinChi = totalTinChiBB + Math.round(totalTinChiTC * 10) / 10;

                const tooltipTitle = `
                    Chi tiết học kỳ ${hocKy}:<br>
                    - Tín chỉ bắt buộc: ${totalTinChiBB}<br>
                    - Tín chỉ tự chọn: ${Math.round(totalTinChiTC * 10) / 10}<br>
                    - Tổng cộng: ${totalTinChi}
                `.trim();

                hocPhanList.forEach((ctdt, index) => {
                    let tienQuyetText = '-';

                    if (ctdt.hocPhanTienQuyetIds && Array.isArray(ctdt.hocPhanTienQuyetIds) && ctdt.hocPhanTienQuyetIds.length > 0) {
                        tienQuyetText = ctdt.hocPhanTienQuyetIds.join(', ');
                    } else if (ctdt.hocPhanTienQuyet && Array.isArray(ctdt.hocPhanTienQuyet)) {
                        if (ctdt.hocPhanTienQuyet.length > 0) {
                            const firstItem = ctdt.hocPhanTienQuyet[0];
                            if (firstItem.tenHocPhan) {
                                tienQuyetText = ctdt.hocPhanTienQuyet
                                    .map(hp => hp.tenHocPhan)
                                    .join(', ');
                            } else if (firstItem.id) {
                                tienQuyetText = ctdt.hocPhanTienQuyet
                                    .map(hp => hp.id)
                                    .join(', ');
                            }
                        }
                    }

                    html += `
                        <tr>
                            ${index === 0 ? `
                                <td rowspan="${rowSpan}" class="fw-bold">
                                    <div>${hocKy}</div>
                                    <small class="text-muted" style="font-size: 11px;">${totalTinChi} TC</small>
                                </td>
                            ` : ''}
                            <td>
                                 ${ctdt.maHocPhan ?
                                     `<span class="badge bg-dark">${ctdt.maHocPhan}</span>` :
                                     '<span class="text-muted">-</span>'}
                            </td>
                            <td>${globalIndex++}</td>
                            <td class="text-start">${ctdt.tenHocPhan || ''}</td>
                            <td>${ctdt.tinChi || 0}</td>
                            <td>
                                <span class="badge ${ctdt.loai === 'BB' ? 'bg-primary' : 'bg-success'}">
                                    ${ctdt.loai || 'BB'}
                                </span>
                            </td>
                            <td>${ctdt.nhomTC || '-'}</td>
                            <td class="text-start small" title="${tienQuyetText}">
                                ${tienQuyetText.length > 50 ? tienQuyetText.substring(0, 50) + '...' : tienQuyetText}
                            </td>
                            <td>${ctdt.nganh || '-'}</td>
                            <td>${ctdt.chuyenNganh || '-'}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn-edit" onclick="editCTDT(${ctdt.id})" title="Sửa">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button class="btn-delete" onclick="deleteCTDT(${ctdt.id})" title="Xóa">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `;
                });

                html += `
                    <tr class="table-info">
                        <td colspan="3" class="text-end fw-bold">Tổng kết học kỳ ${hocKy}:</td>
                        <td class="fw-bold">${totalTinChi}</td>
                        <td colspan="6">
                            <small class="text-muted">
                                <i>Bắt buộc: ${totalTinChiBB} TC | Tự chọn: ${Math.round(totalTinChiTC * 10) / 10} TC</i>
                            </small>
                        </td>
                    </tr>
                `;

                totalCreditsAllSemesters += totalTinChi;
            });

            html += `
                <tr class="table-success">
                    <td colspan="3" class="text-end fw-bold">TỔNG CỘNG TẤT CẢ HỌC KỲ:</td>
                    <td class="fw-bold">${totalCreditsAllSemesters}</td>
                    <td colspan="6" class="text-muted">
                        <i>Tổng số tín chỉ toàn chương trình</i>
                    </td>
                </tr>
            `;
        }

        // Ghi vào tbody của tab đang active
        $(getActiveTableBody()).html(html);
        updateActiveCount(ctdtList.length);
    }

    // Tìm kiếm CTDT — đọc filter từ tab đang active
    function searchCTDT() {
        const nganh = CTDT_APP.currentNganh;
        const tbodySelector = getActiveTableBody();

        let hocKy, filterNganh, chuyenNganh, loai, tabNganh;

        if (nganh === 'all') {
            hocKy       = $('#filterHocKy').val();
            filterNganh = $('#filterNganh').val();
            chuyenNganh = $('#filterChuyenNganh').val();
            loai        = $('#filterLoai').val();
            tabNganh    = null;
        } else {
            const safe  = nganh.replace(/[^a-zA-Z0-9]/g, '_');
            const pane  = $(`#${safe}`);
            hocKy       = pane.find('.tab-filter-hocky').val();
            chuyenNganh = pane.find('.tab-filter-chuyennganh').val();
            loai        = pane.find('.tab-filter-loai').val();
            tabNganh    = nganh;   // dùng tabNganh để filter đúng học phần trong tab
            filterNganh = null;    // không filter theo field nganh của học phần
        }

        showLoading(tbodySelector);

        $.ajax({
            url: '/profile/api/admin/ctdt/search',
            type: 'GET',
            data: {
                hocKy:       hocKy       || null,
                nganh:       filterNganh || null,
                tabNganh:    tabNganh    || null,
                chuyenNganh: chuyenNganh || null,
                loai:        loai        || null
            },
            success: function(results) {
                renderCTDTTable(results);
                hideLoading(tbodySelector);
            },
            error: function() {
                showToast('Lỗi khi tìm kiếm CTĐT', 'error');
                hideLoading(tbodySelector);
            }
        });
    }

    // Reset tìm kiếm
    function resetCTDTSearch() {
        const nganh = CTDT_APP.currentNganh;
        if (nganh === 'all') {
            $('#filterHocKy, #filterNganh, #filterChuyenNganh, #filterLoai').val('');
        } else {
            const safe = nganh.replace(/[^a-zA-Z0-9]/g, '_');
            const pane = $(`#${safe}`);
            pane.find('.tab-filter-hocky, .tab-filter-chuyennganh, .tab-filter-loai').val('');
        }
        loadCTDTData(nganh);
    }

    // Mở modal CTDT
    function openCTDTModal(ctdtId = null) {
        CTDT_APP = {
            ...CTDT_APP,
            hocPhanList: [],
            nhomHocPhanList: [],
            currentCTDTId: ctdtId,
            hocPhanCounter: 0,
            nhomHocPhanCounter: 0
        };

        if (ctdtId) {
            // Code chỉnh sửa như cũ
            const modalHtml = `
                <div class="modal fade" id="ctdtModal" tabindex="-1" data-bs-backdrop="static">
                    <div class="modal-dialog modal-dialog-centered modal-xl">
                        <div class="modal-content">
                            <div class="modal-header py-2" style="background: var(--vlu-red); color: white;">
                                <h6 class="modal-title font-weight-bold">Chỉnh sửa CTĐT</h6>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body p-4">
                                <div id="ctdtModalLoading" class="text-center py-5">
                                    <div class="spinner-border text-primary mb-3"></div>
                                    <div class="text-muted">Đang tải dữ liệu...</div>
                                </div>
                                <div id="ctdtFormContent" style="display: none;"></div>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            $('#modalContainer').html(modalHtml);
            const modal = new bootstrap.Modal(document.getElementById('ctdtModal'));
            modal.show();

            loadCTDTForEdit(ctdtId);
        } else {
            const modalHtml = getCTDTModalHTML();
            const tempDiv = $('<div>').html(modalHtml);
            tempDiv.find('#ctdtModalTitle').text('Thêm chương trình đào tạo');

            // Set ngành mặc định nếu đang ở tab ngành cụ thể
            if (CTDT_APP.currentNganh !== 'all') {
                tempDiv.find('#ctdtNganh').val(CTDT_APP.currentNganh);
                tempDiv.find('#ctdtNganh').prop('disabled', true);
            }

            $('#modalContainer').html(tempDiv.html());
            const modal = new bootstrap.Modal(document.getElementById('ctdtModal'));
            modal.show();

            setTimeout(() => {
                loadNganhForCTDT();
                if (CTDT_APP.currentNganh !== 'all') {
                    $('#ctdtNganh').val(CTDT_APP.currentNganh);
                    $('#ctdtNganh').prop('disabled', true);
                }
                initTienQuyetAutocomplete();
            }, 300);
        }
    }

    function loadHocPhanTienQuyetForModal() {
        $.ajax({
            url: '/profile/api/admin/ctdt/hoc-phan',
            type: 'GET',
            success: function(hocPhanList) {
                let options = '<option value="">-- Chọn học phần tiên quyết --</option>';
                hocPhanList.forEach(hp => {
                    options += `<option value="${hp.id}">${hp.tenHocPhan} (HK${hp.hocKy}, ${hp.tinChi} TC)</option>`;
                });

                $('#ctdtModal .hocphan-tienquyet').each(function() {
                    $(this).html(options);
                });
            },
            error: function() {
            }
        });
    }

    function loadNganhForCTDT() {
        $.ajax({
            url: '/profile/api/admin/load-departments',
            type: 'GET',
            success: function(data) {
                let html = '<option value="">-- Chọn nganh --</option>';
                if (data.distinctDepts && data.distinctDepts.length > 0) {
                    data.distinctDepts.forEach(dept => {
                        html += `<option value="${dept}">${dept}</option>`;
                    });
                }
                $('#ctdtNganh').html(html);
            },
            error: function() {
                $('#ctdtNganh').html('<option value="">-- Chọn nganh --</option>');
            }
        });
    }

    function updateChuyenNganh() {
        const nganh = $('#ctdtNganh').val();

        if (!nganh) {
            $('#ctdtChuyenNganh').html('<option value="">-- Chọn chuyên ngành --</option>');
            return;
        }

        $.ajax({
            url: '/profile/api/admin/get-majors',
            type: 'GET',
            data: { departmentName: nganh },
            success: function(majors) {
                let html = '<option value="">-- Chọn chuyên ngành --</option>';
                if (majors && majors.length > 0) {
                    majors.forEach(major => {
                        html += `<option value="${major}">${major}</option>`;
                    });
                }
                $('#ctdtChuyenNganh').html(html);
            },
            error: function() {
                $('#ctdtChuyenNganh').html('<option value="">-- Chọn chuyên ngành --</option>');
            }
        });
    }

    function toggleNhomTC() {
        const loai = $('#ctdtLoai').val();
        if (loai === 'TC') {
            $('#nhomTCContainer').show();
        } else {
            $('#nhomTCContainer').hide();
        }
    }

    function addNhomHocPhan() {
        const nhomId = `nhom-${CTDT_APP.nhomHocPhanCounter++}`;

        const nhomHtml = `
            <div class="card mb-3 nhom-hoc-phan-card" id="${nhomId}">
                <div class="card-header py-2 bg-info text-white d-flex justify-content-between align-items-center">
                    <h6 class="mb-0"><i class="fas fa-layer-group me-2"></i>Nhóm học phần mới</h6>
                    <button type="button" class="btn btn-danger btn-sm py-0 px-2" onclick="removeNhomHocPhan('${nhomId}')">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="card-body">
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label class="form-label small">Tên nhóm học phần</label>
                            <input type="text" class="form-control form-control-sm nhom-ten"
                                   placeholder="Ví dụ: Nhóm môn cơ sở ngành">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small">Số môn cần học (nếu là tự chọn)</label>
                            <input type="text" class="form-control form-control-sm nhom-so-mon"
                                   placeholder="Ví dụ: 2/3 (học 2 trong 3 môn)">
                        </div>
                    </div>

                    <div class="text-end mb-3">
                        <button type="button" class="btn btn-outline-primary btn-sm" onclick="addHocPhanToNhom('${nhomId}')">
                            <i class="fas fa-plus me-1"></i> Thêm học phần vào nhóm
                        </button>
                    </div>

                    <div class="hoc-phan-nhom-container" id="hoc-phan-${nhomId}">
                        <!-- Các học phần trong nhóm sẽ được thêm vào đây -->
                    </div>
                </div>
            </div>
        `;

        $('#nhomHocPhanContainer').append(nhomHtml);
        CTDT_APP.nhomHocPhanList.push(nhomId);
    }

    function removeNhomHocPhan(nhomId) {
        $(`#${nhomId}`).remove();
        CTDT_APP.nhomHocPhanList = CTDT_APP.nhomHocPhanList.filter(id => id !== nhomId);
    }

    function addHocPhanToNhom(nhomId) {
        const hocPhanId = `hocphan-${CTDT_APP.hocPhanCounter++}`;

        const hocPhanHtml = `
            <div class="card mb-2" id="${hocPhanId}">
                <div class="card-body p-3">
                    <div class="row align-items-center">
                        <div class="col-md-2 mb-2">
                            <input type="text" class="form-control form-control-sm hocphan-ma"
                                    placeholder="Mã học phần">
                        </div>
                        <div class="col-md-4 mb-2">
                            <input type="text" class="form-control form-control-sm hocphan-ten"
                                   placeholder="Tên học phần *" required>
                        </div>
                        <div class="col-md-2 mb-2">
                            <input type="number" class="form-control form-control-sm hocphan-tinchi"
                                   placeholder="Tín chỉ *" min="0" max="10" required>
                        </div>
                        <div class="col-md-5 mb-2">
                            <label class="form-label small d-block">Học phần tiên quyết</label>
                            <div class="tienquyet-container hocphan-tienquyet-container" id="tq-container-${hocPhanId}">
                                <div class="input-group input-group-sm mb-2">
                                    <input type="text" class="form-control form-control-sm hocphan-tienquyet-search"
                                           placeholder="Nhập tên học phần..."
                                           data-hocphan-id="${hocPhanId}" style="font-size: 12px;">
                                    <button class="btn btn-outline-secondary btn-sm" type="button" onclick="searchTienQuyetForHocPhan('${hocPhanId}')">
                                        <i class="fas fa-search"></i>
                                    </button>
                                </div>

                                <div class="tienquyet-results hocphan-tienquyet-results" id="tq-results-${hocPhanId}" style="display: none; max-height: 150px;">
                                    <div id="tq-results-list-${hocPhanId}" class="p-1"></div>
                                </div>

                                <div class="selected-tienquyet mt-2">
                                    <small class="text-muted d-block mb-1">Đã chọn: <span id="selected-count-${hocPhanId}">0</span></small>
                                    <div id="selected-list-${hocPhanId}" class="d-flex flex-wrap gap-1" style="font-size: 11px;"></div>
                                </div>

                                <input type="hidden" class="hocphan-tienquyet-ids" data-hocphan-id="${hocPhanId}" value="[]">
                            </div>
                        </div>
                        <div class="col-md-1 mb-2 text-center">
                            <button type="button" class="btn btn-danger btn-sm" onclick="removeHocPhan('${hocPhanId}')">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        $(`#hoc-phan-${nhomId}`).append(hocPhanHtml);

        // Khởi tạo autocomplete cho học phần này
        initTienQuyetForHocPhan(hocPhanId);

        CTDT_APP.hocPhanList.push({
            id: hocPhanId,
            nhomId: nhomId,
            element: $(`#${hocPhanId}`)
        });
    }

    function loadHocPhanForTienQuyetSingle(hocPhanId) {
        $.ajax({
            url: '/profile/api/admin/ctdt/hoc-phan',
            type: 'GET',
            success: function(hocPhanList) {
                let options = '<option value="">-- Chọn học phần tiên quyết --</option>';
                hocPhanList.forEach(hp => {
                    options += `<option value="${hp.id}">${hp.tenHocPhan} (HK${hp.hocKy}, ${hp.tinChi} TC)</option>`;
                });

                $(`#${hocPhanId} .hocphan-tienquyet`).html(options);
            },
            error: function() {
                $(`#${hocPhanId} .hocphan-tienquyet`).html('<option value="">-- Lỗi tải dữ liệu --</option>');
            }
        });
    }

    function addHocPhan() {
        const hocPhanId = `hocphan-${CTDT_APP.hocPhanCounter++}`;

        const hocPhanHtml = `
            <div class="card mb-3" id="${hocPhanId}">
                <div class="card-header py-2 bg-light d-flex justify-content-between align-items-center">
                    <h6 class="mb-0"><i class="fas fa-book me-2"></i>Học phần độc lập</h6>
                    <button type="button" class="btn btn-danger btn-sm py-0 px-2" onclick="removeHocPhan('${hocPhanId}')">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-3 mb-3">
                             <label class="form-label small">Mã học phần</label>
                             <input type="text" class="form-control form-control-sm hocphan-ma"
                                    placeholder="Ví dụ: 71ITBS10203">
                        </div>
                        <div class="col-md-5 mb-3">
                            <label class="form-label small">Tên học phần *</label>
                            <input type="text" class="form-control form-control-sm hocphan-ten" required>
                        </div>
                        <div class="col-md-2 mb-3">
                            <label class="form-label small">Số tín chỉ *</label>
                            <input type="number" class="form-control form-control-sm hocphan-tinchi"
                                   min="0" max="10" required>
                        </div>
                        <div class="col-md-2 mb-3">
                            <label class="form-label small">Loại</label>
                            <select class="form-select form-select-sm hocphan-loai" onchange="toggleNhomTCForHocPhan('${hocPhanId}')">
                                <option value="BB">Bắt buộc</option>
                                <option value="TC">Tự chọn</option>
                            </select>
                        </div>
                        <div class="col-md-3 mb-3" id="nhomtc-${hocPhanId}" style="display: none;">
                            <label class="form-label small">Nhóm TC</label>
                            <input type="text" class="form-control form-control-sm hocphan-nhomtc"
                                   placeholder="Ví dụ: 2/4 hoặc 3/6">
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-12 mb-3">
                            <label class="form-label small">Học phần tiên quyết</label>
                            <div class="tienquyet-container hocphan-tienquyet-container" id="tq-container-${hocPhanId}">
                                <div class="input-group input-group-sm mb-2">
                                    <input type="text" class="form-control form-control-sm hocphan-tienquyet-search"
                                           placeholder="Nhập tên học phần để tìm kiếm..."
                                           data-hocphan-id="${hocPhanId}">
                                    <button class="btn btn-outline-secondary" type="button" onclick="searchTienQuyetForHocPhan('${hocPhanId}')">
                                        <i class="fas fa-search"></i>
                                    </button>
                                </div>

                                <div class="tienquyet-results hocphan-tienquyet-results" id="tq-results-${hocPhanId}" style="display: none;">
                                    <div id="tq-results-list-${hocPhanId}" class="p-2"></div>
                                </div>

                                <div class="selected-tienquyet mt-2">
                                    <small class="text-muted d-block mb-1">Đã chọn: <span id="selected-count-${hocPhanId}">0</span> học phần</small>
                                    <div id="selected-list-${hocPhanId}" class="selected-tienquyet-list d-flex flex-wrap gap-1"></div>
                                </div>

                                <input type="hidden" class="hocphan-tienquyet-ids" data-hocphan-id="${hocPhanId}" value="[]">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        $('#hocPhanListContainer').append(hocPhanHtml);

        // Khởi tạo autocomplete cho học phần này
        initTienQuyetForHocPhan(hocPhanId);

        CTDT_APP.hocPhanList.push({
            id: hocPhanId,
            nhomId: null,
            element: $(`#${hocPhanId}`)
        });
    }

    function loadHocPhanForTienQuyet(hocPhanId) {
        $.ajax({
            url: '/profile/api/admin/ctdt/hoc-phan',
            type: 'GET',
            success: function(hocPhanList) {
                let options = '<option value="">-- Chọn học phần tiên quyết --</option>';
                if (hocPhanList && Array.isArray(hocPhanList)) {
                    hocPhanList.forEach(hp => {
                        // KHÔNG hiển thị học phần có ID đang chỉnh sửa
                        const currentHocPhanElement = $(`#${hocPhanId}`);
                        let currentHocPhanName = '';
                        if (currentHocPhanElement.length) {
                            currentHocPhanName = currentHocPhanElement.find('.hocphan-ten').val() || '';
                        }

                        // Thêm option
                        options += `<option value="${hp.id}">${hp.tenHocPhan} (HK${hp.hocKy || ''}, ${hp.tinChi || 0} TC)</option>`;
                    });
                }
                $(`#${hocPhanId} .hocphan-tienquyet`).html(options);
            },
            error: function() {
                $(`#${hocPhanId} .hocphan-tienquyet`).html('<option value="">-- Lỗi tải dữ liệu --</option>');
            }
        });
    }

    function removeHocPhan(hocPhanId) {
        $(`#${hocPhanId}`).remove();
        CTDT_APP.hocPhanList = CTDT_APP.hocPhanList.filter(hp => hp.id !== hocPhanId);
    }

    function saveAllCTDT() {
        const hocKy = $('#ctdtHocKy').val();
        if (!hocKy) {
            showToast('Vui lòng chọn học kỳ', 'error');
            return;
        }

        const nganh = CTDT_APP.currentNganh !== 'all' ? CTDT_APP.currentNganh : $('#ctdtNganh').val();
        const chuyenNganh = $('#ctdtChuyenNganh').val();
        const loai = $('#ctdtLoai').val();
        const nhomTC = $('#ctdtNhomTC').val();

        if (!nganh && CTDT_APP.currentNganh === 'all') {
            showToast('Vui lòng chọn ngành', 'error');
            return;
        }

        const allHocPhanData = [];

        // Xử lý học phần trong nhóm
        CTDT_APP.nhomHocPhanList.forEach(nhomId => {
            const nhomTen = $(`#${nhomId} .nhom-ten`).val() || `Nhóm ${CTDT_APP.nhomHocPhanList.indexOf(nhomId) + 1}`;
            const nhomSoMon = $(`#${nhomId} .nhom-so-mon`).val() || '';

            const hocPhanInNhom = CTDT_APP.hocPhanList.filter(hp => hp.nhomId === nhomId);

            hocPhanInNhom.forEach(hp => {
                const hocPhanEl = hp.element;
                const maHocPhan = hocPhanEl.find('.hocphan-ma').val();
                const tenHocPhan = hocPhanEl.find('.hocphan-ten').val();
                const tinChi = hocPhanEl.find('.hocphan-tinchi').val();

                const tienQuyetIds = getSelectedTienQuyetIdsForHocPhan(hp.id);

                if (tenHocPhan && tinChi) {
                    const hocPhanData = {
                        maHocPhan: maHocPhan || null,
                        hocKy: parseInt(hocKy),
                        tenHocPhan: tenHocPhan,
                        tinChi: parseInt(tinChi),
                        loai: loai,
                        nhomTC: loai === 'TC' ? (nhomSoMon || nhomTC) : null,
                        nganh: nganh || null,
                        chuyenNganh: chuyenNganh || null,
                        nhomHocPhan: nhomTen,
                        hocPhanTienQuyetIds: tienQuyetIds
                    };
                    allHocPhanData.push(hocPhanData);
                }
            });
        });

        // Xử lý học phần độc lập
        const independentHocPhan = CTDT_APP.hocPhanList.filter(hp => hp.nhomId === null);

        independentHocPhan.forEach(hp => {
            const hocPhanEl = hp.element;
            const maHocPhan = hocPhanEl.find('.hocphan-ma').val();
            const tenHocPhan = hocPhanEl.find('.hocphan-ten').val();
            const tinChi = hocPhanEl.find('.hocphan-tinchi').val();
            const hpLoai = hocPhanEl.find('.hocphan-loai').val();
            const hpNhomTC = hocPhanEl.find('.hocphan-nhomtc').val();

            const tienQuyetIds = getSelectedTienQuyetIdsForHocPhan(hp.id);

            if (tenHocPhan && tinChi) {
                const hocPhanData = {
                    maHocPhan: maHocPhan || null,
                    hocKy: parseInt(hocKy),
                    tenHocPhan: tenHocPhan,
                    tinChi: parseInt(tinChi),
                    loai: hpLoai,
                    nhomTC: hpLoai === 'TC' ? hpNhomTC : null,
                    nganh: nganh || null,
                    chuyenNganh: chuyenNganh || null,
                    nhomHocPhan: null,
                    hocPhanTienQuyetIds: tienQuyetIds
                };

                allHocPhanData.push(hocPhanData);
            }
        });

        if (allHocPhanData.length === 0) {
            showToast('Vui lòng thêm ít nhất một học phần', 'error');
            return;
        }

        showLoading('#ctdtModal .modal-body');

        $.ajax({
            url: '/profile/api/admin/ctdt/save-batch',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(allHocPhanData),
            success: function(response) {
                hideLoading('#ctdtModal .modal-body');

                showToast(`Đã lưu ${allHocPhanData.length} học phần thành công`, 'success');

                setTimeout(() => {
                    $('#ctdtModal').modal('hide');
                    if (APP.currentTab === 'admin-ctdt') {
                        loadCTDTData(CTDT_APP.currentNganh);
                    }
                }, 1000);
            },
            error: function(xhr, status, error) {
                hideLoading('#ctdtModal .modal-body');
                let errorMsg = 'Lỗi khi lưu CTĐT';
                if (xhr.responseText) {
                    try {
                        const errorResponse = JSON.parse(xhr.responseText);
                        errorMsg = errorResponse.message || errorResponse;
                    } catch (e) {
                        errorMsg = xhr.responseText.substring(0, 100);
                    }
                }
                showToast(errorMsg, 'error');
            }
        });
    }

    function editCTDT(ctdtId) {
        openCTDTModal(ctdtId);
    }

    function loadCTDTForEdit(ctdtId) {
        $('#ctdtModalLoading').show();
        $('#ctdtFormContent').hide();

        // Reset selected list
        TienQuyet.selectedHocPhan = [];

        $.ajax({
            url: `/profile/api/admin/ctdt/${ctdtId}`,
            type: 'GET',
            success: function(ctdtData) {
                $.ajax({
                    url: '/profile/api/admin/ctdt/filters',
                    type: 'GET',
                    success: function(filters) {
                        const formHtml = generateCTDTFormHtml(ctdtData, filters);
                        $('#ctdtFormContent').html(formHtml);

                        // Load danh sách học phần trước
                        loadAllHocPhan();

                        // Khởi tạo autocomplete và load dữ liệu đã chọn
                        setTimeout(() => {
                            initTienQuyetAutocomplete();
                            if (ctdtData.hocPhanTienQuyetIds && ctdtData.hocPhanTienQuyetIds.length > 0) {
                                // Chờ cho danh sách học phần được load xong
                                setTimeout(() => {
                                    loadSelectedTienQuyet(ctdtData.hocPhanTienQuyetIds);
                                }, 300);
                            }
                        }, 100);

                        $('#ctdtModalLoading').hide();
                        $('#ctdtFormContent').show();
                    },
                    error: function() {
                        $('#ctdtFormContent').html(`
                            <div class="alert alert-danger">
                                <i class="fas fa-exclamation-triangle me-2"></i>
                                Lỗi khi tải form
                            </div>
                        `);
                        $('#ctdtModalLoading').hide();
                        $('#ctdtFormContent').show();
                    }
                });
            },
            error: function(xhr) {
                $('#ctdtModalLoading').hide();
                $('#ctdtFormContent').html(`
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Lỗi khi tải thông tin học phần: ${xhr.responseText || 'Unknown error'}
                    </div>
                `);
                $('#ctdtFormContent').show();
            }
        });
    }

    function loadTienQuyetOptions(selectedIds = []) {
        $.ajax({
            url: '/profile/api/admin/ctdt/hoc-phan',
            type: 'GET',
            success: function(hocPhanList) {
                let options = '<option value="">-- Chọn học phần tiên quyết --</option>';
                hocPhanList.forEach(hp => {
                    const isSelected = selectedIds.includes(hp.id);
                    options += `<option value="${hp.id}" ${isSelected ? 'selected' : ''}>
                        ${hp.tenHocPhan} (HK${hp.hocKy}, ${hp.tinChi} TC)
                    </option>`;
                });

                $('#editTienQuyet').html(options);
            },
            error: function() {
                $('#editTienQuyet').html('<option value="">-- Chọn học phần tiên quyết --</option>');
            }
        });
    }

    function generateCTDTFormHtml(ctdtData, filters) {
        const data = ctdtData || {};

        return `
            <form id="ctdtEditForm">
                <input type="hidden" id="ctdtId" value="${data.id || ''}">
                <input type="hidden" id="ctdtTabNganh" value="${data.tabNganh || ''}">
                <input type="hidden" id="ctdtMaHocPhan" value="${data.maHocPhan || ''}">

                <div class="row mb-3">
                    <div class="col-md-6">
                        <label class="form-label small">Học kỳ *</label>
                        <select id="editHocKy" class="form-select form-select-sm" required>
                            <option value="">-- Chọn học kỳ --</option>
                            ${filters.hocKyList ? filters.hocKyList.map(hk =>
                                `<option value="${hk}" ${data.hocKy === hk ? 'selected' : ''}>Học kỳ ${hk}</option>`
                            ).join('') : ''}
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small">Tên học phần *</label>
                        <input type="text" id="editTenHocPhan" class="form-control form-control-sm"
                               value="${data.tenHocPhan || ''}" required>
                    </div>
                </div>

                <div class="row mb-3">
                    <div class="col-md-4">
                        <label class="form-label small">Số tín chỉ *</label>
                        <input type="number" id="editTinChi" class="form-control form-control-sm"
                               value="${data.tinChi || ''}" min="0" max="10" required>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small">Loại *</label>
                        <select id="editLoai" class="form-select form-select-sm" required onchange="toggleEditNhomTC()">
                            <option value="BB" ${data.loai === 'BB' ? 'selected' : ''}>Bắt buộc (BB)</option>
                            <option value="TC" ${data.loai === 'TC' ? 'selected' : ''}>Tự chọn (TC)</option>
                        </select>
                    </div>
                    <div class="col-md-4" id="editNhomTCContainer" style="${data.loai === 'TC' ? '' : 'display: none;'}">
                        <label class="form-label small">Nhóm TC</label>
                        <input type="text" id="editNhomTC" class="form-control form-control-sm"
                               value="${data.nhomTC || ''}" placeholder="Ví dụ: 2/5">
                    </div>
                </div>

                <div class="row mb-3">
                    <div class="col-md-6">
                        <label class="form-label small">Nganh</label>
                        <select id="editNganh" class="form-select form-select-sm">
                            <option value="">-- Chọn nganh --</option>
                            ${filters.nganhList ? filters.nganhList.map(k =>
                                `<option value="${k}" ${data.nganh === k ? 'selected' : ''}>${k}</option>`
                            ).join('') : ''}
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small">Chuyên ngành</label>
                        <select id="editChuyenNganh" class="form-select form-select-sm">
                            <option value="">-- Chọn chuyên ngành --</option>
                            ${filters.chuyenNganhList ? filters.chuyenNganhList.map(cn =>
                                `<option value="${cn}" ${data.chuyenNganh === cn ? 'selected' : ''}>${cn}</option>`
                            ).join('') : ''}
                        </select>
                    </div>
                </div>

                <div class="row mb-4">
                    <div class="col-md-12">
                        <label class="form-label small">Học phần tiên quyết</label>
                        <div class="tienquyet-container">
                            <div class="input-group input-group-sm mb-2">
                                <input type="text" class="form-control form-control-sm tienquyet-search"
                                       placeholder="Nhập tên học phần để tìm kiếm...">
                                <button class="btn btn-outline-secondary" type="button" onclick="searchTienQuyet()">
                                    <i class="fas fa-search"></i>
                                </button>
                            </div>

                            <div class="tienquyet-results" style="display: none;">
                                <div id="tienquyetResultsList" class="p-2"></div>
                            </div>

                            <div class="selected-tienquyet mt-2">
                                <small class="text-muted d-block mb-1">Đã chọn: <span id="selectedCount">0</span> học phần</small>
                                <div id="selectedTienQuyetList" class="selected-tienquyet-list d-flex flex-wrap gap-1">
                                    <!-- Các học phần đã chọn sẽ hiển thị ở đây -->
                                </div>
                            </div>

                            <input type="hidden" id="tienQuyetIds" name="tienQuyetIds" value="${JSON.stringify(data.hocPhanTienQuyetIds || [])}">
                        </div>
                        <small class="text-muted">Nhập tên học phần để tìm kiếm và chọn học phần tiên quyết</small>
                    </div>
                </div>

                <div class="d-flex justify-content-center gap-2">
                    <button type="button" class="btn btn-vlu-blue btn-sm px-4" onclick="saveCTDT()">
                        <i class="fas fa-save me-1"></i> ${data.id ? 'Cập nhật' : 'Lưu'}
                    </button>
                    <button type="button" class="btn btn-danger btn-sm px-4" data-bs-dismiss="modal">
                        Đóng
                    </button>
                </div>
            </form>
        `;
    }

    // Thêm hàm toggle cho edit form
    function toggleEditNhomTC() {
        const loai = $('#editLoai').val();
        if (loai === 'TC') {
            $('#editNhomTCContainer').show();
        } else {
            $('#editNhomTCContainer').hide();
        }
    }

    function saveCTDT() {
        const form = $('#ctdtEditForm')[0];
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const ctdtId = $('#ctdtId').val();
        const loai = $('#editLoai').val();

        // Lấy danh sách ID học phần tiên quyết từ autocomplete
        const tienQuyetIds = getSelectedTienQuyetIds();

        const data = {
            id: ctdtId || null,
            // FIX LỖI 2: Gửi kèm tabNganh để backend không set null khi sửa
            tabNganh: $('#ctdtTabNganh').val() || null,
            maHocPhan: $('#ctdtMaHocPhan').val() || null,
            hocKy: parseInt($('#editHocKy').val()),
            tenHocPhan: $('#editTenHocPhan').val(),
            tinChi: parseInt($('#editTinChi').val()),
            loai: loai,
            nhomTC: loai === 'TC' ? $('#editNhomTC').val() : null,
            nganh: $('#editNganh').val() || null,
            chuyenNganh: $('#editChuyenNganh').val() || null,
            hocPhanTienQuyetIds: tienQuyetIds
        };

        console.log("Saving CTDT with data:", data);

        $('#ctdtModalLoading').show();
        $('#ctdtFormContent').hide();

        $.ajax({
            url: '/profile/api/admin/ctdt/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                $('#ctdtModal').modal('hide');

                if (APP.currentTab === 'admin-ctdt') {
                    loadCTDTData(CTDT_APP.currentNganh);
                }
            },
            error: function(xhr, status, error) {
                $('#ctdtModalLoading').hide();
                $('#ctdtFormContent').show();

                let errorMsg = 'Lỗi khi lưu học phần';
                if (xhr.responseText) {
                    try {
                        const errorResponse = JSON.parse(xhr.responseText);
                        errorMsg = errorResponse.message || errorResponse;
                    } catch (e) {
                        errorMsg = xhr.responseText.substring(0, 100);
                    }
                }
                showToast(errorMsg, 'error');
            }
        });
    }

    function deleteCTDT(ctdtId) {
        if (!confirm('Bạn có chắc chắn muốn xóa học phần này?')) {
            return;
        }

        showLoading();

        $.ajax({
            url: `/profile/api/admin/ctdt/delete/${ctdtId}`,
            type: 'DELETE',
            success: function(response) {
                hideLoading();

                if (APP.currentTab === 'admin-ctdt') {
                    loadCTDTData(CTDT_APP.currentNganh);
                }
            },
            error: function(xhr) {
                hideLoading();
                showToast(xhr.responseText || 'Lỗi khi xóa CTĐT', 'error');
            }
        });
    }

// ============ INITIALIZATION ============
    // Initialize CTDT when tab is loaded
    $(document).on('ctdtTabLoaded', function() {
        loadCTDTData(CTDT_APP.currentNganh);
    });

    // Thêm sự kiện change cho editLoai
    $(document).on('change', '#editLoai', function() {
        const loai = $(this).val();
        if (loai === 'TC') {
            $('#editNhomTCContainer').show();
        } else {
            $('#editNhomTCContainer').hide();
        }
    });

// ============ TIEN QUYET AUTOCOMPLETE FUNCTIONS ============
    let TienQuyet = {
        allHocPhan: [],
        selectedHocPhan: [],
        searchTimer: null,
        currentModalId: null
    };

// ============ TIEN QUYET AUTOCOMPLETE FOR INDIVIDUAL HOC PHAN ============
    // Khởi tạo autocomplete cho một học phần cụ thể
    function initTienQuyetForHocPhan(hocPhanId) {
        // Load danh sách học phần nếu chưa có
        if (TienQuyet.allHocPhan.length === 0) {
            loadAllHocPhan();
        }

        const searchInput = $(`#tq-container-${hocPhanId} .hocphan-tienquyet-search`);
        const resultsDiv = $(`#tq-results-${hocPhanId}`);

        // Gắn sự kiện input
        searchInput.off('input').on('input', function() {
            const searchTerm = $(this).val().trim();
            const currentHocPhanId = $(this).data('hocphan-id');

            clearTimeout(TienQuyet.searchTimer);

            if (searchTerm.length === 0) {
                resultsDiv.hide();
                return;
            }

            if (searchTerm.length < 2) {
                showTienQuyetResultsForHocPhan(currentHocPhanId, [], 'Nhập ít nhất 2 ký tự');
                return;
            }

            TienQuyet.searchTimer = setTimeout(() => {
                searchHocPhanForTienQuyet(currentHocPhanId, searchTerm);
            }, 300);
        });

        // Gắn sự kiện keydown
        searchInput.off('keydown').on('keydown', function(e) {
            if (e.key === 'Escape') {
                resultsDiv.hide();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                const hocPhanId = $(this).data('hocphan-id');
                searchTienQuyetForHocPhan(hocPhanId);
            }
        });

        // Click ngoài để đóng kết quả
        $(document).on('click', function(e) {
            if (!$(e.target).closest(`#tq-container-${hocPhanId}`).length) {
                resultsDiv.hide();
            }
        });
    }

    // Tìm kiếm học phần tiên quyết cho một học phần cụ thể
    function searchHocPhanForTienQuyet(hocPhanId, searchTerm) {
        const searchLower = removeAccents(searchTerm.toLowerCase());
        const results = TienQuyet.allHocPhan.filter(hp => {
            if (!hp.tenHocPhan) return false;

            // Lọc ra học phần đã chọn cho học phần này
            const selectedIds = getSelectedTienQuyetIdsForHocPhan(hocPhanId);
            if (selectedIds.includes(hp.id)) {
                return false;
            }

            const hpName = removeAccents(hp.tenHocPhan.toLowerCase());
            return hpName.includes(searchLower);
        });

        showTienQuyetResultsForHocPhan(hocPhanId, results, searchTerm);
    }

    // Hiển thị kết quả cho một học phần cụ thể
    function showTienQuyetResultsForHocPhan(hocPhanId, results, searchTerm) {
        const resultsContainer = $(`#tq-results-list-${hocPhanId}`);
        const resultsDiv = $(`#tq-results-${hocPhanId}`);

        let html = '';

        if (results.length === 0) {
            html = `
                <div class="tienquyet-item text-muted text-center py-2">
                    Không tìm thấy học phần "${searchTerm}"
                </div>
            `;
        } else {
            results.forEach(hp => {
                const loaiBadge = hp.loai === 'BB' ? 'bg-primary' : 'bg-success';
                html += `
                    <div class="tienquyet-item"
                         onclick="selectTienQuyetForHocPhan('${hocPhanId}', ${hp.id}, '${hp.tenHocPhan.replace(/'/g, "\\'")}', ${hp.hocKy || 0}, ${hp.tinChi || 0}, '${hp.loai || 'BB'}')">
                        <div class="d-flex justify-content-between align-items-center">
                            <div class="me-2">
                                <strong style="font-size: 12px;">${hp.tenHocPhan || ''}</strong>
                                <div class="small text-muted">
                                    HK${hp.hocKy || ''} • ${hp.tinChi || 0} TC
                                </div>
                            </div>
                            <span class="badge ${loaiBadge}" style="font-size: 10px;">${hp.loai || 'BB'}</span>
                        </div>
                    </div>
                `;
            });
        }

        resultsContainer.html(html);
        resultsDiv.show();
    }

    // Chọn học phần tiên quyết cho một học phần cụ thể
    function selectTienQuyetForHocPhan(hocPhanId, id, name, hocKy, tinChi, loai) {
        // Lấy danh sách đã chọn hiện tại
        let selectedList = getSelectedTienQuyetForHocPhan(hocPhanId);

        // Kiểm tra trùng
        if (selectedList.some(hp => hp.id === id)) {
            showToast('Học phần này đã được chọn', 'warning');
            return;
        }

        // Thêm vào danh sách
        selectedList.push({
            id: id,
            name: name,
            hocKy: hocKy,
            tinChi: tinChi,
            loai: loai
        });

        // Lưu lại
        saveSelectedTienQuyetForHocPhan(hocPhanId, selectedList);

        // Cập nhật giao diện
        updateSelectedTienQuyetListForHocPhan(hocPhanId, selectedList);

        // Xóa ô tìm kiếm và ẩn kết quả
        $(`#tq-container-${hocPhanId} .hocphan-tienquyet-search`).val('');
        $(`#tq-results-${hocPhanId}`).hide();

        // Focus lại ô tìm kiếm
        $(`#tq-container-${hocPhanId} .hocphan-tienquyet-search`).focus();
    }

    // Lấy danh sách đã chọn cho một học phần
    function getSelectedTienQuyetForHocPhan(hocPhanId) {
        const hiddenInput = $(`#tq-container-${hocPhanId} .hocphan-tienquyet-ids`);
        try {
            const idsJson = hiddenInput.val();
            if (!idsJson || idsJson === '[]') {
                return [];
            }

            // Nếu chỉ có mảng ID, cần lấy thông tin đầy đủ
            const ids = JSON.parse(idsJson);
            if (Array.isArray(ids) && ids.length > 0) {
                // Kiểm tra xem có phải là mảng object đầy đủ không
                if (typeof ids[0] === 'object' && ids[0].id) {
                    return ids;
                } else {
                    // Chỉ có ID, cần lấy thông tin từ allHocPhan
                    return ids.map(id => {
                        const hp = TienQuyet.allHocPhan.find(h => h.id === id);
                        return hp ? {
                            id: hp.id,
                            name: hp.tenHocPhan,
                            hocKy: hp.hocKy,
                            tinChi: hp.tinChi,
                            loai: hp.loai
                        } : null;
                    }).filter(hp => hp !== null);
                }
            }
            return [];
        } catch (e) {
            console.error('Error parsing selected tien quyet:', e);
            return [];
        }
    }

    // Lưu danh sách đã chọn cho một học phần
    function saveSelectedTienQuyetForHocPhan(hocPhanId, selectedList) {
        const hiddenInput = $(`#tq-container-${hocPhanId} .hocphan-tienquyet-ids`);
        // Chỉ lưu mảng ID để gửi lên server
        const ids = selectedList.map(hp => hp.id);
        hiddenInput.val(JSON.stringify(ids));
    }

    // Cập nhật giao diện danh sách đã chọn cho một học phần
    function updateSelectedTienQuyetListForHocPhan(hocPhanId, selectedList) {
        const container = $(`#selected-list-${hocPhanId}`);
        const countElement = $(`#selected-count-${hocPhanId}`);

        let html = '';

        selectedList.forEach((hp, index) => {
            html += `
                <div class="selected-tienquyet-item" style="font-size: 11px; padding: 2px 6px;">
                    <span>${hp.name.substring(0, 15)}${hp.name.length > 15 ? '...' : ''}</span>
                    <button type="button" class="remove-btn" onclick="removeTienQuyetFromHocPhan('${hocPhanId}', ${hp.id})" title="Xóa">
                        <i class="fas fa-times" style="font-size: 10px;"></i>
                    </button>
                </div>
            `;
        });

        container.html(html);
        countElement.text(selectedList.length);
    }

    // Xóa học phần tiên quyết từ một học phần
    function removeTienQuyetFromHocPhan(hocPhanId, id) {
        let selectedList = getSelectedTienQuyetForHocPhan(hocPhanId);
        selectedList = selectedList.filter(hp => hp.id !== id);

        saveSelectedTienQuyetForHocPhan(hocPhanId, selectedList);
        updateSelectedTienQuyetListForHocPhan(hocPhanId, selectedList);

        // Nếu đang có từ khóa tìm kiếm, tìm lại
        const searchTerm = $(`#tq-container-${hocPhanId} .hocphan-tienquyet-search`).val().trim();
        if (searchTerm.length >= 2) {
            searchHocPhanForTienQuyet(hocPhanId, searchTerm);
        }
    }

    // Tìm kiếm bằng nút cho học phần cụ thể
    function searchTienQuyetForHocPhan(hocPhanId) {
        const searchTerm = $(`#tq-container-${hocPhanId} .hocphan-tienquyet-search`).val().trim();
        if (searchTerm.length >= 2) {
            searchHocPhanForTienQuyet(hocPhanId, searchTerm);
        } else {
            showTienQuyetResultsForHocPhan(hocPhanId, [], 'Nhập ít nhất 2 ký tự');
        }
    }

    // Lấy danh sách ID đã chọn cho một học phần
    function getSelectedTienQuyetIdsForHocPhan(hocPhanId) {
        const selectedList = getSelectedTienQuyetForHocPhan(hocPhanId);
        return selectedList.map(hp => hp.id);
    }

    // Toggle hiển thị nhóm TC cho học phần
    function toggleNhomTCForHocPhan(hocPhanId) {
        const loai = $(`#${hocPhanId} .hocphan-loai`).val();
        const nhomTCContainer = $(`#nhomtc-${hocPhanId}`);

        if (loai === 'TC') {
            nhomTCContainer.show();
        } else {
            nhomTCContainer.hide();
            $(`#${hocPhanId} .hocphan-nhomtc`).val('');
        }
    }

    // Load tất cả học phần
        function loadAllHocPhan() {
            $.ajax({
                url: '/profile/api/admin/ctdt/hoc-phan',
                type: 'GET',
                success: function(hocPhanList) {
                    TienQuyet.allHocPhan = hocPhanList || [];
                },
                error: function() {
                    TienQuyet.allHocPhan = [];
                }
            });
        }

        // Tìm kiếm học phần
        function searchHocPhan(searchTerm) {
            const searchLower = removeAccents(searchTerm.toLowerCase());
            const results = TienQuyet.allHocPhan.filter(hp => {
                if (!hp.tenHocPhan) return false;

                // Lọc ra học phần đã chọn
                if (TienQuyet.selectedHocPhan.some(selected => selected.id === hp.id)) {
                    return false;
                }

                const hpName = removeAccents(hp.tenHocPhan.toLowerCase());
                return hpName.includes(searchLower);
            });

            showTienQuyetResults(results, searchTerm);
        }

        // Hiển thị kết quả
            function showTienQuyetResults(results, searchTerm) {
                const resultsContainer = $('#tienquyetResultsList');
                const resultsDiv = $('.tienquyet-results');

                if (!resultsContainer.length) return;

                let html = '';

                if (results.length === 0) {
                    html = `
                        <div class="tienquyet-item text-muted text-center py-2">
                            ${searchTerm === 'Nhập ít nhất 2 ký tự' ?
                                'Nhập ít nhất 2 ký tự để tìm kiếm' :
                                `Không tìm thấy học phần "${searchTerm}"`}
                        </div>
                    `;
                } else {
                    results.forEach(hp => {
                        const loaiBadge = hp.loai === 'BB' ? 'bg-primary' : 'bg-success';
                        html += `
                            <div class="tienquyet-item"
                                 onclick="selectTienQuyetItem(${hp.id}, '${hp.tenHocPhan ? hp.tenHocPhan.replace(/'/g, "\\'") : ''}',
                                          ${hp.hocKy || 0}, ${hp.tinChi || 0}, '${hp.loai || 'BB'}')">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div class="me-2">
                                        <strong style="font-size: 13px;">${hp.tenHocPhan || ''}</strong>
                                        <div class="small text-muted">
                                            ${hp.maHocPhan ? `Mã: ${hp.maHocPhan} • ` : ''}HK${hp.hocKy || ''} • ${hp.tinChi || 0} TC
                                        </div>
                                    </div>
                                    <span class="badge ${loaiBadge}" style="font-size: 11px;">${hp.loai || 'BB'}</span>
                                </div>
                            </div>
                        `;
                    });
                }

                resultsContainer.html(html);
                resultsDiv.show();
            }

        // Chọn học phần từ kết quả tìm kiếm
            function selectTienQuyetItem(id, name, hocKy, tinChi, loai) {
                // Kiểm tra trùng
                if (TienQuyet.selectedHocPhan.some(hp => hp.id === id)) {
                    showToast('Học phần này đã được chọn', 'warning');
                    return;
                }

                // Thêm vào danh sách
                TienQuyet.selectedHocPhan.push({
                    id: id,
                    name: name,
                    hocKy: hocKy,
                    tinChi: tinChi,
                    loai: loai
                });

                // Cập nhật giao diện
                updateSelectedTienQuyetList();

                // Xóa ô tìm kiếm và ẩn kết quả
                $('.tienquyet-search').val('');
                $('.tienquyet-results').hide();

                // Focus lại ô tìm kiếm
                $('.tienquyet-search').focus();
            }

        // Cập nhật danh sách đã chọn
            function updateSelectedTienQuyetList() {
                const container = $('#selectedTienQuyetList');
                const countElement = $('#selectedCount');

                if (!container.length) return;

                let html = '';

                TienQuyet.selectedHocPhan.forEach((hp, index) => {
                    html += `
                        <div class="selected-tienquyet-item">
                            <span>${hp.name}</span>
                            <small class="text-muted">(HK${hp.hocKy}, ${hp.tinChi}TC)</small>
                            <button type="button" class="remove-btn" onclick="removeSelectedTienQuyet(${hp.id})" title="Xóa">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    `;
                });

                container.html(html);
                countElement.text(TienQuyet.selectedHocPhan.length);

                // Cập nhật hidden input
                const ids = TienQuyet.selectedHocPhan.map(hp => hp.id);
                $('#tienQuyetIds').val(JSON.stringify(ids));
            }

        // Xóa học phần đã chọn
            function removeSelectedTienQuyet(id) {
                TienQuyet.selectedHocPhan = TienQuyet.selectedHocPhan.filter(hp => hp.id !== id);
                updateSelectedTienQuyetList();

                // Nếu đang có từ khóa tìm kiếm, tìm lại
                const searchTerm = $('.tienquyet-search').val().trim();
                if (searchTerm.length >= 2) {
                    searchHocPhan(searchTerm);
                }
            }

            // Tìm kiếm bằng nút
            function searchTienQuyet() {
                const searchTerm = $('.tienquyet-search').val().trim();
                if (searchTerm.length >= 2) {
                    searchHocPhan(searchTerm);
                } else {
                    showTienQuyetResults([], 'Nhập ít nhất 2 ký tự');
                }
            }

            // Load học phần đã chọn từ dữ liệu cũ
            function loadSelectedTienQuyet(selectedIds) {
                if (!selectedIds || !Array.isArray(selectedIds) || selectedIds.length === 0) {
                    TienQuyet.selectedHocPhan = [];
                    updateSelectedTienQuyetList();
                    return;
                }

                // Lấy thông tin đầy đủ
                const selectedFullInfo = selectedIds.map(id => {
                    const hp = TienQuyet.allHocPhan.find(h => h.id === id);
                    return hp ? {
                        id: hp.id,
                        name: hp.tenHocPhan,
                        hocKy: hp.hocKy,
                        tinChi: hp.tinChi,
                        loai: hp.loai
                    } : null;
                }).filter(hp => hp !== null);

                TienQuyet.selectedHocPhan = selectedFullInfo;
                updateSelectedTienQuyetList();
            }

        // Lấy danh sách ID đã chọn
            function getSelectedTienQuyetIds() {
                return TienQuyet.selectedHocPhan.map(hp => hp.id);
            }

// ============ IMPORT EXCEL FUNCTIONS ============
    // Mở modal import Excel
    function openImportModal() {
        const currentNganh = CTDT_APP.currentNganh;
        const isSpecificTab = currentNganh && currentNganh !== 'all';

        const nganhInfoHtml = isSpecificTab
            ? `<div class="alert alert-primary mb-3 py-2">
                <i class="fas fa-tag me-2"></i>
                Đang import cho ngành: <strong>${currentNganh}</strong>
                <div class="small text-muted mt-1">Các học phần trong file sẽ được gán vào ngành này. Nếu file đã có cột "Ngành", giá trị trong file sẽ được ưu tiên.</div>
               </div>`
            : `<div class="alert alert-secondary mb-3 py-2">
                <i class="fas fa-info-circle me-2"></i>
                Đang ở tab <strong>Tất cả</strong>. Hãy chuyển sang tab ngành cụ thể để import đúng ngành, hoặc đảm bảo file có cột "Ngành".
               </div>`;

        const modalHtml = `
            <div class="modal fade" id="importModal" tabindex="-1" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content">
                        <div class="modal-header py-2" style="background: var(--vlu-green); color: white;">
                            <h6 class="modal-title font-weight-bold">
                                <i class="fas fa-file-import me-2"></i>Import từ file Excel
                                ${isSpecificTab ? `<small class="ms-2 opacity-75">— ${currentNganh}</small>` : ''}
                            </h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            ${nganhInfoHtml}
                            <!-- Hướng dẫn -->
                            <div class="alert alert-info mb-4">
                                <h6 class="alert-heading"><i class="fas fa-info-circle me-2"></i>Hướng dẫn</h6>
                                <ul class="mb-0 small">
                                    <li>Tải file mẫu để biết cấu trúc dữ liệu</li>
                                    <li>File Excel phải có các cột theo đúng thứ tự</li>
                                    <li>Chỉ chấp nhận file .xlsx hoặc .xls</li>
                                    <li>Dung lượng file tối đa 5MB</li>
                                    <li>Học phần cùng tên nhưng khác ngành sẽ được lưu riêng biệt</li>
                                </ul>
                            </div>

                            <!-- Form upload -->
                            <div class="card mb-4">
                                <div class="card-body">
                                    <div class="mb-3">
                                        <label class="form-label small fw-bold">Chọn file Excel</label>
                                        <input type="file" id="excelFile" class="form-control form-control-sm"
                                               accept=".xlsx,.xls" required>
                                        <div class="form-text">Chỉ chấp nhận file Excel (.xlsx, .xls)</div>
                                    </div>

                                    <div class="d-flex justify-content-between align-items-center">
                                        <button type="button" class="btn btn-outline-primary btn-sm" onclick="downloadTemplate()">
                                            <i class="fas fa-download me-1"></i> Tải file mẫu
                                        </button>
                                        <button type="button" class="btn btn-success btn-sm px-4" onclick="uploadExcel()">
                                            <i class="fas fa-upload me-1"></i> Upload
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <!-- Kết quả import -->
                            <div id="importResults" style="display: none;">
                                <div class="card">
                                    <div class="card-header py-2 bg-light">
                                        <h6 class="mb-0"><i class="fas fa-list-check me-2"></i>Kết quả import</h6>
                                    </div>
                                    <div class="card-body">
                                        <div id="importResultsContent"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        $('#modalContainer').html(modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('importModal'));
        modal.show();
    }

    // Tải file mẫu
    function downloadTemplate() {
        showLoading();

        $.ajax({
            url: '/profile/api/admin/ctdt/download-template',
            type: 'GET',
            xhrFields: {
                responseType: 'blob'
            },
            success: function(blob) {
                hideLoading();

                // Tạo link tải file
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'CTDT_Template.xlsx';
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            },
            error: function(xhr) {
                hideLoading();
                showToast('Lỗi khi tải file mẫu', 'error');
            }
        });
    }

    // Upload file Excel
    function uploadExcel() {
        const fileInput = $('#excelFile')[0];

        if (!fileInput.files || fileInput.files.length === 0) {
            showToast('Vui lòng chọn file Excel', 'error');
            return;
        }

        const file = fileInput.files[0];
        const fileSizeMB = file.size / (1024 * 1024);

        // Kiểm tra định dạng file
        const validExtensions = ['.xlsx', '.xls'];
        const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

        if (!validExtensions.includes(fileExtension)) {
            showToast('Chỉ chấp nhận file Excel (.xlsx, .xls)', 'error');
            return;
        }

        // Kiểm tra kích thước file
        if (fileSizeMB > 5) {
            showToast('Kích thước file không được vượt quá 5MB', 'error');
            return;
        }

        // Tạo FormData
        const formData = new FormData();
        formData.append('file', file);

        // Gửi kèm ngành hiện tại nếu đang ở tab ngành cụ thể
        const currentNganh = CTDT_APP.currentNganh;
        if (currentNganh && currentNganh !== 'all') {
            formData.append('targetNganh', currentNganh);
        }

        // Hiển thị loading
        showLoading('#importModal .modal-body');
        $('#importResults').hide();

        // Gửi request
        $.ajax({
            url: '/profile/api/admin/ctdt/import-excel',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                hideLoading('#importModal .modal-body');
                showImportResults(response);
            },
            error: function(xhr) {
                hideLoading('#importModal .modal-body');
                let errorMsg = 'Lỗi khi import file';
                if (xhr.responseText) {
                    try {
                        const errorResponse = JSON.parse(xhr.responseText);
                        errorMsg = errorResponse.message || errorResponse;
                    } catch (e) {
                        errorMsg = xhr.responseText.substring(0, 100);
                    }
                }
                showToast(errorMsg, 'error');
            }
        });
    }

    // Hiển thị kết quả import
    function showImportResults(result) {
        const totalRecords = result.totalRecords || 0;
        const successCount = result.successCount || 0;
        const errorCount = result.errorCount || 0;
        const results = result.results || [];

        let html = `
            <div class="mb-3">
                <div class="alert ${successCount === totalRecords ? 'alert-success' : 'alert-warning'}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <strong>Tổng số bản ghi:</strong> ${totalRecords} <br>
                            <strong>Thành công:</strong> ${successCount} <br>
                            <strong>Lỗi:</strong> ${errorCount}
                        </div>
                        <div>
                            ${successCount === totalRecords ?
                                '<span class="badge bg-success">HOÀN TẤT</span>' :
                                '<span class="badge bg-warning text-dark">CÓ LỖI</span>'}
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Hiển thị chi tiết lỗi nếu có
        if (errorCount > 0) {
            html += `
                <div class="mb-3">
                    <h6 class="text-danger mb-2"><i class="fas fa-exclamation-triangle me-2"></i>Chi tiết lỗi</h6>
                    <div class="table-responsive" style="max-height: 300px; overflow-y: auto;">
                        <table class="table table-sm table-bordered">
                            <thead class="table-light">
                                <tr>
                                    <th width="60">Dòng</th>
                                    <th>Lỗi</th>
                                    <th width="100">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
            `;

            results.forEach(item => {
                if (item.status === 'error') {
                    html += `
                        <tr>
                            <td class="text-center">${item.row}</td>
                            <td class="text-danger small">${item.message}</td>
                            <td class="text-center">
                                <span class="badge bg-danger">LỖI</span>
                            </td>
                        </tr>
                    `;
                }
            });

            html += `
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
        }

        // Hiển thị kết quả thành công
        if (successCount > 0) {
            html += `
                <div class="mb-3">
                    <h6 class="text-success mb-2"><i class="fas fa-check-circle me-2"></i>Đã import thành công</h6>
                    <div class="table-responsive" style="max-height: 200px; overflow-y: auto;">
                        <table class="table table-sm table-bordered">
                            <thead class="table-light">
                                <tr>
                                    <th width="60">Dòng</th>
                                    <th>Mã HP</th>
                                    <th>Tên học phần</th>
                                    <th width="100">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
            `;

            results.forEach(item => {
                if (item.status === 'success') {
                    const data = item.data || {};
                    html += `
                        <tr>
                            <td class="text-center">${item.row}</td>
                            <td>${data.maHocPhan || '-'}</td>
                            <td class="small">${data.tenHocPhan || '-'}</td>
                            <td class="text-center">
                                <span class="badge bg-success">THÀNH CÔNG</span>
                            </td>
                        </tr>
                    `;
                }
            });

            html += `
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
        }

        // Nút đóng và tải lại dữ liệu
        html += `
            <div class="d-flex justify-content-between mt-4">
                <button type="button" class="btn btn-secondary btn-sm" onclick="closeImportModal()">
                    Đóng
                </button>
                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-outline-primary btn-sm" onclick="downloadTemplate()">
                        <i class="fas fa-redownload me-1"></i> Tải lại mẫu
                    </button>
                    ${successCount > 0 ? `
                        <button type="button" class="btn btn-vlu-blue btn-sm" onclick="reloadCTDTAfterImport()">
                            <i class="fas fa-sync me-1"></i> Tải lại dữ liệu
                        </button>
                    ` : ''}
                </div>
            </div>
        `;

        $('#importResultsContent').html(html);
        $('#importResults').show();

        // FIX LỖI 3: Tự đóng modal và reload khi import hoàn toàn thành công (không có lỗi)
        if (successCount > 0 && errorCount === 0) {
            showToast(`Import thành công ${successCount} học phần!`, 'success');
            setTimeout(() => {
                closeImportModal();
                if (APP.currentTab === 'admin-ctdt') {
                    loadCTDTData(CTDT_APP.currentNganh);
                }
            }, 1800);
        }
    }

    // Đóng modal import
    function closeImportModal() {
        $('#importModal').modal('hide');
        setTimeout(() => {
            $('#modalContainer').empty();
        }, 300);
    }

    // Tải lại dữ liệu CTDT sau khi import
    function reloadCTDTAfterImport() {
        closeImportModal();
        if (APP.currentTab === 'admin-ctdt') {
            loadCTDTData(CTDT_APP.currentNganh);
        }
    }

    // ============ INITIALIZE TIEN QUYET AUTOCOMPLETE FOR EDIT MODAL ============
    function initTienQuyetAutocomplete() {
        // Load tất cả học phần trước
        loadAllHocPhan();

        const searchInput = $('.tienquyet-search');
        const resultsDiv = $('.tienquyet-results');

        if (!searchInput.length) return;

        // Gắn sự kiện input
        searchInput.off('input').on('input', function() {
            const searchTerm = $(this).val().trim();

            clearTimeout(TienQuyet.searchTimer);

            if (searchTerm.length === 0) {
                resultsDiv.hide();
                return;
            }

            if (searchTerm.length < 2) {
                showTienQuyetResults([], 'Nhập ít nhất 2 ký tự');
                return;
            }

            TienQuyet.searchTimer = setTimeout(() => {
                searchHocPhan(searchTerm);
            }, 300);
        });

        // Gắn sự kiện keydown
        searchInput.off('keydown').on('keydown', function(e) {
            if (e.key === 'Escape') {
                resultsDiv.hide();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                searchTienQuyet();
            }
        });

        // Click ngoài để đóng kết quả
        $(document).on('click', function(e) {
            if (!$(e.target).closest('.tienquyet-container').length) {
                resultsDiv.hide();
            }
        });

        // Khởi tạo selected list từ dữ liệu hiện tại (nếu có)
        const tienQuyetIdsInput = $('#tienQuyetIds');
        if (tienQuyetIdsInput.length && tienQuyetIdsInput.val()) {
            try {
                const selectedIds = JSON.parse(tienQuyetIdsInput.val());
                if (Array.isArray(selectedIds) && selectedIds.length > 0) {
                    // Load danh sách học phần trước, sau đó set selected
                    setTimeout(() => {
                        loadSelectedTienQuyet(selectedIds);
                    }, 500);
                }
            } catch (e) {
                console.error('Error parsing tienQuyetIds:', e);
            }
        }
    }

    // ============ ENSURE ALL HOC PHAN ARE LOADED ============
    function loadAllHocPhan() {
        if (TienQuyet.allHocPhan.length > 0) {
            return; // Đã load rồi
        }

        $.ajax({
            url: '/profile/api/admin/ctdt/hoc-phan',
            type: 'GET',
            success: function(hocPhanList) {
                TienQuyet.allHocPhan = hocPhanList || [];
                console.log('Loaded', TienQuyet.allHocPhan.length, 'hoc phan for autocomplete');
            },
            error: function() {
                console.error('Failed to load hoc phan list');
                TienQuyet.allHocPhan = [];
            }
        });
    }

// ============ WINDOW ASSIGNMENTS ============
    window.APP = APP;

    window.loadCTDTData = loadCTDTData;
    window.loadAllCTDT = loadAllCTDT;
    window.initCTTDTabs = initCTTDTabs;
    window.renderCTTDTabs = renderCTTDTabs;
    window.switchCTDTTab = switchCTDTTab;
    window.openCreatePlanModal = openCreatePlanModal;
    window.createNewPlan = createNewPlan;
    window.resetCTDTForm = resetCTDTForm;
    window.searchCTDT = searchCTDT;
    window.resetCTDTSearch = resetCTDTSearch;
    window.updateTabCounts = updateTabCounts;
    window.deletePlanData = deletePlanData;
    window.getActiveTableBody = getActiveTableBody;
    window.updateActiveCount = updateActiveCount;
    window.populateTabFilters = populateTabFilters;
// ============ DEPARTMENT MANAGEMENT ============
    function loadDepartmentData() {
        if ($('#deptTableBody').length === 0) return;
        showLoading('#deptTableBody');
        $.ajax({
            url: APP.endpoints.loadDepartments,
            type: 'GET',
            success: function(data) {
                renderDepartmentTable(data.departments);
                populateDeptFilter(data.distinctDepts);
                hideLoading('#deptTableBody');
            },
            error: function() {
                $('#deptTableBody').html(`
                    <tr><td colspan="4" class="text-center py-5 text-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>Lỗi khi tải dữ liệu
                    </td></tr>
                `);
                hideLoading('#deptTableBody');
                showToast('Lỗi khi tải danh sách nganh', 'error');
            }
        });
    }

    function renderDepartmentTable(departments) {
        let html = '';
        if (!departments || departments.length === 0) {
            html = `<tr><td colspan="4" class="text-center py-5 text-muted">
                <i class="fas fa-database me-2"></i>Chưa có dữ liệu
            </td></tr>`;
        } else {
            const groupedDepts = {};
            departments.forEach(dept => {
                if (!groupedDepts[dept.departmentName]) groupedDepts[dept.departmentName] = [];
                groupedDepts[dept.departmentName].push(dept);
            });
            let index = 1;
            Object.keys(groupedDepts).forEach(deptName => {
                const majors = groupedDepts[deptName];
                const rowSpan = majors.length;
                majors.forEach((dept, i) => {
                    html += `<tr>${i === 0 ? `<td rowspan="${rowSpan}">${index++}</td>` : ''}
                        ${i === 0 ? `<td rowspan="${rowSpan}" class="fw-bold"><div>${dept.departmentName || ''}</div>
                        <button class="btn btn-link btn-sm p-0 mt-1" onclick="viewDepartmentStats('${dept.departmentName}')" title="Xem thống kê">
                        <small><i class="fas fa-chart-bar me-1"></i>Thống kê</small></button></td>` : ''}
                        <td>${dept.majorName || ''}</td>
                        <td><div class="action-buttons">
                            <button class="btn-edit" onclick="editDepartment(${dept.id})" title="Sửa"><i class="fas fa-edit"></i></button>
                            <button class="btn-delete" onclick="deleteDepartment(${dept.id})" title="Xóa"><i class="fas fa-trash"></i></button>
                        </div></td></tr>`;
                });
            });
        }
        $('#deptTableBody').html(html);
        $('#deptCount').text(departments ? departments.length : 0);
    }

    function openDepartmentModal(deptId = null) {
        APP.currentDepartmentId = deptId;
        const modalTitle = deptId ? 'Chỉnh sửa nganh và chuyên ngành' : 'Thêm nganh và chuyên ngành';
        const modalHtml = `
            <div class="modal fade" id="deptModal" tabindex="-1" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header py-2" style="background: var(--vlu-red); color: white;">
                            <h6 class="modal-title font-weight-bold">${modalTitle}</h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <form id="deptForm">
                                <input type="hidden" id="deptId" name="id">
                                <div class="mb-3"><label class="form-label small">Tên nganh *</label>
                                    <input type="text" id="departmentName" name="departmentName" class="form-control form-control-sm" required>
                                    <div class="form-text">Nhập tên nganh (ví dụ: Công nghệ thông tin)</div>
                                </div>
                                <div class="mb-4"><label class="form-label small">Tên chuyên ngành *</label>
                                    <input type="text" id="majorName" name="majorName" class="form-control form-control-sm" required>
                                    <div class="form-text">Nhập tên chuyên ngành (ví dụ: Công nghệ phần mềm)</div>
                                </div>
                                <div class="d-flex justify-content-center gap-2 mt-4">
                                    <button type="button" class="btn btn-vlu-blue btn-sm px-4" onclick="saveDepartment()">
                                        ${deptId ? 'Cập nhật' : 'Thêm mới'}
                                    </button>
                                    <button type="button" class="btn btn-danger btn-sm px-4" data-bs-dismiss="modal">Đóng</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>`;
        $('#modalContainer').html(modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('deptModal'));
        if (deptId) loadDepartmentDataForEdit(deptId);
        else $('#deptForm')[0].reset();
        modal.show();
    }

    function editDepartment(deptId) {
        openDepartmentModal(deptId);
    }

    async function loadDepartmentDataForEdit(deptId) {
        try {
            $('#modalLoadingOverlay').show();
            $('#modalFormContent').hide();
            const response = await $.ajax({
                url: `${APP.endpoints.getDepartment}/${deptId}`,
                type: 'GET',
                dataType: 'json'
            });
            $('#deptId').val(response.id || '');
            $('#departmentName').val(response.departmentName || '');
            $('#majorName').val(response.majorName || '');
            setTimeout(() => {
                $('#modalLoadingOverlay').fadeOut(300, function() {
                    $('#modalFormContent').fadeIn(300);
                });
                $('#departmentName').focus();
            }, 300);
        } catch (error) {
            $('#modalLoadingOverlay').fadeOut(300, function() {
                $('#modalFormContent').fadeIn(300);
            });
            showToast('Lỗi khi tải thông tin nganh', 'error');
            setTimeout(() => { if ($('#deptModal').length) $('#deptModal').modal('hide'); }, 2000);
        }
    }

    function saveDepartment() {
        const form = $('#deptForm')[0];
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const data = {
            id: $('#deptId').val(),
            departmentName: $('#departmentName').val(),
            majorName: $('#majorName').val()
        };
        showLoading('#deptModal .modal-body');
        $.ajax({
            url: APP.endpoints.saveDepartment,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                hideLoading('#deptModal .modal-body');
                $('#deptModal').modal('hide');
                showToast('Lưu nganh/chuyên ngành thành công', 'success');
                if (APP.currentTab === 'admin-nganh') loadDepartmentData();
            },
            error: function(xhr) {
                hideLoading('#deptModal .modal-body');
                showToast(xhr.responseText || 'Lỗi khi lưu nganh', 'error');
            }
        });
    }

    function deleteDepartment(deptId) {
        if (!confirm('Bạn có chắc chắn muốn xóa chuyên ngành này?')) return;
        showLoading();
        $.ajax({
            url: `${APP.endpoints.deleteDepartment}/${deptId}`,
            type: 'DELETE',
            success: function(response) {
                hideLoading();
                showToast('Xóa chuyên ngành thành công', 'success');
                if (APP.currentTab === 'admin-nganh') loadDepartmentData();
            },
            error: function(xhr) {
                hideLoading();
                showToast(xhr.responseText || 'Lỗi khi xóa nganh', 'error');
            }
        });
    }

// ============ SEARCH FUNCTIONS ============
    function searchDepartments() {
        const keyword = $('#searchDeptKeyword').val();
        const departmentName = $('#filterDeptName').val();
        showLoading('#deptTableBody');
        $.ajax({
            url: APP.endpoints.searchDepartments,
            type: 'GET',
            data: { keyword: keyword, departmentName: departmentName },
            success: function(departments) {
                renderDepartmentTable(departments);
                hideLoading('#deptTableBody');
            },
            error: function() {
                hideLoading('#deptTableBody');
                showToast('Lỗi khi tìm kiếm nganh', 'error');
            }
        });
    }

    function resetDeptSearch() {
        $('#searchDeptKeyword, #filterDeptName').val('');
        loadDepartmentData();
    }

    function viewDepartmentStats(departmentName) {
        showLoading();
        $.ajax({
            url: APP.endpoints.getStudentCountByDept,
            type: 'GET',
            data: { department: departmentName },
            success: function(stats) {
                hideLoading();
                const modalHtml = `
                    <div class="modal fade" id="deptStatsModal">
                        <div class="modal-dialog modal-dialog-centered"><div class="modal-content">
                            <div class="modal-header" style="background: var(--vlu-blue); color: white;">
                                <h6 class="modal-title">Thống kê nganh: ${departmentName}</h6>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div class="row mb-3">
                                    <div class="col-6"><div class="card text-center border-primary">
                                        <div class="card-body py-3"><h3 class="text-primary">${stats.totalStudents || 0}</h3>
                                        <p class="small mb-0 text-muted">Tổng sinh viên</p></div></div></div>
                                    <div class="col-6"><div class="card text-center border-success">
                                        <div class="card-body py-3"><h3 class="text-success">${stats.activeStudents || 0}</h3>
                                        <p class="small mb-0 text-muted">Đang học</p></div></div></div>
                                </div>
                                <div class="mb-3"><h6 class="mb-2">Chuyên ngành trong nganh:</h6>
                                    <ul class="list-group">
                                        ${stats.majors && stats.majors.length > 0 ?
                                            stats.majors.map(major => `
                                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                                    ${major.name}<span class="badge bg-primary rounded-pill">${major.count}</span>
                                                </li>`).join('') :
                                            '<li class="list-group-item text-muted text-center">Chưa có dữ liệu</li>'}
                                    </ul>
                                </div>
                                <div class="d-flex justify-content-end">
                                    <button class="btn btn-vlu-blue btn-sm" onclick="loadTab('admin-sv'); $('#filterStudentDept').val('${departmentName}'); searchStudents(); $('#deptStatsModal').modal('hide');">
                                        <i class="fas fa-users me-1"></i>Xem sinh viên
                                    </button>
                                </div>
                            </div>
                        </div></div>
                    </div>`;
                $('#modalContainer').html(modalHtml);
                const modal = new bootstrap.Modal(document.getElementById('deptStatsModal'));
                modal.show();
            },
            error: function() {
                hideLoading();
                showToast('Không thể tải thống kê nganh', 'error');
            }
        });
    }

// ============ WINDOW ASSIGNMENTS ============
// Export for debugging
    window.APP = APP;
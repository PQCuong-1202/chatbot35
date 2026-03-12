// ============ STUDENT MANAGEMENT ============
    function loadStudentData() {
        if ($('#studentTableBody').length === 0) {
            return;
        }

        showLoading('#studentTableBody');

        // Reset bộ lọc
        $('#filterStudentMajor').html('<option value="">-- Chuyên ngành --</option>');
        $('#searchStudentKeyword').val('');
        $('#filterStudentDept').val('');
        $('#filterStudentCourse').val('');

        $.ajax({
            url: APP.endpoints.loadStudents,
            type: 'GET',
            success: function(students) {
                // Lưu danh sách hiện tại
                APP.currentStudents = students;

                renderStudentTable(students);

                // Load danh sách nganh
                loadDepartmentOptions('#filterStudentDept', function() {
                    // Setup event khi nganh thay đổi
                    $('#filterStudentDept').off('change').on('change', function() {
                        const deptName = $(this).val();

                        // Reset major dropdown
                        $('#filterStudentMajor').html('<option value="">-- Đang tải... --</option>');

                        if (deptName && deptName !== '') {
                            loadMajorOptions(deptName, '#filterStudentMajor', function() {
                                // Callback khi load xong majors
                            });
                        } else {
                            $('#filterStudentMajor').html('<option value="">-- Chọn chuyên ngành --</option>');
                        }
                    });
                });

                // Load danh sách khóa
                loadCourseOptions();

                hideLoading('#studentTableBody');

                // Hiển thị thông báo
            },
            error: function(xhr, status, error) {
                $('#studentTableBody').html(`
                    <tr>
                        <td colspan="8" class="text-center py-5 text-danger">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            Lỗi khi tải dữ liệu sinh viên: ${error}
                        </td>
                    </tr>
                `);
                hideLoading('#studentTableBody');
                showToast('Lỗi khi tải danh sách sinh viên', 'error');
            }
        });
    }

    function loadAllMajors(selector) {
        $.ajax({
            url: '/profile/api/admin/get-all-majors',
            type: 'GET',
            success: function(allMajors) {
                let html = '<option value="">-- Tất cả chuyên ngành --</option>';
                if (allMajors && allMajors.length > 0) {
                    allMajors.forEach(major => {
                        html += `<option value="${major}">${major}</option>`;
                    });
                }
                $(selector).html(html);
            },
            error: function() {
                $(selector).html('<option value="">-- Lỗi tải chuyên ngành --</option>');
            }
        });
    }

    function renderStudentTable(students) {
        let html = '';
        if (students.length === 0) {
            html = `<tr><td colspan="9" class="text-center py-5 text-muted">
                <i class="fas fa-database me-2"></i>Chưa có dữ liệu
            </td></tr>`;
        } else {
            students.forEach((student, index) => {
                const birthStr = student.birth ? formatDateToDDMMYYYY(new Date(student.birth)) : 'N/A';
                const statusClass = student.enabled === 0 ? 'status-active' : 'status-inactive';
                const statusText = student.enabled === 0 ? 'Còn học' : 'Đã nghỉ';
                const isNew = isRecentlyUpdated(student.lastUpdated);
                const newBadge = isNew ? `<span class="badge bg-danger badge-sm ms-1"><i class="fas fa-star me-1"></i>MỚI</span>` : '';

                html += `
                    <tr class="${isNew ? 'table-warning' : ''}">
                        <td>${index + 1}</td>
                        <td class="fw-bold">${student.mssv || ''}${newBadge}</td>
                        <td>${student.fullName || ''}</td>
                        <td>${birthStr}</td>
                        <td><div class="small fw-bold">${student.department || ''}</div>
                            <div class="text-muted" style="font-size: 11px;">${student.major || ''}</div></td>
                        <td>${student.course || ''}</td>
                        <td><span class="student-status-badge ${statusClass}">${statusText}</span></td>
                        <td><div class="action-buttons">
                            <button class="btn-view" onclick="viewStudentDetail('${student.mssv}')" title="Xem chi tiết"><i class="fas fa-eye"></i></button>
                            <button class="btn-edit" onclick="editStudent(${student.id})" title="Sửa"><i class="fas fa-edit"></i></button>
                            <button class="btn-delete" onclick="deleteStudent(${student.id})" title="Xóa"><i class="fas fa-trash"></i></button>
                        </div></td>
                    </tr>`;
            });
        }
        $('#studentTableBody').html(html);
        $('#studentCount').text(students.length);
    }

    function isRecentlyUpdated(lastUpdatedString) {
        if (!lastUpdatedString) return false;
        try {
            const lastUpdated = new Date(lastUpdatedString);
            const now = new Date();
            return Math.abs(now - lastUpdated) / 36e5 <= 1;
        } catch (e) { return false; }
    }

    async function openStudentModal(studentId = null) {
        APP.currentStudentId = studentId;
        const modalTitle = studentId ? 'Sửa thông tin sinh viên' : 'Thêm sinh viên';

        const modalHtml = `
            <div class="modal fade" id="studentModal" tabindex="-1" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content">
                        <div class="modal-header py-2" style="background: var(--vlu-red); color: white;">
                            <h6 class="modal-title font-weight-bold">${modalTitle}</h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <div id="modalLoadingOverlay" class="text-center py-5">
                                <div class="spinner-border text-primary mb-3"></div>
                                <div class="text-muted">Đang tải dữ liệu...</div>
                            </div>
                            <div id="modalFormContent" style="display: none;">
                                <form id="studentForm" novalidate>
                                    <input type="hidden" id="studentId" name="id">
                                    <div class="row mb-3">
                                        <div class="col-md-6"><label class="form-label small">MSSV *</label>
                                            <input type="text" id="mssv" name="mssv" class="form-control form-control-sm" required
                                                   oninput="validateMssvRealTime(this.value, $('#studentId').val())">
                                            <div class="form-text">Nhập mã số sinh viên</div>
                                        </div>
                                        <div class="col-md-6"><label class="form-label small">Họ và tên *</label>
                                            <input type="text" id="fullName" name="fullName" class="form-control form-control-sm" required>
                                        </div>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6"><label class="form-label small">Ngày sinh *</label>
                                            <input type="text" id="birth" name="birth" class="form-control form-control-sm"
                                                   placeholder="dd/mm/yyyy" required oninput="formatDateInput(this)">
                                        </div>
                                        <div class="col-md-6"><label class="form-label small">Khóa</label>
                                            <input type="text" id="course" name="course" class="form-control form-control-sm"
                                                   placeholder="VD: K26" oninput="this.value = this.value.toUpperCase()">
                                        </div>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6"><label class="form-label small">Nganh</label>
                                            <select id="department" name="department" class="form-select form-select-sm">
                                                <option value="">-- Chọn nganh --</option>
                                            </select>
                                        </div>
                                        <div class="col-md-6"><label class="form-label small">Chuyên ngành</label>
                                            <select id="major" name="major" class="form-select form-select-sm">
                                                <option value="">-- Chọn chuyên ngành --</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6"><label class="form-label small">Trạng thái</label>
                                            <select id="enabled" name="enabled" class="form-select form-select-sm">
                                                <option value="0">Còn học</option><option value="1">Đã nghỉ</option>
                                            </select>
                                        </div>
                                        <div class="col-md-6"><label class="form-label small">Giới tính</label>
                                            <select id="sex" name="sex" class="form-select form-select-sm">
                                                <option value="">-- Chọn giới tính --</option>
                                                <option value="Nam">Nam</option><option value="Nữ">Nữ</option><option value="Khác">Khác</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6"><label class="form-label small">Email</label>
                                            <input type="email" id="gmail" name="gmail" class="form-control form-control-sm">
                                        </div>
                                        <div class="col-md-6"><label class="form-label small">Số điện thoại</label>
                                            <input type="text" id="phone" name="phone" class="form-control form-control-sm">
                                        </div>
                                    </div>
                                    <div class="row mb-4"><div class="col-12">
                                        <label class="form-label small">Địa chỉ</label>
                                        <textarea id="address" name="address" class="form-control form-control-sm" rows="2"></textarea>
                                    </div></div>
                                    <div class="d-flex justify-content-center gap-2 mt-4">
                                        <button type="button" class="btn btn-vlu-blue btn-sm px-4" onclick="saveStudent()" id="saveStudentBtn">
                                            ${studentId ? 'Cập nhật' : 'Thêm sinh viên'}
                                        </button>
                                        <button type="button" class="btn btn-danger btn-sm px-4" data-bs-dismiss="modal">Đóng</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;

        $('#modalContainer').html(modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('studentModal'));
        modal.show();

        await new Promise(resolve => setTimeout(resolve, 300));
        mssvValidation = { valid: false, checking: false, timer: null };

        try {
            await new Promise((resolve) => {
                loadDepartmentOptions('#department', resolve);
            });

            if (studentId) {
                await loadStudentDataForEdit(studentId);
            } else {
                $('#modalLoadingOverlay').hide();
                $('#modalFormContent').show();
                $('#studentForm')[0].reset();
                $('#studentId').val('');
            }

            $('#department').off('change').on('change', function() {
                const deptName = $(this).val();
                if (deptName) loadMajorsForSelect(deptName, '#major');
                else $('#major').html('<option value="">-- Chọn chuyên ngành --</option>');
            });

        } catch (error) {
            showToast('Lỗi khởi tạo form', 'error');
            modal.hide();
        }
    }

    function editStudent(studentId) {
        openStudentModal(studentId);
    }

    async function loadStudentDataForEdit(studentId) {
        try {
            $('#modalLoadingOverlay').show();
            $('#modalFormContent').hide();

            const deptSelect = $('#department');
            if (deptSelect.find('option').length <= 1) {
                await new Promise((resolve) => { loadDepartmentOptions('#department', resolve); });
            }

            const response = await $.ajax({
                url: `${APP.endpoints.getStudent}/${studentId}`,
                type: 'GET',
                dataType: 'json'
            });

            $('#studentId').val(response.id || '');
            $('#mssv').val(response.mssv || '');
            $('#fullName').val(response.fullName || '');
            $('#course').val(response.course || '');
            $('#enabled').val(response.enabled !== undefined ? response.enabled : 0);
            $('#sex').val(response.sex || '');
            $('#gmail').val(response.gmail || '');
            $('#phone').val(response.phone || '');
            $('#address').val(response.address || '');

            let formattedBirth = '';
            if (response.birth) {
                if (typeof response.birth === 'string') {
                    if (response.birth.includes('-')) {
                        const parts = response.birth.split('-');
                        if (parts.length === 3) formattedBirth = `${parts[2]}/${parts[1]}/${parts[0]}`;
                    } else if (response.birth.includes('/')) {
                        formattedBirth = response.birth;
                    } else {
                        const date = new Date(response.birth);
                        if (!isNaN(date.getTime())) {
                            formattedBirth = date.getDate().toString().padStart(2, '0') + '/' +
                                            (date.getMonth() + 1).toString().padStart(2, '0') + '/' +
                                            date.getFullYear();
                        }
                    }
                }
            }
            $('#birth').val(formattedBirth);

            const departmentName = response.department || '';
            deptSelect.val(departmentName);

            if (departmentName && departmentName.trim() !== '') {
                await new Promise((resolve) => {
                    loadMajorsForSelect(departmentName, '#major', function() {
                        $('#major').val(response.major || '');
                        resolve();
                    });
                });
            } else {
                $('#major').html('<option value="">-- Chọn chuyên ngành --</option>');
            }

            updateMssvValidationUI(true, 'MSSV hợp lệ');

            setTimeout(() => {
                $('#modalLoadingOverlay').fadeOut(300, function() {
                    $('#modalFormContent').fadeIn(300);
                });
                if (formattedBirth) {
                    const validation = validateDateInput(formattedBirth);
                    updateDateValidationUI(validation.isValid, validation.message);
                }
                $('#mssv').focus();
            }, 300);

        } catch (error) {
            $('#modalLoadingOverlay').fadeOut(300, function() {
                $('#modalFormContent').fadeIn(300);
            });
            showToast('Lỗi khi tải thông tin sinh viên', 'error');
        }
    }

    function viewStudentDetail(mssv) {
        showLoading();
        $.ajax({
            url: APP.endpoints.getStudentByMssv,
            type: 'GET',
            data: { mssv: mssv },
            success: function(student) {
                hideLoading();
                const birthStr = student.birth ? formatDateToDDMMYYYY(new Date(student.birth)) : 'N/A';
                const statusClass = student.enabled === 0 ? 'status-active' : 'status-inactive';
                const statusText = student.enabled === 0 ? 'Còn học' : 'Đã nghỉ';

                const modalHtml = `
                    <div class="modal fade" id="studentDetailModal" data-bs-backdrop="static">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content">
                                <div class="modal-header" style="background: var(--vlu-red); color: white;">
                                    <h6 class="modal-title">Thông tin chi tiết sinh viên</h6>
                                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="row mb-2"><div class="col-4 fw-bold">MSSV:</div><div class="col-8">${student.mssv}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Họ tên:</div><div class="col-8">${student.fullName}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Ngày sinh:</div><div class="col-8">${birthStr}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Giới tính:</div><div class="col-8">${student.sex || 'N/A'}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Nganh:</div><div class="col-8">${student.department || 'N/A'}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Chuyên ngành:</div><div class="col-8">${student.major || 'N/A'}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Khóa:</div><div class="col-8">${student.course || 'N/A'}</div></div>
                                    <div class="row mb-3"><div class="col-4 fw-bold">Trạng thái:</div><div class="col-8">
                                        <span class="student-status-badge ${statusClass}">${statusText}</span></div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Email:</div><div class="col-8">${student.gmail || 'N/A'}</div></div>
                                    <div class="row mb-2"><div class="col-4 fw-bold">Điện thoại:</div><div class="col-8">${student.phone || 'N/A'}</div></div>
                                    <div class="row mb-3"><div class="col-4 fw-bold">Địa chỉ:</div><div class="col-8">${student.address || 'N/A'}</div></div>
                                    <div class="d-flex justify-content-end">
                                        <button class="btn btn-vlu-blue btn-sm me-2" onclick="handleEditFromDetail(${student.id})">
                                            <i class="fas fa-edit me-1"></i>Sửa
                                        </button>
                                        <button class="btn btn-danger btn-sm" data-bs-dismiss="modal">Đóng</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;

                const oldModal = document.getElementById('studentDetailModal');
                if (oldModal) {
                    const oldModalInstance = bootstrap.Modal.getInstance(oldModal);
                    if (oldModalInstance) oldModalInstance.hide();
                    $(oldModal).remove();
                }

                $('#modalContainer').html(modalHtml);
                const modalElement = document.getElementById('studentDetailModal');
                const modal = new bootstrap.Modal(modalElement);
                modalElement.addEventListener('hidden.bs.modal', function() { $(this).remove(); });
                modal.show();
            },
            error: function() {
                hideLoading();
                showToast('Không thể tải thông tin sinh viên', 'error');
            }
        });
    }

    function handleEditFromDetail(studentId) {
        const detailModal = document.getElementById('studentDetailModal');
        if (detailModal) {
            const detailModalInstance = bootstrap.Modal.getInstance(detailModal);
            if (detailModalInstance) detailModalInstance.hide();
            $(detailModal).on('hidden.bs.modal', function() {
                $(this).remove();
                setTimeout(() => { editStudent(studentId); }, 300);
            });
        } else {
            setTimeout(() => { editStudent(studentId); }, 300);
        }
    }

    function saveStudent() {
        const studentId = $('#studentId').val();
        if (!studentId && !mssvValidation.valid) {
            showToast('Vui lòng nhập MSSV hợp lệ', 'error');
            $('#mssv').focus();
            return;
        }

        if (mssvValidation.checking) {
            showToast('Đang kiểm tra MSSV, vui lòng đợi...', 'warning');
            return;
        }

        const birthInput = $('#birth').val().trim();
        const dateValidation = validateDateInput(birthInput);
        if (!dateValidation.isValid) {
            showToast(dateValidation.message, 'error');
            updateDateValidationUI(false, dateValidation.message);
            $('#birth').focus();
            return;
        }

        const birthDate = parseDDMMYYYYToDate(birthInput);
        if (!birthDate) {
            showToast('Ngày sinh không hợp lệ', 'error');
            return;
        }

        const formattedBirth = formatDateToYYYYMMDD(birthDate);

        if (!$('#mssv').val().trim() || !$('#fullName').val().trim()) {
            showToast('MSSV và Họ tên không được để trống', 'error');
            return;
        }

        const formData = {
            id: studentId || null,
            mssv: $('#mssv').val().trim(),
            fullName: $('#fullName').val().trim(),
            birth: formattedBirth,
            course: $('#course').val() ? $('#course').val().toUpperCase().trim() : '',
            department: $('#department').val() || '',
            major: $('#major').val() || '',
            enabled: parseInt($('#enabled').val()) || 0,
            sex: $('#sex').val() || '',
            gmail: $('#gmail').val() || '',
            phone: $('#phone').val() || '',
            address: $('#address').val() || ''
        };

        const saveBtn = $('#saveStudentBtn');
        const originalText = saveBtn.html();
        saveBtn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...');
        $('#modalFormContent').hide();
        $('#modalLoadingOverlay').show().find('.text-muted').text('Đang lưu dữ liệu...');

        $.ajax({
            url: APP.endpoints.saveStudent,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(formData),
            success: function(response) {
                $('#studentModal').modal('hide');
                saveBtn.prop('disabled', false).html(originalText);
                showToast('Lưu sinh viên thành công', 'success');
                if (APP.currentTab === 'admin-sv') {
                    loadStudentData();
                    setTimeout(() => { highlightNewStudent(formData.mssv); }, 500);
                }
            },
            error: function(xhr) {
                $('#modalLoadingOverlay').hide();
                $('#modalFormContent').show();
                saveBtn.prop('disabled', false).html(originalText);
                let errorMsg = 'Lỗi khi lưu sinh viên';
                if (xhr.responseText) {
                    try {
                        const errorResponse = JSON.parse(xhr.responseText);
                        errorMsg = errorResponse.message || errorResponse;
                    } catch (e) {
                        if (xhr.responseText.includes('MSSV')) errorMsg = 'Lỗi: MSSV đã tồn tại';
                        else errorMsg = xhr.responseText.substring(0, 100);
                    }
                }
                showToast(errorMsg, 'error');
            }
        });
    }

    function deleteStudent(studentId) {
        if (!confirm('Bạn có chắc chắn muốn xóa sinh viên này?')) return;
        showLoading();
        $.ajax({
            url: `${APP.endpoints.deleteStudent}/${studentId}`,
            type: 'DELETE',
            success: function(response) {
                hideLoading();
                showToast('Xóa sinh viên thành công', 'success');
                if (APP.currentTab === 'admin-sv') loadStudentData();
            },
            error: function(xhr) {
                hideLoading();
                showToast(xhr.responseText || 'Lỗi khi xóa sinh viên', 'error');
            }
        });
    }

// ============ SEARCH FUNCTIONS ============
    function searchStudents() {
        const keyword = $('#searchStudentKeyword').val();
        const department = $('#filterStudentDept').val();
        const major = $('#filterStudentMajor').val();
        const course = $('#filterStudentCourse').val();

        showLoading('#studentTableBody');

        $.ajax({
            url: APP.endpoints.searchStudents,
            type: 'GET',
            data: {
                keyword: keyword || null,
                department: department || null,
                major: major || null,
                course: course || null
            },
            success: function(students) {
                APP.currentStudents = students;
                renderStudentTable(students);
                hideLoading('#studentTableBody');

                if (students.length === 0) {
                    showToast('Không tìm thấy sinh viên nào phù hợp', 'info');
                }
            },
            error: function(xhr, status, error) {
                hideLoading('#studentTableBody');
                showToast('Lỗi khi tìm kiếm sinh viên: ' + (xhr.responseText || error), 'error');
            }
        });
    }

    function loadMajorOptions(departmentName, selector, callback) {
        if (!departmentName || departmentName.trim() === '' || departmentName === '-- Chọn nganh --') {
            let html = '<option value="">-- Chọn chuyên ngành --</option>';
            $(selector).html(html);
            if (callback) callback();
            return;
        }

        $.ajax({
            url: APP.endpoints.getMajors,
            type: 'GET',
            data: { departmentName: departmentName },
            success: function(majors) {
                let html = '<option value="">-- Tất cả chuyên ngành --</option>';
                if (majors && Array.isArray(majors) && majors.length > 0) {
                    majors.forEach(major => {
                        html += `<option value="${major}">${major}</option>`;
                    });
                } else {
                    html += '<option value="" disabled>Không có chuyên ngành</option>';
                }

                $(selector).html(html);
                if (callback) callback();
            },
            error: function(xhr, status, error) {
                $(selector).html('<option value="">-- Lỗi tải chuyên ngành --</option>');
                if (callback) callback();
            }
        });
    }

    function resetStudentSearch() {
        $('#searchStudentKeyword, #filterStudentDept, #filterStudentCourse').val('');
        $('#filterStudentMajor').html('<option value="">-- Tìm theo chuyên ngành --</option>');
        loadStudentData();
    }

    function loadCourseOptions() {
        $.ajax({
            url: APP.endpoints.getCourses,
            type: 'GET',
            success: function(courses) {
                let html = '<option value="">-- Khóa --</option>';
                if (courses && courses.length > 0) {
                    courses.forEach(course => { html += `<option value="${course}">${course}</option>`; });
                }
                $('#filterStudentCourse').html(html);
            },
            error: function() {
                $('#filterStudentCourse').html(`
                    <option value="">-- Khóa --</option>
                    <option value="K26">K26</option><option value="K27">K27</option>
                    <option value="K28">K28</option><option value="K29">K29</option>
                    <option value="K30">K30</option>
                `);
            }
        });
    }

// ============ WINDOW ASSIGNMENTS ============
// Export for debugging
    window.APP = APP;
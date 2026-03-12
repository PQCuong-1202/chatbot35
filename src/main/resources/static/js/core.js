// ============ GLOBAL VARIABLES ============
    const APP = {
        baseUrl: '',
        currentTab: null, // Sẽ được set trong loadUserInfo
        userRole: '',
        userInfo: null,
        currentStudentId: null,
        isInitialLoad: true, // Flag để track lần load đầu tiên
        endpoints: {
            userInfo: '/profile/api/user-info',
            changePassword: '/profile/api/change-password',
            loadStudents: '/profile/api/admin/load-students',
            loadDepartments: '/profile/api/admin/load-departments',
            getMajors: '/profile/api/admin/get-majors',
            saveDepartment: '/profile/api/admin/save-department',
            deleteDepartment: '/profile/api/admin/delete-department',
            getDepartment: '/profile/api/admin/departments',
            saveStudent: '/profile/api/admin/save-student',
            deleteStudent: '/profile/api/admin/delete-student',

            getStudent: '/profile/api/admin/students',
            searchStudents: '/profile/api/admin/search-students',
            searchDepartments: '/profile/api/admin/search-departments',
            getStudentByMssv: '/profile/api/admin/student-by-mssv',
            getStudentCountByDept: '/profile/api/admin/department-stats',
            checkMssv: '/profile/api/admin/check-mssv',
            getCourses: '/profile/api/admin/get-courses',
            saveCTDT: '/profile/api/admin/ctdt/save',
            saveBatchCTDT: '/profile/api/admin/ctdt/save-batch',
            getAllCTDT: '/profile/api/admin/ctdt/all',
            searchCTDT: '/profile/api/admin/ctdt/search',
            deleteCTDT: '/profile/api/admin/ctdt/delete',
            getCTDT: '/profile/api/admin/ctdt',
            getCTDTFilters: '/profile/api/admin/ctdt/filters',
            getHocPhanList: '/profile/api/admin/ctdt/hoc-phan',

            importCTDTExcel: '/profile/api/admin/ctdt/import-excel',
            downloadCTDTTemplate: '/profile/api/admin/ctdt/download-template'
        }
    };

    let mssvValidation = { valid: false, checking: false, timer: null };

// ============ INITIALIZATION ============
    $(document).ready(function() {
        loadUserInfo(function() {
            setupMenu();
            loadTab(APP.currentTab);
            setupHistory();
        });

        $(document).on('keypress', '#searchStudentKeyword, #searchDeptKeyword', function(e) {
            if (e.which === 13) {
                if ($(this).attr('id') === 'searchStudentKeyword') searchStudents();
                else searchDepartments();
            }
        });

        $(document).on('blur', '#birth', function() {
            const birthInput = $(this).val().trim();
            if (birthInput) {
                const validation = validateDateInput(birthInput);
                updateDateValidationUI(validation.isValid, validation.message);
            }
        });
    });

// ============ USER MANAGEMENT ============
    function loadUserInfo(callback) {
        showLoading();
        $.ajax({
            url: APP.endpoints.userInfo,
            type: 'GET',
            success: function(response) {
                APP.userInfo = response;

                APP.userRole = response.role || 'USER';
                // Hiển thị nút Admin trên header nếu là ADMIN
                if (APP.userRole === 'ADMIN') {
                    const adminBtn = document.getElementById('adminBtn');
                    if (adminBtn) {
                       adminBtn.classList.remove('d-none');
                    }
                }
                $('#userInfo').text(response.mssv + ' | ' + response.fullName);

                // Lấy tab từ URL parameter hoặc set mặc định theo role
                const urlParams = new URLSearchParams(window.location.search);
                const tabFromUrl = urlParams.get('tab');

                if (tabFromUrl) {
                    // Nếu có tab trong URL, dùng tab đó
                    APP.currentTab = tabFromUrl;
                } else {
                    // Nếu không có tab trong URL, set mặc định theo role
                    if (APP.userRole === 'ADMIN') {
                        APP.currentTab = 'admin-sv'; // Mặc định admin vào trang quản lý sinh viên
                    } else {
                        APP.currentTab = 'profile'; // Mặc định user vào trang profile
                    }
                }

                hideLoading();
                if (callback) callback();
            },
            error: function(xhr) {
                showToast('Lỗi khi tải thông tin người dùng', 'error');
                hideLoading();
            }
        });
    }

    function setupMenu() {
        let menuHtml = '';
        if (APP.userRole === 'ADMIN') {
            menuHtml = `
                <a class="sub-link" onclick="loadTab('admin-sv')"><i class="fas fa-users me-2"></i>Quản lý sinh viên</a>
                <a class="sub-link" onclick="loadTab('admin-nganh')"><i class="fas fa-building me-2"></i>Quản lý Ngành</a>
                <a class="sub-link" onclick="loadTab('admin-ctdt')"><i class="fas fa-book me-2"></i>Quản lý KHĐT</a>
            `;
        } else {
            menuHtml = `
                <a class="sub-link" onclick="loadTab('profile')"><i class="fas fa-user-circle me-2"></i>Thông tin cá nhân</a>
                <a class="sub-link" onclick="loadTab('notifications')"><i class="fas fa-bell me-2"></i>Thông báo học tập<span class="badge bg-danger badge-sm ms-1" id="notificationSidebarBadge" style="display: none;">0</span></a>
                <a class="sub-link" onclick="loadTab('chatbot')"><i class="fas fa-robot me-2"></i>Chatbot hỗ trợ</a>
                <a class="sub-link" onclick="loadTab('ctdt')"><i class="fas fa-graduation-cap me-2"></i>Kế hoạch đào tạo</a>
            `;
        }
        $('#dynamicMenu').html(menuHtml);
    }

// ============ TAB MANAGEMENT ============
    function loadTab(tabName, pushState = true) {
        APP.currentTab = tabName;

        if (pushState) {
            const url = new URL(window.location);
            url.searchParams.set('tab', tabName);

            // Lần đầu tiên load hoặc khi reload, dùng replaceState
            // Lần sau dùng pushState để có thể back/forward
            if (APP.isInitialLoad) {
                history.replaceState({ tab: tabName }, '', url);
                APP.isInitialLoad = false;
            } else {
                history.pushState({ tab: tabName }, '', url);
            }
        }

        $('.sub-link').removeClass('active');
        $(`.sub-link[onclick*="${tabName}"]`).addClass('active');
        updatePageTitle(tabName);
        loadTabContent(tabName);
    }

    function loadTabContent(tabName) {
        console.log('Loading tab content:', tabName);
        showLoading('#mainContentArea');
        let fragmentName = '';
        let loadFunction = null;

        switch(tabName) {
                case 'admin-sv':
                    fragmentName = 'sv_manage';
                    loadFunction = function() {
                        console.log('Loading student data');
                        if (typeof loadStudentData === 'function') loadStudentData();
                    };
                    break;
                case 'admin-nganh':
                    fragmentName = 'department_manage';
                    loadFunction = function() {
                        console.log('Loading department data');
                        if (typeof loadDepartmentData === 'function') loadDepartmentData();
                    };
                    break;
                case 'admin-ctdt':
                    fragmentName = 'ctdt_manage';
                    loadFunction = function() {
                        console.log('Loading CTDT data');
                        // Trigger sự kiện để ctdt-management biết tab đã load
                        setTimeout(() => {
                            $(document).trigger('ctdtTabLoaded');
                        }, 200);
                    };
                    break;
                case 'profile':
                    fragmentName = 'profile_info';
                    loadFunction = function() {
                        console.log('Loading profile data');
                        if (typeof loadUserProfileData === 'function') loadUserProfileData();
                    };
                    break;
                case 'notifications':
                    fragmentName = 'notifications';
                    loadFunction = function() {
                        console.log('Loading notifications');
                        if (typeof loadNotifications === 'function') loadNotifications();
                    };
                    break;
                case 'chatbot':
                    fragmentName = 'chatbot';
                    loadFunction = function() {
                        console.log('Initializing chatbot');
                        if (typeof initChatbot === 'function') initChatbot();
                    };
                    break;
                case 'ctdt':
                    fragmentName = 'ctdt';
                    loadFunction = function() {
                        console.log('Initializing user CTDT');
                        if (typeof initUserCTDT === 'function') initUserCTDT();
                    };
                    break;
                default:
                    fragmentName = 'profile_info';
                    loadFunction = function() {
                        if (typeof loadUserProfileData === 'function') loadUserProfileData();
                    };
            }


        $.ajax({
                url: `/profile/fragments/${fragmentName}`,
                type: 'GET',
                success: function(html) {
                    $('#mainContentArea').html(html);
                    if (loadFunction) {
                        setTimeout(loadFunction, 100);
                    }
                    hideLoading('#mainContentArea');
                },
                error: function(xhr, status, error) {
                    console.error('Error loading fragment:', error);
                    // Fallback
                    $.ajax({
                        url: `/profile/load-fragment?fragment=${fragmentName}`,
                        success: function(html) {
                            $('#mainContentArea').html(html);
                            if (loadFunction) {
                                setTimeout(loadFunction, 100);
                            }
                            hideLoading('#mainContentArea');
                        },
                        error: function() {
                            $('#mainContentArea').html(`
                                <div class="alert alert-danger">
                                    <i class="fas fa-exclamation-triangle me-2"></i>
                                    Lỗi khi tải nội dung.
                                </div>
                            `);
                            hideLoading('#mainContentArea');
                        }
                    });
            }
        });
    }

    function updatePageTitle(tabName) {
        const titles = {
            'admin-sv': 'Quản lý sinh viên',
            'admin-nganh': 'Quản lý ngành',
            'admin-ctdt': 'Quản lý chương trình đào tạo',
            'profile': 'Thông tin cá nhân',
            'chatbot': 'Chatbot hỗ trợ',
            'ctdt': 'Chương trình đào tạo',
        };
        $('#pageTitle').text(titles[tabName] || 'VLU Student Portal');
    }

    function setupHistory() {
        window.addEventListener('popstate', function(event) {
            if (event.state && event.state.tab) {
                loadTab(event.state.tab, false);
            }
        });
    }
// ============ UTILITY FUNCTIONS ============
    function showLoading(selector = 'body') {
        if (selector === 'body') {
            $('#globalLoading').show();
        } else {
            $(selector).html(`
                <div class="text-center py-5" id="loading-content">
                    <div class="spinner-border spinner-border-lg text-primary mb-3"></div>
                    <div class="loading-text">Đang tải dữ liệu...</div>
                    <div class="small text-muted mt-2">Vui lòng đợi trong giây lát</div>
                </div>
            `);
        }
    }

    function hideLoading(selector = 'body') {
        if (selector === 'body') {
            $('#globalLoading').fadeOut(300);
        } else {
            const loadingEl = $(selector).find('#loading-content');
            if (loadingEl.length) loadingEl.fadeOut(300, function() { $(this).remove(); });
        }
    }

    function showToast(message, type = 'info') {
        const toastId = 'toast-' + Date.now();
        const toastHtml = `
            <div class="toast ${type}" id="${toastId}" data-bs-delay="3000">
                <div class="toast-header">
                    <strong class="me-auto">
                        ${type === 'success' ? 'Thành công' : type === 'error' ? 'Lỗi' : 'Thông báo'}
                    </strong>
                    <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body">${message}</div>
            </div>`;
        $('#toastContainer').append(toastHtml);
        const toast = new bootstrap.Toast(document.getElementById(toastId));
        toast.show();
        $(`#${toastId}`).on('hidden.bs.toast', function() { $(this).remove(); });
    }

// ============ DATE FUNCTIONS ============
    function formatDateInput(input) {
        let value = input.value.replace(/\D/g, '');
        if (value.length > 2) value = value.substring(0, 2) + '/' + value.substring(2);
        if (value.length > 5) value = value.substring(0, 5) + '/' + value.substring(5, 9);
        input.value = value;
    }

    function validateDateInput(dateString) {
        if (!dateString) return { isValid: false, message: 'Ngày sinh không được để trống' };
        const dateRegex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
        if (!dateRegex.test(dateString)) return { isValid: false, message: 'Định dạng phải là dd/mm/yyyy' };
        const parts = dateString.split('/');
        const day = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10);
        const year = parseInt(parts[2], 10);
        if (month < 1 || month > 12) return { isValid: false, message: 'Tháng phải từ 01 đến 12' };
        const daysInMonth = new Date(year, month, 0).getDate();
        if (day < 1 || day > daysInMonth) return { isValid: false, message: `Tháng ${month} chỉ có ${daysInMonth} ngày` };
        const currentYear = new Date().getFullYear();
        if (year < 1900 || year > currentYear) return { isValid: false, message: `Năm phải từ 1900 đến ${currentYear}` };
        const inputDate = new Date(year, month - 1, day);
        const today = new Date(); today.setHours(0, 0, 0, 0);
        if (inputDate > today) return { isValid: false, message: 'Ngày sinh không thể trong tương lai' };
        return { isValid: true, message: 'Ngày sinh hợp lệ' };
    }

    function updateDateValidationUI(isValid, message) {
        const birthInput = $('#birth');
        const feedback = $('#birth-feedback');
        if (isValid) {
            birthInput.removeClass('is-invalid').addClass('is-valid');
            if (feedback.length) feedback.remove();
            if (!birthInput.next().hasClass('valid-feedback')) birthInput.after(`<div class="valid-feedback">${message}</div>`);
        } else {
            birthInput.removeClass('is-valid').addClass('is-invalid');
            if (feedback.length) feedback.text(message);
            else birthInput.after(`<div id="birth-feedback" class="invalid-feedback">${message}</div>`);
        }
    }

    function formatDateToDDMMYYYY(date) {
        if (!date) return 'N/A';
        try {
            const d = new Date(date);
            if (isNaN(d.getTime())) return 'N/A';
            const day = String(d.getDate()).padStart(2, '0');
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const year = d.getFullYear();
            return `${day}/${month}/${year}`;
        } catch (e) { return 'N/A'; }
    }

    function parseDDMMYYYYToDate(dateString) {
        if (!dateString) return null;
        const match = dateString.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
        if (match) {
            const day = parseInt(match[1], 10);
            const month = parseInt(match[2], 10) - 1;
            const year = parseInt(match[3], 10);
            const date = new Date(year, month, day);
            if (date.getDate() === day && date.getMonth() === month && date.getFullYear() === year) return date;
        }
        const date = new Date(dateString);
        return isNaN(date.getTime()) ? null : date;
    }

    function formatDateToYYYYMMDD(date) {
        if (!date || !(date instanceof Date) || isNaN(date.getTime())) return null;
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

// ============ HELPER FUNCTIONS ============
    function loadDepartmentOptions(selector, callback) {
        $.ajax({
            url: APP.endpoints.loadDepartments,
            type: 'GET',
            success: function(data) {
                let html = '<option value="">-- Chọn ngành --</option>';
                if (data.distinctDepts && data.distinctDepts.length > 0) {
                    data.distinctDepts.forEach(dept => { html += `<option value="${dept}">${dept}</option>`; });
                }
                $(selector).html(html);
                if (callback) callback();
            }
        });
    }

    function loadMajorsForSelect(departmentName, selector, callback) {
        if (!departmentName || departmentName.trim() === '') {
            $(selector).html('<option value="">-- Chọn chuyên ngành --</option>');
            if (callback) setTimeout(callback, 100);
            return;
        }
        $.ajax({
            url: APP.endpoints.getMajors,
            type: 'GET',
            data: { departmentName: departmentName },
            success: function(majors) {
                let html = '<option value="">-- Chọn chuyên ngành --</option>';
                if (majors && Array.isArray(majors) && majors.length > 0) {
                    majors.forEach(major => { html += `<option value="${major}">${major}</option>`; });
                } else { html += '<option value="" disabled>Không có chuyên ngành</option>'; }
                $(selector).html(html);
                if (callback) setTimeout(callback, 50);
            },
            error: function() {
                $(selector).html('<option value="">-- Chọn chuyên ngành --</option>');
                if (callback) setTimeout(callback, 50);
            }
        });
    }

    function populateDeptFilter(distinctDepts) {
        let html = '<option value="">-- Tìm theo ngành --</option>';
        if (distinctDepts && distinctDepts.length > 0) {
            distinctDepts.forEach(dept => { html += `<option value="${dept}">${dept}</option>`; });
        }
        $('#filterDeptName').html(html);
    }

// ============ SORTING FUNCTIONS ============
    function sortStudents(sortType) {
        if (!APP.currentStudents || APP.currentStudents.length === 0) {
            showToast('Không có dữ liệu để sắp xếp', 'warning');
            return;
        }

        let sortedStudents = [...APP.currentStudents];

        switch(sortType) {
            case 'recent':
                sortedStudents.sort((a, b) => {
                    const dateA = a.lastUpdated ? new Date(a.lastUpdated) : new Date(0);
                    const dateB = b.lastUpdated ? new Date(b.lastUpdated) : new Date(0);
                    return dateB - dateA;
                });
                break;

            case 'name':
                sortedStudents.sort((a, b) => {
                    const nameA = removeAccents(a.fullName || '').toLowerCase();
                    const nameB = removeAccents(b.fullName || '').toLowerCase();
                    return nameA.localeCompare(nameB, 'vi');
                });
                break;

            case 'mssv':
                sortedStudents.sort((a, b) => {
                    const mssvA = a.mssv || '';
                    const mssvB = b.mssv || '';
                    return mssvA.localeCompare(mssvB);
                });
                break;

            default:
                sortedStudents.sort((a, b) => (a.id || 0) - (b.id || 0));
        }

        APP.currentStudents = sortedStudents;
        renderStudentTable(sortedStudents);

        const sortNames = {
            'recent': 'mới nhất',
            'name': 'tên A-Z',
            'mssv': 'MSSV'
        };
        showToast(`Đã sắp xếp theo ${sortNames[sortType] || sortType}`, 'success');
    }

    function removeAccents(str) {
        if (!str) return '';
        return str.normalize('NFD')
                  .replace(/[\u0300-\u036f]/g, '')
                  .replace(/đ/g, 'd').replace(/Đ/g, 'D');
    }

    function highlightNewStudent(mssv) {
        setTimeout(() => {
            $('#studentTableBody tr').each(function() {
                const rowMssv = $(this).find('td:nth-child(2)').text().trim();
                if (rowMssv === mssv) {
                    $(this).addClass('table-success');
                    $('html, body').animate({ scrollTop: $(this).offset().top - 100 }, 500);
                    setTimeout(() => { $(this).removeClass('table-success'); }, 3000);
                    return false;
                }
            });
        }, 800);
    }

    function validateMssvRealTime(mssv, currentId = null) {
        clearTimeout(mssvValidation.timer);
        if (!mssv || mssv.trim().length < 3) {
            updateMssvValidationUI(false, 'MSSV quá ngắn (tối thiểu 3 ký tự)');
            return;
        }
        mssvValidation.checking = true;
        $('#mssv').addClass('is-validating').removeClass('is-invalid is-valid');
        mssvValidation.timer = setTimeout(() => {
            $.ajax({
                url: APP.endpoints.checkMssv,
                type: 'GET',
                data: { mssv: mssv, currentId: currentId || $('#studentId').val() || null },
                success: function(response) {
                    mssvValidation.checking = false;
                    $('#mssv').removeClass('is-validating');
                    if (response.exists) updateMssvValidationUI(false, response.message);
                    else updateMssvValidationUI(true, response.message);
                },
                error: function() {
                    mssvValidation.checking = false;
                    $('#mssv').removeClass('is-validating');
                    updateMssvValidationUI(false, 'Lỗi kiểm tra MSSV');
                }
            });
        }, 500);
    }

    function updateMssvValidationUI(isValid, message) {
        const mssvInput = $('#mssv');
        const feedback = $('#mssv-feedback');
        mssvValidation.valid = isValid;
        if (isValid) {
            mssvInput.removeClass('is-invalid').addClass('is-valid');
            if (feedback.length) feedback.remove();
        } else {
            mssvInput.removeClass('is-valid').addClass('is-invalid');
            if (!feedback.length) mssvInput.after(`<div id="mssv-feedback" class="invalid-feedback">${message}</div>`);
            else feedback.text(message);
        }
    }

// ============ WINDOW ASSIGNMENTS ============
// Export for debugging
    window.APP = APP;
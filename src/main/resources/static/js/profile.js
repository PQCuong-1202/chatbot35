// ============ PROFILE FUNCTIONS ============
    let originalAvatarData = null;
    let avatarFile = null;
    let profileDepartmentsLoaded = false;

    // Load profile data
    function loadUserProfileData() {
        $('#profileContent').html(`
            <div class="text-center py-4">
                <div class="spinner-border spinner-border-sm text-primary me-2"></div>
                Đang tải thông tin...
            </div>
        `);

        $.ajax({
            url: '/profile/api/user-profile-with-avatar',
            type: 'GET',
            success: function(user) {
                renderProfileView(user);
            },
            error: function() {
                $('#profileContent').html(`
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>Lỗi khi tải thông tin cá nhân
                    </div>
                `);
            }
        });
    }

    function renderProfileView(user) {
        const birthStr = user.birth ? formatDateToDDMMYYYY(new Date(user.birth)) : 'N/A';

        // Hiển thị avatar
        let avatarHtml = '';
        if (user.hasImage && user.imageBase64) {
            avatarHtml = `
                <img src="data:image/jpeg;base64,${user.imageBase64}"
                     alt="Avatar" class="img-fluid rounded-circle" style="width: 120px; height: 120px; object-fit: cover;">
            `;
        } else {
            avatarHtml = `
                <div class="rounded-circle bg-light d-flex align-items-center justify-content-center"
                     style="width: 120px; height: 120px; border: 2px solid #dee2e6;">
                    <i class="fas fa-user fa-3x text-muted"></i>
                </div>
            `;
        }

        const html = `
            <div class="card">
                <div class="card-body">
                    <div class="row align-items-center mb-4">
                        <div class="col-md-3 text-center">
                            ${avatarHtml}
                        </div>
                        <div class="col-md-9">
                            <h4 class="mb-1">${user.fullName || ''}</h4>
                            <h6 class="text-muted mb-3">${user.mssv || ''}</h6>
                            <button class="btn btn-vlu-blue btn-sm" onclick="enableProfileEdit()">
                                <i class="fas fa-edit me-1"></i> Chỉnh sửa thông tin
                            </button>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Ngày sinh</label>
                                <p class="mb-0 fw-bold">${birthStr}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Giới tính</label>
                                <p class="mb-0">${user.sex || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Ngành</label>
                                <p class="mb-0">${user.department || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Chuyên ngành</label>
                                <p class="mb-0">${user.major || 'Chưa cập nhật'}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Khóa</label>
                                <p class="mb-0">${user.course || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Lớp</label>
                                <p class="mb-0">${user.studentClass || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Email cá nhân</label>
                                <p class="mb-0">${user.gmail || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Số điện thoại</label>
                                <p class="mb-0">${user.phone || 'Chưa cập nhật'}</p>
                            </div>
                            <div class="profile-info-row">
                                <label class="form-label small text-muted">Địa chỉ</label>
                                <p class="mb-0">${user.address || 'Chưa cập nhật'}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        $('#profileContent').html(html);
    }

    function enableProfileEdit() {
        $.ajax({
            url: '/profile/api/user-profile-with-avatar',
            type: 'GET',
            success: function(user) {
                // Set form values
                $('#userId').val(user.id);
                $('#editMssv').val(user.mssv || '');
                $('#editFullName').val(user.fullName || '');

                // Format date
                if (user.birth) {
                    const date = new Date(user.birth);
                    const formattedDate = date.getDate().toString().padStart(2, '0') + '/' +
                                        (date.getMonth() + 1).toString().padStart(2, '0') + '/' +
                                        date.getFullYear();
                    $('#editBirth').val(formattedDate);
                }

                $('#editSex').val(user.sex || '');
                $('#editCourse').val(user.course || '');
                $('#editStudentClass').val(user.studentClass || '');
                $('#editGmail').val(user.gmail || '');
                $('#editGmailVlu').val(user.gmailVlu || '');
                $('#editPhone').val(user.phone || '');
                $('#editNation').val(user.nation || 'Việt Nam');
                $('#editAddress').val(user.address || '');

                // Load departments if not loaded
                if (!profileDepartmentsLoaded) {
                    loadDepartmentsForProfile(user.department, user.major);
                } else {
                    // Set department and major
                    $('#editDepartment').val(user.department || '');
                    loadMajorsForProfile(user.department, user.major);
                }

                // Set avatar
                updateAvatarPreview(user.imageBase64, user.hasImage);

                // Switch to edit mode
                $('#profileContent').hide();
                $('#editProfileContent').show();
            },
            error: function() {
                showToast('Lỗi khi tải thông tin để chỉnh sửa', 'error');
            }
        });
    }

    function updateAvatarPreview(imageBase64, hasImage) {
        const defaultIcon = $('#defaultAvatarIcon');
        const avatarImage = $('#avatarImage');
        const removeBtn = $('#removeAvatarBtn');

        if (hasImage && imageBase64) {
            avatarImage.attr('src', 'data:image/jpeg;base64,' + imageBase64);
            avatarImage.show();
            defaultIcon.hide();
            removeBtn.show();
        } else {
            avatarImage.hide();
            defaultIcon.show();
            removeBtn.hide();
        }
    }

    function previewAvatar(input) {
        if (input.files && input.files[0]) {
            const file = input.files[0];

            // Validate file size (max 2MB)
            if (file.size > 2 * 1024 * 1024) {
                showToast('Kích thước ảnh không được vượt quá 2MB', 'error');
                return;
            }

            // Validate file type
            if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
                showToast('Chỉ chấp nhận ảnh định dạng JPG hoặc PNG', 'error');
                return;
            }

            avatarFile = file;

            const reader = new FileReader();
            reader.onload = function(e) {
                $('#avatarImage').attr('src', e.target.result);
                $('#avatarImage').show();
                $('#defaultAvatarIcon').hide();
                $('#removeAvatarBtn').show();
            };
            reader.readAsDataURL(file);
        }
    }

    function removeAvatar() {
        avatarFile = null;
        $('#avatarImage').hide();
        $('#defaultAvatarIcon').show();
        $('#removeAvatarBtn').hide();
        $('#avatarUpload').val('');
    }

    function loadDepartmentsForProfile(departmentName = null, majorName = null) {
        $.ajax({
            url: APP.endpoints.loadDepartments,
            type: 'GET',
            success: function(data) {
                let html = '<option value="">-- Chọn ngành --</option>';
                if (data.distinctDepts && data.distinctDepts.length > 0) {
                    data.distinctDepts.forEach(dept => {
                        html += `<option value="${dept}">${dept}</option>`;
                    });
                }
                $('#editDepartment').html(html);

                if (departmentName) {
                    $('#editDepartment').val(departmentName);
                    loadMajorsForProfile(departmentName, majorName);
                }

                // Set up department change event
                $('#editDepartment').off('change').on('change', function() {
                    const dept = $(this).val();
                    loadMajorsForProfile(dept);
                });

                profileDepartmentsLoaded = true;
            },
            error: function() {
                $('#editDepartment').html('<option value="">-- Lỗi tải danh sách ngành --</option>');
            }
        });
    }

    function loadMajorsForProfile(departmentName, currentMajor = null) {
        if (!departmentName) {
            $('#editMajor').html('<option value="">-- Chọn chuyên ngành --</option>');
            return;
        }

        $.ajax({
            url: APP.endpoints.getMajors,
            type: 'GET',
            data: { departmentName: departmentName },
            success: function(majors) {
                let html = '<option value="">-- Chọn chuyên ngành --</option>';
                if (majors && Array.isArray(majors) && majors.length > 0) {
                    majors.forEach(major => {
                        html += `<option value="${major}" ${currentMajor === major ? 'selected' : ''}>${major}</option>`;
                    });
                } else {
                    html += '<option value="" disabled>Không có chuyên ngành</option>';
                }
                $('#editMajor').html(html);
            },
            error: function() {
                $('#editMajor').html('<option value="">-- Chọn chuyên ngành --</option>');
            }
        });
    }

    function saveProfile() {
        // Validate form
        const form = $('#profileForm')[0];
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        // Validate date
        const birthInput = $('#editBirth').val().trim();
        const dateValidation = validateDateInput(birthInput);
        if (!dateValidation.isValid) {
            showToast(dateValidation.message, 'error');
            $('#editBirth').focus();
            return;
        }

        const birthDate = parseDDMMYYYYToDate(birthInput);
        if (!birthDate) {
            showToast('Ngày sinh không hợp lệ', 'error');
            return;
        }

        const formattedBirth = formatDateToYYYYMMDD(birthDate);

        // Prepare form data
        const formData = new FormData();
        formData.append('id', $('#userId').val());
        formData.append('fullName', $('#editFullName').val().trim());
        formData.append('birth', formattedBirth);
        formData.append('sex', $('#editSex').val() || '');
        formData.append('department', $('#editDepartment').val() || '');
        formData.append('major', $('#editMajor').val() || '');
        formData.append('course', $('#editCourse').val() ? $('#editCourse').val().toUpperCase().trim() : '');
        formData.append('studentClass', $('#editStudentClass').val() || '');
        formData.append('gmail', $('#editGmail').val() || '');
        formData.append('gmailVlu', $('#editGmailVlu').val() || '');
        formData.append('phone', $('#editPhone').val() || '');
        formData.append('nation', $('#editNation').val() || 'Việt Nam');
        formData.append('address', $('#editAddress').val() || '');

        // Add image if changed
        if (avatarFile) {
            formData.append('image', avatarFile);
        } else if ($('#removeAvatarBtn').is(':visible') === false && $('#defaultAvatarIcon').is(':visible')) {
            // User removed avatar (default icon is showing)
            formData.append('removeImage', 'true');
        }

        showLoading();

        // Send request
        $.ajax({
            url: '/profile/api/update-profile',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                hideLoading();
                showToast('Cập nhật thông tin thành công', 'success');

                // Update session user info
                loadUserInfo(function() {
                    // Switch back to view mode
                    cancelEditProfile();
                    // Reload profile data
                    loadUserProfileData();
                });
            },
            error: function(xhr) {
                hideLoading();
                let errorMsg = 'Lỗi khi cập nhật thông tin';
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

    function cancelEditProfile() {
        $('#profileContent').show();
        $('#editProfileContent').hide();
        avatarFile = null;
        $('#avatarUpload').val('');
    }

    // Event listener for date input formatting
    $(document).ready(function() {
        // This will be called when the profile tab loads
        $(document).on('input', '#editBirth', function() {
            formatDateInput(this);
        });
    });

// ============ WINDOW ASSIGNMENTS ============
// Export for debugging
    window.APP = APP;
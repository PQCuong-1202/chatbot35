// ============ PASSWORD MODAL ============
    function openPwdModal() {
        const modalHtml = `
            <div class="modal fade" id="pwdModal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header py-2" style="background: var(--vlu-red); color: white;">
                            <h6 class="modal-title">Đổi mật khẩu</h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-4">
                            <div class="row mb-3 align-items-center">
                                <label class="col-4 small">Mật khẩu cũ:</label><div class="col-8">
                                    <input type="password" id="currentPassword" class="form-control form-control-sm"></div>
                            </div>
                            <div class="row mb-3 align-items-center">
                                <label class="col-4 small">Mật khẩu mới:</label><div class="col-8">
                                    <input type="password" id="newPassword" class="form-control form-control-sm"></div>
                            </div>
                            <div class="row mb-4 align-items-center">
                                <label class="col-4 small">Nhập lại:</label><div class="col-8">
                                    <input type="password" id="confirmPassword" class="form-control form-control-sm"></div>
                            </div>
                            <div class="d-flex justify-content-end gap-2">
                                <button class="btn btn-vlu-blue btn-sm px-3" onclick="changePassword()">Cập nhật mật khẩu</button>
                                <button class="btn btn-danger btn-sm px-3" data-bs-dismiss="modal">Đóng</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
        $('#modalContainer').html(modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('pwdModal'));
        modal.show();
    }

    function changePassword() {
        const current = $('#currentPassword').val();
        const newPass = $('#newPassword').val();
        const confirm = $('#confirmPassword').val();
        if (!current || !newPass || !confirm) {
            showToast('Vui lòng nhập đầy đủ thông tin', 'error');
            return;
        }
        if (newPass !== confirm) {
            showToast('Mật khẩu mới không khớp', 'error');
            return;
        }
        showLoading();
        $.ajax({
            url: APP.endpoints.changePassword,
            type: 'POST',
            data: { oldPassword: current, newPassword: newPass },
            success: function(response) {
                hideLoading();
                $('#pwdModal').modal('hide');
                showToast(response, 'success');
                $('#currentPassword, #newPassword, #confirmPassword').val('');
            },
            error: function(xhr) {
                hideLoading();
                showToast(xhr.responseText || 'Lỗi khi đổi mật khẩu', 'error');
            }
        });
    }

// ============ WINDOW ASSIGNMENTS ============
// Export for debugging
    window.APP = APP;
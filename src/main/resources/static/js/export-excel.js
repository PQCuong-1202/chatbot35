// ============ EXPORT EXCEL FUNCTIONS ============
    // Hàm removeAccents đã có sẵn trong file, nếu chưa có thì thêm
    function removeAccents(str) {
        if (!str) return '';
        return str.normalize('NFD')
                  .replace(/[\u0300-\u036f]/g, '')
                  .replace(/đ/g, 'd').replace(/Đ/g, 'D');
    }

    // Thêm trước khi xuất
    function showExportNotification() {
        const notificationHtml = `
            <div class="alert alert-info alert-dismissible fade show" role="alert">
                <i class="fas fa-file-excel me-2"></i>
                <strong>Đang xuất file Excel...</strong> Vui lòng đợi trong giây lát.
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        // Thêm vào đầu content body
        $('#mainContentArea').prepend(notificationHtml);

        // Tự động đóng sau 5 giây
        setTimeout(() => {
            $('.alert').alert('close');
        }, 5000);
    }

    // Xuất file excel
    function exportCTDTToExcelFull() {
        showLoading();
        showToast('Đang tạo file Excel với dữ liệu thời gian thực...', 'info');

        // Tạo URL với timestamp để tránh cache
        const exportUrl = '/profile/api/user/export-ctdt-excel-full';
        const timestamp = new Date().getTime();
        const url = `${exportUrl}?_=${timestamp}`;

        // Sử dụng fetch API
        fetch(url, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // Lấy filename từ headers
            const contentDisposition = response.headers.get('content-disposition');
            let filename = `CTDT_Full_${new Date().getTime()}.xlsx`;

            if (contentDisposition) {
                const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (match && match[1]) {
                    filename = decodeURIComponent(match[1].replace(/['"]/g, ''));
                }
            }

            return response.blob().then(blob => ({ blob, filename }));
        })
        .then(({ blob, filename }) => {
            // Kiểm tra kích thước file
            if (blob.size === 0) {
                throw new Error('File tạo ra trống');
            }

            // Tạo URL tải về
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.download = filename;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();

            // Dọn dẹp
            setTimeout(() => {
                document.body.removeChild(link);
                window.URL.revokeObjectURL(downloadUrl);
            }, 100);

            hideLoading();
            showToast(`Xuất file thành công: ${filename}`, 'success');

            // Hiển thị thông tin file
            const fileSize = (blob.size / 1024 / 1024).toFixed(2);
            console.log(`File exported: ${filename}, Size: ${fileSize} MB`);

        })
        .catch(error => {
            hideLoading();
            showToast('Lỗi khi xuất file: ' + error.message, 'error');
            console.error('Export error:', error);
        });
    }

// ============ WINDOW ASSIGNMENTS ============
    window.APP = APP;
    window.exportCTDTToExcelFull = exportCTDTToExcelFull;
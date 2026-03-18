// ============ NOTIFICATION FUNCTIONS ============
// Biến toàn cục cho notifications
let NOTIFICATIONS = {
    allNotifications: [],
    filteredNotifications: []
};

// Endpoints cho notifications
APP.endpoints.notifications = {
    getNotifications: '/profile/api/user/notifications',
    getUnreadCount: '/profile/api/user/notifications/unread-count',
    markAsRead: '/profile/api/user/notifications',
    markAllRead: '/profile/api/user/notifications/mark-all-read',
    checkNotifications: '/profile/api/user/check-notifications'
};

// Load thông báo khi tab được mở
function loadNotifications() {
    if (!$('#notificationsList').length) return;

    $('#notificationsList').html(`
        <div class="text-center py-4">
            <div class="spinner-border spinner-border-sm text-primary me-2"></div>
            Đang tải thông báo...
        </div>
    `);

    $.ajax({
        url: APP.endpoints.notifications.getNotifications,
        type: 'GET',
        success: function(notifications) {
            NOTIFICATIONS.allNotifications = notifications;
            filterNotifications();
            updateUnreadCount();
        },
        error: function() {
            $('#notificationsList').html(`
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Lỗi khi tải thông báo
                </div>
            `);
        }
    });
}

// Lọc thông báo
function filterNotifications() {
    const typeFilter = $('#notificationFilterType').val();
    const readFilter = $('#notificationFilterRead').val();

    let filtered = NOTIFICATIONS.allNotifications;

    if (typeFilter) {
        filtered = filtered.filter(n => n.type === typeFilter);
    }

    if (readFilter !== '') {
        const isRead = readFilter === 'true';
        filtered = filtered.filter(n => n.isRead === isRead);
    }

    NOTIFICATIONS.filteredNotifications = filtered;
    renderNotifications(filtered);
}

// Render danh sách thông báo
function renderNotifications(notifications) {
    if (notifications.length === 0) {
        $('#notificationsList').hide();
        $('#emptyNotifications').show();
        return;
    }

    $('#emptyNotifications').hide();
    $('#notificationsList').show();

    let html = '';
    notifications.forEach(notification => {
        const typeClass = getNotificationTypeClass(notification.type);
        const typeIcon = getNotificationTypeIcon(notification.type);
        const timeAgo = formatTimeAgo(notification.createdAt);
        const readClass = notification.isRead ? '' : 'border-start-3 border-start-primary';

        html += `
            <div class="card mb-3 shadow-sm notification-item ${readClass} notification-type-${notification.type.toLowerCase()}"
                 data-id="${notification.id}"
                 onclick="viewNotificationDetail(${notification.id})">
                <div class="card-body p-3">
                    <div class="d-flex">
                        <div class="notification-icon me-3">
                            <i class="${typeIcon} fa-2x"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <div>
                                    <span class="badge ${typeClass} me-2">${getNotificationTypeText(notification.type)}</span>
                                    ${notification.isRead ? '' : '<span class="badge bg-danger me-2">MỚI</span>'}
                                </div>
                                <small class="text-muted">${timeAgo}</small>
                            </div>
                            <h6 class="mb-2 fw-bold">${notification.title}</h6>
                            <p class="text-muted small mb-2" style="max-height: 60px; overflow: hidden;">
                                ${notification.message.replace(/\n/g, '<br>')}
                            </p>
                            <div class="d-flex justify-content-end">
                                <button class="btn btn-link btn-sm text-primary p-0 me-3"
                                        onclick="event.stopPropagation(); viewNotificationDetail(${notification.id})">
                                    <i class="fas fa-eye me-1"></i> Chi tiết
                                </button>
                                ${!notification.isRead ?
                                    `<button class="btn btn-link btn-sm text-success p-0"
                                            onclick="event.stopPropagation(); markNotificationAsRead(${notification.id})">
                                        <i class="fas fa-check me-1"></i> Đánh dấu đã đọc
                                    </button>` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });

    $('#notificationsList').html(html);
}

// Xem chi tiết thông báo
function viewNotificationDetail(notificationId) {
    const notification = NOTIFICATIONS.allNotifications.find(n => n.id === notificationId);
    if (!notification) return;

    // Đánh dấu đã đọc
    if (!notification.isRead) {
        markNotificationAsRead(notificationId, false);
    }

    // Format message với line breaks
    const formattedMessage = notification.message.replace(/\n/g, '<br>');

    // Cập nhật modal
    $('#notificationDetailTitle').text(notification.title);
    $('#notificationDetailTime').text(formatDateTime(notification.createdAt));
    $('#notificationDetailMessage').html(formattedMessage);

    // Cập nhật header color
    const typeClass = getNotificationTypeClass(notification.type);
    const typeIcon = getNotificationTypeIcon(notification.type);
    $('#notificationModalHeader').removeClass().addClass(`modal-header py-2 ${typeClass} text-white`);
    $('#notificationModalHeader .modal-title').html(`<i class="${typeIcon} me-2"></i>${getNotificationTypeText(notification.type)}`);

    // Cập nhật action button
    const actionBtn = $('#notificationActionBtn');
    if (notification.actionUrl) {
        actionBtn.show().attr('href', notification.actionUrl);
    } else {
        actionBtn.hide();
    }

    // Hiển thị modal
    const modal = new bootstrap.Modal(document.getElementById('notificationDetailModal'));
    modal.show();
}

// Đánh dấu thông báo đã đọc
function markNotificationAsRead(notificationId, reload = true) {
    $.ajax({
        url: `${APP.endpoints.notifications.markAsRead}/${notificationId}/read`,
        type: 'POST',
        success: function() {
            // Cập nhật UI
            const notification = NOTIFICATIONS.allNotifications.find(n => n.id === notificationId);
            if (notification) {
                notification.isRead = true;
            }

            if (reload) {
                filterNotifications();
            }
            updateUnreadCount();
            showToast('Đã đánh dấu đã đọc', 'success');
        },
        error: function() {
            showToast('Lỗi khi đánh dấu đã đọc', 'error');
        }
    });
}

// Đánh dấu tất cả đã đọc
function markAllNotificationsAsRead() {
    if (!confirm('Đánh dấu tất cả thông báo là đã đọc?')) {
        return;
    }

    $.ajax({
        url: APP.endpoints.notifications.markAllRead,
        type: 'POST',
        success: function() {
            // Cập nhật tất cả thông báo
            NOTIFICATIONS.allNotifications.forEach(n => n.isRead = true);
            filterNotifications();
            updateUnreadCount();
            showToast('Đã đánh dấu tất cả đã đọc', 'success');
        },
        error: function() {
            showToast('Lỗi khi đánh dấu đã đọc', 'error');
        }
    });
}

// Cập nhật số lượng chưa đọc
function updateUnreadCount() {
    $.ajax({
        url: APP.endpoints.notifications.getUnreadCount,
        type: 'GET',
        success: function(response) {
            const count = response.count || 0;
            $('#unreadCountBadge').text(count);

            // Cập nhật badge trên menu
            const menuBadge = $('#notificationMenuBadge');
            const sidebarBadge = $('#notificationSidebarBadge');

            if (count > 0) {
                menuBadge.text(count).show();
                sidebarBadge.text(count).show();
            } else {
                menuBadge.hide();
                sidebarBadge.hide();
            }
        }
    });
}

// Kích hoạt kiểm tra thông báo
function triggerNotificationCheck() {
    showLoading('#notificationsList');

    $.ajax({
        url: APP.endpoints.notifications.checkNotifications,
        type: 'POST',
        success: function(response) {
            hideLoading('#notificationsList');
            showToast('Đã kiểm tra thông báo mới', 'success');
            loadNotifications();
        },
        error: function() {
            hideLoading('#notificationsList');
            showToast('Lỗi khi kiểm tra thông báo', 'error');
        }
    });
}

// Helper functions cho notifications
function getNotificationTypeClass(type) {
    switch(type) {
        case 'SUCCESS': return 'bg-success';
        case 'INFO': return 'bg-info';
        case 'WARNING': return 'bg-warning';
        case 'DANGER': return 'bg-danger';
        default: return 'bg-secondary';
    }
}

function getNotificationTypeIcon(type) {
    switch(type) {
        case 'SUCCESS': return 'fas fa-check-circle';
        case 'INFO': return 'fas fa-info-circle';
        case 'WARNING': return 'fas fa-exclamation-triangle';
        case 'DANGER': return 'fas fa-times-circle';
        default: return 'fas fa-bell';
    }
}

function getNotificationTypeText(type) {
    switch(type) {
        case 'SUCCESS': return 'Thành công';
        case 'INFO': return 'Thông tin';
        case 'WARNING': return 'Cảnh báo';
        case 'DANGER': return 'Quan trọng';
        default: return 'Thông báo';
    }
}

function formatTimeAgo(timestamp) {
    if (!timestamp) return '';

    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 7) return `${diffDays} ngày trước`;

    return date.toLocaleDateString('vi-VN');
}

function formatDateTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleString('vi-VN', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Tự động kiểm tra thông báo mỗi 5 phút
setInterval(updateUnreadCount, 5 * 60 * 1000);

// Khi tab notifications được load
$(document).on('notificationsTabLoaded', function() {
    loadNotifications();
});

// Thêm vào document ready
$(document).ready(function() {
    // Cập nhật thông báo khi tab được focus
    $(window).on('focus', function() {
        updateUnreadCount();
    });

    // Cập nhật thông báo khi quay lại từ tab khác
    $(document).on('visibilitychange', function() {
        if (!document.hidden) {
            updateUnreadCount();
        }
    });
});

// ============ WINDOW ASSIGNMENTS ============
window.APP = APP;
window.NOTIFICATIONS = NOTIFICATIONS;
window.loadNotifications = loadNotifications;
window.filterNotifications = filterNotifications;
window.viewNotificationDetail = viewNotificationDetail;
window.markNotificationAsRead = markNotificationAsRead;
window.markAllNotificationsAsRead = markAllNotificationsAsRead;
window.triggerNotificationCheck = triggerNotificationCheck;

// ================================================================
// GỢI Ý MÔN HỌC TỰ ĐỘNG — hàm riêng, không đụng code cũ
// Trigger: chỉ theo thời gian, không liên quan trạng thái môn học
// ================================================================
function autoSendCourseRecommendation() {
    // Bước 1: Fetch danh sách thông báo GỢI Ý hiện có trong DB
    $.ajax({
        url: APP.endpoints.notifications.getNotifications,
        type: 'GET',
        success: function(current) {
            var beforeIds = current
                .filter(function(n) { return n.title && n.title.indexOf('GỢI Ý HỌC KỲ') !== -1; })
                .map(function(n) { return n.id; });

            // Bước 2: Gọi backend tạo thông báo mới nếu đến hạn
            $.ajax({
                url: '/profile/api/user/notifications/send-course-recommendation',
                type: 'POST',
                success: function(afterList) {
                    if (!Array.isArray(afterList)) return;

                    // Bước 3: So sánh — tìm ID xuất hiện mới sau khi backend chạy
                    var newRecs = afterList.filter(function(n) {
                        return beforeIds.indexOf(n.id) === -1;
                    });

                    if (newRecs.length === 0) return;

                    // Cập nhật NOTIFICATIONS để badge và tab đồng bộ
                    NOTIFICATIONS.allNotifications = afterList.concat(
                        NOTIFICATIONS.allNotifications.filter(function(n) {
                            return n.title && n.title.indexOf('GỢI Ý HỌC KỲ') === -1;
                        })
                    );

                    // Toast từng thông báo gợi ý mới
                    newRecs.forEach(function(n) {
                        showToast('💡 ' + n.title, 'info');
                    });

                    updateUnreadCount();

                    if ($('#notificationsList').is(':visible')) {
                        filterNotifications();
                    }
                }
            });
        }
    });
}

// Chạy ngay khi script load — bắt gợi ý HK1 lần đầu
setTimeout(autoSendCourseRecommendation, 1500);

// Polling mỗi 60 giây — backend tự quyết định đủ hạn chưa
// TEST_MODE=true : 1 phút → gợi ý HK tiếp
// TEST_MODE=false: 90 ngày → gợi ý HK tiếp
// Frontend gọi API kiểm tra mỗi 60 giây để bắt kịp ngay khi backend tạo xong
setInterval(autoSendCourseRecommendation, 10 * 1000);

window.autoSendCourseRecommendation = autoSendCourseRecommendation;
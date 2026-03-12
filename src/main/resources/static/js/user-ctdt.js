// ============ USER CTDT FUNCTIONS ============
APP.endpoints.userCTDT = {
    myCTDT: '/profile/api/user/my-ctdt',
    updateStatus: '/profile/api/user/update-ctdt-status',
    stats: '/profile/api/user/ctdt-stats',
    hasCTDT: '/profile/api/user/has-ctdt',
    initialize: '/profile/api/user/initialize-ctdt',
    getUserCTDT: '/profile/api/user/ctdt',
    filterCTDT: '/profile/api/user/ctdt/filter',
    updateCTDTStatusAlt: '/profile/api/user/ctdt/update-status',
    exportCTDTExcel: '/profile/api/user/export-ctdt-excel-full',
    userProfile: '/profile/api/user-profile',
    ctdtStatsCalculated: '/profile/api/user/ctdt-stats-calculated'
};

// USER CTDT variables
let USER_CTDT = {
    allCTDT: [],
    filteredCTDT: [],
    currentCTDTId: null,
    currentStatus: null
};

// Load thông tin user khi vào tab CTDT
function loadUserInfoForCTDT() {
    $.ajax({
        url: APP.endpoints.userCTDT.userProfile,
        type: 'GET',
        success: function(user) {
            $('#userDepartment').text(user.department || 'Chưa cập nhật');
            $('#userMajor').text(user.major || 'Chưa cập nhật');
            $('#userCourse').text(user.course || 'Chưa cập nhật');
        },
        error: function() {
            $('#userDepartment').text('Lỗi tải dữ liệu');
            $('#userMajor').text('Lỗi tải dữ liệu');
            $('#userCourse').text('Lỗi tải dữ liệu');
        }
    });
}

// Initialize CTDT when tab is loaded
function initUserCTDT() {
    console.log("DEBUG: initUserCTDT - Tab CTDT đang được khởi tạo");

    if ($('#userCTDTTableBody').length) {
        console.log("DEBUG: Tìm thấy bảng CTDT");
        // Load thông tin user trước
        loadUserInfoForCTDT();
        checkAndInitializeUserCTDT();
        // Thêm sự kiện cho filter
        $('#userFilterHocKy, #userFilterLoai, #userFilterTrangThai').off('change').on('change', function() {
            searchUserCTDT();
        });
    } else {
        console.error("DEBUG: KHÔNG tìm thấy bảng CTDT! Kiểm tra HTML");
    }
}

// Hàm check và khởi tạo CTDT
function checkAndInitializeUserCTDT() {
    showLoading('#userCTDTTableBody');

    $.ajax({
        url: APP.endpoints.userCTDT.hasCTDT,
        type: 'GET',
        success: function(response) {
            if (response.hasCTDT) {
                console.log("DEBUG: Người dùng đã có CTDT, đang tải dữ liệu...");
                loadUserCTDT();
            } else {
                // Hiển thị thông báo và nút khởi tạo
                $('#userCTDTTableBody').html(`
                    <tr>
                        <td colspan="10" class="text-center py-5">
                            <div class="alert alert-info">
                                <i class="fas fa-info-circle me-2"></i>
                                Bạn chưa có chương trình đào tạo.<br>
                                Vui lòng khởi tạo để bắt đầu theo dõi.
                            </div>
                            <button class="btn btn-vlu-blue btn-sm mt-2" onclick="initializeUserCTDT()">
                                <i class="fas fa-play me-1"></i> Khởi tạo CTĐT
                            </button>
                        </td>
                    </tr>
                `);
                hideLoading('#userCTDTTableBody');
            }
        },
        error: function() {
            hideLoading('#userCTDTTableBody');
            showToast('Lỗi khi kiểm tra CTĐT', 'error');
        }
    });
}

// Khởi tạo CTDT cho user
function initializeUserCTDT() {
    showLoading('#userCTDTTableBody');

    $.ajax({
        url: APP.endpoints.userCTDT.initialize,
        type: 'POST',
        success: function(response) {
//            showToast(response, 'success');
            loadUserCTDT();
        },
        error: function(xhr) {
            hideLoading('#userCTDTTableBody');
            showToast(xhr.responseText || 'Lỗi khi khởi tạo CTĐT', 'error');
        }
    });
}

// Load user CTDT data
function loadUserCTDT() {
    console.log("DEBUG: loadUserCTDT - Đang tải dữ liệu CTDT");
    showLoading('#userCTDTTableBody');

    // Load thông tin user
    loadUserInfoForCTDT();

    // Load CTDT
    $.ajax({
        url: APP.endpoints.userCTDT.myCTDT,
        type: 'GET',
        success: function(userCTDTList) {
            console.log("DEBUG: Đã tải được", userCTDTList.length, "môn học");
            USER_CTDT.allCTDT = userCTDTList;
            USER_CTDT.filteredCTDT = [...userCTDTList];

            renderUserCTDTTable(userCTDTList);
            loadUserCTDTFilters();

            // Gọi hàm cập nhật thống kê TIẾN ĐỘ
            console.log("DEBUG: Gọi updateUserCTDTStatistics...");
            updateUserCTDTStatistics();

            hideLoading('#userCTDTTableBody');
        },
        error: function(xhr) {
            $('#userCTDTTableBody').html(`
                <tr>
                    <td colspan="10" class="text-center py-5 text-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Lỗi khi tải chương trình đào tạo
                    </td>
                </tr>
            `);
            hideLoading('#userCTDTTableBody');
            showToast('Lỗi khi tải chương trình đào tạo', 'error');
        }
    });
}

// Load filters
function loadUserCTDTFilters() {
    const hocKySet = new Set();
    USER_CTDT.allCTDT.forEach(ctdt => {
        if (ctdt.hocKy) hocKySet.add(ctdt.hocKy);
    });

    let hocKyHtml = '<option value="">- Tất cả học kỳ -</option>';
    Array.from(hocKySet).sort((a, b) => a - b).forEach(hk => {
        hocKyHtml += `<option value="${hk}">Học kỳ ${hk}</option>`;
    });

    $('#userFilterHocKy').html(hocKyHtml);
}

// ============ HÀM UPDATE PROGRESS BARS ============
function updateProgressBars(
    bbRequired, bbCompleted,
    tcRequiredTotal, tcCompletedSimple,
    totalRequired, totalCompleted
) {
    console.log("DEBUG: updateProgressBars - Đang cập nhật thanh tiến độ");
    console.log("DEBUG: Dữ liệu nhận được:", {
        bbRequired, bbCompleted,
        tcRequiredTotal, tcCompletedSimple,
        totalRequired, totalCompleted
    });

    // Tính phần trăm với kiểm tra hợp lệ
    let bbPercent = 0;
    let tcPercent = 0;
    let totalPercent = 0;

    // Kiểm tra dữ liệu hợp lệ
    const isValid = (value) => !isNaN(value) && isFinite(value);

    // 1. TIẾN ĐỘ BẮT BUỘC: theo quy tắc X/Y
    if (isValid(bbRequired) && bbRequired > 0 && isValid(bbCompleted)) {
        const percent = (bbCompleted / bbRequired) * 100;
        bbPercent = Math.min(Math.round(percent), 100);
    }

    // 2. TIẾN ĐỘ TỰ CHỌN: tính theo công thức mới
    if (isValid(tcRequiredTotal) && tcRequiredTotal > 0 && isValid(tcCompletedSimple)) {
        const percent = (tcCompletedSimple / tcRequiredTotal) * 100;
        tcPercent = Math.min(Math.round(percent), 100);
    }

    // 3. TIẾN ĐỘ TỔNG: (BB đã học + TC đã học) / (BB phải học + TC phải học)
    if (isValid(totalRequired) && totalRequired > 0 && isValid(totalCompleted)) {
        const percent = (totalCompleted / totalRequired) * 100;
        totalPercent = Math.min(Math.round(percent), 100);
    }

    // Đảm bảo không có NaN
    bbPercent = isNaN(bbPercent) ? 0 : bbPercent;
    tcPercent = isNaN(tcPercent) ? 0 : tcPercent;
    totalPercent = isNaN(totalPercent) ? 0 : totalPercent;

    console.log("DEBUG: Phần trăm tính được:", {
        bbPercent: bbPercent + "%",
        tcPercent: tcPercent + "%",
        totalPercent: totalPercent + "%"
    });

    // Cập nhật hiển thị %
    const bbDisplay = bbPercent + '%';
    const tcDisplay = tcPercent + '%';
    const totalDisplay = totalPercent + '%';

    // Cập nhật progress bars
    $('#bbProgress').css('width', bbPercent + '%').attr('aria-valuenow', bbPercent);
    $('#tcProgress').css('width', tcPercent + '%').attr('aria-valuenow', tcPercent);
    $('#totalProgress').css('width', totalPercent + '%').attr('aria-valuenow', totalPercent);

    // Cập nhật phần trăm trong text
    $('#bbCompletionPercentage').text(bbDisplay);
    $('#tcCompletionPercentage').text(tcDisplay);
    $('#completionPercentage').text(totalDisplay);
}

// ============ HÀM UPDATE STATISTICS VỚI LOGIC MỚI ============
function updateUserCTDTStatistics() {
    console.log("DEBUG: updateUserCTDTStatistics - Đang cập nhật thống kê");

    // Sử dụng endpoint backend để lấy thống kê
    $.ajax({
        url: APP.endpoints.userCTDT.ctdtStatsCalculated,
        type: 'GET',
        success: function(stats) {
            console.log("DEBUG: Thống kê từ backend:", stats);

            // Cập nhật các giá trị số
            $('#totalSubjects').text(stats.totalSubjects || 0);
            $('#completedSubjects').text(stats.completedSubjects || 0);
            $('#pendingSubjects').text(stats.pendingSubjects || 0);

            // CẬP NHẬT VỚI LOGIC MỚI:
            // 1. BẮT BUỘC: theo quy tắc X/Y
            const bbRequired = stats.bbRequired || 0;
            const bbCompleted = stats.bbCompleted || 0;

            $('#bbCreditsRequired').text(bbRequired.toFixed(1));
            $('#bbCreditsCompleted').text(bbCompleted.toFixed(1));

            // 2. TỰ CHỌN - PHẦN TIẾN ĐỘ: dùng dữ liệu từ backend
            const tcRequiredTotal = stats.tcRequiredTotal || calculateTotalTCRequiredAllSemesters(USER_CTDT.allCTDT || []);
            const tcCompletedSimple = stats.tcCompletedSimple || calculateCompletedTCCreditsSimple(USER_CTDT.allCTDT || []);

            $('#tcCreditsRequired').text(tcRequiredTotal.toFixed(1));
            $('#tcCreditsCompleted').text(tcCompletedSimple.toFixed(1));

            // 3. TÍNH TỔNG TIẾN ĐỘ THEO CÔNG THỨC MỚI:
            // Tổng phải học = BB phải học (X/Y) + TC phải học (tổng của tất cả học kỳ theo X/Y)
            // Tổng đã học = BB đã học (X/Y) + TC đã học (đơn giản)
            const totalRequired = bbRequired + tcRequiredTotal;
            const totalCompleted = bbCompleted + tcCompletedSimple;

            $('#totalCreditsRequired').text(totalRequired.toFixed(1));
            $('#totalCreditsCompleted').text(totalCompleted.toFixed(1));

            // Cập nhật các ô khác
            $('#totalCredits').text(totalCompleted.toFixed(1));
            $('#bbCredits').text(bbCompleted.toFixed(1));
            $('#tcCredits').text(tcCompletedSimple.toFixed(1));
            $('#completedCredits').text(totalCompleted.toFixed(1));

            // Cập nhật progress bars với LOGIC MỚI
            updateProgressBars(
                bbRequired,                 // BB phải học (X/Y)
                bbCompleted,                // BB đã học (X/Y)
                tcRequiredTotal,           // TC phải học (tổng của tất cả học kỳ theo X/Y)
                tcCompletedSimple,         // TC đã học (đơn giản)
                totalRequired,             // Tổng phải học (theo công thức mới)
                totalCompleted             // Tổng đã học (theo công thức mới)
            );

            console.log("DEBUG: Đã cập nhật xong thống kê (logic mới)");
            console.log("DEBUG: - BB phải học:", bbRequired, "BB đã học:", bbCompleted);
            console.log("DEBUG: - TC phải học (tổng):", tcRequiredTotal, "TC đã học:", tcCompletedSimple);
            console.log("DEBUG: - Tổng phải học:", totalRequired, "Tổng đã học:", totalCompleted);
            console.log("DEBUG: - Tỷ lệ tổng:", ((totalCompleted / totalRequired) * 100).toFixed(1) + "%");
        },
        error: function(xhr) {
            console.error("DEBUG: Lỗi khi lấy thống kê từ backend:", xhr.responseText);
            // Fallback: tính toán ở frontend
            updateUserCTDTStatisticsOld();
        }
    });
}

// ============ HÀM TÍNH TÍN CHỈ TC ĐƠN GIẢN CHO PHẦN TIẾN ĐỘ ============
function calculateTotalTCRequiredAllSemesters(ctdtList) {
    if (!ctdtList || ctdtList.length === 0) return 0;

    // Nhóm theo học kỳ
    const groupedByHocKy = {};
    ctdtList.forEach(ctdt => {
        if (ctdt.loai === 'TC') {
            const hocKy = ctdt.hocKy || 0;
            if (!groupedByHocKy[hocKy]) {
                groupedByHocKy[hocKy] = [];
            }
            groupedByHocKy[hocKy].push(ctdt);
        }
    });

    let totalTCRequired = 0;

    // Tính cho từng học kỳ
    Object.keys(groupedByHocKy).forEach(hocKy => {
        const tcSubjects = groupedByHocKy[hocKy];

        // Nhóm các môn TC theo nhómTC
        const tcGroups = {};
        const tcNoGroup = [];

        tcSubjects.forEach(ctdt => {
            const nhomTC = ctdt.nhomTC ? ctdt.nhomTC.trim() : '';

            if (nhomTC === '') {
                tcNoGroup.push(ctdt);
            } else {
                if (!tcGroups[nhomTC]) {
                    tcGroups[nhomTC] = [];
                }
                tcGroups[nhomTC].push(ctdt);
            }
        });

        // 1. TC không nhóm: tính toàn bộ tín chỉ
        tcNoGroup.forEach(ctdt => {
            totalTCRequired += ctdt.tinChi || 0;
        });

        // 2. TC có nhóm: tính theo công thức (tổng tín chỉ nhóm) × (X/Y)
        Object.keys(tcGroups).forEach(nhomTC => {
            const groupSubjects = tcGroups[nhomTC];

            // Tính tổng tín chỉ của nhóm
            let groupTotalCredits = 0;
            groupSubjects.forEach(ctdt => {
                groupTotalCredits += ctdt.tinChi || 0;
            });

            // Parse X/Y
            const parts = nhomTC.split('/');

            if (parts.length === 2) {
                try {
                    const x = parseInt(parts[0].trim()); // Số tín chỉ phải học (ví dụ: 2)
                    const y = parseInt(parts[1].trim()); // Tổng số môn trong nhóm (ví dụ: 16)

                    if (!isNaN(x) && !isNaN(y) && y > 0) {
                        // CÔNG THỨC: (tổng tín chỉ nhóm) × (X/Y)
                        const requiredCredits = groupTotalCredits * (x / y);
                        totalTCRequired += requiredCredits;

                        console.log(`DEBUG: Nhóm ${nhomTC} - Tổng tín chỉ: ${groupTotalCredits}, X/Y: ${x}/${y}, Cần học: ${requiredCredits.toFixed(1)}`);
                    } else {
                        // Nếu không parse được, tính bình thường
                        totalTCRequired += groupTotalCredits;
                    }
                } catch (e) {
                    // Tính bình thường
                    totalTCRequired += groupTotalCredits;
                }
            } else {
                // Không phải định dạng X/Y, tính bình thường
                totalTCRequired += groupTotalCredits;
            }
        });
    });

    return Math.round(totalTCRequired * 10) / 10;
}

// ============ HÀM TÍNH TỔNG TC ĐÃ HỌC (ĐƠN GIẢN) ============
function calculateCompletedTCCreditsSimple(ctdtList) {
    if (!ctdtList || ctdtList.length === 0) return 0;

    let completed = 0;
    ctdtList.forEach(ctdt => {
        if (ctdt.loai === 'TC' && ctdt.trangThai === 0) {
            completed += ctdt.tinChi || 0;
        }
    });

    return Math.round(completed * 10) / 10;
}

// Fallback: tính toán ở frontend
function updateUserCTDTStatisticsOld() {
    console.log("DEBUG: Sử dụng tính toán thống kê ở frontend");
    const allCTDT = USER_CTDT.allCTDT;
    const currentCTDT = USER_CTDT.filteredCTDT.length > 0 ? USER_CTDT.filteredCTDT : allCTDT;

    if (!currentCTDT || currentCTDT.length === 0) {
        // Reset tất cả giá trị
        resetAllStatistics();
        return;
    }

    // Tính toán tín chỉ THEO QUY TẮC X/Y cho phần tổng kết
    const credits = calculateCreditsFromCTDTList(currentCTDT);

    // Tính toán theo LOGIC MỚI cho phần tiến độ TC
    const tcRequiredTotal = calculateTotalTCRequiredAllSemesters(currentCTDT);
    const tcCompletedSimple = calculateCompletedTCCreditsSimple(currentCTDT);

    // Tính số môn
    const total = currentCTDT.length;
    const completed = currentCTDT.filter(c => c.trangThai === 0).length;
    const pending = total - completed;

    // Cập nhật các giá trị số
    $('#totalSubjects').text(total);
    $('#completedSubjects').text(completed);
    $('#pendingSubjects').text(pending);

    // Cập nhật với LOGIC MỚI:
    // 1. BẮT BUỘC: theo quy tắc X/Y
    $('#bbCreditsRequired').text(credits.bbRequired.toFixed(1));
    $('#bbCreditsCompleted').text(credits.bbCompleted.toFixed(1));

    // 2. TỰ CHỌN - THEO LOGIC MỚI
    $('#tcCreditsRequired').text(tcRequiredTotal.toFixed(1));
    $('#tcCreditsCompleted').text(tcCompletedSimple.toFixed(1));

    // 3. TỔNG TIẾN ĐỘ THEO CÔNG THỨC MỚI
    const totalRequired = credits.bbRequired + tcRequiredTotal;
    const totalCompleted = credits.bbCompleted + tcCompletedSimple;

    $('#totalCreditsRequired').text(totalRequired.toFixed(1));
    $('#totalCreditsCompleted').text(totalCompleted.toFixed(1));

    // Cập nhật các ô khác
    $('#totalCredits').text(totalCompleted.toFixed(1));
    $('#bbCredits').text(credits.bbCompleted.toFixed(1));
    $('#tcCredits').text(tcCompletedSimple.toFixed(1));
    $('#completedCredits').text(totalCompleted.toFixed(1));

    // Cập nhật progress bars với LOGIC MỚI
    updateProgressBars(
        credits.bbRequired,
        credits.bbCompleted,
        tcRequiredTotal,           // Tổng TC cần học của tất cả học kỳ (X/Y)
        tcCompletedSimple,         // Tổng TC đã học (đơn giản)
        totalRequired,             // Tổng phải học (theo công thức mới)
        totalCompleted             // Tổng đã học (theo công thức mới)
    );

    console.log("DEBUG: Frontend tính toán:");
    console.log("DEBUG: - BB phải học:", credits.bbRequired, "BB đã học:", credits.bbCompleted);
    console.log("DEBUG: - TC phải học (tổng):", tcRequiredTotal, "TC đã học:", tcCompletedSimple);
    console.log("DEBUG: - Tổng phải học:", totalRequired, "Tổng đã học:", totalCompleted);
    console.log("DEBUG: - Tỷ lệ tổng:", ((totalCompleted / totalRequired) * 100).toFixed(1) + "%");
}

function resetAllStatistics() {
    $('#totalSubjects').text('0');
    $('#completedSubjects').text('0');
    $('#pendingSubjects').text('0');
    $('#totalCredits').text('0.0');
    $('#bbCredits').text('0.0');
    $('#tcCredits').text('0.0');
    $('#completedCredits').text('0.0');
    $('#bbCreditsRequired').text('0.0');
    $('#bbCreditsCompleted').text('0.0');
    $('#tcCreditsRequired').text('0.0');
    $('#tcCreditsCompleted').text('0.0');
    $('#totalCreditsRequired').text('0.0');
    $('#totalCreditsCompleted').text('0.0');

    updateProgressBars(0, 0, 0, 0, 0, 0);
}

// Hàm tính tín chỉ THEO QUY TẮC X/Y (cho phần tổng kết học kỳ) - GIỮ NGUYÊN
function calculateCreditsFromCTDTList(ctdtList) {
    if (!ctdtList || ctdtList.length === 0) {
        return {
            bbRequired: 0,
            bbCompleted: 0,
            tcRequired: 0,
            tcCompleted: 0,
            totalRequired: 0,
            totalCompleted: 0
        };
    }

    let totalBBCreditsRequired = 0;
    let totalTCCreditsRequired = 0;
    let totalBBCreditsCompleted = 0;
    let totalTCCreditsCompleted = 0;

    // 1. TÍNH CÁC MÔN BẮT BUỘC (BB) - đơn giản
    const bbSubjects = ctdtList.filter(ctdt => ctdt.loai === 'BB');
    bbSubjects.forEach(ctdt => {
        const tinChi = ctdt.tinChi || 0;
        const trangThai = ctdt.trangThai || 1;

        totalBBCreditsRequired += tinChi;
        if (trangThai === 0) {
            totalBBCreditsCompleted += tinChi;
        }
    });

    // 2. NHÓM CÁC MÔN TỰ CHỌN THEO NHÓMTC
    const tcGroups = {};
    const tcSubjectsNoGroup = [];

    const tcSubjects = ctdtList.filter(ctdt => ctdt.loai === 'TC');
    tcSubjects.forEach(ctdt => {
        const tinChi = ctdt.tinChi || 0;
        const trangThai = ctdt.trangThai || 1;
        const nhomTC = ctdt.nhomTC ? ctdt.nhomTC.trim() : '';

        if (!nhomTC) {
            // Môn TC không có nhóm
            tcSubjectsNoGroup.push({
                tinChi: tinChi,
                trangThai: trangThai
            });
        } else {
            // Môn TC có nhóm
            if (!tcGroups[nhomTC]) {
                tcGroups[nhomTC] = {
                    totalCredits: 0,
                    completedCredits: 0
                };
            }
            tcGroups[nhomTC].totalCredits += tinChi;
            if (trangThai === 0) {
                tcGroups[nhomTC].completedCredits += tinChi;
            }
        }
    });

    // 3. TÍNH TC KHÔNG NHÓM
    tcSubjectsNoGroup.forEach(subject => {
        totalTCCreditsRequired += subject.tinChi;
        if (subject.trangThai === 0) {
            totalTCCreditsCompleted += subject.tinChi;
        }
    });

    // 4. TÍNH TC CÓ NHÓM (theo quy tắc X/Y)
    Object.keys(tcGroups).forEach(nhomTC => {
        const group = tcGroups[nhomTC];

        // Phân tích nhóm TC (ví dụ: "1/3")
        const parts = nhomTC.split('/');

        if (parts.length === 2) {
            const x = parseFloat(parts[0].trim()); // Số tín chỉ phải học
            const y = parseFloat(parts[1].trim()); // Tổng tín chỉ trong nhóm

            if (!isNaN(x) && !isNaN(y) && y > 0) {
                const ratio = x / y;

                // Tính theo quy tắc nhóm TC (X/Y)
                totalTCCreditsRequired += group.totalCredits * ratio;
                totalTCCreditsCompleted += group.completedCredits * ratio;
            } else {
                // Nếu không phải định dạng số, tính bình thường
                totalTCCreditsRequired += group.totalCredits;
                totalTCCreditsCompleted += group.completedCredits;
            }
        } else {
            // Không phải định dạng X/Y, tính bình thường
            totalTCCreditsRequired += group.totalCredits;
            totalTCCreditsCompleted += group.completedCredits;
        }
    });

    // Làm tròn
    totalBBCreditsRequired = Math.round(totalBBCreditsRequired * 10) / 10;
    totalTCCreditsRequired = Math.round(totalTCCreditsRequired * 10) / 10;
    totalBBCreditsCompleted = Math.round(totalBBCreditsCompleted * 10) / 10;
    totalTCCreditsCompleted = Math.round(totalTCCreditsCompleted * 10) / 10;

    return {
        bbRequired: totalBBCreditsRequired,
        bbCompleted: totalBBCreditsCompleted,
        tcRequired: totalTCCreditsRequired,
        tcCompleted: totalTCCreditsCompleted,
        totalRequired: Math.round((totalBBCreditsRequired + totalTCCreditsRequired) * 10) / 10,
        totalCompleted: Math.round((totalBBCreditsCompleted + totalTCCreditsCompleted) * 10) / 10
    };
}

// Render user CTDT table (TÍNH THEO QUY TẮC X/Y CHO TỪNG HỌC KỲ) - GIỮ NGUYÊN
function renderUserCTDTTable(ctdtList) {
    let html = '';
    let semesterTotals = [];
    let totalCreditsRequiredAllSemesters = 0;
    let totalCreditsCompletedAllSemesters = 0;
    let totalSubjectsAllSemesters = 0;
    let completedSubjectsAllSemesters = 0;

    if (!ctdtList || ctdtList.length === 0) {
        html = `
            <tr>
                <td colspan="11" class="text-center py-5 text-muted">
                    <i class="fas fa-database me-2"></i>
                    Không có học phần nào trong chương trình đào tạo của bạn
                </td>
            </tr>
        `;
    } else {
        // ============ NHÓM THEO HỌC KỲ ============
        const groupedByHocKy = {};
        ctdtList.forEach(ctdt => {
            const hocKy = ctdt.hocKy || 0;
            if (!groupedByHocKy[hocKy]) {
                groupedByHocKy[hocKy] = [];
            }
            groupedByHocKy[hocKy].push(ctdt);
        });

        // ============ SẮP XẾP HỌC KỲ ============
        const sortedHocKys = Object.keys(groupedByHocKy).sort((a, b) => a - b);

        let globalIndex = 1;

        sortedHocKys.forEach(hocKy => {
            let hocPhanList = groupedByHocKy[hocKy];

            // ============ SẮP XẾP THEO YÊU CẦU ============
            const nhom1 = [];
            const nhom2 = [];
            const nhom3 = [];
            const nhom4 = [];

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
                        nhom1.push(ctdt);
                    }
                } else if (loai === 'TC') {
                    nhom4.push(ctdt);
                }
            });

            // Sắp xếp trong từng nhóm
            const sortFunction = (a, b) => {
                const nganhA = a.nganh ? a.nganh.trim() : '';
                const nganhB = b.nganh ? b.nganh.trim() : '';
                if (nganhA !== nganhB) {
                    return nganhA.localeCompare(nganhB, 'vi');
                }

                const cnA = a.chuyenNganh ? a.chuyenNganh.trim() : '';
                const cnB = b.chuyenNganh ? b.chuyenNganh.trim() : '';
                if (cnA !== cnB) {
                    return cnA.localeCompare(cnB, 'vi');
                }

                const tenA = a.tenHocPhan ? a.tenHocPhan.trim() : '';
                const tenB = b.tenHocPhan ? b.tenHocPhan.trim() : '';
                return tenA.localeCompare(tenB, 'vi');
            };

            nhom1.sort(sortFunction);
            nhom2.sort(sortFunction);
            nhom3.sort(sortFunction);
            nhom4.sort(sortFunction);

            hocPhanList = [...nhom1, ...nhom2, ...nhom3, ...nhom4];

            const rowSpan = hocPhanList.length;

            // Tính tín chỉ cho học kỳ này THEO QUY TẮC X/Y
            const credits = calculateCreditsFromCTDTList(hocPhanList);

            let semesterSubjects = hocPhanList.length;
            let semesterCompletedSubjects = hocPhanList.filter(c => c.trangThai === 0).length;

            // Lưu tổng tín chỉ của học kỳ này
            semesterTotals.push({
                hocKy: hocKy,
                totalTinChi: credits.totalRequired,
                totalTinChiCompleted: credits.totalCompleted,
                totalTinChiBB: credits.bbRequired,
                totalTinChiTC: credits.tcRequired,
                semesterSubjects: semesterSubjects,
                semesterCompletedSubjects: semesterCompletedSubjects
            });

            // Cộng vào tổng toàn chương trình
            totalSubjectsAllSemesters += semesterSubjects;
            completedSubjectsAllSemesters += semesterCompletedSubjects;
            totalCreditsRequiredAllSemesters += credits.totalRequired;
            totalCreditsCompletedAllSemesters += credits.totalCompleted;

            const tooltipTitle = `
                Chi tiết học kỳ ${hocKy}:<br>
                - Tổng môn: ${semesterSubjects}<br>
                - Đã học: ${semesterCompletedSubjects}<br>
                - Tổng tín chỉ phải học: ${credits.totalRequired.toFixed(1)}<br>
                - Tín chỉ đã học: ${credits.totalCompleted.toFixed(1)}<br>
                - Tín chỉ bắt buộc phải học: ${credits.bbRequired.toFixed(1)}<br>
                - Tín chỉ tự chọn phải học: ${credits.tcRequired.toFixed(1)}
            `.trim();

            // Render từng học phần
            hocPhanList.forEach((ctdt, index) => {
                let tienQuyetText = '-';

                if (ctdt.hocPhanTienQuyetNames && Array.isArray(ctdt.hocPhanTienQuyetNames) && ctdt.hocPhanTienQuyetNames.length > 0) {
                    tienQuyetText = ctdt.hocPhanTienQuyetNames.join(', ');
                } else if (ctdt.hocPhanTienQuyet && Array.isArray(ctdt.hocPhanTienQuyet) && ctdt.hocPhanTienQuyet.length > 0) {
                    tienQuyetText = ctdt.hocPhanTienQuyet
                        .map(hp => hp.tenHocPhan || `ID: ${hp.id}`)
                        .join(', ');
                } else if (ctdt.hocPhanTienQuyetIds && Array.isArray(ctdt.hocPhanTienQuyetIds) && ctdt.hocPhanTienQuyetIds.length > 0) {
                    tienQuyetText = ctdt.hocPhanTienQuyetIds.join(', ');
                }

                // Tạo badge trạng thái với click event
                const statusBadge = ctdt.trangThai === 0 ?
                    `<span class="badge bg-success cursor-pointer" onclick="changeCTDTStatus(${ctdt.id}, 0)">Đã học</span>` :
                    `<span class="badge bg-warning cursor-pointer" onclick="changeCTDTStatus(${ctdt.id}, 1)">Chưa học</span>`;

                html += `
                    <tr>
                        ${index === 0 ? `
                            <td rowspan="${rowSpan}" class="fw-bold">
                                <div>${hocKy}</div>
                                <small class="text-muted" style="font-size: 11px;" title="${tooltipTitle}">
                                    ${credits.totalRequired.toFixed(1)} TC
                                </small>
                            </td>
                        ` : ''}
                        <td>
                            ${ctdt.maHocPhan ?
                                `<span class="badge bg-dark" style="font-size: 11px;">${ctdt.maHocPhan}</span>` :
                                '<span class="text-muted">-</span>'}
                        </td>
                        <td>${globalIndex++}</td>
                        <td class="text-start">${ctdt.tenHocPhan || ''}</td>
                        <td>${ctdt.tinChi || 0}</td>
                        <td>
                            <span class="badge ${ctdt.loai === 'BB' ? 'bg-primary' : 'bg-success'}" style="font-size: 11px;">
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
                            ${statusBadge}
                        </td>
                    </tr>
                `;
            });

            // Thêm hàng tổng kết học kỳ (THEO QUY TẮC X/Y)
            html += `
                <tr class="table-info">
                    <td colspan="3" class="text-end fw-bold">Tổng kết học kỳ ${hocKy}:</td>
                    <td class="fw-bold">${credits.totalRequired.toFixed(1)}</td>
                    <td colspan="7">
                        <small class="text-muted">
                            <i>Bắt buộc: ${credits.bbRequired.toFixed(1)} TC | Tự chọn: ${credits.tcRequired.toFixed(1)} TC</i>
                        </small>
                    </td>
                </tr>
            `;
        });

        // Thêm hàng tổng kết toàn bộ
        html += `
            <tr class="table-success">
                <td colspan="3" class="text-end fw-bold" style="background-color: #198754; color: white;">
                    TỔNG TÍN CHỈ:
                </td>
                <td class="fw-bold" style="background-color: #198754; color: white; font-size: 16px;">
                    ${totalCreditsRequiredAllSemesters.toFixed(1)}
                </td>
                <td colspan="7" style="background-color: #198754; color: white;">
                    <small>
                        <i>Tổng tín chỉ phải học của tất cả học kỳ (theo quy tắc X/Y)</i>
                    </small>
                </td>
            </tr>
        `;
    }

    $('#userCTDTTableBody').html(html);

    // Cập nhật tiến độ sau khi render bảng
    updateUserCTDTStatistics();
}

// Search functions
function searchUserCTDT() {
    const hocKy = $('#userFilterHocKy').val();
    const loai = $('#userFilterLoai').val();
    const trangThai = $('#userFilterTrangThai').val();

    let filtered = USER_CTDT.allCTDT;

    if (hocKy) {
        filtered = filtered.filter(c => c.hocKy == hocKy);
    }

    if (loai) {
        filtered = filtered.filter(c => c.loai === loai);
    }

    if (trangThai !== '') {
        filtered = filtered.filter(c => c.trangThai == trangThai);
    }

    USER_CTDT.filteredCTDT = filtered;
    renderUserCTDTTable(filtered);
}

function resetUserCTDTSearch() {
    $('#userFilterHocKy').val('');
    $('#userFilterLoai').val('');
    $('#userFilterTrangThai').val('');

    USER_CTDT.filteredCTDT = [...USER_CTDT.allCTDT];
    renderUserCTDTTable(USER_CTDT.allCTDT);
}

// Change status functions (giữ nguyên)
function changeCTDTStatus(userCTDTId, currentStatus) {
    const userCTDT = USER_CTDT.allCTDT.find(c => c.id === userCTDTId);

    if (!userCTDT) {
        showToast('Không tìm thấy học phần', 'error');
        return;
    }

    // Tạo modal HTML mới với ID unique
    const modalId = 'statusModal-' + Date.now();
    const modalHtml = `
        <div class="modal fade" id="${modalId}" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header py-2" style="background: var(--vlu-blue); color: white;">
                        <h6 class="modal-title">Thay đổi trạng thái học phần</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p id="statusMessage-${modalId}">
                            Bạn đang thay đổi trạng thái học phần:<br>
                            <strong>${userCTDT.tenHocPhan}</strong><br>
                            <small class="text-muted">HK${userCTDT.hocKy} - ${userCTDT.tinChi} tín chỉ</small>
                        </p>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="statusOption-${modalId}" id="statusLearned-${modalId}" value="0" ${currentStatus === 0 ? 'checked' : ''}>
                            <label class="form-check-label" for="statusLearned-${modalId}">
                                <span class="badge bg-success">Đã học</span>
                            </label>
                        </div>
                        <div class="form-check mt-2">
                            <input class="form-check-input" type="radio" name="statusOption-${modalId}" id="statusNotLearned-${modalId}" value="1" ${currentStatus === 1 ? 'checked' : ''}>
                            <label class="form-check-label" for="statusNotLearned-${modalId}">
                                <span class="badge bg-warning">Chưa học</span>
                            </label>
                        </div>
                    </div>
                    <div class="modal-footer py-2">
                        <button type="button" class="btn btn-vlu-blue btn-sm" onclick="confirmStatusChangeNew('${modalId}', ${userCTDTId})">Xác nhận</button>
                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Hủy</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Xóa modal cũ nếu có
    $('#statusModalContainer').empty().html(modalHtml);

    // Hiển thị modal mới
    const modalElement = document.getElementById(modalId);
    const modal = new bootstrap.Modal(modalElement);
    modal.show();

    // Xóa modal khi đóng
    modalElement.addEventListener('hidden.bs.modal', function () {
        $(this).remove();
    });
}

function confirmStatusChangeNew(modalId, userCTDTId) {
    const newStatus = $(`#${modalId} input[name="statusOption-${modalId}"]:checked`).val();

    if (newStatus === undefined) {
        showToast('Vui lòng chọn trạng thái mới', 'error');
        return;
    }

    // Hiển thị loading
    $(`#${modalId} .modal-body`).append(`
        <div class="modal-loading-overlay">
            <div class="spinner-border spinner-border-sm text-primary"></div>
            <span class="ms-2">Đang cập nhật...</span>
        </div>
    `);

    // Gửi request
    $.ajax({
        url: `${APP.endpoints.userCTDT.updateStatus}/${userCTDTId}`,
        type: 'POST',
        data: { trangThai: newStatus },
        success: function(response) {
            // Đóng modal
            const modal = bootstrap.Modal.getInstance(document.getElementById(modalId));
            if (modal) {
                modal.hide();
            }

            // Xóa modal
            $(`#${modalId}`).remove();

            // ============ XỬ LÝ REVERT TIÊN QUYẾT ============
            // Backend trả về object JSON khi có vi phạm tiên quyết
            if (response && typeof response === 'object' && response.resultType === 'REVERTED_TO_NOT_LEARNED') {
                const revertedStatus = response.revertedStatus; // = 1 (chưa học)

                // Cập nhật trạng thái về "Chưa học" trong bộ nhớ cục bộ
                const idx = USER_CTDT.allCTDT.findIndex(c => c.id === userCTDTId);
                if (idx !== -1) {
                    USER_CTDT.allCTDT[idx].trangThai = revertedStatus;
                    const fIdx = USER_CTDT.filteredCTDT.findIndex(c => c.id === userCTDTId);
                    if (fIdx !== -1) {
                        USER_CTDT.filteredCTDT[fIdx].trangThai = revertedStatus;
                    }
                }

                // Re-render bảng với trạng thái đã revert
                renderUserCTDTTable(USER_CTDT.filteredCTDT.length > 0 ? USER_CTDT.filteredCTDT : USER_CTDT.allCTDT);

                // Cập nhật badge thông báo (vì backend đã tạo notification mới)
                if (typeof updateUnreadCount === 'function') {
                    setTimeout(updateUnreadCount, 500);
                }

                // Hiển thị toast cảnh báo
                showToast('⚠️ ' + response.message, 'warning');
                return;
            }
            // ============ KẾT THÚC XỬ LÝ REVERT ============

            // Cập nhật dữ liệu cục bộ (trường hợp thành công bình thường)
            const index = USER_CTDT.allCTDT.findIndex(c => c.id === userCTDTId);
            if (index !== -1) {
                USER_CTDT.allCTDT[index].trangThai = parseInt(newStatus);

                const filteredIndex = USER_CTDT.filteredCTDT.findIndex(c => c.id === userCTDTId);
                if (filteredIndex !== -1) {
                    USER_CTDT.filteredCTDT[filteredIndex].trangThai = parseInt(newStatus);
                }
            }

            // Re-render bảng - sẽ tự động cập nhật tiến độ
            renderUserCTDTTable(USER_CTDT.filteredCTDT.length > 0 ? USER_CTDT.filteredCTDT : USER_CTDT.allCTDT);

            // Cập nhật số thông báo ngay lập tức (nếu có)
            if (typeof updateUnreadCount === 'function') {
                updateUnreadCount();
            }

            // Tự động gợi ý môn học (HK1 ngay, HK2+ theo chu kỳ thời gian)
            if (typeof autoSendCourseRecommendation === 'function') {
                setTimeout(autoSendCourseRecommendation, 500);
            }

//            showToast(response, 'success');
        },
        error: function(xhr) {
            $(`#${modalId} .modal-loading-overlay`).remove();

            let errorMsg = xhr.responseText || 'Lỗi khi cập nhật trạng thái';
            if (xhr.status === 404) {
                errorMsg = 'Không tìm thấy học phần';
            } else if (xhr.status === 403) {
                errorMsg = 'Bạn không có quyền thay đổi học phần này';
            }

            showToast(errorMsg, 'error');
        }
    });
}

function exportCTDTToExcelFull() {
    if (!confirm('Bạn có muốn xuất chương trình đào tạo đầy đủ ra file Excel?\n\nFile sẽ bao gồm:\n✓ Thông tin chi tiết học phần\n✓ Tiến độ theo thời gian thực\n✓ Tổng hợp theo học kỳ\n✓ Thống kê X/Y')) {
        return;
    }

    showLoading();
    showToast('Đang tạo file Excel với dữ liệu thời gian thực...', 'info');

    // Tạo URL với timestamp để tránh cache
    const exportUrl = APP.endpoints.userCTDT.exportCTDTExcelFull;
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

// Cập nhật hàm exportCTDTToExcel cũ để có 2 tùy chọn
function showExportOptions() {
    const optionsHtml = `
        <div class="modal fade" id="exportOptionsModal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-primary text-white py-2">
                        <h6 class="modal-title">Chọn loại file xuất</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row justify-content-center">
                            <div class="col-md-6 col-lg-5">
                                <div class="card border-success">
                                    <div class="card-body text-center">
                                        <i class="fas fa-chart-line fa-3x text-success mb-3"></i>
                                        <h6>File Đầy Đủ</h6>
                                        <p class="small text-muted">
                                            Bao gồm tiến độ thời gian thực, thống kê X/Y
                                        </p>
                                        <button class="btn btn-success btn-sm" onclick="exportCTDTToExcelFull()">
                                            <i class="fas fa-download me-1"></i> Xuất Đầy Đủ
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer py-2">
                        <small class="text-muted">* File đầy đủ bao gồm dữ liệu thống kê theo thời gian thực</small>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Thêm modal vào container
    $('#modalContainer').html(optionsHtml);

    // Hiển thị modal
    const modal = new bootstrap.Modal(document.getElementById('exportOptionsModal'));
    modal.show();

    // Dọn dẹp khi modal đóng
    $('#exportOptionsModal').on('hidden.bs.modal', function() {
        $(this).remove();
    });
}


// ============ WINDOW ASSIGNMENTS ============
window.APP = APP;
window.initUserCTDT = initUserCTDT;
window.loadUserCTDT = loadUserCTDT;
window.searchUserCTDT = searchUserCTDT;
window.resetUserCTDTSearch = resetUserCTDTSearch;
window.initializeUserCTDT = initializeUserCTDT;
window.changeCTDTStatus = changeCTDTStatus;
window.confirmStatusChangeNew = confirmStatusChangeNew;
window.exportCTDTToExcelFull = exportCTDTToExcelFull;
window.showExportOptions = showExportOptions;
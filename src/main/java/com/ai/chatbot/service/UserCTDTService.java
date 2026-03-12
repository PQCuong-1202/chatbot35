package com.ai.chatbot.service;

import com.ai.chatbot.model.*;
import com.ai.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserCTDTService {

    @Autowired
    private UserCTDTRepository userCTDTRepo;

    @Autowired
    private CTDTRepository ctdtRepo;

    @Autowired
    private UserRepository userRepo;

    /**
     * Khởi tạo CTĐT cho user dựa trên ngành/chuyên ngành
     */
    @Transactional
    public String initializeCTDTForUser(User user) {
        try {
            String department = user.getDepartment();
            String major = user.getMajor();

            if (department == null || department.trim().isEmpty()) {
                return "User chưa có thông tin ngành, không thể khởi tạo CTĐT";
            }

            // Kiểm tra xem user đã có CTĐT chưa
            boolean hasCTDT = hasUserCTDT(user.getId());
            if (hasCTDT) {
                return "User đã có chương trình đào tạo";
            }

            // Lấy tất cả CTĐT phù hợp với ngành/chuyên ngành của user
            List<CTDT> suitableCTDT = getCTDTForUserDepartment(department, major);

            if (suitableCTDT.isEmpty()) {
                return "Không tìm thấy chương trình đào tạo phù hợp với ngành/chuyên ngành của bạn";
            }

            // Tạo UserCTDT từ CTDT phù hợp
            List<UserCTDT> userCTDTList = new ArrayList<>();
            for (CTDT ctdt : suitableCTDT) {
                UserCTDT userCTDT = new UserCTDT();
                userCTDT.setUser(user);
                userCTDT.setCtdt(ctdt);
                userCTDT.setHocKy(ctdt.getHocKy());
                userCTDT.setTenHocPhan(ctdt.getTenHocPhan());
                userCTDT.setMaHocPhan(ctdt.getMaHocPhan());
                userCTDT.setTinChi(ctdt.getTinChi());
                userCTDT.setLoai(ctdt.getLoai());
                userCTDT.setNhomTC(ctdt.getNhomTC());
                userCTDT.setNganh(ctdt.getNganh());
                userCTDT.setChuyenNganh(ctdt.getChuyenNganh());
                userCTDT.setNhomHocPhan(ctdt.getNhomHocPhan());
                userCTDT.setTrangThai(1); // Mặc định là chưa học
                userCTDT.setCreatedAt(LocalDateTime.now());
                userCTDT.setUpdatedAt(LocalDateTime.now());

                userCTDTList.add(userCTDT);
            }

            userCTDTRepo.saveAll(userCTDTList);

            return "Đã khởi tạo " + userCTDTList.size() + " học phần cho chương trình đào tạo";

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi khởi tạo CTĐT: " + e.getMessage();
        }
    }

    /**
     * Lấy CTĐT phù hợp với ngành/chuyên ngành của user
     */
    private List<CTDT> getCTDTForUserDepartment(String department, String major) {
        List<CTDT> allCTDT = ctdtRepo.findAll();
        List<CTDT> suitableCTDT = new ArrayList<>();

        // Lấy danh sách tất cả tabNganh tồn tại trong CTĐT
        List<String> allTabNganh = ctdtRepo.findDistinctTabNganh();

        // Tìm tabNganh phù hợp với ngành của user:
        // Nếu tên ngành của user khớp với một tabNganh -> user thuộc tab đó
        // Dùng equalsIgnoreCase để so sánh linh hoạt hơn
        String matchedTabNganh = allTabNganh.stream()
                .filter(tab -> tab != null && tab.equalsIgnoreCase(department))
                .findFirst()
                .orElse(null);

        for (CTDT ctdt : allCTDT) {
            String ctdtDept = ctdt.getNganh();
            String ctdtMajor = ctdt.getChuyenNganh();
            String ctdtTab = ctdt.getTabNganh();

            // ── BƯỚC 1: Xét điều kiện tabNganh ──────────────────────────────────
            // Nếu học phần thuộc một tabNganh cụ thể thì chỉ sinh viên cùng tab
            // mới được nhận học phần đó.
            // - Học phần không có tabNganh (null/trống) -> không bị lọc bởi tab
            // - Học phần CÓ tabNganh -> chỉ lấy nếu user khớp tab đó
            boolean tabPassed;
            if (ctdtTab == null || ctdtTab.trim().isEmpty()) {
                // Không gắn tab -> áp dụng cho mọi sinh viên (không lọc theo tab)
                tabPassed = true;
            } else if (matchedTabNganh != null && ctdtTab.equalsIgnoreCase(matchedTabNganh)) {
                // Học phần thuộc đúng tab ngành của sinh viên
                tabPassed = true;
            } else {
                // Học phần thuộc tab ngành khác -> bỏ qua
                tabPassed = false;
            }

            if (!tabPassed) {
                continue;
            }

            // ── BƯỚC 2: Giữ nguyên logic lọc theo nganh/chuyenNganh ban đầu ────
            // Logic hiển thị:
            // 1. Nganh = null/trống, chuyên ngành = null/trống -> Hiển thị cho tất cả
            // 2. Nganh giống, chuyên ngành = null/trống -> Hiển thị cho user cùng ngành
            // 3. Nganh giống VÀ chuyên ngành giống -> Hiển thị cho user cùng ngành và chuyên ngành
            boolean isSuitable = false;

            // Trường hợp 1: Dành cho tất cả
            if ((ctdtDept == null || ctdtDept.trim().isEmpty()) &&
                    (ctdtMajor == null || ctdtMajor.trim().isEmpty())) {
                isSuitable = true;
            }
            // Trường hợp 2: Cùng ngành, chuyên ngành null/trống
            else if (department != null && department.equals(ctdtDept) &&
                    (ctdtMajor == null || ctdtMajor.trim().isEmpty())) {
                isSuitable = true;
            }
            // Trường hợp 3: Cùng ngành VÀ cùng chuyên ngành
            else if (department != null && department.equals(ctdtDept) &&
                    major != null && major.equals(ctdtMajor)) {
                isSuitable = true;
            }

            if (isSuitable) {
                suitableCTDT.add(ctdt);
            }
        }

        return suitableCTDT;
    }

    /**
     * Kiểm tra user đã có CTĐT chưa
     */
    public boolean hasUserCTDT(Long userId) {
        return userCTDTRepo.countByUserId(userId) > 0;
    }

    /**
     * Lấy CTĐT của user với bộ lọc
     */
    public List<UserCTDT> filterUserCTDT(Long userId, Integer hocKy, String loai, Integer trangThai) {
        List<UserCTDT> userCTDT = userCTDTRepo.findByUserId(userId);

        List<UserCTDT> filtered = new ArrayList<>();
        for (UserCTDT uctdt : userCTDT) {
            boolean match = true;

            if (hocKy != null && uctdt.getHocKy() != null && !uctdt.getHocKy().equals(hocKy)) {
                match = false;
            }

            if (loai != null && !loai.isEmpty() && uctdt.getLoai() != null &&
                    !uctdt.getLoai().equals(loai)) {
                match = false;
            }

            if (trangThai != null && uctdt.getTrangThai() != null &&
                    !uctdt.getTrangThai().equals(trangThai)) {
                match = false;
            }

            if (match) {
                filtered.add(uctdt);
            }
        }

        return filtered;
    }

    /**
     * Cập nhật trạng thái học phần của user
     */
    @Transactional
    public String updateUserCTDTStatus(Long userId, Long ctdtId, Integer trangThai) {
        try {
            UserCTDT userCTDT = userCTDTRepo.findByUserIdAndCtdtId(userId, ctdtId);

            if (userCTDT == null) {
                return "Không tìm thấy học phần trong chương trình đào tạo của bạn";
            }

            userCTDT.setTrangThai(trangThai);
            userCTDT.setUpdatedAt(LocalDateTime.now());
            userCTDTRepo.save(userCTDT);

            String statusText = trangThai == 0 ? "đã học" : "chưa học";
            return "Đã cập nhật trạng thái học phần thành " + statusText;

        } catch (Exception e) {
            return "Lỗi khi cập nhật trạng thái: " + e.getMessage();
        }
    }

    /**
     * Lấy thống kê CTĐT của user
     */
    public UserCTDTStats getUserCTDTStats(Long userId) {
        List<UserCTDT> userCTDT = userCTDTRepo.findByUserId(userId);

        UserCTDTStats stats = new UserCTDTStats();

        int totalSubjects = userCTDT.size();
        int completedSubjects = 0;
        int pendingSubjects = 0;
        int totalCredits = 0;
        int completedCredits = 0;
        int bbCredits = 0;
        int tcCredits = 0;

        for (UserCTDT uctdt : userCTDT) {
            Integer tinChi = uctdt.getTinChi() != null ? uctdt.getTinChi() : 0;
            Integer trangThai = uctdt.getTrangThai() != null ? uctdt.getTrangThai() : 1;
            String loai = uctdt.getLoai();

            totalCredits += tinChi;

            if ("BB".equals(loai)) {
                bbCredits += tinChi;
            } else if ("TC".equals(loai)) {
                tcCredits += tinChi;
            }

            if (trangThai == 0) {
                completedSubjects++;
                completedCredits += tinChi;
            } else {
                pendingSubjects++;
            }
        }

        stats.setTotalSubjects(totalSubjects);
        stats.setCompletedSubjects(completedSubjects);
        stats.setPendingSubjects(pendingSubjects);
        stats.setTotalCredits(totalCredits);
        stats.setCompletedCredits(completedCredits);
        stats.setBbCredits(bbCredits);
        stats.setTcCredits(tcCredits);

        return stats;
    }

    /**
     * Inner class để lưu thống kê
     */
    public static class UserCTDTStats {
        private int totalSubjects;
        private int completedSubjects;
        private int pendingSubjects;
        private int totalCredits;
        private int completedCredits;
        private int bbCredits;
        private int tcCredits;

        // Getters and Setters
        public int getTotalSubjects() { return totalSubjects; }
        public void setTotalSubjects(int totalSubjects) { this.totalSubjects = totalSubjects; }

        public int getCompletedSubjects() { return completedSubjects; }
        public void setCompletedSubjects(int completedSubjects) { this.completedSubjects = completedSubjects; }

        public int getPendingSubjects() { return pendingSubjects; }
        public void setPendingSubjects(int pendingSubjects) { this.pendingSubjects = pendingSubjects; }

        public int getTotalCredits() { return totalCredits; }
        public void setTotalCredits(int totalCredits) { this.totalCredits = totalCredits; }

        public int getCompletedCredits() { return completedCredits; }
        public void setCompletedCredits(int completedCredits) { this.completedCredits = completedCredits; }

        public int getBbCredits() { return bbCredits; }
        public void setBbCredits(int bbCredits) { this.bbCredits = bbCredits; }

        public int getTcCredits() { return tcCredits; }
        public void setTcCredits(int tcCredits) { this.tcCredits = tcCredits; }
    }

    public List<UserCTDT> getUserCTDT(Long userId) {
        return userCTDTRepo.findByUserIdOrderByHocKyAsc(userId);
    }
}
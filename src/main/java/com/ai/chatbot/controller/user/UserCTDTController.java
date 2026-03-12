package com.ai.chatbot.controller.user;

import com.ai.chatbot.model.CTDT;
import com.ai.chatbot.model.User;
import com.ai.chatbot.model.UserCTDT;
import com.ai.chatbot.repository.CTDTRepository;
import com.ai.chatbot.repository.UserCTDTRepository;
import com.ai.chatbot.service.UserCTDTService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.ai.chatbot.service.ExcelService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import com.ai.chatbot.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/api/user")
public class UserCTDTController {
    @Autowired private UserCTDTService userCTDTService;
    @Autowired private UserCTDTRepository userCTDTRepo;
    @Autowired private ExcelService excelService;
    @Autowired private NotificationService notificationService;
    @Autowired private CTDTRepository ctdtRepo;

    // ============ INITIALIZE USER CTDT ============
    @PostMapping("/initialize-ctdt")
    @ResponseBody
    public ResponseEntity<?> initializeUserCTDT(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            String result = userCTDTService.initializeCTDTForUser(user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi khởi tạo CTĐT: " + e.getMessage());
        }
    }

    // ============ GET MY CTDT ============
    @GetMapping("/my-ctdt")
    @ResponseBody
    public ResponseEntity<?> getMyCTDT(HttpSession session,
                                       @RequestParam(required = false) Integer hocKy,
                                       @RequestParam(required = false) String loai,
                                       @RequestParam(required = false) Integer trangThai) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            List<UserCTDT> myCTDT = userCTDTService.filterUserCTDT(
                    user.getId(), hocKy, loai, trangThai);
            // Convert to response DTO
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (UserCTDT uctdt : myCTDT) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", uctdt.getId());
                item.put("ctdtId", uctdt.getCtdt() != null ? uctdt.getCtdt().getId() : null);
                item.put("maHocPhan", uctdt.getMaHocPhan());
                item.put("hocKy", uctdt.getHocKy());
                item.put("tenHocPhan", uctdt.getTenHocPhan());
                item.put("tinChi", uctdt.getTinChi());
                item.put("loai", uctdt.getLoai());
                item.put("nhomTC", uctdt.getNhomTC());
                item.put("nganh", uctdt.getNganh());
                item.put("chuyenNganh", uctdt.getChuyenNganh());
                item.put("nhomHocPhan", uctdt.getNhomHocPhan());
                item.put("trangThai", uctdt.getTrangThai());
                item.put("createdAt", uctdt.getCreatedAt());
                item.put("updatedAt", uctdt.getUpdatedAt());
                // THÊM HỌC PHẦN TIÊN QUYẾT
                if (uctdt.getCtdt() != null) {
                    // Fetch đầy đủ học phần tiên quyết với thông tin tên
                    CTDT fullCTDT = ctdtRepo.findById(uctdt.getCtdt().getId())
                            .orElse(null);
                    if (fullCTDT != null && fullCTDT.getHocPhanTienQuyet() != null) {
                        List<Map<String, Object>> tienQuyetList = new ArrayList<>();
                        List<String> tienQuyetNames = new ArrayList<>();
                        List<Long> tienQuyetIds = new ArrayList<>();
                        for (CTDT hp : fullCTDT.getHocPhanTienQuyet()) {
                            Map<String, Object> hpMap = new HashMap<>();
                            hpMap.put("id", hp.getId());
                            hpMap.put("maHocPhan", hp.getMaHocPhan());
                            hpMap.put("tenHocPhan", hp.getTenHocPhan());
                            hpMap.put("hocKy", hp.getHocKy());
                            hpMap.put("tinChi", hp.getTinChi());
                            tienQuyetList.add(hpMap);
                            tienQuyetNames.add(hp.getTenHocPhan());
                            tienQuyetIds.add(hp.getId());
                        }
                        item.put("hocPhanTienQuyet", tienQuyetList);
                        item.put("hocPhanTienQuyetNames", tienQuyetNames);
                        item.put("hocPhanTienQuyetIds", tienQuyetIds);
                    }
                }
                responseList.add(item);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy CTĐT: " + e.getMessage());
        }
    }

    // ============ UPDATE CTDT STATUS ============
    @PostMapping("/update-ctdt-status/{userCTDTId}")
    @ResponseBody
    public ResponseEntity<?> updateMyCTDTStatus(@PathVariable Long userCTDTId,
                                                @RequestParam Integer trangThai,
                                                HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            // Tìm UserCTDT theo ID
            Optional<UserCTDT> userCTDTOpt = userCTDTRepo.findById(userCTDTId);
            if (!userCTDTOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy học phần trong chương trình đào tạo của bạn");
            }

            UserCTDT userCTDT = userCTDTOpt.get();

            // Kiểm tra xem học phần này có thuộc về user không
            if (!userCTDT.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không có quyền thay đổi học phần này");
            }

            // Lưu trạng thái cũ
            Integer oldStatus = userCTDT.getTrangThai();

            // Nếu trạng thái không thay đổi, không làm gì cả
            if (oldStatus != null && oldStatus.equals(trangThai)) {
                return ResponseEntity.ok("Trạng thái không thay đổi");
            }

            // Lưu trạng thái mới trước
            userCTDT.setTrangThai(trangThai);
            userCTDT.setUpdatedAt(LocalDateTime.now());
            userCTDTRepo.save(userCTDT);

            // ============ KIỂM TRA HỌC PHẦN TIÊN QUYẾT SAU KHI LƯU ============
            // Chỉ kiểm tra khi vừa đánh dấu "Đã học" (trangThai = 0)
            if (trangThai == 0 && userCTDT.getCtdt() != null) {
                CTDT fullCtdt = ctdtRepo.findById(userCTDT.getCtdt().getId()).orElse(null);
                if (fullCtdt != null && fullCtdt.getHocPhanTienQuyet() != null
                        && !fullCtdt.getHocPhanTienQuyet().isEmpty()) {

                    // Map: ctdtId -> trangThai hiện tại của user
                    List<UserCTDT> allUserCTDT = userCTDTRepo.findByUserId(user.getId());
                    Map<Long, Integer> ctdtStatusMap = new HashMap<>();
                    for (UserCTDT uc : allUserCTDT) {
                        if (uc.getCtdt() != null) {
                            ctdtStatusMap.put(uc.getCtdt().getId(), uc.getTrangThai());
                        }
                    }

                    // Thu thập danh sách tiên quyết chưa học
                    List<Map<String, Object>> prerequisiteNotMet = new ArrayList<>();
                    for (CTDT prereq : fullCtdt.getHocPhanTienQuyet()) {
                        Integer prereqStatus = ctdtStatusMap.get(prereq.getId());
                        if (prereqStatus == null || prereqStatus != 0) {
                            Map<String, Object> prereqInfo = new HashMap<>();
                            prereqInfo.put("id", prereq.getId());
                            prereqInfo.put("maHocPhan", prereq.getMaHocPhan());
                            prereqInfo.put("tenHocPhan", prereq.getTenHocPhan());
                            prereqInfo.put("hocKy", prereq.getHocKy());
                            prereqInfo.put("tinChi", prereq.getTinChi());
                            prerequisiteNotMet.add(prereqInfo);
                        }
                    }

                    if (!prerequisiteNotMet.isEmpty()) {
                        // 1. Gửi thông báo DANGER vào hệ thống thông báo
                        try {
                            notificationService.createPrerequisiteViolationNotification(
                                    user, userCTDT, prerequisiteNotMet);
                            System.out.println("Prerequisite violation notification sent for: "
                                    + userCTDT.getTenHocPhan());
                        } catch (Exception notifEx) {
                            System.err.println("Error sending prereq notification: " + notifEx.getMessage());
                        }

                        // 2. Đặt lại trạng thái học phần B về "Chưa học" (trangThai = 1)
                        userCTDT.setTrangThai(1);
                        userCTDT.setUpdatedAt(LocalDateTime.now());
                        userCTDTRepo.save(userCTDT);
                        System.out.println("Reverted " + userCTDT.getTenHocPhan() + " to NOT_LEARNED");

                        // 3. Trả về response đặc biệt để frontend revert UI về "Chưa học"
                        Map<String, Object> revertResponse = new HashMap<>();
                        revertResponse.put("resultType", "REVERTED_TO_NOT_LEARNED");
                        revertResponse.put("subjectId", userCTDTId);
                        revertResponse.put("subjectName", userCTDT.getTenHocPhan());
                        revertResponse.put("revertedStatus", 1);
                        revertResponse.put("missingPrerequisites", prerequisiteNotMet);
                        revertResponse.put("message",
                                "Học phần \"" + userCTDT.getTenHocPhan()
                                        + "\" đã bị đặt lại về \"Chưa học\" vì chưa hoàn thành học phần tiên quyết.");
                        return ResponseEntity.ok(revertResponse);
                    }
                }
            }
            // ============ KẾT THÚC KIỂM TRA TIÊN QUYẾT ============

            // GỌI REAL-TIME NOTIFICATION CHECK
            try {
                notificationService.checkRealTimeNotifications(user.getId(), userCTDTId, trangThai);
                System.out.println("Real-time notification checked for user: " + user.getId());
            } catch (Exception e) {
                System.err.println("Error generating real-time notifications: " + e.getMessage());
            }

            String statusText = trangThai == 0 ? "đã học" : "chưa học";
            String oldStatusText = (oldStatus != null && oldStatus == 0) ? "đã học" : "chưa học";

            return ResponseEntity.ok("Đã thay đổi từ '" + oldStatusText + "' sang '" + statusText + "'");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    // ============ GET USER CTDT ============
    @GetMapping("/ctdt")
    @ResponseBody
    public ResponseEntity<?> getUserCTDT(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            // Lấy CTDT theo nganh và chuyên ngành của user
            String department = user.getDepartment();
            String major = user.getMajor();
            List<CTDT> userCTDT;
            if (department != null && !department.trim().isEmpty() &&
                    major != null && !major.trim().isEmpty()) {
                // Lấy môn học theo nganh/chuyên ngành + môn tự chọn chung
                userCTDT = ctdtRepo.findAll().stream()
                        .filter(c ->
                                (c.getNganh() == null || c.getNganh().isEmpty() || c.getNganh().equals(department)) &&
                                        (c.getChuyenNganh() == null || c.getChuyenNganh().isEmpty() ||
                                                c.getChuyenNganh().equals(major) || "TC".equals(c.getLoai()))
                        )
                        .collect(Collectors.toList());
            } else {
                // Nếu user chưa có nganh/chuyên ngành, lấy tất cả môn tự chọn
                userCTDT = ctdtRepo.findByLoai("TC");
            }
            return ResponseEntity.ok(userCTDT);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải CTĐT: " + e.getMessage());
        }
    }

    // ============ FILTER USER CTDT ============
    @GetMapping("/ctdt/filter")
    @ResponseBody
    public ResponseEntity<?> filterUserCTDT(
            @RequestParam(required = false) Integer hocKy,
            @RequestParam(required = false) Integer trangThai,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            // Lấy CTDT của user
            ResponseEntity<?> response = getUserCTDT(session);
            if (response.getStatusCode() != HttpStatus.OK || !(response.getBody() instanceof List)) {
                return ResponseEntity.ok(new ArrayList<CTDT>());
            }
            @SuppressWarnings("unchecked")
            List<CTDT> userCTDT = (List<CTDT>) response.getBody();
            List<CTDT> filteredList = userCTDT.stream()
                    .filter(c -> {
                        boolean matchHocKy = true;
                        if (hocKy != null) {
                            matchHocKy = c.getHocKy() != null && c.getHocKy().equals(hocKy);
                        }
                        return matchHocKy;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(filteredList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lọc CTĐT: " + e.getMessage());
        }
    }

    // ============ GET CTDT STATS ============
    @GetMapping("/ctdt-stats")
    @ResponseBody
    public ResponseEntity<?> getMyCTDTStats(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            UserCTDTService.UserCTDTStats stats = userCTDTService.getUserCTDTStats(user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("totalSubjects", stats.getTotalSubjects());
            response.put("completedSubjects", stats.getCompletedSubjects());
            response.put("pendingSubjects", stats.getPendingSubjects());
            response.put("totalCredits", stats.getTotalCredits());
            response.put("completedCredits", stats.getCompletedCredits());
            response.put("bbCredits", stats.getBbCredits());
            response.put("tcCredits", stats.getTcCredits());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // ============ CHECK IF USER HAS CTDT ============
    @GetMapping("/has-ctdt")
    @ResponseBody
    public ResponseEntity<?> hasCTDT(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            boolean hasCTDT = userCTDTService.hasUserCTDT(user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("hasCTDT", hasCTDT);
            response.put("userId", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra: " + e.getMessage());
        }
    }

    // ============ UPDATE CTDT STATUS (USER) ============
    @PostMapping("/ctdt/update-status/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCTDTStatus(
            @PathVariable Long id,
            @RequestParam Integer trangThai,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            Optional<CTDT> ctdtOpt = ctdtRepo.findById(id);
            if (!ctdtOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy học phần");
            }
            CTDT ctdt = ctdtOpt.get();
            // Kiểm tra xem user có quyền thay đổi học phần này không
            boolean hasPermission =
                    (ctdt.getNganh() == null || ctdt.getNganh().isEmpty() || ctdt.getNganh().equals(user.getDepartment())) &&
                            (ctdt.getChuyenNganh() == null || ctdt.getChuyenNganh().isEmpty() ||
                                    ctdt.getChuyenNganh().equals(user.getMajor()) || "TC".equals(ctdt.getLoai()));

            if (!hasPermission) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không có quyền thay đổi học phần này");
            }
            // Cập nhật trạng thái
            ctdt.setUpdatedAt(LocalDateTime.now());
            ctdtRepo.save(ctdt);
            String statusText = trangThai == 0 ? "đã học" : "chưa học";
            return ResponseEntity.ok("Đã đánh dấu học phần là " + statusText);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    // ============ EXPORT USER CTDT TO EXCEL WITH REAL-TIME DATA ============
    @GetMapping("/export-ctdt-excel-full")
    @ResponseBody
    public ResponseEntity<Resource> exportUserCTDTExcelFull(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Lấy CTDT của user với tất cả thông tin
            List<UserCTDT> ctdtList = userCTDTRepo.findByUserIdOrderByHocKyAsc(user.getId());

            // Fetch full CTDT details for prerequisites
            for (UserCTDT userCTDT : ctdtList) {
                if (userCTDT.getCtdt() != null && userCTDT.getCtdt().getId() != null) {
                    CTDT fullCTDT = ctdtRepo.findById(userCTDT.getCtdt().getId()).orElse(null);
                    userCTDT.setCtdt(fullCTDT);
                }
            }

            // Tạo file Excel với thông tin đầy đủ
            byte[] excelBytes = excelService.exportUserCTDTWithProgressToExcel(user, ctdtList);

            // Tạo filename
            String safeFileName = user.getFullName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("CTDT_%s_%s_%s.xlsx", user.getMssv(), safeFileName, timestamp);

            // Tạo resource
            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            // Log thông tin
            System.out.println("Exporting Full Excel for user: " + user.getMssv());
            System.out.println("Filename: " + filename);
            System.out.println("File size: " + excelBytes.length + " bytes");
            System.out.println("Number of courses: " + ctdtList.size());

            // Tạo response
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Export-Time", LocalDateTime.now().toString())
                    .header("X-User-MSSV", user.getMssv())
                    .contentLength(excelBytes.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ GET CTDT STATS CALCULATED ============
    @GetMapping("/ctdt-stats-calculated")
    @ResponseBody
    public ResponseEntity<?> getMyCTDTStatsCalculated(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            List<UserCTDT> userCTDTList = userCTDTRepo.findByUserId(user.getId());

            Map<String, Object> stats = calculateStatsForProgress(userCTDTList);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    private Map<String, Object> calculateStatsForProgress(List<UserCTDT> userCTDTList) {
        Map<String, Object> stats = new HashMap<>();

        if (userCTDTList == null || userCTDTList.isEmpty()) {
            stats.put("totalSubjects", 0);
            stats.put("completedSubjects", 0);
            stats.put("pendingSubjects", 0);
            stats.put("bbRequired", 0);
            stats.put("bbCompleted", 0);
            stats.put("tcRequiredTotal", 0);      // Tổng TC cần học của tất cả học kỳ
            stats.put("tcCompletedSimple", 0);    // Tổng TC đã học (đơn giản)
            return stats;
        }

        double bbRequired = 0;
        double bbCompleted = 0;
        double tcRequiredTotal = 0;    // Tổng TC cần học của tất cả học kỳ
        double tcCompletedSimple = 0;  // Tổng TC đã học (đơn giản)

        // 1. TÍNH BẮT BUỘC (BB) - theo quy tắc X/Y
        for (UserCTDT ctdt : userCTDTList) {
            if ("BB".equals(ctdt.getLoai())) {
                Integer tinChi = ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
                Integer trangThai = ctdt.getTrangThai() != null ? ctdt.getTrangThai() : 1;

                bbRequired += tinChi;
                if (trangThai == 0) {
                    bbCompleted += tinChi;
                }
            }
        }

        // 2. TÍNH TỰ CHỌN (TC) - theo logic mới
        // Nhóm theo học kỳ
        Map<Integer, List<UserCTDT>> tcByHocKy = new HashMap<>();
        for (UserCTDT ctdt : userCTDTList) {
            if ("TC".equals(ctdt.getLoai())) {
                Integer hocKy = ctdt.getHocKy() != null ? ctdt.getHocKy() : 0;
                if (!tcByHocKy.containsKey(hocKy)) {
                    tcByHocKy.put(hocKy, new ArrayList<>());
                }
                tcByHocKy.get(hocKy).add(ctdt);

                // Tính tổng TC đã học (đơn giản)
                Integer tinChi = ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
                Integer trangThai = ctdt.getTrangThai() != null ? ctdt.getTrangThai() : 1;
                if (trangThai == 0) {
                    tcCompletedSimple += tinChi;
                }
            }
        }

        // Tính cho từng học kỳ
        for (Map.Entry<Integer, List<UserCTDT>> entry : tcByHocKy.entrySet()) {
            List<UserCTDT> tcList = entry.getValue();

            // Nhóm theo nhómTC
            Map<String, List<UserCTDT>> tcGroups = new HashMap<>();
            List<UserCTDT> tcNoGroup = new ArrayList<>();

            for (UserCTDT ctdt : tcList) {
                String nhomTC = ctdt.getNhomTC() != null ? ctdt.getNhomTC().trim() : "";

                if (nhomTC.isEmpty()) {
                    tcNoGroup.add(ctdt);
                } else {
                    if (!tcGroups.containsKey(nhomTC)) {
                        tcGroups.put(nhomTC, new ArrayList<>());
                    }
                    tcGroups.get(nhomTC).add(ctdt);
                }
            }

            // 2.1. TC không nhóm: cộng toàn bộ tín chỉ
            for (UserCTDT ctdt : tcNoGroup) {
                tcRequiredTotal += ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
            }

            // 2.2. TC có nhóm: tính theo công thức (tổng tín chỉ nhóm) × (X/Y)
            for (Map.Entry<String, List<UserCTDT>> groupEntry : tcGroups.entrySet()) {
                String nhomTC = groupEntry.getKey();
                List<UserCTDT> group = groupEntry.getValue();

                // Tính tổng tín chỉ của nhóm
                double groupTotalCredits = 0;
                for (UserCTDT ctdt : group) {
                    groupTotalCredits += ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
                }

                // Parse X/Y
                String[] parts = nhomTC.split("/");

                if (parts.length == 2) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());

                        if (y > 0) {
                            // CÔNG THỨC: (tổng tín chỉ nhóm) × (X/Y)
                            double requiredForProgress = groupTotalCredits * ((double) x / y);
                            tcRequiredTotal += requiredForProgress;
                        } else {
                            // Nếu y = 0, tính bình thường
                            tcRequiredTotal += groupTotalCredits;
                        }
                    } catch (NumberFormatException e) {
                        // Tính bình thường
                        tcRequiredTotal += groupTotalCredits;
                    }
                } else {
                    // Không phải định dạng X/Y, tính bình thường
                    tcRequiredTotal += groupTotalCredits;
                }
            }
        }

        // Làm tròn
        bbRequired = Math.round(bbRequired * 10) / 10.0;
        bbCompleted = Math.round(bbCompleted * 10) / 10.0;
        tcRequiredTotal = Math.round(tcRequiredTotal * 10) / 10.0;
        tcCompletedSimple = Math.round(tcCompletedSimple * 10) / 10.0;

        int totalSubjects = userCTDTList.size();
        int completedSubjects = (int) userCTDTList.stream()
                .filter(c -> c.getTrangThai() != null && c.getTrangThai() == 0)
                .count();
        int pendingSubjects = totalSubjects - completedSubjects;

        stats.put("totalSubjects", totalSubjects);
        stats.put("completedSubjects", completedSubjects);
        stats.put("pendingSubjects", pendingSubjects);
        stats.put("bbRequired", bbRequired);
        stats.put("bbCompleted", bbCompleted);
        stats.put("tcRequiredTotal", tcRequiredTotal);    // Tổng TC cần học của tất cả học kỳ
        stats.put("tcCompletedSimple", tcCompletedSimple); // Tổng TC đã học (đơn giản)

        return stats;
    }
}
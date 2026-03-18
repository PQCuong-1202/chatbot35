package com.ai.chatbot.service;

import com.ai.chatbot.model.*;
import com.ai.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    // =====================================================================
    //  ⚙️ CẤU HÌNH CHẾ ĐỘ TEST
    //
    //  TEST_MODE = true  → Gửi gợi ý tiếp sau 5 PHÚT  (để kiểm thử nhanh)
    //  TEST_MODE = false → Gửi gợi ý tiếp sau 90 NGÀY (chế độ production)
    //
    //  Sau khi test xong, đổi lại thành FALSE trước khi deploy.
    // =====================================================================
    private static final boolean TEST_MODE = true;

    /** Thời gian chờ giữa 2 lần gợi ý (đơn vị: phút) */
    // TEST_MODE = true → dùng số bên trái dấu "?" là 2L là 2 phút
    // TEST_MODE = false → dùng số bên phải dấu ":" là ngày 90L bằng 90 ngày
    private static final long RECOMMENDATION_INTERVAL_MINUTES = TEST_MODE ? 1L : (90L * 24 * 60);
    /** Anti-spam: không gửi lại nếu đã có thông báo gợi ý trong khoảng thời gian này (phút) */
    private static final long RECOMMENDATION_SPAM_GUARD_MINUTES = TEST_MODE ? 1L : (72L * 60);

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private UserCTDTRepository userCTDTRepo;

    @Autowired
    private CTDTRepository ctdtRepo;

    @Autowired
    private UserRepository userRepo;

    // =====================================================================
    //  ENTRY POINT: gọi sau khi user thay đổi trạng thái 1 học phần
    // =====================================================================

    @Transactional
    public void checkRealTimeNotifications(Long userId, Long userCTDTId, Integer newStatus) {
        System.out.println("===== CHECKING REAL-TIME NOTIFICATIONS =====");
        System.out.println("User ID: " + userId + " | UserCTDT ID: " + userCTDTId + " | New status: " + newStatus);

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) { System.out.println("User not found!"); return; }

        List<UserCTDT> allUserCTDT = userCTDTRepo.findByUserIdOrderByHocKyAsc(userId);
        if (allUserCTDT.isEmpty()) { System.out.println("No CTDT found!"); return; }

        UserCTDT changedSubject = userCTDTRepo.findById(userCTDTId).orElse(null);
        if (changedSubject == null) { System.out.println("Changed subject not found!"); return; }

        System.out.println("Changed: " + changedSubject.getTenHocPhan() + " | HK" + changedSubject.getHocKy());

        Map<Integer, List<UserCTDT>> bySemester = allUserCTDT.stream()
                .collect(Collectors.groupingBy(s -> s.getHocKy() != null ? s.getHocKy() : 0));

        int changedSemester = changedSubject.getHocKy() != null ? changedSubject.getHocKy() : 0;

        checkSemesterCompletion(user, changedSemester, bySemester);
        checkMissingInPreviousSemesters(user, changedSemester, bySemester);
        checkExcessElectives(user, changedSemester, bySemester);

        if (newStatus != null && newStatus == 0) {
            checkPrerequisites(user, changedSubject, allUserCTDT);
        }

        checkOverallProgress(user, allUserCTDT, bySemester);

        System.out.println("===== FINISHED REAL-TIME CHECK =====\n");
    }

    // =====================================================================
    //  1. HOÀN THÀNH HỌC KỲ
    // =====================================================================

    private void checkSemesterCompletion(User user,
                                         int changedSemester,
                                         Map<Integer, List<UserCTDT>> bySemester) {
        List<UserCTDT> semSubjects = bySemester.get(changedSemester);
        if (semSubjects == null || semSubjects.isEmpty()) return;
        if (!isSemesterCompleted(semSubjects)) return;

        // Lấy tất cả học phần
        List<UserCTDT> all = bySemester.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());

        // TÍNH % TIẾN ĐỘ THEO ĐÚNG CÔNG THỨC CỦA PROGRESS BARS
        // Tính BB
        double bbRequired = 0;
        double bbCompleted = 0;

        // Tính TC theo công thức X/Y
        double tcRequired = 0;
        double tcCompletedSimple = 0;

        for (UserCTDT s : all) {
            Integer tinChi = s.getTinChi() != null ? s.getTinChi() : 0;
            Integer trangThai = s.getTrangThai() != null ? s.getTrangThai() : 1;

            if ("BB".equals(s.getLoai())) {
                bbRequired += tinChi;
                if (trangThai == 0) {
                    bbCompleted += tinChi;
                }
            } else if ("TC".equals(s.getLoai())) {
                // Tính TC đã học đơn giản
                if (trangThai == 0) {
                    tcCompletedSimple += tinChi;
                }
            }
        }

        // Tính TC required theo nhóm
        Map<String, List<UserCTDT>> tcGroups = new HashMap<>();
        List<UserCTDT> tcNoGroup = new ArrayList<>();

        for (UserCTDT s : all) {
            if ("TC".equals(s.getLoai())) {
                String nhomTC = s.getNhomTC();
                if (nhomTC != null && !nhomTC.trim().isEmpty()) {
                    tcGroups.computeIfAbsent(nhomTC.trim(), k -> new ArrayList<>()).add(s);
                } else {
                    tcNoGroup.add(s);
                }
            }
        }

        // TC không nhóm
        for (UserCTDT s : tcNoGroup) {
            tcRequired += s.getTinChi() != null ? s.getTinChi() : 0;
        }

        // TC có nhóm theo X/Y
        for (Map.Entry<String, List<UserCTDT>> entry : tcGroups.entrySet()) {
            String nhomTC = entry.getKey();
            List<UserCTDT> group = entry.getValue();

            double groupTotal = group.stream()
                    .mapToInt(gs -> gs.getTinChi() != null ? gs.getTinChi() : 0)
                    .sum();

            String[] parts = nhomTC.split("/");
            if (parts.length == 2) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    if (y > 0) {
                        tcRequired += groupTotal * ((double) x / y);
                    } else {
                        tcRequired += groupTotal;
                    }
                } catch (NumberFormatException e) {
                    tcRequired += groupTotal;
                }
            } else {
                tcRequired += groupTotal;
            }
        }

        // Tổng hợp
        double totalRequired = bbRequired + tcRequired;
        double totalCompleted = bbCompleted + tcCompletedSimple;

        double progress = totalRequired > 0 ? (totalCompleted * 100.0) / totalRequired : 0;

        // Log để debug
        System.out.println("===== TÍNH TIẾN ĐỘ CHO THÔNG BÁO =====");
        System.out.println("BB: " + bbCompleted + "/" + bbRequired);
        System.out.println("TC: " + tcCompletedSimple + "/" + tcRequired);
        System.out.println("TỔNG: " + totalCompleted + "/" + totalRequired);
        System.out.println("TIẾN ĐỘ: " + String.format("%.1f", progress) + "%");

        String titleKey = "HOÀN THÀNH HK" + changedSemester;
        if (!hasRecentNotification(user.getId(), titleKey, 24 * 60)) {
            createSemesterCompletedNotification(user, changedSemester, progress);
            System.out.println("✅ Notification: semester completed HK" + changedSemester);
        }
    }

    private boolean isSemesterCompleted(List<UserCTDT> subjects) {
        List<UserCTDT> bb = subjects.stream().filter(s -> "BB".equals(s.getLoai())).collect(Collectors.toList());
        List<UserCTDT> tc = subjects.stream().filter(s -> "TC".equals(s.getLoai())).collect(Collectors.toList());

        boolean allBBDone = bb.stream().allMatch(s -> Integer.valueOf(0).equals(s.getTrangThai()));

        double required   = calculateRequiredTC(tc);
        double completedC = tc.stream()
                .filter(s -> Integer.valueOf(0).equals(s.getTrangThai()))
                .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();

        return allBBDone && (completedC >= required - 0.1);
    }

    // =====================================================================
    //  2. HỌC THIẾU Ở HỌC KỲ TRƯỚC
    // =====================================================================

    private void checkMissingInPreviousSemesters(User user,
                                                 int changedSemester,
                                                 Map<Integer, List<UserCTDT>> bySemester) {
        if (changedSemester <= 1) return;

        for (int hk = 1; hk < changedSemester; hk++) {
            List<UserCTDT> semSubjects = bySemester.get(hk);
            if (semSubjects == null) continue;

            List<UserCTDT> missingBB = semSubjects.stream()
                    .filter(s -> "BB".equals(s.getLoai()) && Integer.valueOf(1).equals(s.getTrangThai()))
                    .collect(Collectors.toList());

            List<UserCTDT> tcList    = semSubjects.stream().filter(s -> "TC".equals(s.getLoai())).collect(Collectors.toList());
            double requiredTC  = calculateRequiredTC(tcList);
            double completedTC = tcList.stream()
                    .filter(s -> Integer.valueOf(0).equals(s.getTrangThai()))
                    .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
            double missingTC   = requiredTC - completedTC;

            if (!missingBB.isEmpty() || missingTC > 0.1) {
                String titleKey = "HỌC THIẾU HK" + hk;
                if (!hasRecentNotification(user.getId(), titleKey, 12 * 60)) {
                    createMissingSubjectsNotification(user, hk, changedSemester, missingBB, missingTC);
                    System.out.println("⚠️ Notification: missing subjects in HK" + hk);
                }
            }
        }
    }

    // =====================================================================
    //  3. HỌC THỪA TC
    // =====================================================================

    private void checkExcessElectives(User user,
                                      int changedSemester,
                                      Map<Integer, List<UserCTDT>> bySemester) {
        List<UserCTDT> semSubjects = bySemester.get(changedSemester);
        if (semSubjects == null) return;

        List<UserCTDT> tc = semSubjects.stream().filter(s -> "TC".equals(s.getLoai())).collect(Collectors.toList());
        if (tc.isEmpty()) return;

        double required  = calculateRequiredTC(tc);
        double completed = tc.stream()
                .filter(s -> Integer.valueOf(0).equals(s.getTrangThai()))
                .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
        double excess    = completed - required;

        if (excess > 0.1) {
            String titleKey = "HỌC THỪA HK" + changedSemester;
            if (!hasRecentNotification(user.getId(), titleKey, 6 * 60)) {
                createExcessElectivesNotification(user, changedSemester, excess);
                System.out.println("🟡 Notification: excess electives in HK" + changedSemester);
            }
        }
    }

    // =====================================================================
    //  4. ĐIỀU KIỆN TIÊN QUYẾT
    // =====================================================================

    private void checkPrerequisites(User user, UserCTDT changedSubject, List<UserCTDT> allUserCTDT) {
        if (changedSubject.getCtdt() == null) return;
        CTDT fullCTDT = ctdtRepo.findById(changedSubject.getCtdt().getId()).orElse(null);
        if (fullCTDT == null) return;

        List<CTDT> prereqs = fullCTDT.getHocPhanTienQuyet();
        if (prereqs == null || prereqs.isEmpty()) return;

        Set<Long> learnedIds = allUserCTDT.stream()
                .filter(s -> Integer.valueOf(0).equals(s.getTrangThai()))
                .map(s -> s.getCtdt() != null ? s.getCtdt().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> missing = prereqs.stream()
                .filter(p -> !learnedIds.contains(p.getId()))
                .map(CTDT::getTenHocPhan)
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            String subjectName = changedSubject.getTenHocPhan();
            String titleKey    = "CHƯA ĐỦ ĐIỀU KIỆN: " + subjectName;
            if (!hasRecentNotification(user.getId(), titleKey, 60)) {
                createPrerequisiteWarning(user, changedSubject, missing);
                System.out.println("🚫 Notification: missing prerequisites for " + subjectName);
            }
        }
    }

    // =====================================================================
    //  5. TIẾN ĐỘ TỔNG THỂ
    // =====================================================================

    private void checkOverallProgress(User user,
                                      List<UserCTDT> allUserCTDT,
                                      Map<Integer, List<UserCTDT>> bySemester) {
        if (allUserCTDT.isEmpty()) return;

        int total     = allUserCTDT.size();
        int completed = (int) allUserCTDT.stream()
                .filter(s -> Integer.valueOf(0).equals(s.getTrangThai())).count();
        double progress = total > 0 ? (completed * 100.0) / total : 0;

        int maxSemester = bySemester.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        int highestActiveSemester = bySemester.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(s -> Integer.valueOf(0).equals(s.getTrangThai())))
                .mapToInt(Map.Entry::getKey).max().orElse(0);

        boolean isInFinalPhase = highestActiveSemester >= 8 || highestActiveSemester >= maxSemester;

        if (isInFinalPhase && progress < 80) {
            String titleKey = "TRỄ TIẾN ĐỘ";
            if (!hasRecentNotification(user.getId(), titleKey, 72 * 60)) {
                createDelayWarning(user, highestActiveSemester, maxSemester, progress);
                System.out.println("⏰ Notification: behind schedule at HK"
                        + highestActiveSemester + " — " + String.format("%.1f", progress) + "%");
            }
        }
    }

    // =====================================================================
    //  6. GỢI Ý MÔN HỌC THEO HỌC KỲ
    //
    //  TRƯỜNG HỢP A — Chưa có thông báo "GỢI Ý HỌC KỲ" nào:
    //    → Gửi ngay gợi ý toàn bộ môn HK1.
    //
    //  TRƯỜNG HỢP B — Đã có, nhưng đã qua đủ thời gian chờ:
    //    (TEST_MODE=true  → 1 phút)
    //    (TEST_MODE=false → 90 ngày)
    //    → Parse HK đã gợi ý cuối, thu thập môn còn thiếu (carry-over),
    //      lấy toàn bộ môn HK tiếp theo, gửi gợi ý mới.
    //
    //  Anti-spam: bỏ qua nếu vừa gợi ý chưa đủ thời gian chờ.
    // =====================================================================

    @Transactional
    public void checkCourseRecommendations(Long userId) {
        System.out.println("===== CHECKING COURSE RECOMMENDATIONS (TEST_MODE=" + TEST_MODE + ") =====");

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        List<UserCTDT> allUserCTDT = userCTDTRepo.findByUserIdOrderByHocKyAsc(userId);
        if (allUserCTDT.isEmpty()) return;

        // Sinh viên đã hoàn thành tất cả → không cần gợi ý nữa
        boolean allCompleted = allUserCTDT.stream()
                .allMatch(s -> Integer.valueOf(0).equals(s.getTrangThai()));
        if (allCompleted) {
            System.out.println("💡 All subjects completed — skipping recommendations.");
            return;
        }

        // Nhóm theo học kỳ (TreeMap → tự sắp xếp tăng dần)
        Map<Integer, List<UserCTDT>> bySemester = allUserCTDT.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHocKy() != null ? s.getHocKy() : 0,
                        TreeMap::new,
                        Collectors.toList()
                ));
        List<Integer> sortedSemesters = new ArrayList<>(bySemester.keySet());
        if (sortedSemesters.isEmpty()) return;

        // Anti-spam theo RECOMMENDATION_SPAM_GUARD_MINUTES
        if (hasRecentNotification(userId, "GỢI Ý HỌC KỲ", RECOMMENDATION_SPAM_GUARD_MINUTES)) {
            System.out.println("💡 Recommendation sent within spam-guard window — skipping.");
            return;
        }

        // Tìm thông báo gợi ý MỚI NHẤT
        Optional<Notification> latestSuggestion = findLatestCourseRecommendation(userId);

        if (!latestSuggestion.isPresent()) {
            // ── TRƯỜNG HỢP A: Lần đầu ───────────────────────────────────────
            int firstHK = sortedSemesters.get(0);
            List<UserCTDT> hk1Subjects = bySemester.get(firstHK);

            System.out.println("💡 First-time → HK" + firstHK + " (" + hk1Subjects.size() + " subjects)");
            createCourseRecommendationNotification(user, firstHK, hk1Subjects, Collections.emptyList());

        } else {
            // ── TRƯỜNG HỢP B: Kiểm tra thời gian chờ ───────────────────────
            LocalDateTime lastTime = latestSuggestion.get().getCreatedAt();
            LocalDateTime nextAllowed = lastTime.plusMinutes(RECOMMENDATION_INTERVAL_MINUTES);

            if (LocalDateTime.now().isBefore(nextAllowed)) {
                long minutesLeft = java.time.Duration.between(LocalDateTime.now(), nextAllowed).toMinutes();
                System.out.println("💡 Next recommendation allowed in " + minutesLeft + " minute(s) — skipping.");
                return;
            }

            // Parse HK đã gợi ý lần cuối
            int lastHK = extractSemesterFromTitle(latestSuggestion.get().getTitle());
            if (lastHK <= 0) lastHK = sortedSemesters.get(0);

            int nextHK = lastHK + 1;
            if (!bySemester.containsKey(nextHK)) {
                System.out.println("💡 No HK" + nextHK + " found — all semesters already recommended.");
                return;
            }

            // Thu thập môn còn thiếu từ HK1..lastHK
            List<UserCTDT> carryOver     = collectMissingSubjects(bySemester, lastHK);
            List<UserCTDT> nextHKSubjects = new ArrayList<>(bySemester.get(nextHK));

            System.out.println("💡 Periodic → HK" + nextHK
                    + " (" + nextHKSubjects.size() + " subjects) + carry-over: " + carryOver.size());

            createCourseRecommendationNotification(user, nextHK, nextHKSubjects, carryOver);
        }

        System.out.println("===== COURSE RECOMMENDATIONS DONE =====\n");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers riêng cho tính năng gợi ý môn học
    // ─────────────────────────────────────────────────────────────────────

    /** Tìm thông báo gợi ý MỚI NHẤT (nhận diện qua "GỢI Ý HỌC KỲ" trong tiêu đề). */
    private Optional<Notification> findLatestCourseRecommendation(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> n.getTitle() != null && n.getTitle().contains("GỢI Ý HỌC KỲ"))
                .findFirst();
    }

    /**
     * Trích xuất số HK từ tiêu đề.
     * VD: "💡 GỢI Ý HỌC KỲ 3 (Kèm môn còn thiếu)" → 3
     */
    private int extractSemesterFromTitle(String title) {
        if (title == null) return -1;
        try {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("HỌC KỲ (\\d+)").matcher(title);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) { }
        return -1;
    }

    /**
     * Thu thập môn còn THIẾU từ HK1..targetHK (inclusive).
     * BB chưa học + TC chưa đủ tín chỉ yêu cầu.
     */
    private List<UserCTDT> collectMissingSubjects(Map<Integer, List<UserCTDT>> bySemester, int targetHK) {
        List<UserCTDT> missing = new ArrayList<>();

        for (int hk = 1; hk <= targetHK; hk++) {
            List<UserCTDT> semSubjects = bySemester.get(hk);
            if (semSubjects == null) continue;

            semSubjects.stream()
                    .filter(s -> "BB".equals(s.getLoai()) && Integer.valueOf(1).equals(s.getTrangThai()))
                    .forEach(missing::add);

            List<UserCTDT> tcList = semSubjects.stream()
                    .filter(s -> "TC".equals(s.getLoai())).collect(Collectors.toList());
            double requiredTC  = calculateRequiredTC(tcList);
            double completedTC = tcList.stream()
                    .filter(s -> Integer.valueOf(0).equals(s.getTrangThai()))
                    .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();

            if (requiredTC - completedTC > 0.1) {
                tcList.stream()
                        .filter(s -> Integer.valueOf(1).equals(s.getTrangThai()))
                        .forEach(missing::add);
            }
        }

        return missing;
    }

    // =====================================================================
    //  TÍNH TÍN CHỈ TC YÊU CẦU (công thức X/Y)
    // =====================================================================

    private double calculateRequiredTC(List<UserCTDT> tcSubjects) {
        if (tcSubjects == null || tcSubjects.isEmpty()) return 0;

        double required = 0;
        Map<String, List<UserCTDT>> groups  = new LinkedHashMap<>();
        List<UserCTDT>              noGroup = new ArrayList<>();

        for (UserCTDT s : tcSubjects) {
            String nhom = s.getNhomTC();
            if (nhom != null && !nhom.trim().isEmpty()) {
                groups.computeIfAbsent(nhom.trim(), k -> new ArrayList<>()).add(s);
            } else {
                noGroup.add(s);
            }
        }

        for (UserCTDT s : noGroup) {
            required += s.getTinChi() != null ? s.getTinChi() : 0;
        }

        for (Map.Entry<String, List<UserCTDT>> e : groups.entrySet()) {
            String nhom  = e.getKey();
            double total = e.getValue().stream()
                    .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
            String[] parts = nhom.split("/");
            if (parts.length == 2) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    required += (y > 0) ? total * ((double) x / y) : total;
                } catch (NumberFormatException ex) {
                    required += total;
                }
            } else {
                required += total;
            }
        }
        return required;
    }

    // =====================================================================
    //  HELPER: kiểm tra đã có thông báo tương tự trong N phút gần đây chưa
    //  (đơn vị đã đổi sang PHÚT thay vì giờ để nhất quán với TEST_MODE)
    // =====================================================================

    private boolean hasRecentNotification(Long userId, String titleContains, long withinMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(withinMinutes);
        List<Notification> recent = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return recent.stream()
                .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isAfter(cutoff))
                .anyMatch(n -> n.getTitle() != null && n.getTitle().contains(titleContains));
    }

    // =====================================================================
    //  TẠO THÔNG BÁO
    // =====================================================================

    /** 1. Thông báo hoàn thành học kỳ → SUCCESS */
    private void createSemesterCompletedNotification(User user, int semester, double overallProgress) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType("SUCCESS");
        n.setTitle("✅ HOÀN THÀNH HK" + semester);
        n.setMessage(String.format(
                "Chúc mừng! Bạn đã hoàn thành học kỳ %d.\n\n" +
                        "📊 Tiến độ toàn khoá: %.1f%%\n\n" +
                        "Hãy tiếp tục với học kỳ tiếp theo!",
                semester, overallProgress));
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + semester);
        n.setExpiresAt(LocalDateTime.now().plusDays(30));
        notificationRepo.save(n);
    }

    /** 2. Thông báo học thiếu → WARNING */
    private void createMissingSubjectsNotification(User user, int missingSemester,
                                                   int currentSemester,
                                                   List<UserCTDT> missingBB,
                                                   double missingTC) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("Bạn đang học kỳ %d nhưng HỌC KỲ %d vẫn còn thiếu:\n\n",
                currentSemester, missingSemester));

        if (!missingBB.isEmpty()) {
            msg.append("📘 MÔN BẮT BUỘC CHƯA HỌC:\n");
            for (UserCTDT s : missingBB) {
                msg.append(String.format("  • %s (%d TC)\n",
                        s.getTenHocPhan(), s.getTinChi() != null ? s.getTinChi() : 0));
            }
            msg.append("\n");
        }
        if (missingTC > 0.1) {
            msg.append(String.format("📗 TỰ CHỌN CÒN THIẾU: %.1f TC\n\n", missingTC));
        }
        msg.append("💡 Hoàn thành các môn này để đảm bảo tiến độ!");

        Notification n = new Notification();
        n.setUser(user);
        n.setType("WARNING");
        n.setTitle("⚠️ HỌC THIẾU HK" + missingSemester);
        n.setMessage(msg.toString());
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + missingSemester);
        n.setExpiresAt(LocalDateTime.now().plusDays(14));
        notificationRepo.save(n);
    }

    /** 3. Thông báo học thừa TC → WARNING */
    private void createExcessElectivesNotification(User user, int semester, double excessTC) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType("WARNING");
        n.setTitle("🟡 HỌC THỪA TC HK" + semester);
        n.setMessage(String.format(
                "Bạn đã đăng ký/học THỪA %.1f TC tự chọn ở học kỳ %d.\n\n" +
                        "📌 Lưu ý: Tín chỉ thừa không được tính vào yêu cầu tốt nghiệp.\n" +
                        "💡 Cân nhắc tập trung hoàn thành các môn bắt buộc còn lại.",
                excessTC, semester));
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + semester);
        n.setExpiresAt(LocalDateTime.now().plusDays(14));
        notificationRepo.save(n);
    }

    /** 4. Cảnh báo tiên quyết → DANGER */
    private void createPrerequisiteWarning(User user, UserCTDT subject, List<String> missingPrereqs) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format(
                "Bạn vừa đánh dấu đã học \"%s\" nhưng CHƯA hoàn thành môn tiên quyết:\n\n",
                subject.getTenHocPhan()));
        for (String prereq : missingPrereqs) {
            msg.append(String.format("  🔴 %s\n", prereq));
        }
        msg.append(String.format("\n⚠️ Vui lòng học các môn tiên quyết trước khi học \"%s\".",
                subject.getTenHocPhan()));

        Notification n = new Notification();
        n.setUser(user);
        n.setType("DANGER");
        n.setTitle("🚫 CHƯA ĐỦ ĐIỀU KIỆN: " + subject.getTenHocPhan());
        n.setMessage(msg.toString());
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + subject.getHocKy());
        n.setExpiresAt(LocalDateTime.now().plusDays(14));
        notificationRepo.save(n);
    }

    /** 5. Cảnh báo trễ tiến độ → DANGER */
    private void createDelayWarning(User user, int currentSemester, int maxSemester, double progress) {
        int remaining = maxSemester - currentSemester;
        String remainingText = remaining <= 0
                ? "Bạn đang ở học kỳ cuối"
                : String.format("Bạn đang ở học kỳ %d, còn %d học kỳ nữa", currentSemester, remaining);

        Notification n = new Notification();
        n.setUser(user);
        n.setType("DANGER");
        n.setTitle("⏰ CẢNH BÁO TRỄ TIẾN ĐỘ");
        n.setMessage(String.format(
                "%s nhưng tiến độ toàn khoá chỉ đạt %.1f%%.\n\n" +
                        "⚠️ Nguy cơ không tốt nghiệp đúng hạn!\n\n" +
                        "💡 Hãy tập trung hoàn thành các môn còn lại càng sớm càng tốt.",
                remainingText, progress));
        n.setActionUrl("/profile?tab=ctdt");
        n.setExpiresAt(LocalDateTime.now().plusDays(30));
        notificationRepo.save(n);
    }

    /**
     * 6. Tạo thông báo GỢI Ý MÔN HỌC → INFO.
     *
     * Cấu trúc:
     *   [A] Môn còn thiếu từ HK trước (carry-over) — nếu có
     *   [B] Danh sách môn targetHK, phân nhóm BB / TC (có nhóm X/Y)
     *   [C] Thống kê + lời khuyên
     *
     * Tiêu đề luôn có dạng "GỢI Ý HỌC KỲ <số>" để hệ thống parse được.
     */
    private void createCourseRecommendationNotification(User user,
                                                        int targetHK,
                                                        List<UserCTDT> semesterSubjects,
                                                        List<UserCTDT> carryOverSubjects) {
        StringBuilder msg = new StringBuilder();

        // ── [A] Carry-over ────────────────────────────────────────────────
        if (!carryOverSubjects.isEmpty()) {
            int carryCredits = carryOverSubjects.stream()
                    .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();

            msg.append(String.format(
                    "⚠️ BẠN CÒN THIẾU %d MÔN (%d TC) TỪ CÁC HỌC KỲ TRƯỚC — Cần hoàn thành sớm!\n",
                    carryOverSubjects.size(), carryCredits));

            Map<Integer, List<UserCTDT>> carryByHK = new TreeMap<>();
            for (UserCTDT s : carryOverSubjects) {
                int hk = s.getHocKy() != null ? s.getHocKy() : 0;
                carryByHK.computeIfAbsent(hk, k -> new ArrayList<>()).add(s);
            }

            for (Map.Entry<Integer, List<UserCTDT>> entry : carryByHK.entrySet()) {
                msg.append(String.format("\n  📌 Học kỳ %d:\n", entry.getKey()));
                for (UserCTDT s : entry.getValue()) {
                    String loaiLabel = "BB".equals(s.getLoai()) ? "Bắt buộc" : "Tự chọn";
                    msg.append(String.format("    🔶 %s — %d TC (%s)\n",
                            s.getTenHocPhan(),
                            s.getTinChi() != null ? s.getTinChi() : 0,
                            loaiLabel));
                }
            }
            msg.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        }

        // ── [B] Môn mới của targetHK ──────────────────────────────────────
        if (!semesterSubjects.isEmpty()) {
            int semCredits = semesterSubjects.stream()
                    .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();

            if (carryOverSubjects.isEmpty()) {
                msg.append(String.format(
                        "Đây là danh sách học phần gợi ý cho Học kỳ %d của bạn:\n\n", targetHK));
            } else {
                msg.append(String.format(
                        "📚 HỌC PHẦN MỚI — HỌC KỲ %d (%d TC):\n\n", targetHK, semCredits));
            }

            List<UserCTDT> bbList = semesterSubjects.stream()
                    .filter(s -> "BB".equals(s.getLoai())).collect(Collectors.toList());
            List<UserCTDT> tcList = semesterSubjects.stream()
                    .filter(s -> "TC".equals(s.getLoai())).collect(Collectors.toList());

            if (!bbList.isEmpty()) {
                int bbCredits = bbList.stream()
                        .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
                msg.append(String.format("📘 MÔN BẮT BUỘC — %d TC:\n", bbCredits));
                for (UserCTDT s : bbList) {
                    String done = Integer.valueOf(0).equals(s.getTrangThai()) ? " ✓" : "";
                    msg.append(String.format("  ✦ %s — %d TC%s\n",
                            s.getTenHocPhan(), s.getTinChi() != null ? s.getTinChi() : 0, done));
                }
                msg.append("\n");
            }

            if (!tcList.isEmpty()) {
                double tcRequired = calculateRequiredTC(tcList);
                int tcTotalRaw    = tcList.stream()
                        .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
                msg.append(String.format("📗 MÔN TỰ CHỌN — Cần chọn đủ %.0f/%d TC:\n",
                        tcRequired, tcTotalRaw));

                Map<String, List<UserCTDT>> byNhom = new LinkedHashMap<>();
                List<UserCTDT> noNhom = new ArrayList<>();
                for (UserCTDT s : tcList) {
                    String nhom = s.getNhomTC();
                    if (nhom != null && !nhom.trim().isEmpty()) {
                        byNhom.computeIfAbsent(nhom.trim(), k -> new ArrayList<>()).add(s);
                    } else {
                        noNhom.add(s);
                    }
                }

                for (UserCTDT s : noNhom) {
                    String done = Integer.valueOf(0).equals(s.getTrangThai()) ? " ✓" : "";
                    msg.append(String.format("  ✦ %s — %d TC%s\n",
                            s.getTenHocPhan(), s.getTinChi() != null ? s.getTinChi() : 0, done));
                }

                for (Map.Entry<String, List<UserCTDT>> e : byNhom.entrySet()) {
                    int nhomTotal = e.getValue().stream()
                            .mapToInt(s -> s.getTinChi() != null ? s.getTinChi() : 0).sum();
                    msg.append(String.format("  🔹 Nhóm %s (tổng %d TC, chọn theo tỉ lệ nhóm):\n",
                            e.getKey(), nhomTotal));
                    for (UserCTDT s : e.getValue()) {
                        String done = Integer.valueOf(0).equals(s.getTrangThai()) ? " ✓" : "";
                        msg.append(String.format("    ✦ %s — %d TC%s\n",
                                s.getTenHocPhan(), s.getTinChi() != null ? s.getTinChi() : 0, done));
                    }
                }
                msg.append("\n");
            }

            long doneCount = semesterSubjects.stream()
                    .filter(s -> Integer.valueOf(0).equals(s.getTrangThai())).count();
            msg.append(String.format("📊 Học kỳ %d: %d học phần — %d TC",
                    targetHK, semesterSubjects.size(), semCredits));
            if (doneCount > 0) {
                msg.append(String.format(" (đã hoàn thành: %d/%d)", doneCount, semesterSubjects.size()));
            }
            msg.append("\n");
        }

        // ── [C] Lời khuyên ────────────────────────────────────────────────
        msg.append("\n");
        if (!carryOverSubjects.isEmpty()) {
            msg.append("💡 Hãy dành thời gian hoàn thành các môn còn thiếu từ học kỳ trước\n");
            msg.append("   để bảo đảm tiến độ tốt nghiệp đúng hạn!");
        } else {
            msg.append("💡 Đăng ký môn học sớm để đảm bảo có suất trong lớp học phần.\n");
            msg.append("   Chúc bạn học tốt!");
        }

        // Thêm nhãn TEST_MODE vào tiêu đề để dễ phân biệt khi test
        String testLabel = TEST_MODE ? " " : "";
        String title = "💡 GỢI Ý HỌC KỲ " + targetHK
                + (carryOverSubjects.isEmpty() ? "" : " (Kèm môn còn thiếu)")
                + testLabel;

        Notification n = new Notification();
        n.setUser(user);
        n.setType("INFO");
        n.setTitle(title);
        n.setMessage(msg.toString());
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + targetHK);
        // Thời hạn: TEST_MODE → 1 ngày, Production → 90 ngày
        n.setExpiresAt(TEST_MODE
                ? LocalDateTime.now().plusDays(1)
                : LocalDateTime.now().plusDays(90));
        notificationRepo.save(n);

        System.out.println("💡 Saved: \"" + title + "\"");
    }

    // =====================================================================
    //  PUBLIC METHODS (gọi từ Controller)
    // =====================================================================

    @Transactional
    @SuppressWarnings("unchecked")
    public void createPrerequisiteViolationNotification(
            User user,
            UserCTDT subject,
            java.util.List<java.util.Map<String, Object>> missingPrerequisites) {

        StringBuilder msg = new StringBuilder();
        msg.append(String.format(
                "Bạn vừa đánh dấu \"Đã học\" môn \"%s\" nhưng chưa hoàn thành học phần tiên quyết.\n\n",
                subject.getTenHocPhan()));
        msg.append("📋 HỌC PHẦN TIÊN QUYẾT CHƯA HỌC:\n");

        for (java.util.Map<String, Object> prereq : missingPrerequisites) {
            String ma  = prereq.get("maHocPhan") != null ? prereq.get("maHocPhan").toString() : "";
            String ten = prereq.get("tenHocPhan") != null ? prereq.get("tenHocPhan").toString() : "";
            Object tc  = prereq.get("tinChi");
            msg.append(String.format("  🔴 %s%s (%s TC)\n",
                    ma.isEmpty() ? "" : "[" + ma + "] ", ten,
                    tc != null ? tc.toString() : "?"));
        }

        msg.append("\n⚠️ Trạng thái môn \"").append(subject.getTenHocPhan())
                .append("\" đã được tự động đặt lại về \"Chưa học\".\n");
        msg.append("💡 Hãy hoàn thành các môn tiên quyết trước rồi cập nhật lại.");

        Notification n = new Notification();
        n.setUser(user);
        n.setType("DANGER");
        n.setTitle("🚫 CHƯA ĐỦ ĐIỀU KIỆN: " + subject.getTenHocPhan());
        n.setMessage(msg.toString());
        n.setActionUrl("/profile?tab=ctdt&hocKy=" + subject.getHocKy());
        n.setExpiresAt(LocalDateTime.now().plusDays(14));
        notificationRepo.save(n);

        System.out.println("✅ Created prerequisite violation notification for: " + subject.getTenHocPhan());
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findActiveNotificationsByUser(userId, LocalDateTime.now());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                notificationRepo.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepo.markAllAsReadByUser(userId);
    }

    public int getUnreadCount(Long userId) {
        return notificationRepo.countUnreadNotifications(userId);
    }

    @Transactional
    public void deleteExpiredNotifications(Long userId) {
        notificationRepo.deleteExpiredNotifications(userId, LocalDateTime.now());
    }

    /**
     * Kiểm tra tổng thể thủ công (nút "Kiểm tra thông báo mới").
     * Bao gồm gợi ý môn học theo chu kỳ.
     */

    // =====================================================================
    //  GỢI Ý MÔN HỌC TỰ ĐỘNG — TÁCH BIỆT, KHÔNG ĐỤNG CODE CŨ
    //
    //  Trigger: chỉ dựa vào THỜI GIAN (RECOMMENDATION_INTERVAL_MINUTES)
    //  - HK1 : gợi ý ngay lần đầu, không check trạng thái
    //  - HK2+: sau đủ thời gian kể từ lần gợi ý trước → gợi ý HK tiếp
    //
    //  Nội dung: khi tạo thông báo HK tiếp, đọc trạng thái hiện tại
    //  để biết môn nào HK trước còn thiếu → kèm vào thông báo
    // =====================================================================

    @Transactional
    public void sendCourseRecommendation(Long userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        List<UserCTDT> all = userCTDTRepo.findByUserIdOrderByHocKyAsc(userId);
        if (all.isEmpty()) return;

        Map<Integer, List<UserCTDT>> bySemester = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHocKy() != null ? s.getHocKy() : 0,
                        TreeMap::new, Collectors.toList()));
        List<Integer> sortedHK = new ArrayList<>(bySemester.keySet());
        if (sortedHK.isEmpty()) return;

        Optional<Notification> latest = findLatestCourseRecommendation(userId);

        // ── HK1: chưa có gợi ý nào → gợi ý ngay, không check trạng thái ──
        if (!latest.isPresent()) {
            int firstHK = sortedHK.get(0);
            System.out.println("[RECOMMEND] First time → HK" + firstHK);
            createCourseRecommendationNotification(
                    user, firstHK, bySemester.get(firstHK), Collections.emptyList());
            return;
        }

        // ── HK2+: chỉ trigger theo thời gian ─────────────────────────────
        LocalDateTime lastTime    = latest.get().getCreatedAt();
        LocalDateTime nextAllowed = lastTime.plusMinutes(RECOMMENDATION_INTERVAL_MINUTES);
        if (LocalDateTime.now().isBefore(nextAllowed)) {
            long left = java.time.Duration.between(LocalDateTime.now(), nextAllowed).toMinutes();
            System.out.println("[RECOMMEND] Not yet — " + left + " minute(s) remaining.");
            return;
        }

        // Xác định HK tiếp theo cần gợi ý
        int lastHK = extractSemesterFromTitle(latest.get().getTitle());
        if (lastHK <= 0) lastHK = sortedHK.get(0);
        int nextHK = lastHK + 1;

        if (!bySemester.containsKey(nextHK)) {
            System.out.println("[RECOMMEND] No HK" + nextHK + " — all semesters covered.");
            return;
        }

        // Đọc trạng thái hiện tại để lấy môn còn thiếu ở các HK trước
        // (chỉ dùng để làm nội dung thông báo, không phải điều kiện trigger)
        List<UserCTDT> missing = collectMissingSubjects(bySemester, lastHK);

        System.out.println("[RECOMMEND] Time elapsed → HK" + nextHK
                + " | missing carry-over: " + missing.size());
        createCourseRecommendationNotification(
                user, nextHK, bySemester.get(nextHK), missing);
    }

    @Transactional
    public void checkOverallStatus(Long userId) {
        System.out.println("===== MANUAL CHECK for user: " + userId + " =====");

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        List<UserCTDT> all = userCTDTRepo.findByUserIdOrderByHocKyAsc(userId);
        if (all.isEmpty()) return;

        Map<Integer, List<UserCTDT>> bySemester = all.stream()
                .collect(Collectors.groupingBy(s -> s.getHocKy() != null ? s.getHocKy() : 0));

        Integer currentSemester = bySemester.keySet().stream().sorted()
                .filter(hk -> bySemester.get(hk).stream()
                        .anyMatch(s -> Integer.valueOf(1).equals(s.getTrangThai())))
                .findFirst().orElse(null);

        if (currentSemester != null) {
            checkMissingInPreviousSemesters(user, currentSemester, bySemester);
            checkExcessElectives(user, currentSemester, bySemester);
        }

        checkOverallProgress(user, all, bySemester);

        // ✅ Gợi ý môn học theo chu kỳ
        checkCourseRecommendations(userId);

        System.out.println("===== MANUAL CHECK DONE =====\n");
    }
}
package com.ai.chatbot.service;

import com.ai.chatbot.model.User;
import com.ai.chatbot.model.UserCTDT;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Xuất CTDT của user với tất cả dữ liệu trong 1 sheet duy nhất
     */
    public byte[] exportUserCTDTWithProgressToExcel(User user, List<UserCTDT> ctdtList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CTDT_" + user.getMssv());

            // ============ ĐỊNH NGHĨA STYLES ============

            // Title style
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Section header style
            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            Font sectionHeaderFont = workbook.createFont();
            sectionHeaderFont.setBold(true);
            sectionHeaderFont.setFontHeightInPoints((short) 11);
            sectionHeaderStyle.setFont(sectionHeaderFont);
            sectionHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Data styles
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataCenterStyle = workbook.createCellStyle();
            dataCenterStyle.cloneStyleFrom(dataStyle);
            dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            // Status styles
            CellStyle completedStyle = workbook.createCellStyle();
            completedStyle.cloneStyleFrom(dataCenterStyle);
            completedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            completedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font completedFont = workbook.createFont();
            completedFont.setBold(true);
            completedFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            completedStyle.setFont(completedFont);

            CellStyle pendingStyle = workbook.createCellStyle();
            pendingStyle.cloneStyleFrom(dataCenterStyle);
            pendingStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            pendingStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font pendingFont = workbook.createFont();
            pendingFont.setBold(true);
            pendingFont.setColor(IndexedColors.DARK_YELLOW.getIndex());
            pendingStyle.setFont(pendingFont);

            // Label style
            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);

            // Value style
            CellStyle valueStyle = workbook.createCellStyle();
            valueStyle.setBorderBottom(BorderStyle.THIN);

            // ============ BẮT ĐẦU ĐIỀN DỮ LIỆU ============
            int rowIdx = 0;

            // ============ PHẦN 1: TIÊU ĐỀ VÀ THÔNG TIN CHUNG ============
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CHƯƠNG TRÌNH ĐÀO TẠO CÁ NHÂN - VLU STUDENT PORTAL");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            Row subTitleRow = sheet.createRow(rowIdx++);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("Xuất ngày: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            CellStyle subTitleStyle = workbook.createCellStyle();
            Font subTitleFont = workbook.createFont();
            subTitleFont.setItalic(true);
            subTitleStyle.setFont(subTitleFont);
            subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleCell.setCellStyle(subTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            rowIdx++; // Dòng trống

            // ============ PHẦN 2: THÔNG TIN SINH VIÊN ============
            Row studentHeaderRow = sheet.createRow(rowIdx++);
            Cell studentHeaderCell = studentHeaderRow.createCell(0);
            studentHeaderCell.setCellValue("THÔNG TIN SINH VIÊN");
            studentHeaderCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 10));

            // Thông tin chi tiết - hàng 1
            Row infoRow1 = sheet.createRow(rowIdx++);

            // MSSV
            infoRow1.createCell(0).setCellValue("MSSV:");
            infoRow1.createCell(1).setCellValue(user.getMssv() != null ? user.getMssv() : "");

            // Họ tên
            infoRow1.createCell(2).setCellValue("Họ tên:");
            infoRow1.createCell(3).setCellValue(user.getFullName() != null ? user.getFullName() : "");

            // Nganh
            infoRow1.createCell(4).setCellValue("Nganh:");
            infoRow1.createCell(5).setCellValue(user.getDepartment() != null ? user.getDepartment() : "");

            // Chuyên ngành - FIXED: Hiển thị đúng giá trị
            infoRow1.createCell(6).setCellValue("Chuyên ngành:");
            infoRow1.createCell(7).setCellValue(user.getMajor() != null ? user.getMajor() : "Chưa cập nhật");

            // Áp dụng style
            for (int i : new int[]{0, 2, 4, 6}) {
                infoRow1.getCell(i).setCellStyle(labelStyle);
            }
            for (int i : new int[]{1, 3, 5, 7}) {
                infoRow1.getCell(i).setCellStyle(valueStyle);
            }

            // Thông tin chi tiết - hàng 2
            Row infoRow2 = sheet.createRow(rowIdx++);

            // Khóa
            infoRow2.createCell(0).setCellValue("Khóa:");
            infoRow2.createCell(1).setCellValue(user.getCourse() != null ? user.getCourse() : "Chưa cập nhật");

            // Lớp
            infoRow2.createCell(2).setCellValue("Lớp:");
            infoRow2.createCell(3).setCellValue(user.getStudentClass() != null ? user.getStudentClass() : "Chưa cập nhật");

            // Ngày sinh
            if (user.getBirth() != null) {
                infoRow2.createCell(4).setCellValue("Ngày sinh:");
                infoRow2.createCell(5).setCellValue(user.getBirth().format(DATE_FORMATTER));
            }

            // Áp dụng style
            for (int i : new int[]{0, 2, 4}) {
                if (infoRow2.getCell(i) != null) {
                    infoRow2.getCell(i).setCellStyle(labelStyle);
                }
            }
            for (int i : new int[]{1, 3, 5}) {
                if (infoRow2.getCell(i) != null) {
                    infoRow2.getCell(i).setCellStyle(valueStyle);
                }
            }

            // Thông tin liên hệ - hàng 3
            Row infoRow3 = sheet.createRow(rowIdx++);

            // Email
            if (user.getGmail() != null && !user.getGmail().isEmpty()) {
                infoRow3.createCell(0).setCellValue("Email:");
                infoRow3.createCell(1).setCellValue(user.getGmail());
                infoRow3.getCell(0).setCellStyle(labelStyle);
                infoRow3.getCell(1).setCellStyle(valueStyle);
            } else {
                infoRow3.createCell(0).setCellValue("Email:");
                infoRow3.createCell(1).setCellValue("Chưa cập nhật");
                infoRow3.getCell(0).setCellStyle(labelStyle);
                infoRow3.getCell(1).setCellStyle(valueStyle);
            }

            // Điện thoại
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                infoRow3.createCell(2).setCellValue("Điện thoại:");
                infoRow3.createCell(3).setCellValue(user.getPhone());
                infoRow3.getCell(2).setCellStyle(labelStyle);
                infoRow3.getCell(3).setCellStyle(valueStyle);
            } else {
                infoRow3.createCell(2).setCellValue("Điện thoại:");
                infoRow3.createCell(3).setCellValue("Chưa cập nhật");
                infoRow3.getCell(2).setCellStyle(labelStyle);
                infoRow3.getCell(3).setCellStyle(valueStyle);
            }

            rowIdx++; // Dòng trống

            // ============ PHẦN 3: TỔNG HỢP TIẾN ĐỘ ============
            Row progressHeaderRow = sheet.createRow(rowIdx++);
            Cell progressHeaderCell = progressHeaderRow.createCell(0);
            progressHeaderCell.setCellValue("TỔNG HỢP TIẾN ĐỘ HOÀN THÀNH");
            progressHeaderCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 10));

            // Tính toán thống kê
            Map<String, Object> stats = calculateRealTimeStats(ctdtList);

            // Hàng thống kê 1
            Row statsRow1 = sheet.createRow(rowIdx++);
            statsRow1.createCell(0).setCellValue("Tổng số môn:");
            statsRow1.createCell(1).setCellValue((Integer) stats.get("totalSubjects"));
            statsRow1.createCell(3).setCellValue("Đã học:");
            statsRow1.createCell(4).setCellValue((Integer) stats.get("completedSubjects"));
            statsRow1.createCell(6).setCellValue("Chưa học:");
            statsRow1.createCell(7).setCellValue((Integer) stats.get("pendingSubjects"));

            // Hàng thống kê 2
            Row statsRow2 = sheet.createRow(rowIdx++);
            double totalRequired = (Double) stats.get("totalRequired");
            double totalCompleted = (Double) stats.get("totalCompleted");
            double completionRate = totalRequired > 0 ? (totalCompleted / totalRequired) * 100 : 0;

            statsRow2.createCell(0).setCellValue("Tổng tín chỉ phải học:");
            statsRow2.createCell(1).setCellValue(totalRequired);
            statsRow2.createCell(3).setCellValue("Tín chỉ đã học:");
            statsRow2.createCell(4).setCellValue(totalCompleted);
            statsRow2.createCell(6).setCellValue("Tỷ lệ hoàn thành:");
            statsRow2.createCell(7).setCellValue(String.format("%.1f%%", completionRate));

            // Hàng thống kê 3
            Row statsRow3 = sheet.createRow(rowIdx++);
            statsRow3.createCell(0).setCellValue("Bắt buộc (đã học/phải học):");
            statsRow3.createCell(1).setCellValue(
                    String.format("%.1f/%.1f TC",
                            (Double) stats.get("bbCompleted"),
                            (Double) stats.get("bbRequired"))
            );
            statsRow3.createCell(3).setCellValue("Tự chọn (đã học/phải học):");
            statsRow3.createCell(4).setCellValue(
                    String.format("%.1f/%.1f TC",
                            (Double) stats.get("tcCompletedSimple"),
                            (Double) stats.get("tcRequiredTotal"))
            );

            // Áp dụng style cho tất cả các hàng thống kê
            for (Row row : Arrays.asList(statsRow1, statsRow2, statsRow3)) {
                for (int i : new int[]{0, 3, 6}) {
                    if (row.getCell(i) != null) {
                        row.getCell(i).setCellStyle(labelStyle);
                    }
                }
                for (int i : new int[]{1, 4, 7}) {
                    if (row.getCell(i) != null) {
                        row.getCell(i).setCellStyle(valueStyle);
                    }
                }
            }

            rowIdx++; // Dòng trống

            // ============ PHẦN 4: CHI TIẾT HỌC PHẦN THEO HỌC KỲ ============
            Row coursesHeaderRow = sheet.createRow(rowIdx++);
            Cell coursesHeaderCell = coursesHeaderRow.createCell(0);
            coursesHeaderCell.setCellValue("CHI TIẾT HỌC PHẦN THEO HỌC KỲ");
            coursesHeaderCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 10));

            rowIdx++; // Dòng trống

            // Sắp xếp dữ liệu theo học kỳ và nhóm
            Map<Integer, List<UserCTDT>> bySemester = ctdtList.stream()
                    .collect(Collectors.groupingBy(UserCTDT::getHocKy));

            List<Integer> sortedSemesters = bySemester.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            int stt = 1;

            for (Integer hocKy : sortedSemesters) {
                List<UserCTDT> semesterCourses = bySemester.get(hocKy);

                // Tiêu đề học kỳ
                Row semesterTitleRow = sheet.createRow(rowIdx++);
                Cell semesterTitleCell = semesterTitleRow.createCell(0);
                semesterTitleCell.setCellValue("HỌC KỲ " + hocKy);
                CellStyle semesterTitleStyle = workbook.createCellStyle();
                Font semesterTitleFont = workbook.createFont();
                semesterTitleFont.setBold(true);
                semesterTitleFont.setColor(IndexedColors.WHITE.getIndex());
                semesterTitleStyle.setFont(semesterTitleFont);
                semesterTitleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
                semesterTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                semesterTitleStyle.setAlignment(HorizontalAlignment.CENTER);
                semesterTitleCell.setCellStyle(semesterTitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 10));

                // Tiêu đề bảng
                Row tableHeaderRow = sheet.createRow(rowIdx++);
                String[] headers = {"STT", "Mã HP", "Tên học phần", "TC", "Loại",
                        "Nhóm TC", "HP tiên quyết", "Nganh", "Chuyên ngành", "Trạng thái", "Cập nhật"};

                for (int i = 0; i < headers.length; i++) {
                    tableHeaderRow.createCell(i).setCellValue(headers[i]);
                    tableHeaderRow.getCell(i).setCellStyle(headerStyle);
                }

                // Sắp xếp môn học trong học kỳ này
                List<UserCTDT> sortedCourses = sortCoursesByGroups(semesterCourses);

                // Thêm từng môn học
                for (UserCTDT ctdt : sortedCourses) {
                    Row dataRow = sheet.createRow(rowIdx++);

                    // STT
                    dataRow.createCell(0).setCellValue(stt++);
                    dataRow.getCell(0).setCellStyle(dataCenterStyle);

                    // Mã HP
                    dataRow.createCell(1).setCellValue(ctdt.getMaHocPhan() != null ? ctdt.getMaHocPhan() : "");
                    dataRow.getCell(1).setCellStyle(dataCenterStyle);

                    // Tên học phần
                    dataRow.createCell(2).setCellValue(ctdt.getTenHocPhan() != null ? ctdt.getTenHocPhan() : "");
                    dataRow.getCell(2).setCellStyle(dataStyle);

                    // Tín chỉ
                    dataRow.createCell(3).setCellValue(ctdt.getTinChi() != null ? ctdt.getTinChi() : 0);
                    dataRow.getCell(3).setCellStyle(dataCenterStyle);

                    // Loại
                    String loaiText = "BB".equals(ctdt.getLoai()) ? "Bắt buộc" : "Tự chọn";
                    dataRow.createCell(4).setCellValue(loaiText);
                    dataRow.getCell(4).setCellStyle(dataCenterStyle);

                    // Nhóm TC
                    dataRow.createCell(5).setCellValue(ctdt.getNhomTC() != null ? ctdt.getNhomTC() : "-");
                    dataRow.getCell(5).setCellStyle(dataCenterStyle);

                    // HP tiên quyết
                    String tienQuyetText = getPrerequisitesText(ctdt);
                    dataRow.createCell(6).setCellValue(tienQuyetText.isEmpty() ? "-" : tienQuyetText);
                    dataRow.getCell(6).setCellStyle(dataStyle);

                    // Nganh
                    dataRow.createCell(7).setCellValue(ctdt.getNganh() != null && !ctdt.getNganh().isEmpty() ? ctdt.getNganh() : "-");
                    dataRow.getCell(7).setCellStyle(dataCenterStyle);

                    // Chuyên ngành
                    dataRow.createCell(8).setCellValue(ctdt.getChuyenNganh() != null && !ctdt.getChuyenNganh().isEmpty() ? ctdt.getChuyenNganh() : "-");
                    dataRow.getCell(8).setCellStyle(dataCenterStyle);

                    // Trạng thái
                    Cell statusCell = dataRow.createCell(9);
                    if (ctdt.getTrangThai() == 0) {
                        statusCell.setCellValue("ĐÃ HỌC");
                        statusCell.setCellStyle(completedStyle);
                    } else {
                        statusCell.setCellValue("CHƯA HỌC");
                        statusCell.setCellStyle(pendingStyle);
                    }

                    // Ngày cập nhật
                    String updateTime = ctdt.getUpdatedAt() != null ?
                            ctdt.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) :
                            ctdt.getCreatedAt() != null ?
                                    ctdt.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) : "-";
                    dataRow.createCell(10).setCellValue(updateTime);
                    dataRow.getCell(10).setCellStyle(dataCenterStyle);
                }

                // Thêm dòng trống giữa các học kỳ
                rowIdx++;
            }

            rowIdx++; // Dòng trống

            // ============ PHẦN 5: TỔNG KẾT THEO HỌC KỲ ============
            Row summaryHeaderRow = sheet.createRow(rowIdx++);
            Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
            summaryHeaderCell.setCellValue("TỔNG KẾT THEO HỌC KỲ");
            summaryHeaderCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 7));

            // Tiêu đề bảng tổng kết
            Row summaryTableHeaderRow = sheet.createRow(rowIdx++);
            String[] summaryHeaders = {"Học kỳ", "Tổng môn", "Đã học", "Chưa học",
                    "Tổng TC", "TC đã học", "TC phải học", "Tỷ lệ"};

            for (int i = 0; i < summaryHeaders.length; i++) {
                summaryTableHeaderRow.createCell(i).setCellValue(summaryHeaders[i]);
                summaryTableHeaderRow.getCell(i).setCellStyle(headerStyle);
            }

            // Dữ liệu tổng kết từng học kỳ
            for (Integer hocKy : sortedSemesters) {
                Row summaryRow = sheet.createRow(rowIdx++);
                List<UserCTDT> semesterCourses = bySemester.get(hocKy);

                // Tính thống kê
                int semesterTotal = semesterCourses.size();
                int semesterCompleted = (int) semesterCourses.stream()
                        .filter(c -> c.getTrangThai() == 0)
                        .count();
                int semesterPending = semesterTotal - semesterCompleted;

                double semesterTotalCredits = semesterCourses.stream()
                        .mapToDouble(c -> c.getTinChi() != null ? c.getTinChi() : 0)
                        .sum();

                double semesterCompletedCredits = semesterCourses.stream()
                        .filter(c -> c.getTrangThai() == 0)
                        .mapToDouble(c -> c.getTinChi() != null ? c.getTinChi() : 0)
                        .sum();

                // Tính tín chỉ phải học
                Map<String, Object> semesterStats = calculateRealTimeStats(semesterCourses);
                double semesterRequiredCredits = (Double) semesterStats.get("totalRequired");

                double semesterCompletionRate = semesterRequiredCredits > 0 ?
                        (semesterCompletedCredits / semesterRequiredCredits) * 100 : 0;

                // Điền dữ liệu
                summaryRow.createCell(0).setCellValue(hocKy);
                summaryRow.createCell(1).setCellValue(semesterTotal);
                summaryRow.createCell(2).setCellValue(semesterCompleted);
                summaryRow.createCell(3).setCellValue(semesterPending);
                summaryRow.createCell(4).setCellValue(semesterTotalCredits);
                summaryRow.createCell(5).setCellValue(semesterCompletedCredits);
                summaryRow.createCell(6).setCellValue(semesterRequiredCredits);
                summaryRow.createCell(7).setCellValue(String.format("%.1f%%", semesterCompletionRate));

                // Style
                for (int i = 0; i < 8; i++) {
                    summaryRow.getCell(i).setCellStyle(dataCenterStyle);
                }
            }

            rowIdx++; // Dòng trống

            // ============ CHÂN TRANG ============
            Row noteRow = sheet.createRow(rowIdx++);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("Dữ liệu được xuất tự động từ hệ thống VLU Student Portal - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            CellStyle noteStyle = workbook.createCellStyle();
            Font noteFont = workbook.createFont();
            noteFont.setItalic(true);
            noteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            noteStyle.setFont(noteFont);
            noteCell.setCellStyle(noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 10));

            // ============ TỰ ĐỘNG ĐIỀU CHỈNH CỘT ============
            for (int i = 0; i < 11; i++) {
                sheet.autoSizeColumn(i);
            }

            // Đặt độ rộng tối ưu
            sheet.setColumnWidth(2, 50 * 256);    // Tên học phần
            sheet.setColumnWidth(6, 40 * 256);    // HP tiên quyết
            sheet.setColumnWidth(8, 25 * 256);    // Chuyên ngành
            sheet.setColumnWidth(10, 15 * 256);   // Cập nhật

            // ============ GHI RA OUTPUT STREAM ============
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    /**
     * Sắp xếp môn học theo nhóm (giống giao diện)
     */
    private List<UserCTDT> sortCoursesByGroups(List<UserCTDT> courses) {
        List<UserCTDT> nhom1 = new ArrayList<>();  // BB: nganh="", chuyenNganh=""
        List<UserCTDT> nhom2 = new ArrayList<>();  // BB: nganh!="", chuyenNganh=""
        List<UserCTDT> nhom3 = new ArrayList<>();  // BB: nganh!="", chuyenNganh!=""
        List<UserCTDT> nhom4 = new ArrayList<>();  // TC

        for (UserCTDT ctdt : courses) {
            String loai = ctdt.getLoai() != null ? ctdt.getLoai() : "BB";
            String nganh = ctdt.getNganh() != null ? ctdt.getNganh().trim() : "";
            String chuyenNganh = ctdt.getChuyenNganh() != null ? ctdt.getChuyenNganh().trim() : "";

            if ("BB".equals(loai)) {
                if (nganh.isEmpty() && chuyenNganh.isEmpty()) {
                    nhom1.add(ctdt);
                } else if (!nganh.isEmpty() && chuyenNganh.isEmpty()) {
                    nhom2.add(ctdt);
                } else if (!nganh.isEmpty() && !chuyenNganh.isEmpty()) {
                    nhom3.add(ctdt);
                } else {
                    nhom1.add(ctdt);
                }
            } else if ("TC".equals(loai)) {
                nhom4.add(ctdt);
            }
        }

        // Sắp xếp trong từng nhóm
        Comparator<UserCTDT> sorter = Comparator
                .comparing((UserCTDT c) -> c.getNganh() != null ? c.getNganh() : "", Comparator.nullsLast(String::compareTo))
                .thenComparing(c -> c.getChuyenNganh() != null ? c.getChuyenNganh() : "", Comparator.nullsLast(String::compareTo))
                .thenComparing(c -> c.getTenHocPhan() != null ? c.getTenHocPhan() : "", Comparator.nullsLast(String::compareTo));

        nhom1.sort(sorter);
        nhom2.sort(sorter);
        nhom3.sort(sorter);
        nhom4.sort(sorter);

        // Gộp lại
        List<UserCTDT> sorted = new ArrayList<>();
        sorted.addAll(nhom1);
        sorted.addAll(nhom2);
        sorted.addAll(nhom3);
        sorted.addAll(nhom4);

        return sorted;
    }

    /**
     * Lấy văn bản học phần tiên quyết
     */
    private String getPrerequisitesText(UserCTDT ctdt) {
        if (ctdt.getCtdt() != null && ctdt.getCtdt().getHocPhanTienQuyet() != null) {
            return ctdt.getCtdt().getHocPhanTienQuyet().stream()
                    .map(hp -> hp.getTenHocPhan() != null ? hp.getTenHocPhan() : "")
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.joining("; "));
        }
        return "";
    }

    /**
     * Tính toán thống kê thời gian thực
     */
    private Map<String, Object> calculateRealTimeStats(List<UserCTDT> ctdtList) {
        Map<String, Object> stats = new HashMap<>();

        if (ctdtList == null || ctdtList.isEmpty()) {
            stats.put("totalSubjects", 0);
            stats.put("completedSubjects", 0);
            stats.put("pendingSubjects", 0);
            stats.put("bbRequired", 0.0);
            stats.put("bbCompleted", 0.0);
            stats.put("tcRequiredTotal", 0.0);
            stats.put("tcCompletedSimple", 0.0);
            stats.put("totalRequired", 0.0);
            stats.put("totalCompleted", 0.0);
            return stats;
        }

        // Tính BB (X/Y rules)
        double bbRequired = 0;
        double bbCompleted = 0;

        for (UserCTDT ctdt : ctdtList) {
            if ("BB".equals(ctdt.getLoai())) {
                Integer tinChi = ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
                Integer trangThai = ctdt.getTrangThai() != null ? ctdt.getTrangThai() : 1;

                bbRequired += tinChi;
                if (trangThai == 0) {
                    bbCompleted += tinChi;
                }
            }
        }

        // Tính TC với logic mới
        Map<Integer, List<UserCTDT>> tcByHocKy = new HashMap<>();
        double tcCompletedSimple = 0;

        for (UserCTDT ctdt : ctdtList) {
            if ("TC".equals(ctdt.getLoai())) {
                Integer hocKy = ctdt.getHocKy() != null ? ctdt.getHocKy() : 0;
                tcByHocKy.computeIfAbsent(hocKy, k -> new ArrayList<>()).add(ctdt);

                Integer tinChi = ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
                Integer trangThai = ctdt.getTrangThai() != null ? ctdt.getTrangThai() : 1;
                if (trangThai == 0) {
                    tcCompletedSimple += tinChi;
                }
            }
        }

        double tcRequiredTotal = 0;

        for (Map.Entry<Integer, List<UserCTDT>> entry : tcByHocKy.entrySet()) {
            List<UserCTDT> tcList = entry.getValue();

            // Group by nhomTC
            Map<String, List<UserCTDT>> tcGroups = new HashMap<>();
            List<UserCTDT> tcNoGroup = new ArrayList<>();

            for (UserCTDT ctdt : tcList) {
                String nhomTC = ctdt.getNhomTC() != null ? ctdt.getNhomTC().trim() : "";

                if (nhomTC.isEmpty()) {
                    tcNoGroup.add(ctdt);
                } else {
                    tcGroups.computeIfAbsent(nhomTC, k -> new ArrayList<>()).add(ctdt);
                }
            }

            // TC không nhóm
            for (UserCTDT ctdt : tcNoGroup) {
                tcRequiredTotal += ctdt.getTinChi() != null ? ctdt.getTinChi() : 0;
            }

            // TC có nhóm: (tổng tín chỉ nhóm) × (X/Y)
            for (Map.Entry<String, List<UserCTDT>> groupEntry : tcGroups.entrySet()) {
                List<UserCTDT> group = groupEntry.getValue();

                double groupTotalCredits = group.stream()
                        .mapToDouble(c -> c.getTinChi() != null ? c.getTinChi() : 0)
                        .sum();

                String[] parts = groupEntry.getKey().split("/");

                if (parts.length == 2) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());

                        if (y > 0) {
                            tcRequiredTotal += groupTotalCredits * ((double) x / y);
                        } else {
                            tcRequiredTotal += groupTotalCredits;
                        }
                    } catch (NumberFormatException e) {
                        tcRequiredTotal += groupTotalCredits;
                    }
                } else {
                    tcRequiredTotal += groupTotalCredits;
                }
            }
        }

        // Làm tròn
        bbRequired = Math.round(bbRequired * 10) / 10.0;
        bbCompleted = Math.round(bbCompleted * 10) / 10.0;
        tcRequiredTotal = Math.round(tcRequiredTotal * 10) / 10.0;
        tcCompletedSimple = Math.round(tcCompletedSimple * 10) / 10.0;

        int totalSubjects = ctdtList.size();
        int completedSubjects = (int) ctdtList.stream()
                .filter(c -> c.getTrangThai() != null && c.getTrangThai() == 0)
                .count();
        int pendingSubjects = totalSubjects - completedSubjects;

        double totalRequired = bbRequired + tcRequiredTotal;
        double totalCompleted = bbCompleted + tcCompletedSimple;

        stats.put("totalSubjects", totalSubjects);
        stats.put("completedSubjects", completedSubjects);
        stats.put("pendingSubjects", pendingSubjects);
        stats.put("bbRequired", bbRequired);
        stats.put("bbCompleted", bbCompleted);
        stats.put("tcRequiredTotal", tcRequiredTotal);
        stats.put("tcCompletedSimple", tcCompletedSimple);
        stats.put("totalRequired", Math.round(totalRequired * 10) / 10.0);
        stats.put("totalCompleted", Math.round(totalCompleted * 10) / 10.0);

        return stats;
    }
}
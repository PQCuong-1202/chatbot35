package com.ai.chatbot.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class ExcelProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ExcelProcessingService.class);

    public String extractExcelContent(MultipartFile file) {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook;

            if (file.getOriginalFilename().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (file.getOriginalFilename().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                return "Định dạng file không được hỗ trợ. Vui lòng tải lên file Excel (.xlsx hoặc .xls)";
            }

            log.info("Processing Excel file with {} sheets", workbook.getNumberOfSheets());

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                content.append("\n=== Sheet: ").append(sheetName).append(" ===\n\n");

                int rowCount = 0;
                int maxRowsToRead = 1000; // Số dòng tối đa muốn đọc

                for (Row row : sheet) {
                    // CHỈ ĐẾM KHI DÒNG CÓ DỮ LIỆU
                    StringBuilder rowContent = new StringBuilder();
                    boolean hasData = false;

                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (!cellValue.isEmpty()) {
                            rowContent.append(cellValue).append(" | ");
                            hasData = true;
                        }
                    }

                    if (hasData) {
                        content.append(rowContent.toString()).append("\n");
                        rowCount++;

                        // Dừng khi đạt đủ số dòng CÓ DỮ LIỆU mong muốn
                        if (rowCount >= maxRowsToRead) {
                            content.append("... (đã đọc ").append(maxRowsToRead).append(" dòng có dữ liệu, còn nhiều dòng khác)\n");
                            break;
                        }
                    }
                }

                if (rowCount == 0) {
                    content.append("(Sheet trống)\n");
                }

                content.append("\n");
            }

            workbook.close();

            log.info("Extracted {} characters from Excel file", content.length());

            if (content.length() == 0) {
                return "Không thể đọc nội dung từ file Excel. Có thể file trống hoặc định dạng không đúng.";
            }

            return content.toString();

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            return "Lỗi khi xử lý file Excel: " + e.getMessage();
        }
    }

    public String summarizeContent(String fullContent) {
        if (fullContent == null || fullContent.isEmpty()) {
            return "File Excel không có nội dung.";
        }

        String[] lines = fullContent.split("\n");
        StringBuilder summary = new StringBuilder();

        summary.append("Tóm tắt nội dung file Excel (lấy toàn bộ dòng chính):\n");

        // MỞ RỘNG DANH SÁCH TỪ KHÓA - thêm các từ thường gặp trong chương trình đào tạo
        Set<String> keywords = new HashSet<>(Arrays.asList(
                "môn học", "học kỳ", "tín chỉ", "mã môn", "tiên quyết",
                "bắt buộc", "tự chọn", "giáo trình", "thực hành", "lý thuyết",
                "điều kiện", "yêu cầu", "nội dung", "mục tiêu", "chuẩn đầu ra",
                // Thêm các từ khóa phổ biến khác
                "mã hp", "số tc", "học phần", "chương trình", "đào tạo",
                "khối lượng", "phân bố", "tiết học", "bài tập", "kiểm tra",
                "thi cử", "điểm số", "đánh giá", "phòng học", "giảng viên",
                "thời gian", "tuần học", "semester", "credit", "course",
                "subject", "prerequisite", "elective", "mandatory"
        ));

        int importantLines = 0;
        int totalLinesAdded = 0;
        int MAX_SUMMARY_LINES = 200; // TĂNG GIỚI HẠN LÊN 350 DÒNG

        // ƯU TIÊN 1: Lấy các dòng chứa từ khóa
        for (String line : lines) {
            if (totalLinesAdded >= MAX_SUMMARY_LINES) break;

            String lowerLine = line.toLowerCase();
            boolean isImportant = false;

            for (String keyword : keywords) {
                if (lowerLine.contains(keyword)) {
                    summary.append("- [KW] ").append(line.trim()).append("\n");
                    importantLines++;
                    totalLinesAdded++;
                    isImportant = true;
                    break;
                }
            }

            if (!isImportant && line.trim().length() > 5) { // Giảm ngưỡng từ 10 xuống 5
                // ƯU TIÊN 2: Lấy các dòng có chứa ký tự đặc biệt của bảng (|) hoặc cấu trúc cột
                if (line.contains("|") || line.matches(".*[A-Z]{2,3}\\d{3,5}.*")) {
                    summary.append("- ").append(line.trim()).append("\n");
                    totalLinesAdded++;
                }
            }
        }

        // Nếu vẫn ít dòng, lấy thêm các dòng đầu tiên
        if (totalLinesAdded < 30 && totalLinesAdded < lines.length) {
            summary.append("\n--- Các dòng bổ sung từ đầu file ---\n");
            int additionalCount = 0;
            for (int i = 0; i < Math.min(lines.length, 50); i++) {
                if (!summary.toString().contains(lines[i].trim()) && lines[i].trim().length() > 3) {
                    summary.append("- ").append(lines[i].trim()).append("\n");
                    additionalCount++;
                    if (additionalCount >= 30) break;
                }
            }
        }

        summary.append("\n=== THỐNG KÊ ===\n");
        summary.append("• Tổng số dòng trong file: ").append(lines.length).append("\n");
        summary.append("• Số dòng đã tóm tắt: ").append(totalLinesAdded).append("\n");
        summary.append("• Số dòng chứa từ khóa: ").append(importantLines);

        return summary.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Kiểm tra nếu là số nguyên
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getCellFormula();
                } catch (Exception e) {
                    return "[Công thức]";
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
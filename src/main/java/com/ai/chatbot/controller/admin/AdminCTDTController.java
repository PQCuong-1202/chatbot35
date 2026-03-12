package com.ai.chatbot.controller.admin;

import com.ai.chatbot.model.CTDT;
import com.ai.chatbot.repository.CTDTRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/api/admin/ctdt")
public class AdminCTDTController {
    @Autowired private CTDTRepository ctdtRepo;

    // ============ GET CTDT BY NGANH (ADMIN) ============
    @GetMapping("/by-nganh/{nganh}")
    @ResponseBody
    public ResponseEntity<?> getCTDTByNganh(@PathVariable String nganh) {
        try {
            List<CTDT> ctdtList = ctdtRepo.findByNganh(nganh);
            return ResponseEntity.ok(ctdtList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải CTĐT theo ngành: " + e.getMessage());
        }
    }

    // ============ GET CTDT BY TAB NGANH (ADMIN) ============
    @GetMapping("/by-tab/{tabNganh}")
    @ResponseBody
    public ResponseEntity<?> getCTDTByTabNganh(@PathVariable String tabNganh) {
        try {
            List<CTDT> ctdtList = ctdtRepo.findByTabNganh(tabNganh);
            return ResponseEntity.ok(ctdtList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải CTĐT theo tab ngành: " + e.getMessage());
        }
    }

    // ============ DELETE CTDT BY NGANH (ADMIN) ============
    @DeleteMapping("/delete-by-nganh/{nganh}")
    @ResponseBody
    public ResponseEntity<?> deleteCTDTByNganh(@PathVariable String nganh) {
        try {
            List<CTDT> ctdtList = ctdtRepo.findByNganh(nganh);
            if (ctdtList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy CTĐT cho ngành: " + nganh);
            }

            // Kiểm tra xem có học phần nào là tiên quyết không
            for (CTDT ctdt : ctdtList) {
                List<CTDT> dependentCourses = ctdtRepo.findByHocPhanTienQuyetContains(ctdt);
                if (!dependentCourses.isEmpty()) {
                    String dependentNames = dependentCourses.stream()
                            .map(CTDT::getTenHocPhan)
                            .collect(Collectors.joining(", "));
                    return ResponseEntity.badRequest()
                            .body("Không thể xóa vì học phần " + ctdt.getTenHocPhan() +
                                    " là tiên quyết của: " + dependentNames);
                }
            }

            ctdtRepo.deleteAll(ctdtList);
            return ResponseEntity.ok("Đã xóa " + ctdtList.size() + " học phần của ngành " + nganh);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ============ DELETE CTDT BY TAB NGANH (ADMIN) ============
    @DeleteMapping("/delete-by-tab/{tabNganh}")
    @ResponseBody
    public ResponseEntity<?> deleteCTDTByTabNganh(@PathVariable String tabNganh) {
        try {
            List<CTDT> ctdtList = ctdtRepo.findByTabNganh(tabNganh);
            if (ctdtList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy CTĐT cho tab: " + tabNganh);
            }
            // Kiểm tra tiên quyết trước khi xóa
            for (CTDT ctdt : ctdtList) {
                List<CTDT> dependentCourses = ctdtRepo.findByHocPhanTienQuyetContains(ctdt);
                if (!dependentCourses.isEmpty()) {
                    String dependentNames = dependentCourses.stream()
                            .map(CTDT::getTenHocPhan)
                            .collect(Collectors.joining(", "));
                    return ResponseEntity.badRequest()
                            .body("Không thể xóa vì học phần " + ctdt.getTenHocPhan() +
                                    " là tiên quyết của: " + dependentNames);
                }
            }
            ctdtRepo.deleteAll(ctdtList);
            return ResponseEntity.ok("Đã xóa " + ctdtList.size() + " học phần của tab " + tabNganh);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ============ GET ALL CTDT (ADMIN) ============
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<?> getAllCTDT() {
        try {
            List<CTDT> ctdtList = ctdtRepo.findAll();
            return ResponseEntity.ok(ctdtList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải danh sách CTĐT: " + e.getMessage());
        }
    }

    // ============ SEARCH CTDT (ADMIN) ============
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchCTDT(
            @RequestParam(required = false) Integer hocKy,
            @RequestParam(required = false) String nganh,
            @RequestParam(required = false) String tabNganh,
            @RequestParam(required = false) String chuyenNganh,
            @RequestParam(required = false) String loai) {
        try {
            List<CTDT> results;
            // Nếu có tabNganh filter thì dùng query theo tab (không phụ thuộc field nganh của học phần)
            if (tabNganh != null && !tabNganh.isEmpty()) {
                results = ctdtRepo.searchCTDTByTab(tabNganh, hocKy, chuyenNganh, loai);
            } else {
                results = ctdtRepo.searchCTDT(hocKy, nganh, chuyenNganh, loai);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tìm kiếm CTĐT: " + e.getMessage());
        }
    }

    // ============ SAVE CTDT (ADMIN) ============
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveCTDT(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== DEBUG saveCTDT ===");
            System.out.println("Request data: " + requestData);
            // Parse dữ liệu từ request
            CTDT ctdt = new CTDT();
            // Set ID nếu có
            if (requestData.get("id") != null && !requestData.get("id").toString().isEmpty()) {
                try {
                    Long id = Long.parseLong(requestData.get("id").toString());
                    ctdt.setId(id);
                    // FIX LỖI 2: Khi sửa, load bản ghi cũ để giữ lại tabNganh và createdAt
                    CTDT existing = ctdtRepo.findById(id).orElse(null);
                    if (existing != null) {
                        ctdt.setCreatedAt(existing.getCreatedAt());
                        // Giữ tabNganh từ DB nếu request không gửi lên
                        if (requestData.get("tabNganh") == null || requestData.get("tabNganh").toString().isEmpty()) {
                            ctdt.setTabNganh(existing.getTabNganh());
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing ID: " + e.getMessage());
                }
            }
            // Thêm mã học phần
            if (requestData.get("maHocPhan") != null) {
                ctdt.setMaHocPhan(requestData.get("maHocPhan").toString());
            }
            // Set các trường bắt buộc
            if (requestData.get("hocKy") != null) {
                try {
                    ctdt.setHocKy(Integer.parseInt(requestData.get("hocKy").toString()));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing hocKy: " + e.getMessage());
                }
            }
            if (requestData.get("tenHocPhan") != null) {
                ctdt.setTenHocPhan(requestData.get("tenHocPhan").toString());
            }
            if (requestData.get("tinChi") != null) {
                try {
                    ctdt.setTinChi(Integer.parseInt(requestData.get("tinChi").toString()));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing tinChi: " + e.getMessage());
                }
            }
            if (requestData.get("loai") != null) {
                ctdt.setLoai(requestData.get("loai").toString());
            }
            // Set các trường optional
            if (requestData.get("nhomTC") != null) {
                ctdt.setNhomTC(requestData.get("nhomTC").toString());
            }
            if (requestData.get("nganh") != null) {
                ctdt.setNganh(requestData.get("nganh").toString());
            }
            if (requestData.get("chuyenNganh") != null) {
                ctdt.setChuyenNganh(requestData.get("chuyenNganh").toString());
            }
            if (requestData.get("nhomHocPhan") != null) {
                ctdt.setNhomHocPhan(requestData.get("nhomHocPhan").toString());
            }
            // FIX LỖI 2: Nếu request gửi kèm tabNganh thì dùng (ghi đè giá trị đã lấy từ existing)
            if (requestData.get("tabNganh") != null && !requestData.get("tabNganh").toString().isEmpty()) {
                ctdt.setTabNganh(requestData.get("tabNganh").toString());
            }
            // Lấy danh sách ID học phần tiên quyết từ request
            List<Long> tienQuyetIds = new ArrayList<>();
            if (requestData.get("hocPhanTienQuyetIds") != null) {
                try {
                    if (requestData.get("hocPhanTienQuyetIds") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> ids = (List<Object>) requestData.get("hocPhanTienQuyetIds");
                        for (Object idObj : ids) {
                            if (idObj != null) {
                                try {
                                    Long id = Long.parseLong(idObj.toString());
                                    tienQuyetIds.add(id);
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing tienQuyetId: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error extracting tienQuyetIds: " + e.getMessage());
                }
            }
            System.out.println("Extracted tienQuyetIds: " + tienQuyetIds);
            // Validate dữ liệu
            if (ctdt.getTenHocPhan() == null || ctdt.getTenHocPhan().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên học phần không được để trống");
            }
            if (ctdt.getTinChi() == null || ctdt.getTinChi() < 0) {
                return ResponseEntity.badRequest().body("Số tín chỉ không hợp lệ");
            }
            if (ctdt.getHocKy() == null || ctdt.getHocKy() <= 0) {
                return ResponseEntity.badRequest().body("Học kỳ không hợp lệ");
            }
            // Nếu là môn tự chọn, cần nhóm TC
            if ("TC".equals(ctdt.getLoai()) && (ctdt.getNhomTC() == null || ctdt.getNhomTC().trim().isEmpty())) {
                return ResponseEntity.badRequest().body("Môn tự chọn cần có nhóm TC");
            }
            // Kiểm tra học phần tiên quyết có hợp lệ không
            if (!tienQuyetIds.isEmpty()) {
                // Kiểm tra không cho phép tự tham chiếu
                if (ctdt.getId() != null && tienQuyetIds.contains(ctdt.getId())) {
                    return ResponseEntity.badRequest().body("Học phần không thể là tiên quyết của chính nó");
                }
                // Kiểm tra các học phần tiên quyết có tồn tại không
                for (Long tienQuyetId : tienQuyetIds) {
                    if (!ctdtRepo.existsById(tienQuyetId)) {
                        return ResponseEntity.badRequest()
                                .body("Học phần tiên quyết với ID " + tienQuyetId + " không tồn tại");
                    }
                }
            }
            // Tạm thời lưu học phần KHÔNG có học phần tiên quyết
            ctdt.setHocPhanTienQuyet(new ArrayList<>());
            // Cập nhật thời gian
            ctdt.setUpdatedAt(LocalDateTime.now());
            if (ctdt.getId() == null) {
                ctdt.setCreatedAt(LocalDateTime.now());
            } else {
                CTDT existing = ctdtRepo.findById(ctdt.getId()).orElse(null);
                if (existing != null) {
                    ctdt.setCreatedAt(existing.getCreatedAt());
                }
            }
            // Lưu học phần
            CTDT savedCTDT = ctdtRepo.save(ctdt);
            System.out.println("CTDT saved with ID: " + savedCTDT.getId());
            // Nếu có học phần tiên quyết, cập nhật lại
            if (!tienQuyetIds.isEmpty()) {
                updateTienQuyetForCTDT(savedCTDT.getId(), tienQuyetIds);
                System.out.println("Updated tien quyet for CTDT ID: " + savedCTDT.getId());
            }
            return ResponseEntity.ok("Lưu thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lưu CTĐT: " + e.getMessage());
        }
    }

    // ============ SAVE BATCH CTDT (ADMIN) ============
    @PostMapping("/save-batch")
    @ResponseBody
    public ResponseEntity<?> saveBatchCTDT(@RequestBody List<Map<String, Object>> requestDataList) {
        try {
            List<Map<String, Object>> responses = new ArrayList<>();
            List<CTDT> savedCTDTList = new ArrayList<>();
            for (int i = 0; i < requestDataList.size(); i++) {
                Map<String, Object> requestData = requestDataList.get(i);
                try {
                    System.out.println("=== DEBUG saveBatchCTDT Item " + (i + 1) + " ===");
                    System.out.println("Request data: " + requestData);
                    // Parse dữ liệu từ request
                    CTDT ctdt = new CTDT();
                    // Set ID nếu có (cho sửa)
                    if (requestData.get("id") != null && !requestData.get("id").toString().isEmpty()) {
                        try {
                            ctdt.setId(Long.parseLong(requestData.get("id").toString()));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing ID: " + e.getMessage());
                        }
                    }
                    // Thêm mã học phần
                    if (requestData.get("maHocPhan") != null) {
                        ctdt.setMaHocPhan(requestData.get("maHocPhan").toString());
                    }
                    // Set các trường bắt buộc
                    if (requestData.get("hocKy") != null) {
                        try {
                            ctdt.setHocKy(Integer.parseInt(requestData.get("hocKy").toString()));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing hocKy: " + e.getMessage());
                        }
                    }
                    if (requestData.get("tenHocPhan") != null) {
                        ctdt.setTenHocPhan(requestData.get("tenHocPhan").toString());
                    }
                    if (requestData.get("tinChi") != null) {
                        try {
                            ctdt.setTinChi(Integer.parseInt(requestData.get("tinChi").toString()));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing tinChi: " + e.getMessage());
                        }
                    }
                    if (requestData.get("loai") != null) {
                        ctdt.setLoai(requestData.get("loai").toString());
                    }
                    // Set các trường optional
                    if (requestData.get("nhomTC") != null) {
                        ctdt.setNhomTC(requestData.get("nhomTC").toString());
                    }
                    if (requestData.get("nganh") != null) {
                        ctdt.setNganh(requestData.get("nganh").toString());
                    }
                    if (requestData.get("chuyenNganh") != null) {
                        ctdt.setChuyenNganh(requestData.get("chuyenNganh").toString());
                    }
                    if (requestData.get("nhomHocPhan") != null) {
                        ctdt.setNhomHocPhan(requestData.get("nhomHocPhan").toString());
                    }
                    // Lấy danh sách ID học phần tiên quyết từ request - QUAN TRỌNG
                    List<Long> tienQuyetIds = new ArrayList<>();
                    if (requestData.get("hocPhanTienQuyetIds") != null) {
                        try {
                            System.out.println("Found hocPhanTienQuyetIds: " + requestData.get("hocPhanTienQuyetIds"));

                            if (requestData.get("hocPhanTienQuyetIds") instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> ids = (List<Object>) requestData.get("hocPhanTienQuyetIds");
                                for (Object idObj : ids) {
                                    if (idObj != null) {
                                        try {
                                            Long id = Long.parseLong(idObj.toString());
                                            tienQuyetIds.add(id);
                                            System.out.println("Added tienQuyetId: " + id);
                                        } catch (NumberFormatException e) {
                                            System.err.println("Error parsing tienQuyetId: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error extracting tienQuyetIds: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Extracted tienQuyetIds: " + tienQuyetIds);
                    // Validate dữ liệu
                    if (ctdt.getTenHocPhan() == null || ctdt.getTenHocPhan().trim().isEmpty()) {
                        responses.add(Map.of(
                                "status", "error",
                                "message", "Tên học phần không được để trống",
                                "data", requestData
                        ));
                        continue;
                    }
                    // Cập nhật thời gian
                    ctdt.setUpdatedAt(LocalDateTime.now());
                    if (ctdt.getId() == null) {
                        ctdt.setCreatedAt(LocalDateTime.now());
                    }
                    // Lưu học phần TRƯỚC (chưa có tiên quyết)
                    ctdt.setHocPhanTienQuyet(new ArrayList<>()); // Để trống trước
                    CTDT savedCTDT = ctdtRepo.save(ctdt);
                    savedCTDTList.add(savedCTDT);

                    // Nếu có học phần tiên quyết, cập nhật LẠI
                    if (!tienQuyetIds.isEmpty()) {
                        try {
                            System.out.println("Updating tien quyet for CTDT ID: " + savedCTDT.getId());
                            System.out.println("Tien quyet IDs: " + tienQuyetIds);
                            // Gọi method helper để cập nhật
                            updateTienQuyetForCTDT(savedCTDT.getId(), tienQuyetIds);
                            System.out.println("Successfully updated tien quyet for CTDT ID: " + savedCTDT.getId());
                        } catch (Exception e) {
                            System.err.println("Error updating tien quyet: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    responses.add(Map.of(
                            "status", "success",
                            "message", "Lưu thành công",
                            "data", Map.of("id", savedCTDT.getId(), "tenHocPhan", savedCTDT.getTenHocPhan())
                    ));
                } catch (Exception e) {
                    System.err.println("Error processing CTDT item " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                    responses.add(Map.of(
                            "status", "error",
                            "message", "Lỗi: " + e.getMessage(),
                            "data", requestData
                    ));
                }
            }
            // Đếm kết quả
            long successCount = responses.stream()
                    .filter(r -> "success".equals(r.get("status")))
                    .count();
            // Refresh tất cả để đảm bảo dữ liệu đồng bộ
            savedCTDTList.forEach(ctdt -> {
                if (ctdt.getId() != null) {
                    ctdtRepo.findById(ctdt.getId()).ifPresent(refreshed -> {
                        // Không cần làm gì thêm, chỉ để refresh
                    });
                }
            });
            return ResponseEntity.ok(Map.of(
                    "message", "Đã lưu " + successCount + "/" + requestDataList.size() + " học phần",
                    "details", responses
            ));
        } catch (Exception e) {
            System.err.println("Global error in saveBatchCTDT: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lưu nhiều học phần: " + e.getMessage());
        }
    }

    // ============ DELETE CTDT (ADMIN) ============
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCTDT(@PathVariable Long id) {
        try {
            if (!ctdtRepo.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy học phần");
            }
            // Kiểm tra xem học phần này có được dùng làm tiên quyết cho học phần khác không
            List<CTDT> allCTDT = ctdtRepo.findAll();
            List<CTDT> dependentCourses = allCTDT.stream()
                    .filter(c -> c.getHocPhanTienQuyet() != null &&
                            !c.getHocPhanTienQuyet().isEmpty() &&
                            c.getHocPhanTienQuyet().stream().anyMatch(hp -> hp.getId().equals(id)))
                    .collect(Collectors.toList());
            if (!dependentCourses.isEmpty()) {
                String dependentNames = dependentCourses.stream()
                        .map(CTDT::getTenHocPhan)
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest()
                        .body("Không thể xóa vì học phần này là tiên quyết của: " + dependentNames);
            }
            ctdtRepo.deleteById(id);
            return ResponseEntity.ok("Xóa thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ============ GET CTDT BY ID (ADMIN) ============
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getCTDT(@PathVariable Long id) {
        try {
            Optional<CTDT> ctdtOpt = ctdtRepo.findById(id);
            if (!ctdtOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy học phần");
            }
            CTDT ctdt = ctdtOpt.get();
            // Tạo response với đầy đủ thông tin
            Map<String, Object> response = new HashMap<>();
            response.put("id", ctdt.getId());
            response.put("maHocPhan", ctdt.getMaHocPhan());
            response.put("hocKy", ctdt.getHocKy());
            response.put("tenHocPhan", ctdt.getTenHocPhan());
            response.put("tinChi", ctdt.getTinChi());
            response.put("loai", ctdt.getLoai());
            response.put("nhomTC", ctdt.getNhomTC());
            response.put("nganh", ctdt.getNganh());
            response.put("chuyenNganh", ctdt.getChuyenNganh());
            response.put("nhomHocPhan", ctdt.getNhomHocPhan());
            // FIX LỖI 2: Thêm tabNganh vào response để JS gửi lại khi save
            response.put("tabNganh", ctdt.getTabNganh());
            // Lấy danh sách học phần tiên quyết đầy đủ
            if (ctdt.getHocPhanTienQuyet() != null && !ctdt.getHocPhanTienQuyet().isEmpty()) {
                List<Map<String, Object>> tienQuyetList = ctdt.getHocPhanTienQuyet().stream()
                        .map(hp -> {
                            Map<String, Object> hpMap = new HashMap<>();
                            hpMap.put("id", hp.getId());
                            hpMap.put("tenHocPhan", hp.getTenHocPhan());
                            hpMap.put("hocKy", hp.getHocKy());
                            hpMap.put("tinChi", hp.getTinChi());
                            return hpMap;
                        })
                        .collect(Collectors.toList());
                response.put("hocPhanTienQuyet", tienQuyetList);
                // Cũng trả về danh sách ID để tiện xử lý
                List<Long> tienQuyetIds = ctdt.getHocPhanTienQuyet().stream()
                        .map(CTDT::getId)
                        .collect(Collectors.toList());
                response.put("hocPhanTienQuyetIds", tienQuyetIds);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin: " + e.getMessage());
        }
    }

    // ============ GET CTDT FILTERS (ADMIN) ============
    @GetMapping("/filters")
    @ResponseBody
    public ResponseEntity<?> getCTDTFilters(@RequestParam(required = false) String tabNganh) {
        try {
            Map<String, Object> filters = new HashMap<>();
            if (tabNganh != null && !tabNganh.isEmpty()) {
                // Trả về filter theo tab cụ thể
                filters.put("hocKyList", ctdtRepo.findDistinctHocKyByTabNganh(tabNganh));
                filters.put("chuyenNganhList", ctdtRepo.findDistinctChuyenNganhByTabNganh(tabNganh));
            } else {
                // Tab "Tất cả" — trả về tất cả
                filters.put("hocKyList", ctdtRepo.findDistinctHocKy());
                filters.put("nganhList", ctdtRepo.findDistinctNganh());
                filters.put("chuyenNganhList", ctdtRepo.findDistinctChuyenNganh());
            }
            // Luôn trả về danh sách tabNganh để render tabs
            filters.put("tabNganhList", ctdtRepo.findDistinctTabNganh());
            return ResponseEntity.ok(filters);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy bộ lọc: " + e.getMessage());
        }
    }

    // ============ GET HOC PHAN LIST (ADMIN) ============
    @GetMapping("/hoc-phan")
    @ResponseBody
    public ResponseEntity<?> getHocPhanList() {
        try {
            List<CTDT> hocPhanList = ctdtRepo.findAll();
            // Chỉ trả về ID và tên học phần
            List<Map<String, Object>> simplifiedList = hocPhanList.stream()
                    .map(hp -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", hp.getId());
                        map.put("tenHocPhan", hp.getTenHocPhan());
                        map.put("hocKy", hp.getHocKy());
                        map.put("loai", hp.getLoai());
                        map.put("tinChi", hp.getTinChi());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(simplifiedList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách học phần: " + e.getMessage());
        }
    }

    // ============ UPDATE TIEN QUYET FOR CTDT (ADMIN) ============
    @PostMapping("/{id}/tien-quyet")
    @ResponseBody
    public ResponseEntity<?> updateTienQuyet(@PathVariable Long id,
                                             @RequestBody List<Long> tienQuyetIds) {
        try {
            Optional<CTDT> ctdtOpt = ctdtRepo.findById(id);
            if (!ctdtOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy học phần");
            }
            CTDT ctdt = ctdtOpt.get();
            // Kiểm tra không cho phép tự tham chiếu
            if (tienQuyetIds != null && tienQuyetIds.contains(id)) {
                return ResponseEntity.badRequest().body("Học phần không thể là tiên quyết của chính nó");
            }
            // Kiểm tra các học phần tiên quyết có tồn tại không
            if (tienQuyetIds != null) {
                for (Long tienQuyetId : tienQuyetIds) {
                    if (!ctdtRepo.existsById(tienQuyetId)) {
                        return ResponseEntity.badRequest()
                                .body("Học phần tiên quyết với ID " + tienQuyetId + " không tồn tại");
                    }
                }
            }
            // Xóa tất cả quan hệ tiên quyết cũ
            if (ctdt.getHocPhanTienQuyet() != null) {
                ctdt.getHocPhanTienQuyet().clear();
            }
            // Thêm quan hệ tiên quyết mới
            if (tienQuyetIds != null && !tienQuyetIds.isEmpty()) {
                // Lấy tất cả học phần từ database
                List<CTDT> allHocPhan = ctdtRepo.findAll();
                for (Long tienQuyetId : tienQuyetIds) {
                    // Tìm học phần trong database
                    allHocPhan.stream()
                            .filter(hp -> hp.getId().equals(tienQuyetId))
                            .findFirst()
                            .ifPresent(hp -> ctdt.addHocPhanTienQuyet(hp));
                }
            }
            ctdt.setUpdatedAt(LocalDateTime.now());
            ctdtRepo.save(ctdt);
            return ResponseEntity.ok("Cập nhật học phần tiên quyết thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật học phần tiên quyết: " + e.getMessage());
        }
    }

    // ============ CHECK TIEN QUYET (ADMIN) ============
    @GetMapping("/{id}/check-tien-quyet")
    @ResponseBody
    public ResponseEntity<?> checkTienQuyet(@PathVariable Long id) {
        try {
            // Kiểm tra xem học phần này có là tiên quyết của học phần khác không
            List<CTDT> allCTDT = ctdtRepo.findAll();
            List<Map<String, Object>> dependentCourses = allCTDT.stream()
                    .filter(c -> c.getHocPhanTienQuyet() != null &&
                            c.getHocPhanTienQuyet().stream().anyMatch(hp -> hp.getId().equals(id)))
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", c.getId());
                        map.put("tenHocPhan", c.getTenHocPhan());
                        map.put("hocKy", c.getHocKy());
                        return map;
                    })
                    .collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("isTienQuyet", !dependentCourses.isEmpty());
            response.put("dependentCourses", dependentCourses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra học phần tiên quyết: " + e.getMessage());
        }
    }

    // ============ HELPER METHOD - UPDATE TIEN QUYET FOR CTDT ============
    private void updateTienQuyetForCTDT(Long ctdtId, List<Long> tienQuyetIds) {
        try {
            CTDT ctdt = ctdtRepo.findById(ctdtId).orElse(null);
            if (ctdt == null) {
                System.err.println("CTDT not found with ID: " + ctdtId);
                return;
            }
            // Lấy tất cả học phần
            List<CTDT> allHocPhan = ctdtRepo.findAll();
            // Xóa tất cả quan hệ tiên quyết cũ
            if (ctdt.getHocPhanTienQuyet() != null) {
                ctdt.getHocPhanTienQuyet().clear();
            } else {
                ctdt.setHocPhanTienQuyet(new ArrayList<>());
            }
            // Thêm quan hệ tiên quyết mới
            for (Long tienQuyetId : tienQuyetIds) {
                // Không cho phép tự tham chiếu
                if (tienQuyetId.equals(ctdtId)) {
                    System.out.println("Skipping self-reference: " + tienQuyetId);
                    continue;
                }
                // Tìm học phần trong database
                CTDT foundHocPhan = null;
                for (CTDT hp : allHocPhan) {
                    if (hp.getId().equals(tienQuyetId)) {
                        foundHocPhan = hp;
                        break;
                    }
                }
                if (foundHocPhan != null) {
                    // Kiểm tra xem đã có trong danh sách chưa - cách đơn giản
                    boolean exists = false;
                    for (CTDT existing : ctdt.getHocPhanTienQuyet()) {
                        if (existing.getId().equals(foundHocPhan.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        ctdt.getHocPhanTienQuyet().add(foundHocPhan);
                        System.out.println("Added tien quyet: " + foundHocPhan.getId() + " - " + foundHocPhan.getTenHocPhan());
                    } else {
                        System.out.println("Tien quyet already exists: " + foundHocPhan.getId());
                    }
                } else {
                    System.err.println("Hoc phan not found for tien quyet ID: " + tienQuyetId);
                }
            }
            ctdt.setUpdatedAt(LocalDateTime.now());
            ctdtRepo.save(ctdt);
            System.out.println("Successfully updated tien quyet for CTDT ID: " + ctdtId);
        } catch (Exception e) {
            System.err.println("Error in updateTienQuyetForCTDT:");
            e.printStackTrace();
        }
    }

    // ============ FIXED VERSION - PROPERLY HANDLE LOAI (BB/TC) ============
    @PostMapping("/import-excel")
    @ResponseBody
    public ResponseEntity<?> importCTDTFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetNganh", required = false) String targetNganh) {
        try {
            System.out.println("=== DEBUG importCTDTFromExcel ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("Target Nganh: " + targetNganh);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File không được để trống");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body("Chỉ chấp nhận file Excel (.xlsx, .xls)");
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Kích thước file không được vượt quá 5MB");
            }

            List<Map<String, Object>> results = new ArrayList<>();
            Map<String, CTDTData> hocPhanDataMap = new HashMap<>();
            Map<String, List<String>> tienQuyetMap = new HashMap<>();

            try (InputStream is = file.getInputStream()) {
                Workbook workbook = fileName.endsWith(".xlsx") ? new XSSFWorkbook(is) : new HSSFWorkbook(is);
                Sheet sheet = workbook.getSheetAt(0);
                int lastRowNum = sheet.getLastRowNum();
                System.out.println("Tổng số dòng trong file: " + lastRowNum);

                // ============ BƯỚC 1: ĐỌC TOÀN BỘ DỮ LIỆU TỪ FILE ============
                for (int i = 1; i <= lastRowNum; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        System.out.println("Dòng " + (i + 1) + " trống, bỏ qua");
                        continue;
                    }

                    try {
                        CTDTData ctdtData = new CTDTData();
                        ctdtData.rowNum = i + 1;
                        System.out.println("\n--- Đọc dòng " + ctdtData.rowNum + " ---");

                        // 1. Học kỳ (cột 0)
                        Cell hocKyCell = row.getCell(0);
                        if (hocKyCell != null) {
                            try {
                                if (hocKyCell.getCellType() == CellType.NUMERIC) {
                                    ctdtData.hocKy = (int) Math.round(hocKyCell.getNumericCellValue());
                                } else if (hocKyCell.getCellType() == CellType.STRING) {
                                    String hocKyStr = hocKyCell.getStringCellValue().trim();
                                    if (!hocKyStr.isEmpty()) {
                                        ctdtData.hocKy = Integer.parseInt(hocKyStr);
                                    }
                                }
                            } catch (Exception e) {
                                throw new Exception("Học kỳ không hợp lệ ở dòng " + ctdtData.rowNum);
                            }
                        }

                        if (ctdtData.hocKy == null) {
                            throw new Exception("Học kỳ không được để trống ở dòng " + ctdtData.rowNum);
                        }
                        System.out.println("Học kỳ: " + ctdtData.hocKy);

                        // 2. Mã học phần (cột 1)
                        Cell maHocPhanCell = row.getCell(1);
                        if (maHocPhanCell != null) {
                            if (maHocPhanCell.getCellType() == CellType.STRING) {
                                ctdtData.maHocPhan = maHocPhanCell.getStringCellValue().trim();
                            } else if (maHocPhanCell.getCellType() == CellType.NUMERIC) {
                                // Giữ nguyên số, không chuyển thành long để giữ format
                                double value = maHocPhanCell.getNumericCellValue();
                                if (value == Math.floor(value)) {
                                    ctdtData.maHocPhan = String.valueOf((long) value);
                                } else {
                                    ctdtData.maHocPhan = String.valueOf(value);
                                }
                            }
                        }

                        if (ctdtData.maHocPhan != null && !ctdtData.maHocPhan.isEmpty()) {
                            ctdtData.key = ctdtData.maHocPhan;
                            System.out.println("Mã học phần: " + ctdtData.maHocPhan);
                        }

                        // 3. Tên học phần (cột 2) - BẮT BUỘC
                        Cell tenHocPhanCell = row.getCell(2);
                        if (tenHocPhanCell == null) {
                            throw new Exception("Tên học phần không được để trống ở dòng " + ctdtData.rowNum);
                        }

                        if (tenHocPhanCell.getCellType() == CellType.STRING) {
                            ctdtData.tenHocPhan = tenHocPhanCell.getStringCellValue().trim();
                        } else if (tenHocPhanCell.getCellType() == CellType.NUMERIC) {
                            ctdtData.tenHocPhan = String.valueOf(tenHocPhanCell.getNumericCellValue());
                        } else if (tenHocPhanCell.getCellType() == CellType.FORMULA) {
                            // Xử lý công thức
                            switch (tenHocPhanCell.getCachedFormulaResultType()) {
                                case STRING:
                                    ctdtData.tenHocPhan = tenHocPhanCell.getStringCellValue().trim();
                                    break;
                                case NUMERIC:
                                    ctdtData.tenHocPhan = String.valueOf(tenHocPhanCell.getNumericCellValue());
                                    break;
                                default:
                                    ctdtData.tenHocPhan = "";
                            }
                        }

                        if (ctdtData.tenHocPhan == null || ctdtData.tenHocPhan.isEmpty()) {
                            throw new Exception("Tên học phần không được để trống ở dòng " + ctdtData.rowNum);
                        }

                        // Nếu không có mã học phần, dùng tên học phần làm key
                        if (ctdtData.key == null) {
                            ctdtData.key = ctdtData.tenHocPhan;
                        }
                        System.out.println("Tên học phần: " + ctdtData.tenHocPhan);

                        // 4. Số tín chỉ (cột 3)
                        Cell tinChiCell = row.getCell(3);
                        if (tinChiCell != null) {
                            try {
                                if (tinChiCell.getCellType() == CellType.NUMERIC) {
                                    ctdtData.tinChi = (int) Math.round(tinChiCell.getNumericCellValue());
                                } else if (tinChiCell.getCellType() == CellType.STRING) {
                                    String tinChiStr = tinChiCell.getStringCellValue().trim();
                                    if (!tinChiStr.isEmpty()) {
                                        ctdtData.tinChi = Integer.parseInt(tinChiStr);
                                    }
                                } else if (tinChiCell.getCellType() == CellType.FORMULA) {
                                    // Xử lý công thức
                                    if (tinChiCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                                        ctdtData.tinChi = (int) Math.round(tinChiCell.getNumericCellValue());
                                    }
                                }
                            } catch (Exception e) {
                                throw new Exception("Số tín chỉ không hợp lệ ở dòng " + ctdtData.rowNum);
                            }
                        }

                        if (ctdtData.tinChi == null) {
                            throw new Exception("Số tín chỉ không được để trống ở dòng " + ctdtData.rowNum);
                        }
                        System.out.println("Số tín chỉ: " + ctdtData.tinChi);

                        // 5. Loại (cột 4) - BB/TC - QUAN TRỌNG
                        Cell loaiCell = row.getCell(4);
                        if (loaiCell != null) {
                            String loaiValue = "";

                            // Đọc giá trị từ ô
                            if (loaiCell.getCellType() == CellType.STRING) {
                                loaiValue = loaiCell.getStringCellValue().trim();
                            } else if (loaiCell.getCellType() == CellType.NUMERIC) {
                                // Nếu là số, chuyển thành chuỗi
                                double numValue = loaiCell.getNumericCellValue();
                                loaiValue = String.valueOf((int) numValue);
                            } else if (loaiCell.getCellType() == CellType.FORMULA) {
                                // Xử lý công thức
                                if (loaiCell.getCachedFormulaResultType() == CellType.STRING) {
                                    loaiValue = loaiCell.getStringCellValue().trim();
                                } else if (loaiCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                                    double numValue = loaiCell.getNumericCellValue();
                                    loaiValue = String.valueOf((int) numValue);
                                }
                            }

                            // Chuẩn hóa và validate
                            if (!loaiValue.isEmpty()) {
                                loaiValue = loaiValue.toUpperCase().trim();

                                // Xử lý các trường hợp phổ biến
                                if (loaiValue.equals("BB") || loaiValue.equals("BẮT BUỘC") || loaiValue.equals("BAT BUOC")) {
                                    ctdtData.loai = "BB";
                                    System.out.println("Loại: BB (Bắt buộc)");
                                } else if (loaiValue.equals("TC") || loaiValue.equals("TỰ CHỌN") || loaiValue.equals("TU CHON")) {
                                    ctdtData.loai = "TC";
                                    System.out.println("Loại: TC (Tự chọn)");
                                } else if (loaiValue.equals("1") || loaiValue.equals("B")) {
                                    // Một số file dùng 1 cho bắt buộc, 2 cho tự chọn
                                    ctdtData.loai = "BB";
                                    System.out.println("Loại: BB (từ giá trị '" + loaiValue + "')");
                                } else if (loaiValue.equals("2") || loaiValue.equals("T")) {
                                    ctdtData.loai = "TC";
                                    System.out.println("Loại: TC (từ giá trị '" + loaiValue + "')");
                                } else {
                                    // Mặc định là bắt buộc nếu giá trị không hợp lệ
                                    ctdtData.loai = "BB";
                                    System.out.println("Cảnh báo: Loại không hợp lệ '" + loaiValue + "', mặc định thành BB");
                                }
                            } else {
                                // Nếu ô trống, mặc định là bắt buộc
                                ctdtData.loai = "BB";
                                System.out.println("Loại: BB (mặc định - ô trống)");
                            }
                        } else {
                            // Nếu không có ô, mặc định là bắt buộc
                            ctdtData.loai = "BB";
                            System.out.println("Loại: BB (mặc định - không có ô)");
                        }

                        // 6. Nhóm tự chọn (cột 5) - Chỉ cho môn TC
                        Cell nhomTCCell = row.getCell(5);
                        if (nhomTCCell != null) {
                            String nhomTCValue = "";

                            if (nhomTCCell.getCellType() == CellType.STRING) {
                                nhomTCValue = nhomTCCell.getStringCellValue().trim();
                            } else if (nhomTCCell.getCellType() == CellType.NUMERIC) {
                                double numValue = nhomTCCell.getNumericCellValue();
                                if (numValue == Math.floor(numValue)) {
                                    nhomTCValue = String.valueOf((long) numValue);
                                } else {
                                    nhomTCValue = String.valueOf(numValue);
                                }
                            } else if (nhomTCCell.getCellType() == CellType.FORMULA) {
                                if (nhomTCCell.getCachedFormulaResultType() == CellType.STRING) {
                                    nhomTCValue = nhomTCCell.getStringCellValue().trim();
                                } else if (nhomTCCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                                    double numValue = nhomTCCell.getNumericCellValue();
                                    nhomTCValue = String.valueOf((long) numValue);
                                }
                            }

                            if (!nhomTCValue.isEmpty()) {
                                ctdtData.nhomTC = nhomTCValue;
                                System.out.println("Nhóm TC: " + ctdtData.nhomTC);
                            }
                        }

                        // Kiểm tra môn tự chọn nhưng không có nhóm TC
                        if ("TC".equals(ctdtData.loai) && (ctdtData.nhomTC == null || ctdtData.nhomTC.isEmpty())) {
                            System.out.println("Cảnh báo: Môn tự chọn không có nhóm TC ở dòng " + ctdtData.rowNum);
                            // Có thể set giá trị mặc định nếu cần
                            // ctdtData.nhomTC = "1/1";
                        }

                        // Kiểm tra môn bắt buộc nhưng có nhóm TC (nên xóa)
                        if ("BB".equals(ctdtData.loai) && ctdtData.nhomTC != null && !ctdtData.nhomTC.isEmpty()) {
                            System.out.println("Cảnh báo: Môn bắt buộc có nhóm TC, sẽ bị bỏ qua ở dòng " + ctdtData.rowNum);
                            ctdtData.nhomTC = null;
                        }

                        // 7. Ngành (cột 6)
                        Cell nganhCell = row.getCell(6);
                        if (nganhCell != null) {
                            if (nganhCell.getCellType() == CellType.STRING) {
                                ctdtData.nganh = nganhCell.getStringCellValue().trim();
                            } else if (nganhCell.getCellType() == CellType.NUMERIC) {
                                ctdtData.nganh = String.valueOf((long) nganhCell.getNumericCellValue());
                            } else if (nganhCell.getCellType() == CellType.FORMULA) {
                                if (nganhCell.getCachedFormulaResultType() == CellType.STRING) {
                                    ctdtData.nganh = nganhCell.getStringCellValue().trim();
                                }
                            }

                            if (ctdtData.nganh != null && !ctdtData.nganh.isEmpty()) {
                                System.out.println("Ngành: " + ctdtData.nganh);
                            }
                        }

                        // 8. Chuyên ngành (cột 7)
                        Cell chuyenNganhCell = row.getCell(7);
                        if (chuyenNganhCell != null) {
                            if (chuyenNganhCell.getCellType() == CellType.STRING) {
                                ctdtData.chuyenNganh = chuyenNganhCell.getStringCellValue().trim();
                            } else if (chuyenNganhCell.getCellType() == CellType.NUMERIC) {
                                ctdtData.chuyenNganh = String.valueOf((long) chuyenNganhCell.getNumericCellValue());
                            } else if (chuyenNganhCell.getCellType() == CellType.FORMULA) {
                                if (chuyenNganhCell.getCachedFormulaResultType() == CellType.STRING) {
                                    ctdtData.chuyenNganh = chuyenNganhCell.getStringCellValue().trim();
                                }
                            }

                            if (ctdtData.chuyenNganh != null && !ctdtData.chuyenNganh.isEmpty()) {
                                System.out.println("Chuyên ngành: " + ctdtData.chuyenNganh);
                            }
                        }

                        // 9. Học phần tiên quyết (cột 8) - CẢI THIỆN
                        Cell tienQuyetCell = row.getCell(8);
                        if (tienQuyetCell != null) {
                            String tienQuyetStr = "";

                            if (tienQuyetCell.getCellType() == CellType.STRING) {
                                tienQuyetStr = tienQuyetCell.getStringCellValue().trim();
                            } else if (tienQuyetCell.getCellType() == CellType.NUMERIC) {
                                tienQuyetStr = String.valueOf((long) tienQuyetCell.getNumericCellValue());
                            } else if (tienQuyetCell.getCellType() == CellType.FORMULA) {
                                if (tienQuyetCell.getCachedFormulaResultType() == CellType.STRING) {
                                    tienQuyetStr = tienQuyetCell.getStringCellValue().trim();
                                } else if (tienQuyetCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                                    tienQuyetStr = String.valueOf((long) tienQuyetCell.getNumericCellValue());
                                }
                            }

                            if (!tienQuyetStr.isEmpty()) {
                                System.out.println("Học phần tiên quyết (raw): " + tienQuyetStr);

                                List<String> tienQuyetItems = new ArrayList<>();

                                // Xử lý nhiều dạng phân cách
                                // 1. Dấu phẩy
                                String[] commaItems = tienQuyetStr.split(",");
                                for (String item : commaItems) {
                                    String trimmed = item.trim();
                                    if (!trimmed.isEmpty()) {
                                        // Kiểm tra xem có chứa "và", "and" không
                                        if (trimmed.contains(" và ") || trimmed.contains(" and ")) {
                                            String[] andItems = trimmed.split("( và | and )");
                                            for (String andItem : andItems) {
                                                String andTrimmed = andItem.trim();
                                                if (!andTrimmed.isEmpty()) {
                                                    tienQuyetItems.add(andTrimmed);
                                                }
                                            }
                                        } else {
                                            tienQuyetItems.add(trimmed);
                                        }
                                    }
                                }

                                // Nếu không tìm thấy dấu phẩy, thử dấu chấm phẩy
                                if (tienQuyetItems.isEmpty()) {
                                    String[] semicolonItems = tienQuyetStr.split(";");
                                    for (String item : semicolonItems) {
                                        String trimmed = item.trim();
                                        if (!trimmed.isEmpty()) {
                                            tienQuyetItems.add(trimmed);
                                        }
                                    }
                                }

                                if (!tienQuyetItems.isEmpty()) {
                                    // Lưu tạm, sẽ put vào tienQuyetMap sau khi composite key được tạo
                                    ctdtData.pendingTienQuyetItems = tienQuyetItems;
                                    System.out.println("Học phần tiên quyết (parsed): " + tienQuyetItems);
                                }
                            }
                        }

                        // Gán tabNganh = targetNganh nếu đang import từ tab ngành cụ thể.
                        // tabNganh là "container tab" - KHÔNG ghi đè field nganh của học phần.
                        // Học phần có thể không có nganh/chuyenNganh riêng vẫn thuộc tab này.
                        if (targetNganh != null && !targetNganh.isEmpty()) {
                            ctdtData.tabNganh = targetNganh;
                        }

                        // Tạo composite key TRƯỚC, rồi mới put vào các map
                        String mapKey = (ctdtData.tabNganh != null ? ctdtData.tabNganh : "__noTab__")
                                + "|||" + ctdtData.key;
                        ctdtData.key = mapKey;

                        hocPhanDataMap.put(mapKey, ctdtData);

                        // Bây giờ put vào tienQuyetMap với đúng composite key
                        if (ctdtData.pendingTienQuyetItems != null && !ctdtData.pendingTienQuyetItems.isEmpty()) {
                            tienQuyetMap.put(mapKey, ctdtData.pendingTienQuyetItems);
                        }

                        System.out.println("✓ Đã đọc thành công dòng " + ctdtData.rowNum);

                    } catch (Exception e) {
                        System.err.println("✗ Lỗi đọc dòng " + (i + 1) + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                workbook.close();
                System.out.println("\n=== ĐÃ ĐỌC XONG FILE ===");
                System.out.println("Tổng học phần đọc được: " + hocPhanDataMap.size());
                System.out.println("Số học phần có tiên quyết: " + tienQuyetMap.size());

                // Thống kê loại học phần
                long bbCount = hocPhanDataMap.values().stream()
                        .filter(d -> "BB".equals(d.loai))
                        .count();
                long tcCount = hocPhanDataMap.values().stream()
                        .filter(d -> "TC".equals(d.loai))
                        .count();
                System.out.println("Bắt buộc (BB): " + bbCount);
                System.out.println("Tự chọn (TC): " + tcCount);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Lỗi khi đọc file Excel: " + e.getMessage());
            }

            // ============ BƯỚC 2: LƯU HỌC PHẦN VÀO DATABASE ============
            System.out.println("\n=== BẮT ĐẦU LƯU HỌC PHẦN ===");

            Map<String, Long> keyToIdMap = new HashMap<>();
            int savedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;

            for (CTDTData data : hocPhanDataMap.values()) {
                try {
                    System.out.println("\n--- Xử lý học phần: " + data.tenHocPhan + " (dòng " + data.rowNum + ") ---");
                    System.out.println("  Loại: " + data.loai + ", TabNganh: " + data.tabNganh);

                    // Kiểm tra trùng — scope theo tabNganh để mỗi tab có CTDT riêng
                    // (học phần có thể không có nganh/chuyenNganh nhưng vẫn thuộc 1 tab)
                    boolean isDuplicate = false;
                    Long existingId = null;

                    final String effectiveTabNganh = data.tabNganh != null ? data.tabNganh : "";

                    // Tìm theo mã học phần (chỉ trùng nếu cùng tabNganh)
                    if (data.maHocPhan != null && !data.maHocPhan.isEmpty()) {
                        List<CTDT> byMaHocPhan = ctdtRepo.findByMaHocPhan(data.maHocPhan);
                        for (CTDT existing : byMaHocPhan) {
                            String existingTab = existing.getTabNganh() != null ? existing.getTabNganh() : "";
                            if (existingTab.equals(effectiveTabNganh)) {
                                existingId = existing.getId();
                                isDuplicate = true;
                                System.out.println("  ⚠ Trùng mã học phần trong cùng tab: " + data.maHocPhan);
                                break;
                            }
                        }
                    }

                    // Tìm theo tên học phần + học kỳ + tabNganh
                    if (!isDuplicate) {
                        List<CTDT> byTenHocPhan = ctdtRepo.findByTenHocPhan(data.tenHocPhan);
                        for (CTDT existing : byTenHocPhan) {
                            String existingTab = existing.getTabNganh() != null ? existing.getTabNganh() : "";
                            if (existing.getHocKy() != null && existing.getHocKy().equals(data.hocKy)
                                    && existingTab.equals(effectiveTabNganh)) {
                                existingId = existing.getId();
                                isDuplicate = true;
                                System.out.println("  ⚠ Trùng tên + học kỳ + tab: " + data.tenHocPhan);
                                break;
                            }
                        }
                    }

                    if (isDuplicate && existingId != null) {
                        // Học phần đã tồn tại
                        keyToIdMap.put(data.key, existingId);
                        skippedCount++;

                        Map<String, Object> result = new HashMap<>();
                        result.put("row", data.rowNum);
                        result.put("status", "skipped");
                        result.put("message", "Học phần đã tồn tại");
                        result.put("data", Map.of(
                                "maHocPhan", data.maHocPhan != null ? data.maHocPhan : "",
                                "tenHocPhan", data.tenHocPhan,
                                "hocKy", data.hocKy,
                                "loai", data.loai,
                                "tinChi", data.tinChi
                        ));
                        results.add(result);

                        System.out.println("  ⏭ Đã bỏ qua (tồn tại)");

                    } else {
                        // Tạo học phần mới
                        CTDT ctdt = new CTDT();
                        ctdt.setMaHocPhan(data.maHocPhan);
                        ctdt.setHocKy(data.hocKy);
                        ctdt.setTenHocPhan(data.tenHocPhan);
                        ctdt.setTinChi(data.tinChi);
                        ctdt.setLoai(data.loai);

                        // Chỉ set nhóm TC nếu là môn tự chọn
                        if ("TC".equals(data.loai) && data.nhomTC != null && !data.nhomTC.isEmpty()) {
                            ctdt.setNhomTC(data.nhomTC);
                            System.out.println("  Đã set nhóm TC: " + data.nhomTC);
                        } else {
                            ctdt.setNhomTC(null);
                        }

                        // Giữ nguyên nganh/chuyenNganh từ file (có thể null)
                        ctdt.setNganh(data.nganh);
                        ctdt.setChuyenNganh(data.chuyenNganh);
                        // Set tabNganh = tab đang import vào (field quyết định học phần hiện ở tab nào)
                        ctdt.setTabNganh(data.tabNganh);
                        ctdt.setHocPhanTienQuyet(new ArrayList<>());
                        ctdt.setCreatedAt(LocalDateTime.now());
                        ctdt.setUpdatedAt(LocalDateTime.now());

                        CTDT savedCTDT = ctdtRepo.save(ctdt);

                        keyToIdMap.put(data.key, savedCTDT.getId());
                        savedCount++;

                        Map<String, Object> result = new HashMap<>();
                        result.put("row", data.rowNum);
                        result.put("status", "success");
                        result.put("message", "Thêm thành công");
                        result.put("data", Map.of(
                                "maHocPhan", data.maHocPhan,
                                "tenHocPhan", data.tenHocPhan,
                                "hocKy", data.hocKy,
                                "loai", data.loai,
                                "tinChi", data.tinChi,
                                "id", savedCTDT.getId()
                        ));
                        results.add(result);

                        System.out.println("  ✅ Đã lưu thành công, ID: " + savedCTDT.getId() + ", Loại: " + data.loai);
                    }

                } catch (Exception e) {
                    errorCount++;
                    System.err.println("  ❌ Lỗi: " + e.getMessage());
                    e.printStackTrace();

                    Map<String, Object> result = new HashMap<>();
                    result.put("row", data.rowNum);
                    result.put("status", "error");
                    result.put("message", "Lỗi: " + e.getMessage());
                    results.add(result);
                }
            }

            // ============ BƯỚC 3: CẬP NHẬT HỌC PHẦN TIÊN QUYẾT ============
            System.out.println("\n=== BẮT ĐẦU CẬP NHẬT TIÊN QUYẾT ===");

            if (!tienQuyetMap.isEmpty()) {
                // Load tất cả học phần từ database để tìm kiếm
                List<CTDT> allCTDT = ctdtRepo.findAll();

                // Tạo các map để tìm kiếm hiệu quả - SỬA: tạo final copy
                final Map<String, Long> tenHocPhanToIdMap = new HashMap<>();
                final Map<String, Long> maHocPhanToIdMap = new HashMap<>();
                final Map<String, Long> searchMapNoDiacritics = new HashMap<>();

                System.out.println("Tổng số học phần trong DB: " + allCTDT.size());

                // Xây dựng các map tìm kiếm
                for (CTDT ctdt : allCTDT) {
                    final Long ctdtId = ctdt.getId(); // Tạo final copy

                    // Map theo tên học phần (chuẩn hóa)
                    if (ctdt.getTenHocPhan() != null && !ctdt.getTenHocPhan().isEmpty()) {
                        String tenHP = ctdt.getTenHocPhan().trim();
                        tenHocPhanToIdMap.put(tenHP.toLowerCase(), ctdtId);

                        // Map không dấu để tìm kiếm mềm dẻo
                        String tenHPNoDiacritics = removeDiacritics(tenHP.toLowerCase());
                        searchMapNoDiacritics.put(tenHPNoDiacritics, ctdtId);

                        // Map một phần tên học phần (các từ đầu)
                        String[] words = tenHP.split("\\s+");
                        if (words.length > 0) {
                            StringBuilder firstWordsBuilder = new StringBuilder();
                            for (int i = 0; i < Math.min(3, words.length); i++) {
                                firstWordsBuilder.append(words[i]).append(" ");
                            }
                            String firstWords = firstWordsBuilder.toString().trim().toLowerCase();
                            if (!searchMapNoDiacritics.containsKey(firstWords)) {
                                searchMapNoDiacritics.put(removeDiacritics(firstWords), ctdtId);
                            }
                        }
                    }

                    // Map theo mã học phần
                    if (ctdt.getMaHocPhan() != null && !ctdt.getMaHocPhan().isEmpty()) {
                        maHocPhanToIdMap.put(ctdt.getMaHocPhan().trim().toLowerCase(), ctdtId);
                    }
                }

                System.out.println("Map tên học phần: " + tenHocPhanToIdMap.size() + " entries");
                System.out.println("Map mã học phần: " + maHocPhanToIdMap.size() + " entries");
                System.out.println("Map tìm kiếm mềm: " + searchMapNoDiacritics.size() + " entries");

                int tienQuyetSuccess = 0;
                int tienQuyetError = 0;
                int totalTienQuyetFound = 0;

                // Tạo final copy của allCTDT để dùng trong lambda
                final List<CTDT> finalAllCTDT = allCTDT;

                for (Map.Entry<String, List<String>> entry : tienQuyetMap.entrySet()) {
                    String targetKey = entry.getKey();
                    List<String> tienQuyetKeys = entry.getValue();

                    Long targetId = keyToIdMap.get(targetKey);
                    if (targetId == null) {
                        System.err.println("❌ Không tìm thấy ID cho: " + targetKey);
                        tienQuyetError++;
                        continue;
                    }

                    try {
                        System.out.println("\n=== Cập nhật tiên quyết cho học phần ===");
                        System.out.println("Target: " + targetKey + " (ID: " + targetId + ")");
                        System.out.println("Tiên quyết cần tìm: " + tienQuyetKeys);

                        List<Long> tienQuyetIds = new ArrayList<>();

                        for (String tienQuyetKey : tienQuyetKeys) {
                            final String originalKey = tienQuyetKey.trim(); // final copy
                            String searchKey = originalKey.toLowerCase();
                            String searchKeyNoDiacritics = removeDiacritics(searchKey);

                            System.out.println("  Tìm kiếm: '" + originalKey + "'");

                            Long foundId = null;

                            // TRƯỜNG HỢP 1: Tìm bằng tên học phần chính xác
                            foundId = tenHocPhanToIdMap.get(searchKey);
                            if (foundId != null) {
                                System.out.println("    ✅ Tìm thấy chính xác theo tên học phần");
                            }

                            // TRƯỜNG HỢP 2: Tìm bằng mã học phần (nếu có)
                            if (foundId == null && originalKey.matches(".*\\d+.*")) {
                                foundId = maHocPhanToIdMap.get(searchKey);
                                if (foundId != null) {
                                    System.out.println("    ✅ Tìm thấy theo mã học phần");
                                }
                            }

                            // TRƯỜNG HỢP 3: Tìm kiếm mềm dẻo theo từ khớp (word-based matching)
                            // FIX LỖI 1: Thay "contains hai chiều" bằng "đếm từ khớp chính xác"
                            // để tránh false positive: "ứng dụng" không match "Toán cao cấp và ứng dụng"
                            if (foundId == null) {
                                String[] searchWords = searchKeyNoDiacritics.split("\\s+");
                                // Chỉ áp dụng tìm mềm khi searchKey có ít nhất 3 từ
                                if (searchWords.length >= 3) {
                                    // Tính số từ có nghĩa (>= 3 ký tự, loại bỏ giới từ ngắn)
                                    long meaningfulWordCount = Arrays.stream(searchWords)
                                            .filter(w -> w.length() >= 3)
                                            .count();

                                    if (meaningfulWordCount >= 2) {
                                        Long bestId = null;
                                        int bestMatchCount = 0;

                                        for (Map.Entry<String, Long> dbEntry : searchMapNoDiacritics.entrySet()) {
                                            String[] dbWords = dbEntry.getKey().split("\\s+");
                                            int matchCount = 0;

                                            for (String sw : searchWords) {
                                                if (sw.length() >= 3) {
                                                    for (String dw : dbWords) {
                                                        if (dw.equals(sw)) {
                                                            matchCount++;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }

                                            // Điều kiện match: >= 60% từ có nghĩa khớp VÀ ít nhất 2 từ khớp
                                            double matchRatio = (double) matchCount / meaningfulWordCount;
                                            if (matchRatio >= 0.6 && matchCount >= 2 && matchCount > bestMatchCount) {
                                                bestMatchCount = matchCount;
                                                bestId = dbEntry.getValue();
                                            }
                                        }

                                        if (bestId != null) {
                                            foundId = bestId;
                                            System.out.println("    🔍 Tìm thấy theo từ khớp (" + bestMatchCount + "/" + meaningfulWordCount + " từ)");
                                        }
                                    }
                                }
                            }

                            // TRƯỜNG HỢP 4: Tìm theo tên bắt đầu bằng (startsWith) — cho tên rút gọn không có "..."
                            if (foundId == null && searchKeyNoDiacritics.length() >= 10) {
                                for (CTDT ctdt : finalAllCTDT) {
                                    if (ctdt.getTenHocPhan() != null) {
                                        String tenHPNoDiacritics = removeDiacritics(ctdt.getTenHocPhan().toLowerCase().trim());
                                        // searchKey phải là tiền tố của tên HP trong DB (không ngược lại)
                                        if (tenHPNoDiacritics.startsWith(searchKeyNoDiacritics) ||
                                                searchKeyNoDiacritics.startsWith(tenHPNoDiacritics)) {
                                            foundId = ctdt.getId();
                                            System.out.println("    🔍 Tìm thấy theo tiền tố: '" + ctdt.getTenHocPhan() + "'");
                                            break;
                                        }
                                    }
                                }
                            }

                            // TRƯỜNG HỢP ĐẶC BIỆT: Xử lý tên rút gọn có "..."
                            if (foundId == null && originalKey.contains("...")) {
                                String partialKey = originalKey.replace("...", "").trim();
                                if (!partialKey.isEmpty()) {
                                    System.out.println("    🔄 Thử tìm với tên rút gọn: '" + partialKey + "'");
                                    // Tìm lại với tên rút gọn
                                    for (CTDT ctdt : finalAllCTDT) {
                                        if (ctdt.getTenHocPhan() != null &&
                                                ctdt.getTenHocPhan().toLowerCase().startsWith(partialKey.toLowerCase())) {
                                            foundId = ctdt.getId();
                                            System.out.println("    ✅ Tìm thấy với tên rút gọn");
                                            break;
                                        }
                                    }
                                }
                            }

                            if (foundId != null && !foundId.equals(targetId)) {
                                if (!tienQuyetIds.contains(foundId)) {
                                    tienQuyetIds.add(foundId);
                                    totalTienQuyetFound++;

                                    // Tìm tên học phần tương ứng - SỬA: sử dụng stream đúng cách
                                    final Long finalFoundId = foundId; // Tạo final copy
                                    String foundName = finalAllCTDT.stream()
                                            .filter(hp -> hp.getId().equals(finalFoundId))
                                            .map(CTDT::getTenHocPhan)
                                            .findFirst()
                                            .orElse("Unknown");

                                    System.out.println("    ➕ Đã thêm tiên quyết: " + foundName + " (ID: " + foundId + ")");
                                } else {
                                    System.out.println("    ⚠ Đã có trong danh sách, bỏ qua");
                                }
                            } else if (foundId != null && foundId.equals(targetId)) {
                                System.out.println("    ⚠ Tự tham chiếu bị bỏ qua: " + originalKey);
                            } else {
                                System.out.println("    ❌ Không tìm thấy học phần tiên quyết: " + originalKey);
                                tienQuyetError++;
                            }
                        }

                        if (!tienQuyetIds.isEmpty()) {
                            // Gọi method riêng để cập nhật tiên quyết
                            updateTienQuyetForCTDT(targetId, tienQuyetIds);

                            tienQuyetSuccess++;
                            System.out.println("    ✅ Đã cập nhật " + tienQuyetIds.size() + " tiên quyết");

                            // Cập nhật kết quả hiển thị - SỬA: tránh lambda bên trong lambda
                            final Long finalTargetId = targetId; // Tạo final copy
                            for (Map<String, Object> result : results) {
                                if (result.get("row") != null && result.containsKey("data")) {
                                    Map<String, Object> resultData = (Map<String, Object>) result.get("data");
                                    if (resultData != null && finalTargetId.equals(resultData.get("id"))) {
                                        String oldMessage = (String) result.get("message");
                                        result.put("message", oldMessage + " (có " + tienQuyetIds.size() + " tiên quyết)");
                                        break;
                                    }
                                }
                            }
                        } else {
                            System.out.println("    ℹ Không tìm thấy tiên quyết hợp lệ");
                        }

                    } catch (Exception e) {
                        tienQuyetError++;
                        System.err.println("    ❌ Lỗi cập nhật tiên quyết: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                System.out.println("\n📊 Kết quả cập nhật tiên quyết:");
                System.out.println("  ✅ Thành công: " + tienQuyetSuccess + " học phần");
                System.out.println("  📋 Tìm thấy: " + totalTienQuyetFound + " học phần tiên quyết");
                System.out.println("  ❌ Lỗi tìm kiếm: " + tienQuyetError + " mục");

            } else {
                System.out.println("ℹ Không có học phần nào có tiên quyết để cập nhật");
            }

            System.out.println("\n=== 🎯 TỔNG KẾT IMPORT ===");
            System.out.println("📈 Tổng học phần: " + hocPhanDataMap.size());
            System.out.println("✅ Lưu mới: " + savedCount);
            System.out.println("⏭ Bỏ qua (tồn tại): " + skippedCount);
            System.out.println("❌ Lỗi: " + errorCount);

            Map<String, Object> response = new HashMap<>();
            response.put("totalRecords", hocPhanDataMap.size());
            response.put("successCount", savedCount + skippedCount);
            response.put("errorCount", errorCount);
            response.put("savedCount", savedCount);
            response.put("skippedCount", skippedCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi import file Excel: " + e.getMessage());
        }
    }

    // ============ HELPER METHOD: REMOVE DIACRITICS ============
    private String removeDiacritics(String str) {
        if (str == null) return "";

        str = str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        str = str.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        str = str.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        str = str.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        str = str.replaceAll("[ìíịỉĩ]", "i");
        str = str.replaceAll("[ÌÍỊỈĨ]", "I");
        str = str.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        str = str.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        str = str.replaceAll("[ùúụủũưừứựửữ]", "u");
        str = str.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        str = str.replaceAll("[ỳýỵỷỹ]", "y");
        str = str.replaceAll("[ỲÝỴỶỸ]", "Y");
        str = str.replaceAll("[đ]", "d");
        str = str.replaceAll("[Đ]", "D");

        // Loại bỏ các ký tự đặc biệt
        str = str.replaceAll("[\\p{Punct}]", "");

        return str;
    }

    // ============ HELPER CLASS: CTDTData ============
    class CTDTData {
        Integer rowNum;
        String key;
        String maHocPhan;
        Integer hocKy;
        String tenHocPhan;
        Integer tinChi;
        String loai = "BB";
        String nhomTC;
        String nganh;        // Ngành học thuật của học phần (từ file, có thể null)
        String chuyenNganh;  // Chuyên ngành của học phần (từ file, có thể null)
        String tabNganh;     // Tab chứa học phần này (= targetNganh khi import)
        List<String> pendingTienQuyetItems; // Lưu tạm tiên quyết trước khi composite key được tạo
    }

    // ============ DOWNLOAD EXCEL TEMPLATE (ADMIN) ============
    @GetMapping("/download-template")
    @ResponseBody
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        try {
            // Tạo workbook mới
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CTDT_Template");

            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Học kỳ*",
                    "Mã học phần",
                    "Tên học phần*",
                    "Số tín chỉ*",
                    "Loại (BB/TC)*",
                    "Nhóm TC (nếu TC)",
                    "Nganh",
                    "Chuyên ngành",
                    "Nhóm học phần",
                    "Học phần tiên quyết (ID hoặc tên, cách nhau bằng dấu phẩy)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }
            // Thêm dòng ví dụ
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue(1);
            exampleRow.createCell(1).setCellValue("71ITBS10203");
            exampleRow.createCell(2).setCellValue("Nhập môn lập trình");
            exampleRow.createCell(3).setCellValue(3);
            exampleRow.createCell(4).setCellValue("BB");
            exampleRow.createCell(5).setCellValue("");
            exampleRow.createCell(6).setCellValue("Công nghệ thông tin");
            exampleRow.createCell(7).setCellValue("Kỹ thuật phần mềm");
            exampleRow.createCell(8).setCellValue("");
            exampleRow.createCell(9).setCellValue("");

            // Thêm dòng ví dụ tự chọn
            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue(5);
            exampleRow2.createCell(1).setCellValue("71ITBS50201");
            exampleRow2.createCell(2).setCellValue("Lập trình di động");
            exampleRow2.createCell(3).setCellValue(3);
            exampleRow2.createCell(4).setCellValue("TC");
            exampleRow2.createCell(5).setCellValue("2/3");
            exampleRow2.createCell(6).setCellValue("Công nghệ thông tin");
            exampleRow2.createCell(7).setCellValue("Kỹ thuật phần mềm");
            exampleRow2.createCell(8).setCellValue("Nhóm môn tự chọn chuyên ngành");
            exampleRow2.createCell(9).setCellValue("71ITBS10203");

            // Tạo file Excel trong memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            // Trả về file
            byte[] excelBytes = baos.toByteArray();

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentDisposition(ContentDisposition.attachment()
                    .filename("CTDT_Template.xlsx")
                    .build());

            return new ResponseEntity<>(excelBytes, httpHeaders, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
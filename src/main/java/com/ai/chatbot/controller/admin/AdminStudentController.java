package com.ai.chatbot.controller.admin;

import com.ai.chatbot.model.User;
import com.ai.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/api/admin")
public class AdminStudentController {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ============ LOAD STUDENTS ============
    @GetMapping("/load-students")
    @ResponseBody
    public ResponseEntity<?> loadStudents() {
        try {
            List<User> students = userRepository.findAllStudentsOrderByLastUpdated()
                    .stream()
                    .filter(s -> "USER".equals(s.getRole()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải danh sách sinh viên: " + e.getMessage());
        }
    }

    // ============ GET STUDENT BY ID ============
    @GetMapping("/students/{id}")
    @ResponseBody
    public ResponseEntity<?> getStudent(@PathVariable Long id) {
        try {
            System.out.println("=== DEBUG getStudent ===");
            System.out.println("Requested student ID: " + id);
            Optional<User> studentOpt = userRepository.findById(id);
            if (!studentOpt.isPresent()) {
                System.out.println("Student not found in database");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy sinh viên với ID: " + id);
            }
            User student = studentOpt.get();
            // DEBUG: Log thông tin sinh viên
            System.out.println("Found student:");
            System.out.println("  MSSV: " + student.getMssv());
            System.out.println("  Full Name: " + student.getFullName());
            System.out.println("  Birth: " + student.getBirth());
            System.out.println("  Birth Object Type: " + (student.getBirth() != null ? student.getBirth().getClass().getName() : "null"));
            System.out.println("  Department: " + student.getDepartment());
            System.out.println("  Major: " + student.getMajor());
            // Format date thành YYYY-MM-DD trước khi trả về
            String formattedBirth = null;
            if (student.getBirth() != null) {
                try {
                    // Đảm bảo trả về đúng định dạng LocalDate
                    formattedBirth = student.getBirth().toString(); // YYYY-MM-DD
                    System.out.println("Formatted birth for response: " + formattedBirth);
                } catch (Exception e) {
                    System.err.println("Error formatting birth date: " + e.getMessage());
                }
            }
            // Tạo Map response với đúng tên trường
            Map<String, Object> response = new LinkedHashMap<>();
                response.put("id", student.getId());
                response.put("mssv", student.getMssv());
                response.put("fullName", student.getFullName());
                response.put("birth", formattedBirth); // Trả về String YYYY-MM-DD
                response.put("course", student.getCourse());
                response.put("department", student.getDepartment());
                response.put("major", student.getMajor());
                response.put("enabled", student.getEnabled() != null ? student.getEnabled() : 0);
                response.put("sex", student.getSex());
                response.put("gmail", student.getGmail());
                response.put("phone", student.getPhone());
                response.put("address", student.getAddress());
                response.put("nation", student.getNation() != null ? student.getNation() : "Việt Nam");
            // Log toàn bộ response để debug
            System.out.println("=== FINAL RESPONSE ===");
            response.forEach((key, value) -> {
                System.out.println(key + ": " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
            });
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in getStudent API:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi server: " + e.getMessage());
        }
    }

    // ============ SAVE STUDENT ============
    @PostMapping("/save-student")
    @ResponseBody
    public ResponseEntity<?> saveStudent(@RequestBody Map<String, Object> requestData) {
        try {
            // Extract data từ request
            String mssv = (String) requestData.get("mssv");
            String fullName = (String) requestData.get("fullName");
            String birthStr = (String) requestData.get("birth");
            Long id = requestData.get("id") != null ? Long.parseLong(requestData.get("id").toString()) : null;
            System.out.println("=== DEBUG saveStudent ===");
            System.out.println("Received request data:");
            System.out.println("  ID: " + id);
            System.out.println("  MSSV: " + mssv);
            System.out.println("  Full Name: " + fullName);
            System.out.println("  Birth String: " + birthStr);
            System.out.println("  Birth type: " + (birthStr != null ? birthStr.getClass().getName() : "null"));
            // Validate required fields
            if (mssv == null || mssv.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("MSSV không được để trống");
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Họ tên không được để trống");
            }
            if (birthStr == null || birthStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Ngày sinh không được để trống");
            }
            // Parse ngày sinh từ String (có thể là YYYY-MM-DD hoặc dd/MM/yyyy)
            LocalDate birthDate = null;
            try {
                if (birthStr.contains("-")) {
                    // Format: YYYY-MM-DD
                    birthDate = LocalDate.parse(birthStr);
                } else if (birthStr.contains("/")) {
                    // Format: dd/MM/yyyy
                    String[] parts = birthStr.split("/");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        birthDate = LocalDate.of(year, month, day);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi parse ngày sinh: " + e.getMessage());
                return ResponseEntity.badRequest().body("Ngày sinh không hợp lệ. Vui lòng sử dụng định dạng dd/mm/yyyy hoặc yyyy-mm-dd");
            }
            if (birthDate == null) {
                return ResponseEntity.badRequest().body("Không thể xác định định dạng ngày sinh");
            }
            // Tạo đối tượng User
            User student = new User();
            if (id != null) {
                student.setId(id);
            }
            student.setMssv(mssv.trim());
            student.setFullName(fullName.trim());
            student.setBirth(birthDate);
            // Set các trường khác từ requestData
            if (requestData.get("course") != null) {
                student.setCourse(((String) requestData.get("course")).toUpperCase());
            }
            if (requestData.get("department") != null) {
                student.setDepartment((String) requestData.get("department"));
            }
            if (requestData.get("major") != null) {
                student.setMajor((String) requestData.get("major"));
            }
            if (requestData.get("enabled") != null) {
                student.setEnabled(Integer.parseInt(requestData.get("enabled").toString()));
            }
            if (requestData.get("sex") != null) {
                student.setSex((String) requestData.get("sex"));
            }
            if (requestData.get("gmail") != null) {
                student.setGmail((String) requestData.get("gmail"));
            }
            if (requestData.get("phone") != null) {
                student.setPhone((String) requestData.get("phone"));
            }
            if (requestData.get("address") != null) {
                student.setAddress((String) requestData.get("address"));
            }
            // Luôn cập nhật thời gian sửa đổi
            student.setLastUpdated(LocalDateTime.now());
            // Kiểm tra trùng MSSV (nếu là tạo mới)
            if (id == null) {
                boolean mssvExists = userRepository.findByMssv(student.getMssv()).isPresent();
                if (mssvExists) {
                    return ResponseEntity.badRequest().body("MSSV đã tồn tại");
                }
                // Tạo mới: Tạo mật khẩu mặc định từ ngày sinh
                try {
                    String defaultPwd = student.getBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
                    student.setPassword(passwordEncoder.encode(defaultPwd));
                    System.out.println("Tạo mật khẩu mới từ ngày sinh: " + defaultPwd);
                } catch (Exception e) {
                    System.err.println("Lỗi tạo mật khẩu từ ngày sinh: " + e.getMessage());
                    student.setPassword(passwordEncoder.encode("123456"));
                }
            } else {
                // CẬP NHẬT: Kiểm tra xem ngày sinh có thay đổi không
                User existing = userRepository.findById(id).orElse(null);
                if (existing != null) {
                    // Kiểm tra xem MSSV có thay đổi không
                    if (!existing.getMssv().equals(student.getMssv())) {
                        boolean mssvExists = userRepository.findByMssv(student.getMssv()).isPresent();
                        if (mssvExists) {
                            return ResponseEntity.badRequest().body("MSSV đã tồn tại");
                        }
                    }
                    // KIỂM TRA NGÀY SINH CÓ THAY ĐỔI KHÔNG
                    boolean birthChanged = false;
                    if (existing.getBirth() != null && student.getBirth() != null) {
                        // So sánh ngày sinh cũ và mới
                        birthChanged = !existing.getBirth().isEqual(student.getBirth());
                    } else if (existing.getBirth() == null && student.getBirth() != null) {
                        birthChanged = true;
                    } else if (existing.getBirth() != null && student.getBirth() == null) {
                        birthChanged = true;
                    }
                    System.out.println("Kiểm tra thay đổi ngày sinh:");
                    System.out.println("Ngày sinh cũ: " + existing.getBirth());
                    System.out.println("Ngày sinh mới: " + student.getBirth());
                    System.out.println("Có thay đổi: " + birthChanged);
                    if (birthChanged) {
                        // NẾU NGÀY SINH THAY ĐỔI: Tạo mật khẩu mới từ ngày sinh mới
                        try {
                            String defaultPwd = student.getBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
                            student.setPassword(passwordEncoder.encode(defaultPwd));
                            System.out.println("Đã cập nhật mật khẩu mới từ ngày sinh mới: " + defaultPwd);
                        } catch (Exception e) {
                            System.err.println("Lỗi tạo mật khẩu từ ngày sinh mới: " + e.getMessage());
                            student.setPassword(passwordEncoder.encode("123456"));
                        }
                    } else {
                        // Nếu ngày sinh không thay đổi, giữ nguyên mật khẩu
                        student.setPassword(existing.getPassword());
                        System.out.println("🔒 Giữ nguyên mật khẩu cũ (ngày sinh không thay đổi)");
                    }
                }
            }
            // Set default values
            student.setRole("USER");
            if (student.getEnabled() == null) {
                student.setEnabled(0);
            }
            userRepository.save(student);
            System.out.println("Student saved successfully");
            // Thêm thông báo nếu mật khẩu đã thay đổi
            if (id != null) {
                User existing = userRepository.findById(id).orElse(null);
                if (existing != null) {
                    boolean birthChanged = false;
                    if (existing.getBirth() != null && student.getBirth() != null) {
                        birthChanged = !existing.getBirth().isEqual(student.getBirth());
                    }
                    if (birthChanged) {
                        return ResponseEntity.ok("Lưu thành công. Mật khẩu đã được cập nhật theo ngày sinh mới.");
                    }
                }
            }
            return ResponseEntity.ok("Lưu thành công");
        } catch (Exception e) {
            System.err.println("Error saving student:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lưu: " + e.getMessage());
        }
    }

    // ============ DELETE STUDENT ============
    @DeleteMapping("/delete-student/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy sinh viên");
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok("Xóa thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ============ GET STUDENT BY MSSV ============
    @GetMapping("/student-by-mssv")
    @ResponseBody
    public ResponseEntity<?> getStudentByMssv(@RequestParam String mssv) {
        try {
            return userRepository.findByMssv(mssv)
                    .<ResponseEntity<?>>map(student -> ResponseEntity.ok(student))
                    .orElseGet(() -> ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body("Không tìm thấy sinh viên"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    // ============ SEARCH STUDENTS ============
    @GetMapping("/search-students")
    @ResponseBody
    public ResponseEntity<?> searchStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String course) {
        try {
            // DEBUG: Log parameters
            System.out.println("=== DEBUG searchStudents ===");
            System.out.println("keyword: " + keyword);
            System.out.println("department: " + department);
            System.out.println("major: " + major);
            System.out.println("course: " + course);
            // Gọi repository method đơn giản
            List<User> students = userRepository.searchStudents(
                    keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                    department != null && !department.trim().isEmpty() ? department.trim() : null,
                    major != null && !major.trim().isEmpty() ? major.trim() : null,
                    course != null && !course.trim().isEmpty() ? course.trim() : null
            );
            System.out.println("Found " + students.size() + " students");
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tìm kiếm: " + e.getMessage());
        }
    }

    // ============ CHECK MSSV ============
    @GetMapping("/check-mssv")
    @ResponseBody
    public ResponseEntity<?> checkMssv(@RequestParam String mssv,
                                       @RequestParam(required = false) Long currentId) {
        try {
            Optional<User> existingUser = userRepository.findByMssv(mssv);
            Map<String, Object> response = new HashMap<>();
            if (existingUser.isPresent()) {
                // Nếu là chỉnh sửa user hiện tại, cho phép trùng với chính nó
                if (currentId != null && existingUser.get().getId().equals(currentId)) {
                    response.put("exists", false);
                    response.put("message", "MSSV hợp lệ");
                } else {
                    response.put("exists", true);
                    response.put("message", "MSSV đã tồn tại trong hệ thống");
                }
            } else {
                response.put("exists", false);
                response.put("message", "MSSV hợp lệ");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra MSSV: " + e.getMessage());
        }
    }

    // ============ GET ALL MAJORS ============
    @GetMapping("/get-all-majors")
    @ResponseBody
    public ResponseEntity<?> getAllMajors() {
        try {
            List<String> allMajors = userRepository.findAll()
                    .stream()
                    .filter(s -> "USER".equals(s.getRole()))
                    .map(User::getMajor)
                    .filter(major -> major != null && !major.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(allMajors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải tất cả chuyên ngành: " + e.getMessage());
        }
    }

    // ============ GET COURSES ============
    @GetMapping("/get-courses")
    @ResponseBody
    public ResponseEntity<?> getCourses() {
        try {
            List<String> courses = userRepository.findAll()
                    .stream()
                    .filter(s -> "USER".equals(s.getRole()))
                    .map(User::getCourse)
                    .filter(course -> course != null && !course.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải danh sách khóa: " + e.getMessage());
        }
    }

    // ============ DEBUG STUDENT ============
    @GetMapping("/debug-student/{id}")
    @ResponseBody
    public ResponseEntity<?> debugStudent(@PathVariable Long id) {
        try {
            User student = userRepository.findById(id).orElse(null);
            if (student == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy sinh viên");
            }
            System.out.println("DEBUG Student ID: " + id);
            System.out.println("Birth date object: " + student.getBirth());
            System.out.println("Birth date type: " + (student.getBirth() != null ? student.getBirth().getClass().getName() : "null"));

            return ResponseEntity.ok(student);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    // ============ DEBUG CHECK STUDENT ============
    @GetMapping("/debug/check-student/{id}")
    @ResponseBody
    public ResponseEntity<?> checkStudentById(@PathVariable Long id) {
        try {
            Optional<User> studentOpt = userRepository.findById(id);
            if (!studentOpt.isPresent()) {
                return ResponseEntity.ok("Student not found with ID: " + id);
            }
            User student = studentOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", student.getId());
            result.put("mssv", student.getMssv());
            result.put("fullName", student.getFullName());
            result.put("birth", student.getBirth());
            result.put("birthType", student.getBirth() != null ? student.getBirth().getClass().getName() : "null");
            result.put("department", student.getDepartment());
            result.put("major", student.getMajor());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}

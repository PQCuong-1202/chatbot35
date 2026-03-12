package com.ai.chatbot.controller.admin;

import com.ai.chatbot.model.Department;
import com.ai.chatbot.model.User;
import com.ai.chatbot.repository.DepartmentRepository;
import com.ai.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/api/admin")
public class AdminDepartmentController {
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private UserRepository userRepository;

    // ============ LOAD DEPARTMENTS ============
    @GetMapping("/load-departments")
    @ResponseBody
    public ResponseEntity<?> loadDepartments() {
        try {
            List<Department> departments = deptRepo.findAll();
            List<String> distinctDepts = deptRepo.findDistinctDepartmentNames();

            Map<String, Object> response = new HashMap<>();
            response.put("departments", departments);
            response.put("distinctDepts", distinctDepts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải danh sách ngành: " + e.getMessage());
        }
    }

    // ============ GET MAJORS BY DEPARTMENT ============
    @GetMapping("/get-majors")
    @ResponseBody
    public ResponseEntity<?> getMajors(@RequestParam String departmentName) {
        try {
            List<String> majors = deptRepo.findByDepartmentName(departmentName)
                    .stream()
                    .map(Department::getMajorName)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(majors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải chuyên ngành: " + e.getMessage());
        }
    }

    // ============ SAVE DEPARTMENT ============
    @PostMapping("/save-department")
    @ResponseBody
    public ResponseEntity<?> saveDepartment(@RequestBody Department dept) {
        try {
            // Validate
            if (dept.getDepartmentName() == null || dept.getDepartmentName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên ngành không được để trống");
            }
            if (dept.getMajorName() == null || dept.getMajorName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên chuyên ngành không được để trống");
            }

            // Kiểm tra trùng lặp
            List<Department> existing = deptRepo.findByDepartmentName(dept.getDepartmentName());
            boolean isDuplicate = existing.stream()
                    .anyMatch(d -> d.getMajorName().equalsIgnoreCase(dept.getMajorName()) &&
                            (dept.getId() == null || !d.getId().equals(dept.getId())));
            if (isDuplicate) {
                return ResponseEntity.badRequest().body("Chuyên ngành đã tồn tại trong ngành này");
            }
            deptRepo.save(dept);
            return ResponseEntity.ok("Lưu thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lưu: " + e.getMessage());
        }
    }

    // ============ DELETE DEPARTMENT ============
    @DeleteMapping("/delete-department/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            if (!deptRepo.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy ngành/chuyên ngành");
            }
            deptRepo.deleteById(id);
            return ResponseEntity.ok("Xóa thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ============ GET DEPARTMENT BY ID ============
    @GetMapping("/departments/{id}")
    @ResponseBody
    public ResponseEntity<?> getDepartment(@PathVariable Long id) {
        try {
            return deptRepo.findById(id)
                    .<ResponseEntity<?>>map(dept -> {
                        // Tạo Map response để đảm bảo JSON hợp lệ
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", dept.getId());
                        response.put("departmentName", dept.getDepartmentName());
                        response.put("majorName", dept.getMajorName());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body("Không tìm thấy ngành/chuyên ngành"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    // ============ GET DEPARTMENT STATS ============
    @GetMapping("/department-stats")
    @ResponseBody
    public ResponseEntity<?> getDepartmentStats(@RequestParam String department) {
        try {
            List<User> students = userRepository.findAll()
                    .stream()
                    .filter(s -> "USER".equals(s.getRole()))
                    .filter(s -> department.equals(s.getDepartment()))
                    .collect(Collectors.toList());
            List<Department> deptMajors = deptRepo.findByDepartmentName(department);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", students.size());
            stats.put("activeStudents", students.stream()
                    .filter(s -> s.getEnabled() == 0).count());
            // Thống kê theo chuyên ngành
            List<Map<String, Object>> majors = new ArrayList<>();
            for (Department dept : deptMajors) {
                long count = students.stream()
                        .filter(s -> dept.getMajorName().equals(s.getMajor()))
                        .count();
                Map<String, Object> majorStat = new HashMap<>();
                majorStat.put("name", dept.getMajorName());
                majorStat.put("count", count);
                majors.add(majorStat);
            }
            stats.put("majors", majors);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    // ============ SEARCH DEPARTMENTS ============
    @GetMapping("/search-departments")
    @ResponseBody
    public ResponseEntity<?> searchDepartments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentName) {
        try {
            List<Department> departments = deptRepo.findAll()
                    .stream()
                    .filter(d -> {
                        // Filter by keyword
                        boolean matchKeyword = true;
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            String searchTerm = keyword.toLowerCase().trim();
                            String deptName = d.getDepartmentName() != null ? d.getDepartmentName().toLowerCase() : "";
                            String majorName = d.getMajorName() != null ? d.getMajorName().toLowerCase() : "";
                            matchKeyword = deptName.contains(searchTerm) || majorName.contains(searchTerm);
                        }
                        // Filter by department name
                        boolean matchDeptName = true;
                        if (departmentName != null && !departmentName.trim().isEmpty()) {
                            String dept = d.getDepartmentName() != null ? d.getDepartmentName() : "";
                            matchDeptName = dept.equals(departmentName);
                        }
                        return matchKeyword && matchDeptName;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tìm kiếm: " + e.getMessage());
        }
    }
}

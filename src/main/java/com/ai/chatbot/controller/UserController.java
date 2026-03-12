package com.ai.chatbot.controller;

import com.ai.chatbot.model.Department;
import com.ai.chatbot.model.User;
import com.ai.chatbot.model.UserCTDT;
import com.ai.chatbot.repository.DepartmentRepository;
import com.ai.chatbot.repository.UserCTDTRepository;
import com.ai.chatbot.repository.UserRepository;
import com.ai.chatbot.service.UserCTDTService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
public class UserController {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserCTDTService userCTDTService;
    @Autowired private UserCTDTRepository userCTDTRepo;
    @Autowired private DepartmentRepository deptRepo;

    // ============ TRANG CHÍNH PROFILE ============
    @GetMapping("")
    public String index(HttpSession session, Model model,
                        @RequestParam(required = false) String tab) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        // Xác định tab hiện tại
        String currentTab;
        if (tab != null && !tab.isEmpty()) {
            currentTab = tab;
        } else if (session.getAttribute("currentTab") != null) {
            currentTab = (String) session.getAttribute("currentTab");
        } else {
            currentTab = "ADMIN".equals(user.getRole()) ? "admin-sv" : "profile";
        }
        // Lưu tab vào session
        session.setAttribute("currentTab", currentTab);
        model.addAttribute("currentTab", currentTab);
        // Tự động khởi tạo CTDT nếu user chưa có
        if ("USER".equals(user.getRole())) {
            try {
                boolean hasCTDT = userCTDTService.hasUserCTDT(user.getId());
                if (!hasCTDT && user.getDepartment() != null && user.getMajor() != null) {
                    // Chỉ khởi tạo nếu user đã có nganh/chuyên ngành
                    String result = userCTDTService.initializeCTDTForUser(user);
                    System.out.println("Auto-initialized CTDT for user " + user.getId() + ": " + result);
                }
            } catch (Exception e) {
                System.err.println("Error auto-initializing CTDT: " + e.getMessage());
            }
        }
        return "user";
    }

    // ============ LOAD FRAGMENT ============
    @GetMapping("/load-fragment")
    public String loadFragment(@RequestParam String fragment, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        // Map fragment names to template names
        String template = switch(fragment) {
            case "sv_manage" -> "fragments/sv_manage :: listSection";
            case "department_manage" -> "fragments/department_manage :: departmentSection";
            case "profile_info" -> "fragments/profile_info :: profileSection";
            case "ctdt_manage" -> "fragments/ctdt_manage :: ctdtSection";
            case "notifications" -> "fragments/notifications :: notifySection";
            case "chatbot" -> "fragments/chatbot :: chatbotSection";
            case "ctdt" -> "fragments/ctdt :: ctdtViewSection";
            case "results" -> "fragments/results :: resultsSection";
            default -> "fragments/loading :: loading";
        };
        return template;
    }

    // ============ GET FRAGMENT DIRECT ============
    @GetMapping("/fragments/{fragmentName}")
    public String getFragment(@PathVariable String fragmentName, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        // Thư mục: fragments/admin/ và fragments/user/
        String template;
        switch(fragmentName) {
            // Admin fragments
            case "sv_manage":
                template = "fragments/admin/sv_manage :: listSection";
                break;
            case "department_manage":
                template = "fragments/admin/department_manage :: departmentSection";
                break;
            case "ctdt_manage":
                template = "fragments/admin/ctdt_manage :: ctdtSection";
                break;
            // User fragments
            case "profile_info":
                template = "fragments/user/profile_info :: profileSection";
                break;
            case "notifications":
                template = "fragments/user/notifications :: notifySection";
                break;
            case "chatbot":
                template = "fragments/user/chatbot :: chatbotSection";
                break;
            case "ctdt":
                template = "fragments/user/ctdt :: ctdtViewSection";
                break;
            case "results":
                template = "fragments/user/results :: resultsSection";
                break;
            default:
                template = "fragments/loading :: loading";
        }
        return template;
    }

    // ============ STUDENT MANAGEMENT FRAGMENT ============
    @GetMapping("/sv-manage")
    public String studentManagement(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }
        User user = (User) session.getAttribute("loggedInUser");
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/profile";
        }
        // Load dữ liệu sinh viên
        List<User> students = userRepository.findAll()
                .stream()
                .filter(s -> "USER".equals(s.getRole()))
                .collect(Collectors.toList());
        // Load danh sách nganh
        List<String> distinctDepts = deptRepo.findDistinctDepartmentNames();
        model.addAttribute("allStudents", students);
        model.addAttribute("departments", distinctDepts);
        return "fragments/sv_manage :: listSection";
    }

    // ============ DEPARTMENT MANAGEMENT FRAGMENT ============
    @GetMapping("/dept-manage")
    public String departmentManagement(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }
        User user = (User) session.getAttribute("loggedInUser");
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/profile";
        }
        // Load dữ liệu nganh
        List<Department> departments = deptRepo.findAll();
        List<String> distinctDepts = deptRepo.findDistinctDepartmentNames();
        model.addAttribute("allDepts", departments);
        model.addAttribute("distinctDepts", distinctDepts);
        return "fragments/department_manage :: departmentSection";
    }

    // ============ GET USER INFO ============
    @GetMapping("/api/user-info")
    @ResponseBody
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Người dùng chưa đăng nhập");
        }
        // Chỉ trả về thông tin cần thiết
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("mssv", user.getMssv());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole());
        return ResponseEntity.ok(userInfo);
    }

    // ============ GET USER PROFILE ============
    @GetMapping("/api/user-profile")
    @ResponseBody
    public ResponseEntity<?> getUserProfile(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Người dùng chưa đăng nhập");
        }
        return ResponseEntity.ok(user);
    }

    // ============ CHANGE PASSWORD ============
    @PostMapping("/api/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestParam String oldPassword,
                                            @RequestParam String newPassword,
                                            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            // Kiểm tra mật khẩu cũ với BCrypt
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mật khẩu cũ không đúng!");
            }
            // Mã hóa mật khẩu mới
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    // ============ UPDATE PROFILE ============
    @PostMapping("/api/update-profile")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @RequestParam Long id,
            @RequestParam String fullName,
            @RequestParam String birth,
            @RequestParam(required = false) String sex,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String studentClass,
            @RequestParam(required = false) String gmail,
            @RequestParam(required = false) String gmailVlu,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String nation,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage,
            HttpSession session) {

        try {
            User currentUser = (User) session.getAttribute("loggedInUser");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }
            // Verify the user is updating their own profile
            if (!currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn chỉ có thể cập nhật thông tin của chính mình");
            }
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy người dùng");
            }
            User user = userOpt.get();
            // Lưu giá trị cũ để so sánh
            String oldDepartment = user.getDepartment();
            String oldMajor = user.getMajor();
            // Cập nhật thông tin
            user.setDepartment(department);
            user.setMajor(major);
            // Update basic info
            user.setFullName(fullName.trim());
            // Parse birth date
            LocalDate birthDate = null;
            try {
                if (birth.contains("-")) {
                    birthDate = LocalDate.parse(birth);
                } else if (birth.contains("/")) {
                    String[] parts = birth.split("/");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        birthDate = LocalDate.of(year, month, day);
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body("Ngày sinh không hợp lệ. Vui lòng sử dụng định dạng dd/mm/yyyy");
            }
            if (birthDate != null) {
                user.setBirth(birthDate);
            }
            user.setSex(sex);
            user.setDepartment(department);
            user.setMajor(major);
            user.setCourse(course != null ? course.toUpperCase().trim() : null);
            user.setStudentClass(studentClass);
            user.setGmail(gmail);
            user.setGmailVlu(gmailVlu);
            user.setPhone(phone);
            user.setNation(nation != null ? nation : "Việt Nam");
            user.setAddress(address);
            user.setLastUpdated(LocalDateTime.now());
            // Handle image
            if (removeImage) {
                user.setImage(null);
            } else if (image != null && !image.isEmpty()) {
                // Validate image size (max 2MB)
                if (image.getSize() > 2 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body("Kích thước ảnh không được vượt quá 2MB");
                }
                // Validate image type
                String contentType = image.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                    return ResponseEntity.badRequest()
                            .body("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");
                }
                user.setImage(image.getBytes());
            }
            userRepository.save(user);
            // Kiểm tra xem nganh/chuyên ngành có thay đổi không
            boolean departmentChanged = (oldDepartment == null && department != null) ||
                    (oldDepartment != null && !oldDepartment.equals(department));
            boolean majorChanged = (oldMajor == null && major != null) ||
                    (oldMajor != null && !oldMajor.equals(major));
            // Khởi tạo lại CTDT nếu nganh/chuyên ngành thay đổi
            if (departmentChanged || majorChanged) {
                try {
                    // Xóa CTDT cũ (nếu có)
                    List<UserCTDT> oldCTDT = userCTDTRepo.findByUserId(user.getId());
                    if (!oldCTDT.isEmpty()) {
                        userCTDTRepo.deleteAll(oldCTDT);
                    }
                    // Tạo mới CTDT
                    String result = userCTDTService.initializeCTDTForUser(user);
                    System.out.println("CTDT reinitialized for user " + user.getId() + ": " + result);
                } catch (Exception e) {
                    System.err.println("Error reinitializing CTDT for user " + user.getId() + ": " + e.getMessage());
                }
            }
            // Update session
            session.setAttribute("loggedInUser", user);
            return ResponseEntity.ok("Cập nhật thông tin thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    // ============ GET USER PROFILE WITH AVATAR ============
    @GetMapping("/api/user-profile-with-avatar")
    @ResponseBody
    public ResponseEntity<?> getUserProfileWithAvatar(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Người dùng chưa đăng nhập");
        }
        // Tạo Map để trả về với avatar dạng base64
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("mssv", user.getMssv());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("birth", user.getBirth());
        userInfo.put("sex", user.getSex());
        userInfo.put("nation", user.getNation());
        userInfo.put("course", user.getCourse());
        userInfo.put("department", user.getDepartment());
        userInfo.put("major", user.getMajor());
        userInfo.put("studentClass", user.getStudentClass());
        userInfo.put("gmail", user.getGmail());
        userInfo.put("gmailVlu", user.getGmailVlu());
        userInfo.put("phone", user.getPhone());
        userInfo.put("address", user.getAddress());
        userInfo.put("lastUpdated", user.getLastUpdated());
        // Convert image to base64
        if (user.getImage() != null && user.getImage().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(user.getImage());
            userInfo.put("imageBase64", base64Image);
            userInfo.put("hasImage", true);
        } else {
            userInfo.put("hasImage", false);
            userInfo.put("imageBase64", null);
        }
        return ResponseEntity.ok(userInfo);
    }

    // ============ GET USER AVATAR ============
    @GetMapping("/avatar/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(u.getImage()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ TEST API ============
    @GetMapping("/api/test")
    @ResponseBody
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("UserController is working! Timestamp: " + System.currentTimeMillis());
    }
}
package com.ai.chatbot.config;

import com.ai.chatbot.model.User;
import com.ai.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class Admin {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminAccount() {
        return args -> {
            // Kiểm tra xem đã có tài khoản admin dựa trên mssv hoặc role chưa
            if (userRepository.findByMssv("admin").isEmpty()) {
                User admin = new User();

                // Thiết lập thông tin cơ bản
                admin.setMssv("admin");
                admin.setPassword(passwordEncoder.encode("123456")); // Mã hóa mật khẩu
                admin.setFullName("Quản trị viên Hệ thống");

                // Các thông tin bắt buộc khác theo model của bạn
                admin.setBirth(LocalDate.of(1990, 1, 1)); // Ngày sinh mặc định
                admin.setNation("Việt Nam");
                admin.setCourse("ADMIN_COURSE");
                admin.setDepartment("");
                admin.setMajor("");

                // Thiết lập quyền hạn và trạng thái
                admin.setRole("ADMIN"); // Gán quyền ADMIN
                admin.setEnabled(0);    // 0 = "còn học" / đang làm việc

                // Lưu vào cơ sở dữ liệu
                userRepository.save(admin);
                System.out.println(">>> Đã tạo tài khoản ADMIN mặc định: admin/123456");
            } else {
                System.out.println(">>> Tài khoản ADMIN đã tồn tại, bỏ qua bước khởi tạo.");
            }
        };
    }
}
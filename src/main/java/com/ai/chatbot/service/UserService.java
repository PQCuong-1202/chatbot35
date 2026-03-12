package com.ai.chatbot.service;

import com.ai.chatbot.model.User;
import com.ai.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
public class UserService {
    @Autowired private UserRepository userRepository;

    public void initAdmin() {
        if (userRepository.findByMssv("admin").isEmpty()) {
            User admin = new User();
            admin.setMssv("admin");
            admin.setPassword("123456");
            admin.setFullName("Quản trị viên");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }

    public User saveUser(User user) {
        // Tự động tạo mật khẩu mặc định ddMMyyyy nếu là tạo mới
        if (user.getId() == null && user.getBirth() != null) {
            String defaultPwd = user.getBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            user.setPassword(defaultPwd);
        }
        return userRepository.save(user);
    }
}
package com.ai.chatbot.controller;

import com.ai.chatbot.model.User;
import com.ai.chatbot.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session) {
        Optional<User> userOpt = userRepository.findByMssv(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Sử dụng passwordEncoder.matches() để kiểm tra mật khẩu
            if (passwordEncoder.matches(password, user.getPassword())) {
                session.setAttribute("loggedInUser", user);
                return "redirect:/profile";
            }
        }

        return "redirect:/login?error";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
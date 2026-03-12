package com.ai.chatbot.controller.user;

import com.ai.chatbot.model.Notification;
import com.ai.chatbot.model.User;
import com.ai.chatbot.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/api/user")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications")
    @ResponseBody
    public ResponseEntity<?> getUserNotifications(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            System.out.println("Getting notifications for user: " + user.getId());
            List<Notification> notifications = notificationService.getUserNotifications(user.getId());
            System.out.println("Found " + notifications.size() + " notifications");

            List<Map<String, Object>> response = notifications.stream()
                    .map(n -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", n.getId());
                        map.put("type", n.getType());
                        map.put("title", n.getTitle());
                        map.put("message", n.getMessage());
                        map.put("isRead", n.getIsRead());
                        map.put("createdAt", n.getCreatedAt());
                        map.put("actionUrl", n.getActionUrl());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông báo: " + e.getMessage());
        }
    }

    @GetMapping("/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<?> getUnreadNotificationCount(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            int count = notificationService.getUnreadCount(user.getId());
            System.out.println("Unread count for user " + user.getId() + ": " + count);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("userId", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đếm thông báo: " + e.getMessage());
        }
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            notificationService.markAsRead(id, user.getId());
            System.out.println("Marked notification " + id + " as read for user " + user.getId());

            return ResponseEntity.ok("Đã đánh dấu đã đọc");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đánh dấu đã đọc: " + e.getMessage());
        }
    }

    @PostMapping("/notifications/mark-all-read")
    @ResponseBody
    public ResponseEntity<?> markAllNotificationsAsRead(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            notificationService.markAllAsRead(user.getId());
            System.out.println("Marked all notifications as read for user " + user.getId());

            return ResponseEntity.ok("Đã đánh dấu tất cả đã đọc");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đánh dấu đã đọc: " + e.getMessage());
        }
    }

    @PostMapping("/check-notifications")
    @ResponseBody
    public ResponseEntity<?> triggerNotificationCheck(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            System.out.println("Manually triggering notification check for user: " + user.getId());
            notificationService.checkOverallStatus(user.getId()); // Method này đã có trong Service

            return ResponseEntity.ok("Đã kiểm tra và tạo thông báo mới");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra thông báo: " + e.getMessage());
        }
    }

    @PostMapping("/notifications/send-course-recommendation")
    @ResponseBody
    public ResponseEntity<?> sendCourseRecommendation(HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");

            notificationService.sendCourseRecommendation(user.getId());

            List<Notification> all = notificationService.getUserNotifications(user.getId());
            List<Map<String, Object>> result = all.stream()
                    .filter(n -> n.getTitle() != null && n.getTitle().contains("GỢI Ý HỌC KỲ"))
                    .map(n -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", n.getId());
                        map.put("type", n.getType());
                        map.put("title", n.getTitle());
                        map.put("message", n.getMessage());
                        map.put("isRead", n.getIsRead());
                        map.put("createdAt", n.getCreatedAt());
                        map.put("actionUrl", n.getActionUrl());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }
}
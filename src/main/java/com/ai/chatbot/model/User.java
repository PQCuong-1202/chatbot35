package com.ai.chatbot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mssv; // Dùng làm username đăng nhập

    private String password; // Mặc định là ddMMyyyy
    private String fullName;


    private LocalDate birth; // Định dạng dd/mm/yyyy
    private String sex;
    private String nation = "Việt Nam";
    private String enrollmentYear;
    private String course;
    private String department;
    private String major;
    private String typeOfTraining;
    private String advisor;
    private String studentClass;
    private String gmail;
    private String gmailVlu;
    private String phone;
    private String address;
    private LocalDateTime lastUpdated;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image; // Lưu ảnh trực tiếp trong DB

    @Column(nullable = false)
    private String role = "USER"; // ADMIN hoặc USER
    private Integer enabled = 0; // 0: Còn học, 1: Đã nghỉ

}
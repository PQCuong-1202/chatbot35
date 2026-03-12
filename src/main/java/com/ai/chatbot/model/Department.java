package com.ai.chatbot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String departmentName; // Tên ngành (VD: Công nghệ thông tin)
    private String majorName;      // Tên Chuyên ngành (VD: Công nghệ phần mềm)
}
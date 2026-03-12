package com.ai.chatbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ctdt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ctdt_id"}))
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserCTDT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ctdt_id", nullable = false)
    private CTDT ctdt;

    private Integer trangThai = 1; // 0 = đã học, 1 = chưa học
    private Integer hocKy;         // Học kỳ (copy từ CTDT)
    private String tenHocPhan;    // Tên học phần (copy từ CTDT)
    private String maHocPhan;     // Mã học phần (copy từ CTDT)
    private Integer tinChi;       // Tín chỉ (copy từ CTDT)
    private String loai;          // Loại (BB/TC) (copy từ CTDT)
    private String nhomTC;        // Nhóm TC (copy từ CTDT)
    private String nganh;         // Ngành (copy từ CTDT)
    private String chuyenNganh;   // Chuyên ngành (copy từ CTDT)
    private String nhomHocPhan;   // Nhóm học phần (copy từ CTDT)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.ai.chatbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ctdt")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CTDT {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hoc_phan")
    private String maHocPhan;           // Thêm mã học phần

    private Integer hocKy;               // Học kỳ (1, 2, 3, ...)

    @Column(nullable = false)
    private String tenHocPhan;          // Tên học phần

    private Integer tinChi;             // Số tín chỉ

    private String loai;                // BB (bắt buộc) hoặc TC (tự chọn)

    private String nhomTC;              // Nhóm tự chọn (ví dụ: "2/5" - học 2 trong 5 môn)

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "ctdt_tien_quyet",
            joinColumns = @JoinColumn(name = "ctdt_id"),
            inverseJoinColumns = @JoinColumn(name = "tien_quyet_id")
    )
    @JsonIgnoreProperties({"hocPhanTienQuyet"})
    private List<CTDT> hocPhanTienQuyet; // Các học phần tiên quyết

    private String nganh;                // Ngành của học phần (thông tin học thuật, có thể null)

    private String chuyenNganh;         // Chuyên ngành của học phần (có thể null)

    @Column(name = "tab_nganh")
    private String tabNganh;            // Tab ngành chứa học phần này (dùng để phân nhóm tab, không phải ngành học thuật)

    private String nhomHocPhan;         // Nhóm học phần (dùng cho việc nhóm các môn tự chọn)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (hocPhanTienQuyet == null) {
            hocPhanTienQuyet = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============ HELPER METHODS ============

    public void addHocPhanTienQuyet(CTDT hocPhan) {
        if (hocPhanTienQuyet == null) {
            hocPhanTienQuyet = new ArrayList<>();
        }

        // Kiểm tra không cho phép tự tham chiếu
        if (hocPhan != null && !hocPhan.getId().equals(this.id)) {
            // Kiểm tra xem đã có trong danh sách chưa
            boolean exists = hocPhanTienQuyet.stream()
                    .anyMatch(hp -> hp.getId().equals(hocPhan.getId()));

            if (!exists) {
                hocPhanTienQuyet.add(hocPhan);
            }
        }
    }

    public void removeHocPhanTienQuyet(Long hocPhanId) {
        if (hocPhanTienQuyet != null) {
            hocPhanTienQuyet.removeIf(hp -> hp.getId().equals(hocPhanId));
        }
    }

    public void clearHocPhanTienQuyet() {
        if (hocPhanTienQuyet != null) {
            hocPhanTienQuyet.clear();
        }
    }

    public void setHocPhanTienQuyetFromIds(List<Long> tienQuyetIds, List<CTDT> allHocPhan) {
        if (tienQuyetIds == null || tienQuyetIds.isEmpty()) {
            this.hocPhanTienQuyet = new ArrayList<>();
            return;
        }

        if (this.hocPhanTienQuyet == null) {
            this.hocPhanTienQuyet = new ArrayList<>();
        } else {
            this.hocPhanTienQuyet.clear();
        }

        for (Long id : tienQuyetIds) {
            // Không cho phép tự tham chiếu
            if (id.equals(this.id)) {
                continue;
            }

            allHocPhan.stream()
                    .filter(hp -> hp.getId().equals(id))
                    .findFirst()
                    .ifPresent(hp -> {
                        // Kiểm tra xem đã có trong danh sách chưa
                        boolean exists = this.hocPhanTienQuyet.stream()
                                .anyMatch(existing -> existing.getId().equals(hp.getId()));

                        if (!exists) {
                            this.hocPhanTienQuyet.add(hp);
                        }
                    });
        }
    }
}
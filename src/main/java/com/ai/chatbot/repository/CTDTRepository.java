package com.ai.chatbot.repository;

import com.ai.chatbot.model.CTDT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CTDTRepository extends JpaRepository<CTDT, Long> {

    // Tìm theo nganh
    List<CTDT> findByNganh(String nganh);

    // Tìm theo học kỳ
    List<CTDT> findByHocKy(Integer hocKy);

    // Tìm theo chuyên ngành
    List<CTDT> findByChuyenNganh(String chuyenNganh);

    // Tìm theo loại (BB/TC)
    List<CTDT> findByLoai(String loai);

    // Tìm theo nhiều tiêu chí
    @Query("SELECT c FROM CTDT c WHERE " +
            "(:hocKy IS NULL OR c.hocKy = :hocKy) AND " +
            "(:nganh IS NULL OR :nganh = '' OR c.nganh = :nganh) AND " +
            "(:chuyenNganh IS NULL OR :chuyenNganh = '' OR c.chuyenNganh = :chuyenNganh) AND " +
            "(:loai IS NULL OR :loai = '' OR c.loai = :loai) " +
            "ORDER BY c.hocKy, c.nhomHocPhan, c.id")
    List<CTDT> searchCTDT(
            @Param("hocKy") Integer hocKy,
            @Param("nganh") String nganh,
            @Param("chuyenNganh") String chuyenNganh,
            @Param("loai") String loai
    );

    // Lấy danh sách học kỳ duy nhất
    @Query("SELECT DISTINCT c.hocKy FROM CTDT c WHERE c.hocKy IS NOT NULL ORDER BY c.hocKy")
    List<Integer> findDistinctHocKy();

    // Lấy danh sách nganh duy nhất
    @Query("SELECT DISTINCT c.nganh FROM CTDT c WHERE c.nganh IS NOT NULL AND c.nganh <> '' ORDER BY c.nganh")
    List<String> findDistinctNganh();

    // Lấy danh sách chuyên ngành duy nhất
    @Query("SELECT DISTINCT c.chuyenNganh FROM CTDT c WHERE c.chuyenNganh IS NOT NULL AND c.chuyenNganh <> '' ORDER BY c.chuyenNganh")
    List<String> findDistinctChuyenNganh();

    // Tìm học phần theo tên (cho dropdown tiên quyết)
    List<CTDT> findByTenHocPhanContainingIgnoreCase(String tenHocPhan);

    // Thêm phương thức này vào CTDTRepository.java
    @Query("SELECT DISTINCT c.nhomHocPhan FROM CTDT c WHERE c.nhomHocPhan IS NOT NULL AND c.nhomHocPhan <> '' ORDER BY c.nhomHocPhan")
    List<String> findDistinctNhomHocPhan();

    // Thêm phương thức tìm học phần theo nhóm
    List<CTDT> findByNhomHocPhan(String nhomHocPhan);

    @Query("SELECT c FROM CTDT c WHERE c.maHocPhan = :maHocPhan")
    List<CTDT> findByMaHocPhan(@Param("maHocPhan") String maHocPhan);

    @Query("SELECT c FROM CTDT c WHERE c.tenHocPhan = :tenHocPhan")
    List<CTDT> findByTenHocPhan(@Param("tenHocPhan") String tenHocPhan);

    @Query("SELECT c FROM CTDT c JOIN c.hocPhanTienQuyet h WHERE h = :prerequisite")
    List<CTDT> findByHocPhanTienQuyetContains(@Param("prerequisite") CTDT prerequisite);

    // ============ TAB NGANH ============
    // Tìm theo tab ngành (container tab, không phải field nganh của học phần)
    List<CTDT> findByTabNganh(String tabNganh);

    // Lấy danh sách tab ngành duy nhất
    @Query("SELECT DISTINCT c.tabNganh FROM CTDT c WHERE c.tabNganh IS NOT NULL AND c.tabNganh <> '' ORDER BY c.tabNganh")
    List<String> findDistinctTabNganh();

    // Tìm theo tab ngành + các filter khác
    @Query("SELECT c FROM CTDT c WHERE " +
            "(:tabNganh IS NULL OR :tabNganh = '' OR c.tabNganh = :tabNganh) AND " +
            "(:hocKy IS NULL OR c.hocKy = :hocKy) AND " +
            "(:chuyenNganh IS NULL OR :chuyenNganh = '' OR c.chuyenNganh = :chuyenNganh) AND " +
            "(:loai IS NULL OR :loai = '' OR c.loai = :loai) " +
            "ORDER BY c.hocKy, c.nhomHocPhan, c.id")
    List<CTDT> searchCTDTByTab(
            @Param("tabNganh") String tabNganh,
            @Param("hocKy") Integer hocKy,
            @Param("chuyenNganh") String chuyenNganh,
            @Param("loai") String loai
    );

    // Lấy danh sách học kỳ duy nhất theo tab ngành
    @Query("SELECT DISTINCT c.hocKy FROM CTDT c WHERE c.tabNganh = :tabNganh AND c.hocKy IS NOT NULL ORDER BY c.hocKy")
    List<Integer> findDistinctHocKyByTabNganh(@Param("tabNganh") String tabNganh);

    // Lấy danh sách chuyên ngành duy nhất theo tab ngành
    @Query("SELECT DISTINCT c.chuyenNganh FROM CTDT c WHERE c.tabNganh = :tabNganh AND c.chuyenNganh IS NOT NULL AND c.chuyenNganh <> '' ORDER BY c.chuyenNganh")
    List<String> findDistinctChuyenNganhByTabNganh(@Param("tabNganh") String tabNganh);
}
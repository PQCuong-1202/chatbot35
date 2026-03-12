package com.ai.chatbot.repository;

import com.ai.chatbot.model.UserCTDT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCTDTRepository extends JpaRepository<UserCTDT, Long> {

    // Lấy tất cả UserCTDT của một user
    @Query("SELECT u FROM UserCTDT u WHERE u.user.id = :userId ORDER BY u.hocKy ASC, u.id ASC")
    List<UserCTDT> findByUserId(@Param("userId") Long userId);

    // Lấy UserCTDT cụ thể của user
    UserCTDT findByUserIdAndCtdtId(Long userId, Long ctdtId);

    // Đếm số UserCTDT của user
    @Query("SELECT COUNT(u) FROM UserCTDT u WHERE u.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    // Lọc UserCTDT với nhiều điều kiện
    @Query("SELECT u FROM UserCTDT u WHERE u.user.id = :userId " +
            "AND (:hocKy IS NULL OR u.hocKy = :hocKy) " +
            "AND (:loai IS NULL OR u.loai = :loai) " +
            "AND (:trangThai IS NULL OR u.trangThai = :trangThai) " +
            "ORDER BY u.hocKy ASC, u.id ASC")
    List<UserCTDT> findByUserIdAndFilters(
            @Param("userId") Long userId,
            @Param("hocKy") Integer hocKy,
            @Param("loai") String loai,
            @Param("trangThai") Integer trangThai);

    List<UserCTDT> findByUserIdOrderByHocKyAsc(Long userId);
}
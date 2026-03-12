package com.ai.chatbot.repository;

import com.ai.chatbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMssv(String mssv);

    @Query("SELECT u FROM User u WHERE u.role = 'USER' ORDER BY u.lastUpdated DESC")
    List<User> findAllStudentsOrderByLastUpdated();

    @Query("SELECT s FROM User s WHERE " +
            "(:keyword IS NULL OR s.fullName LIKE %:keyword% OR s.mssv LIKE %:keyword%) AND " +
            "(:department IS NULL OR s.department = :department) AND " +
            "(:major IS NULL OR s.major = :major) AND " +
            "(:course IS NULL OR s.course = :course) AND " +
            "s.role = 'USER' " +
            "ORDER BY s.lastUpdated DESC")
    List<User> searchStudents(
            @Param("keyword") String keyword,
            @Param("department") String department,
            @Param("major") String major,
            @Param("course") String course);
}
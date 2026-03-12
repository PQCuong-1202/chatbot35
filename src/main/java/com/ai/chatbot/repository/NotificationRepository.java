package com.ai.chatbot.repository;

import com.ai.chatbot.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findActiveNotificationsByUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId " +
            "AND n.isRead = false " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP)")
    int countUnreadNotifications(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadByUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.expiresAt < :now")
    void deleteExpiredNotifications(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
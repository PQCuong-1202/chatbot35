package com.ai.chatbot.repository;

import com.ai.chatbot.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    List<UploadedFile> findByUserIdAndIsActiveTrueOrderByUploadedAtDesc(Long userId);
    Optional<UploadedFile> findByIdAndUserId(Long id, Long userId);
}
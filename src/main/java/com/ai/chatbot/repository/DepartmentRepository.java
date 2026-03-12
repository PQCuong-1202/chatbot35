package com.ai.chatbot.repository;

import com.ai.chatbot.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // Lấy danh sách các ngành duy nhất (không trùng lặp)
    @Query("SELECT DISTINCT d.departmentName FROM Department d")
    List<String> findDistinctDepartmentNames();

    // Lấy danh sách chuyên ngành thuộc một ngành
    List<Department> findByDepartmentName(String departmentName);
}

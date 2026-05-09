package com.example.repository;

import com.example.entity.HealthIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HealthIndexRepository extends JpaRepository<HealthIndex, Integer> {
    
    // Thêm dòng này để lấy bản ghi chỉ số sức khỏe mới nhất của người dùng
    Optional<HealthIndex> findTopByUser_IdOrderByIdDesc(Integer userId);
}
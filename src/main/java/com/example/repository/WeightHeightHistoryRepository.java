package com.example.repository;

import com.example.entity.WeightHeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WeightHeightHistoryRepository extends JpaRepository<WeightHeightHistory, Integer> {
    // Lấy lịch sử thay đổi chỉ số của một người dùng
   List<WeightHeightHistory> findByUserIdOrderByRecordedAtAsc(Long userId);
}
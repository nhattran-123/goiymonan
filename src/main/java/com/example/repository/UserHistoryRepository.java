package com.example.repository;

import com.example.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    // Nếu Thành muốn lấy lịch sử của 1 user cụ thể
    List<UserHistory> findTop5ByUserIdOrderByEatenAtDesc(Long userId);
}
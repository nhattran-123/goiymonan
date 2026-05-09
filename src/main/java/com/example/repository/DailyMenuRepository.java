package com.example.repository;

import com.example.entity.DailyMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyMenuRepository extends JpaRepository<DailyMenu, Integer> {
    // Tìm thực đơn của một người dùng trong ngày cụ thể
    Optional<DailyMenu> findByUserIdAndMenuDate(Integer userId, LocalDate menuDate);
 long countByMenuDateBetween(LocalDate startDate, LocalDate endDate);
}
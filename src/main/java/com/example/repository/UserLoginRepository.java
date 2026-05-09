package com.example.repository;

import com.example.entity.UserLogin;
import com.example.entity.UserLoginId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserLoginRepository extends JpaRepository<UserLogin, UserLoginId> {
    // Đếm số lượt đăng nhập duy nhất trong một ngày cụ thể[cite: 20, 24]
    long countByLoginDate(LocalDate loginDate);
    List<UserLogin> findByLoginDateBetween(LocalDate startDate, LocalDate endDate);
}
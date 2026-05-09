package com.example.repository;

import com.example.entity.UserLogin;
import com.example.entity.UserLoginId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserLoginRepository extends JpaRepository<UserLogin, UserLoginId> {
    // Đếm số lượt đăng nhập duy nhất trong một ngày cụ thể[cite: 20, 24]
    long countByLoginDate(LocalDate loginDate);
    @Query(value = "SELECT DATE_FORMAT(login_date, '%d/%m') AS day_label, COUNT(DISTINCT id) AS login_count " +
            "FROM user_login " +
            "WHERE login_date >= DATE_SUB(CURRENT_DATE, INTERVAL 9 DAY) " +
            "GROUP BY login_date " +
            "ORDER BY login_date", nativeQuery = true)
    List<Object[]> countDistinctUsersByLoginDateBetweenLast10Days();
}
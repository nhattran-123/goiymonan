package com.example.repository;

import com.example.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealTypeRepository extends JpaRepository<MealType, Integer> {
    // Hiện tại chỉ cần findAll() từ JpaRepository là đủ để lấy 4 bữa Sáng, Trưa, Tối, Phụ
}
package com.example.repository;

import com.example.entity.MenuDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MenuDetailRepository extends JpaRepository<MenuDetail, Integer> {
    
    // Lấy tất cả món ăn thuộc về một thực đơn (theo menuId)
    List<MenuDetail> findByMenuId(Integer menuId);

    // Tìm món ăn theo bữa (Sáng/Trưa/Tối) trong một thực đơn cụ thể
    List<MenuDetail> findByMenuIdAndMealTypeId(Integer menuId, Integer mealTypeId);

    // Xóa tất cả món ăn của một bữa cụ thể để lưu lại (Dùng cho logic Save Batch)
    @Transactional
    @Modifying
    @Query("DELETE FROM MenuDetail md WHERE md.menuId = ?1 AND md.mealTypeId = ?2")
    void deleteByMenuIdAndMealTypeId(Integer menuId, Integer mealTypeId);
}
package com.example.dto;

import lombok.Data;
import java.util.List;

@Data
public class MealSaveRequest {
    private String date;
    private Integer menuId;
    private Integer mealTypeId;
    private List<MealItemDTO> items; // Tên phải là 'items' để khớp với JSON từ JS gửi lên

    @Data
    public static class MealItemDTO {
        private Integer mealTypeId;
        private Integer foodId;
    }
}
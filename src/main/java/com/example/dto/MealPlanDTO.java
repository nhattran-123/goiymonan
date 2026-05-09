package com.example.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MealPlanDTO {
    private Integer menuId;
    private Double targetCalories;
    private Double totalConsumedCalories;
    
    // Đổi tên từ meals thành sections để khớp với JS (menuData.sections)
    private List<MealSectionDTO> sections; 
}

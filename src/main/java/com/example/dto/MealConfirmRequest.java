package com.example.dto;
import lombok.Data;

@Data
public class MealConfirmRequest {
    private Integer menuId;
    private Integer mealTypeId;
    private String selectedDate;
}

package com.example.dto;

import java.util.List;

import lombok.Data;

@Data
public class TodayMealDTO {
    private Integer mealTypeId;
    private String mealName;
    private Float totalCalories;
    private List<FoodDTO> foods;
    private boolean isConfirmed;
}
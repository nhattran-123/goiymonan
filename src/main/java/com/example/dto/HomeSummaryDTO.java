package com.example.dto;
import java.util.List;
import lombok.Data;
@Data

public class HomeSummaryDTO {
    private Integer menuId;
    private String name;

    private float bmi ;
    private String bmiStatus;
    private float bmr;

    private String goalType;

    private float todayCalories;
    private float targetCalories;
    private float remainCalories;

    private List<TodayMealDTO> todayMeals;
    private List<FoodSuggestionDTO> homeSuggestions;
}

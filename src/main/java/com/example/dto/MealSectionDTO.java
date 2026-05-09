package com.example.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MealSectionDTO {
    @JsonProperty("mealTypeId") // Ép tên giống JS dòng 51
    private Integer mealTypeId;
    
    @JsonProperty("foods")
    private List<FoodDTO> foods;
    
    @JsonProperty("confirmed")
    private boolean confirmed;
}
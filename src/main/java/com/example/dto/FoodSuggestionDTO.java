package com.example.dto;

import lombok.Data;

@Data
public class FoodSuggestionDTO {

    private Integer foodId;
    private String foodName;
    private String imageUrl;
    private Double calories;
    private Integer allergyConflictCount;
    private Float suitabilityScore;
    private Integer foodType;
}
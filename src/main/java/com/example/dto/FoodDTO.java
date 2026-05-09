package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodDTO {
    private Integer foodId;
    private String foodName;
    private String description;
    private String recipe;
    private String imageUrl;

    private Double calories;
    private Double protein;
    private Double fat;
    private Double carbohydrate;

    private Boolean isFavorite; 
    private Integer foodType;
    
    private List<IngredientInFoodDTO> ingredients;          
}
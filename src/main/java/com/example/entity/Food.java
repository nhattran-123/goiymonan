package com.example.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "food")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Food_id")
    private Integer foodId;

    @Column(name = "Food_name", length = 510)
    private String foodName;

    @Column(name = "description", columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "recipe", columnDefinition = "nvarchar(max)")
    private String recipe;

    @Column(name = "image_url", columnDefinition = "varchar(max)")
    private String imageUrl;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "fat")
    private Double fat;

    @Column(name = "carbohydrate")
    private Double carbohydrate;

    @Column(name="create_at")
    private LocalDate createAt;

    @JsonIgnore
    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FoodIngredient> foodIngredients;

    @JsonIgnore
    @OneToMany(mappedBy = "food", fetch = FetchType.LAZY)
    private List<FoodDisease> foodDiseases;

    
}

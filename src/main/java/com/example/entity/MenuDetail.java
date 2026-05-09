package com.example.entity;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "menu_detail")
@Data
public class MenuDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Detail_id")
    private Integer detailId;

    @Column(name = "Menu_id")
    private Integer menuId;

    @Column(name = "Food_id")
    private Integer foodId;

    @Column(name = "Meal_type_id")
    private Integer mealTypeId;

    @Column(name = "Is_confirmed")
    private Boolean isConfirmed = false;
}
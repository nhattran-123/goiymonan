package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "daily_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private int menuId;

    @Column(name = "User_id")
    private int userId;

    @Column(name = "plan_id")
    private int planId;

    @Column(name = "Menu_date")
    private LocalDate menuDate;

    @Column(name = "total_calories")
    private double totalCalories;

    @Column(name = "Status", length = 100)
    private String status;
}
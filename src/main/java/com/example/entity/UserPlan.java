package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "user_plan") 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Setting_id")
    private int settingId;

    @Column(name = "User_id")
    private int userId;

    @Column(name = "week_start_day")
    private LocalDate weekStartDay;

    @Column(name = "same_schedule_daily")
    private boolean sameScheduleDaily;

    @Column(name = "nutrition_goal", length = 255)
    private String nutritionGoal;
}
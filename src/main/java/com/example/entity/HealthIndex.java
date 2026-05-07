package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_index")
@Data
public class HealthIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="Id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private float bmi;
    private float bmr;
    private float tdee;
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt = LocalDateTime.now();

}
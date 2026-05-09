package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_height_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeightHeightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @ManyToOne
    @JoinColumn(name = "User_id")
    private User user;

    @Column(name = "Weight")
    private double weight;

    @Column(name = "Height")
    private double height;

    @Column(name = "record_date")
    private LocalDateTime recordedAt;
}
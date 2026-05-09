package com.example.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoryDTO {
    private String foodName;
    private String imageUrl;
    private LocalDateTime eatenAt; 
    private Double calories; 
}
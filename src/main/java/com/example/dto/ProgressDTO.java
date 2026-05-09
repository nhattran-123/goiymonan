package com.example.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProgressDTO {
    private Float todayCalories;
    private Float currentWeight;
    private Float currentHeight;
    private Float targetWeight;
    private Float targetHeight;
    private Integer totalDaysFollowed;
    private String goalLabel;
    private Float bmi;
    private Float weightProgressPercent;
    private Float heightProgressPercent;
    
    // Thêm list này để hiện mục "Lịch sử ăn uống"
    private List<HistoryDTO> recentHistory;
    private List<BodyHistoryDTO> weightHistory;
    private List<BodyHistoryDTO> heightHistory;
}
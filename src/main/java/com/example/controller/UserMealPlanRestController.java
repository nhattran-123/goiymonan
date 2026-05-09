package com.example.controller;

import com.example.dto.MealConfirmRequest; // Thêm import này
import com.example.dto.MealSaveRequest;
import com.example.dto.UserDTO;
import com.example.service.MealPlanService;
import com.example.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserMealPlanRestController {

    private final MealPlanService mealPlanService;
    private final UserService userService;

    // 1. Lấy Dashboard Summary (Trang chủ)
    @GetMapping("/home-summary")
    public ResponseEntity<?> getHomeSummary(Authentication authentication) {
        UserDTO currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(
            mealPlanService.getHomeDashboardData(currentUser.getId())
        );
    }

    // 2. Lấy kế hoạch bữa ăn theo ngày
    @GetMapping("/meal-plan")
    public ResponseEntity<?> getMealPlan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        UserDTO currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(mealPlanService.getMealPlan(currentUser.getId().intValue(), date));
    }

    // 3. Lưu kế hoạch 
    @PostMapping("/meal-plan/save-batch")
    public ResponseEntity<?> saveBatch(@RequestBody MealSaveRequest request) {
        try {
            mealPlanService.saveMealSession(request);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. XÁC NHẬN ĐÃ ĂN 
    @PostMapping("/meal-plan/confirm")
    public ResponseEntity<?> confirmMeal(
            @RequestBody MealConfirmRequest request, 
            Authentication authentication) {
        try {
            UserDTO currentUser = userService.getCurrentUser(authentication);
            mealPlanService.confirmMeal(currentUser.getId().intValue(), request);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Đã xác nhận bữa ăn và cập nhật Calo nạp vào!"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
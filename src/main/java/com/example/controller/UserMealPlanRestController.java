package com.example.controller;

import com.example.dto.UserDTO;
import com.example.service.MealPlanService;
import com.example.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserMealPlanRestController {

    @Autowired private MealPlanService mealPlanService;
    
    private final UserService userService;

    @GetMapping("/home-summary")
    public ResponseEntity<?> getHomeSummary(Authentication authentication) {
        UserDTO currentUser =
            userService.getCurrentUser(authentication);
        return ResponseEntity.ok(
            mealPlanService.getHomeDashboardData(currentUser.getId())
        );
    }

    @GetMapping("/meal-plan")
    public ResponseEntity<?> getMealPlan(@RequestParam String date, Authentication authentication) {
        UserDTO currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(mealPlanService.getDailyMealPlan(currentUser.getId(), date));
    }


    @GetMapping("/all-foods-simple")
    public ResponseEntity<?> getAllFoodsSimple() {
        return ResponseEntity.ok(mealPlanService.getAllFoodsSimple());
    }

    @DeleteMapping("/meal-plan/remove")
    public ResponseEntity<?> removeMealDetail(@RequestParam Integer detailId, Authentication authentication) {
        UserDTO currentUser = userService.getCurrentUser(authentication);
        mealPlanService.removeMealDetail(currentUser.getId(), detailId);
        return ResponseEntity.ok().build();
    }
     

    @PostMapping("/meal-plan/update")
     public ResponseEntity<?> updateMeal(@RequestBody Map<String, Object> payload, Authentication authentication) {
        UserDTO currentUser = userService.getCurrentUser(authentication);
         try {
            mealPlanService.updateUserMeal(currentUser.getId(), payload);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
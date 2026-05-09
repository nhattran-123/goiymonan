package com.example.controller;

import com.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementRestController {

    @Autowired private AdminService adminService;
    @Autowired private FoodService foodService;
    @Autowired private IngredientService ingredientService;
    @Autowired private DiseaseService diseaseService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardData());
    }

    
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam int page, @RequestParam String keyword, @RequestParam String role) {
        return ResponseEntity.ok(adminService.getUsersPaged(page, keyword, role));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestParam boolean active) {
        adminService.toggleUserStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ingredients")
    public ResponseEntity<?> getIngredients(
        @RequestParam int page,
        @RequestParam(defaultValue = "") String keyword) {

            return ResponseEntity.ok(
                ingredientService.getIngredients(keyword, page, 10)
            );
    }

    
    @GetMapping("/foods")
    public ResponseEntity<?> getAllFoods() {
        return ResponseEntity.ok(foodService.getAllFoodsForAdmin());
    }

    @GetMapping("/foods/{id}")
    public ResponseEntity<?> getFoodById(@PathVariable Integer id) {
        return ResponseEntity.ok(foodService.getFoodDetailForAdmin(id));
    }

    @PostMapping("/add-food")
    public ResponseEntity<?> addFood(@RequestParam("imageFile") MultipartFile file, @RequestParam Map<String, Object> params) {
        foodService.saveFoodWithImage(file, params);
        return ResponseEntity.ok().build();
    }

     @PutMapping("/foods/{id}")
    public ResponseEntity<?> updateFood(
            @PathVariable Integer id,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam Map<String, Object> params,
            @RequestParam(value = "ingredientIds", required = false) java.util.List<Integer> ingredientIds,
            @RequestParam(value = "quantities", required = false) java.util.List<Double> quantities,
            @RequestParam(value = "units", required = false) java.util.List<String> units) {
        foodService.updateFoodWithImageAndIngredients(id, imageFile, params, ingredientIds, quantities, units);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/foods/{id}")
    public ResponseEntity<?> deleteFood(@PathVariable Integer id) {
        foodService.deleteFood(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/diseases")
    public ResponseEntity<?> getDiseases() {
        return ResponseEntity.ok(diseaseService.getAllDiseases());
    }

    @GetMapping("/compatibility")
    public ResponseEntity<?> getCompatibility() {
        return ResponseEntity.ok(diseaseService.getAllCompatibility());
    }
    

    @PostMapping("/diseases")
    public ResponseEntity<?> addDisease(@RequestBody com.example.entity.Disease disease) {
        return ResponseEntity.ok(diseaseService.createDisease(disease));
    }

    @PutMapping("/diseases/{id}")
    public ResponseEntity<?> updateDisease(@PathVariable Integer id, @RequestBody com.example.entity.Disease disease) {
        return ResponseEntity.ok(diseaseService.updateDisease(id, disease));
    }

    @DeleteMapping("/diseases/{id}")
    public ResponseEntity<?> deleteDisease(@PathVariable Integer id) {
        diseaseService.deleteDisease(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/compatibility")
    public ResponseEntity<?> addCompatibility(@RequestBody com.example.dto.DiseaseCompatibilityDTO dto) {
        return diseaseService.addCompatibility(dto);
    }

    @DeleteMapping("/compatibility/{id}")
    public ResponseEntity<?> deleteCompatibility(@PathVariable Integer id) {
        diseaseService.deleteCompatibility(id);
        return ResponseEntity.ok().build();
    }

}
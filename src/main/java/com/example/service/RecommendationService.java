package com.example.service;

import com.example.dto.FoodDTO;
import com.example.dto.FoodSuggestionDTO;
import com.example.dto.UserDTO;
import com.example.entity.*;
import com.example.repository.FoodRepository;
import com.example.repository.UserDiseaseRepository;
import com.example.repository.UserAllergyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired 
    private FoodRepository foodRepo;

    @Autowired 
    private UserDiseaseRepository userDiseaseRepo;

    @Autowired 
    private UserAllergyRepository userAllergyRepo;

    /**
     * Lấy danh sách món ăn gợi ý kèm Meta Data (Dùng cho home.js và food_list.js)
     */
    public List<FoodSuggestionDTO> getSuggestedFoodsWithMeta(UserDTO currentUser) {

        List<Food> allFoods = foodRepo.findSafeFoodsForUser(currentUser.getId());

        List<UserDisease> userDiseases =
            userDiseaseRepo.findByUserId(currentUser.getId());

        List<UserAllergy> userAllergies =
            userAllergyRepo.findByUserId(currentUser.getId());

        return allFoods.stream()

            .map(food ->
                    convertToFoodSuggestionDTO(
                            food,
                            currentUser,
                            userDiseases,
                            userAllergies
                    )
            )

            .filter(item ->
                    item.getSuitabilityScore() >= 50
            )

            .sorted((a, b) ->
                    Float.compare(
                            b.getSuitabilityScore(),
                            a.getSuitabilityScore()
                    )
            )

            .collect(Collectors.toList());
   }

    /**
     * Tìm kiếm món ăn kèm Meta Data (Dùng cho search.js)
     */
    public List<FoodDTO> searchFoodsWithSuitability(String keyword) {
        List<Food> foods = foodRepo.findByFoodNameContainingIgnoreCase(keyword);

        return foods.stream()
            .map(this::convertToFoodDTO)
            .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Entity Food sang Map chứa đầy đủ thông tin Frontend yêu cầu
     */
    private FoodSuggestionDTO convertToFoodSuggestionDTO(
        Food food,
        UserDTO user,
        List<UserDisease> userDiseases,
        List<UserAllergy> userAllergies
        ) {

            FoodSuggestionDTO dto = new FoodSuggestionDTO();

            float score = (float) calculateSuitability(
                food,
                user,
                userDiseases,
                userAllergies
            );

            int conflicts = calculateAllergyConflicts(
                food,
                userAllergies
            );

            dto.setFoodId(food.getFoodId());
            dto.setFoodName(food.getFoodName());
            dto.setImageUrl(food.getImageUrl());
            dto.setCalories(food.getCalories());
            dto.setAllergyConflictCount(conflicts);
            dto.setSuitabilityScore(score);
            dto.setFoodType(food.getFoodType());

           return dto;
        }

    /**
     * Công thức tính điểm phù hợp (Suitability Score)
     */
    public double calculateSuitability(Food food, UserDTO user, List<UserDisease> userDiseases, List<UserAllergy> userAllergies) {
        // 1. Tính toán avgRating dựa trên đánh giá món ăn đối với bệnh của User
        Set<Integer> userDiseaseIds = userDiseases.stream()
                .map(ud -> ud.getDisease().getDiseaseId())
                .collect(Collectors.toSet());

        double avgRating = 3.0; // Mặc định trung bình
        
        // Logic: Nếu món ăn có bản ghi FoodDisease khớp với bệnh của User, lấy Rating đó
        if (food.getFoodDiseases() != null && !userDiseaseIds.isEmpty()) {
            double sum = 0;
            int count = 0;
            for (FoodDisease fd : food.getFoodDiseases()) {
                if (userDiseaseIds.contains(fd.getDisease().getDiseaseId())) {
                    sum += fd.getRating();
                    count++;
                }
            }
            if (count > 0) avgRating = sum / count;
        }

        // 2. Tính toán conflict_count
        int conflictCount = calculateAllergyConflicts(food, userAllergies);

        // 3. Áp dụng công thức
        double score = (avgRating * 20.0) - (conflictCount * 25.0);

        // 4. Giới hạn điểm
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        return score;
    }

    /**
     * Tính toán số lượng nguyên liệu gây dị ứng trong món ăn
     */
    private int calculateAllergyConflicts(Food food, List<UserAllergy> userAllergies) {
        if (food.getFoodIngredients() == null || userAllergies.isEmpty()) return 0;

        Set<Integer> allergyIngredientIds = userAllergies.stream()
                .map(ua -> ua.getIngredient().getIngredientId())
                .collect(Collectors.toSet());

        return (int) food.getFoodIngredients().stream()
                .filter(fi -> allergyIngredientIds.contains(fi.getIngredient().getIngredientId()))
                .count();
    }

    private FoodDTO convertToFoodDTO(Food food) {
    FoodDTO dto = new FoodDTO();

    dto.setFoodId(food.getFoodId());
    dto.setFoodName(food.getFoodName());
    dto.setImageUrl(food.getImageUrl());
    dto.setCalories(food.getCalories());

    return dto;
}
}
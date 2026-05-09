package com.example.service;

import com.example.dto.FoodDTO;
import com.example.dto.IngredientDTO;
import com.example.dto.IngredientInFoodDTO;
import com.example.dto.UserDTO;
import com.example.entity.*;
import com.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FoodService {

    @Autowired private FoodRepository foodRepo;
    @Autowired private UserFavoriteRepository favoriteRepo;
    @Autowired private FoodIngredientRepository foodIngRepo;
    @Autowired private AdjustedRecipeRepository adjustedRepo;
    @Autowired private IngredientRepository ingredientRepo;
    @Autowired private RecommendationService recommendationService;
    @Autowired private UserRepository userRepo;
    @Autowired private UserService userService;
    @Autowired private UserDiseaseRepository userDiseaseRepo;
    @Autowired private UserAllergyRepository userAllergyRepo;
    public FoodDTO getFoodDetailForUser(Integer foodId,Long userId){
        Food food = foodRepo.findById(foodId).orElse(null);

        boolean favoriteStatus = favoriteRepo.existsByUser_IdAndFood_FoodId(userId, foodId);
        if(food == null) return null;
        List<FoodIngredient> list = foodIngRepo.findByFood_FoodId(foodId);
        List<IngredientInFoodDTO> ingredientDTOs = list.stream().map(fi -> {
            IngredientInFoodDTO dto = new IngredientInFoodDTO();

            // từ Ingredient
            dto.setIngredientId(fi.getIngredient().getIngredientId());
            dto.setIngredientName(fi.getIngredient().getIngredientName());

            // từ FoodIngredient
            dto.setQuantity(fi.getQuantity());
            dto.setUnit(fi.getUnit());

             return dto;
        }).toList();
        FoodDTO dto = new FoodDTO();
        dto.setFoodId(food.getFoodId());
        dto.setFoodName(food.getFoodName());
        dto.setDescription(food.getDescription());
        dto.setRecipe(food.getRecipe());
        dto.setImageUrl(food.getImageUrl());

        dto.setCalories(food.getCalories());
        dto.setProtein(food.getProtein());
        dto.setFat(food.getFat());
        dto.setCarbohydrate(food.getCarbohydrate());

        dto.setIngredients(ingredientDTOs);
        dto.setIsFavorite(favoriteStatus);
        return dto;
    }

    @Transactional
    public void saveFoodWithImage(MultipartFile file, Map<String, Object> params,
                                  List<Integer> ingredientIds, List<Double> quantities, List<String> units) {
        
        String fileName = (file != null) ? file.getOriginalFilename() : "default_food.jpg"; 
        
        Food food = new Food();
        food.setFoodName((String) params.get("name"));
        food.setDescription((String) params.get("description"));
        food.setRecipe((String) params.get("recipe"));
        food.setImageUrl(fileName);
        
        food.setCalories(Double.parseDouble(params.get("calories").toString()));
        food.setProtein(Double.parseDouble(params.get("protein").toString()));
        food.setFat(Double.parseDouble(params.get("fat").toString()));
        food.setCarbohydrate(Double.parseDouble((params.get("carbohydrate") != null ? params.get("carbohydrate") : params.get("carb")).toString()));
        if (params.get("foodType") != null && !params.get("foodType").toString().isBlank()) {
            food.setFoodType(Integer.parseInt(params.get("foodType").toString()));
        }
        
        Food savedFood = foodRepo.save(food);

        if (ingredientIds == null || quantities == null || units == null) return;

        for (int i = 0; i < ingredientIds.size(); i++) {
            if (i >= quantities.size() || i >= units.size()) break;
            Optional<Ingredient> ingredientOpt = ingredientRepo.findById(ingredientIds.get(i));
            if (ingredientOpt.isEmpty()) continue;

            FoodIngredient foodIngredient = new FoodIngredient();
            foodIngredient.setFood(savedFood);
            foodIngredient.setIngredient(ingredientOpt.get());
            foodIngredient.setQuantity(quantities.get(i));
            foodIngredient.setUnit(units.get(i));
            foodIngRepo.save(foodIngredient);
        }
    }

    /**
     * Lưu món ăn kèm danh sách định lượng nguyên liệu
     */
    @Transactional
public void saveFoodWithIngredients(FoodDTO dto) {
    Food food = new Food();
    food.setFoodName(dto.getFoodName());
    food.setDescription(dto.getDescription());
    food.setRecipe(dto.getRecipe());
    food.setCalories(dto.getCalories());
    food.setProtein(dto.getProtein());
    food.setFat(dto.getFat());
    food.setCarbohydrate(dto.getCarbohydrate());

    Food savedFood = foodRepo.save(food);

    // 🔥 xử lý ingredients mới
    if (dto.getIngredients() != null) {
        for (IngredientInFoodDTO ing : dto.getIngredients()) {
            FoodIngredient fi = new FoodIngredient();

            fi.setFood(savedFood);

            ingredientRepo.findById(ing.getIngredientId())
                .ifPresent(fi::setIngredient);

            fi.setQuantity(ing.getQuantity());
            fi.setUnit(ing.getUnit());

            foodIngRepo.save(fi);
        }
    }
}
    /**
     * Tùy chỉnh công thức nấu ăn riêng cho người dùng
     */
    @Transactional
    public void customizeRecipe(Long userId, Integer foodId, String newRecipe, FoodDTO nutrients) {
        AdjustedRecipe ar = adjustedRepo.findByFoodIdAndUserId(foodId, userId)
            .orElse(new AdjustedRecipe());
        
        ar.setFoodId(foodId);
        ar.setUserId(userId);
        ar.setRecipe(newRecipe);
        ar.setCalories(nutrients.getCalories().floatValue());
        ar.setProtein(nutrients.getProtein().floatValue());
        ar.setFat(nutrients.getFat().floatValue());
        ar.setCarbohydrate(nutrients.getCarbohydrate().floatValue());
        adjustedRepo.save(ar);
    }

    public List<FoodDTO> getAllFoodsForAdmin() {
        return foodRepo.findAll().stream().map(f -> {
            FoodDTO dto = new FoodDTO();
            dto.setFoodId(f.getFoodId());
            dto.setFoodName(f.getFoodName());
            dto.setImageUrl(f.getImageUrl());
            dto.setCalories(f.getCalories());
            dto.setProtein(f.getProtein());
            dto.setFat(f.getFat());
            dto.setCarbohydrate(f.getCarbohydrate());
            return dto;
        }).toList();
   }
    public FoodDTO getFoodDetailForAdmin(Integer foodId) {
        Food food = foodRepo.findById(foodId).orElse(null);
        if (food == null) return null;

        List<IngredientInFoodDTO> ingredientDTOs = foodIngRepo.findByFood_FoodId(foodId).stream().map(fi -> {
            IngredientInFoodDTO dto = new IngredientInFoodDTO();
            dto.setIngredientId(fi.getIngredient().getIngredientId());
            dto.setIngredientName(fi.getIngredient().getIngredientName());
            dto.setQuantity(fi.getQuantity());
            dto.setUnit(fi.getUnit());
            return dto;
        }).toList();

        FoodDTO dto = new FoodDTO();
        dto.setFoodId(food.getFoodId());
        dto.setFoodName(food.getFoodName());
        dto.setDescription(food.getDescription());
        dto.setRecipe(food.getRecipe());
        dto.setImageUrl(food.getImageUrl());
        dto.setCalories(food.getCalories());
        dto.setProtein(food.getProtein());
        dto.setFat(food.getFat());
        dto.setCarbohydrate(food.getCarbohydrate());
        dto.setFoodType(food.getFoodType());
        dto.setIngredients(ingredientDTOs);
        return dto;
    }

    @Transactional
    public void updateFoodWithImageAndIngredients(Integer foodId, MultipartFile imageFile, Map<String, Object> params,
                                                  List<Integer> ingredientIds, List<Double> quantities, List<String> units) {
        Food food = foodRepo.findById(foodId).orElseThrow(() -> new RuntimeException("Food not found"));
        food.setFoodName((String) params.get("name"));
        food.setDescription((String) params.get("description"));
        food.setRecipe((String) params.get("recipe"));
        food.setCalories(Double.parseDouble(params.get("calories").toString()));
        food.setProtein(Double.parseDouble(params.get("protein").toString()));
        food.setFat(Double.parseDouble(params.get("fat").toString()));
        food.setCarbohydrate(Double.parseDouble(params.get("carbohydrate").toString()));
        if (params.get("foodType") != null && !params.get("foodType").toString().isBlank()) {
            food.setFoodType(Integer.parseInt(params.get("foodType").toString()));
        }
        if (imageFile != null && !imageFile.isEmpty() && imageFile.getOriginalFilename() != null) {
            food.setImageUrl(imageFile.getOriginalFilename());
        }
        foodRepo.save(food);

        foodIngRepo.deleteByFoodFoodId(foodId);
        if (ingredientIds == null || quantities == null || units == null) return;

        for (int i = 0; i < ingredientIds.size(); i++) {
            if (i >= quantities.size() || i >= units.size()) break;
            Optional<Ingredient> ingredientOpt = ingredientRepo.findById(ingredientIds.get(i));
            if (ingredientOpt.isEmpty()) continue;
            FoodIngredient fi = new FoodIngredient();
            fi.setFood(food);
            fi.setIngredient(ingredientOpt.get());
            fi.setQuantity(quantities.get(i));
            fi.setUnit(units.get(i));
            foodIngRepo.save(fi);
        }
    }


    public void deleteFood(Integer id) {
        foodRepo.deleteById(id);
    }
}
package com.example.service;

import com.example.dto.*;
import com.example.entity.*;
import com.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MealPlanService {

    @Autowired private DailyMenuRepository dailyMenuRepo;
    @Autowired private MenuDetailRepository menuDetailRepo;
    @Autowired private FoodRepository foodRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private UserGoalRepository userGoalRepo;
    @Autowired private HealthIndexRepository healthIndexRepo;
    @Autowired private RecommendationService recommendationService;
    @Autowired private UserService userService;
    @Autowired private UserHistoryRepository userHistoryRepo;

    /**
     * 1. Lấy dữ liệu tổng hợp cho Trang chủ (Dashboard)
     * Trả về thông tin chỉ số cơ thể và 4 thẻ bữa ăn kèm ảnh món ăn
     */
    public HomeSummaryDTO getHomeDashboardData(Long userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return new HomeSummaryDTO();

        UserDTO userDTO = userService.getUserFullProfile(user.getId());
        HomeSummaryDTO dto = new HomeSummaryDTO();
        dto.setName(user.getName());

        // Tính toán BMI
        float weight = user.getWeight();
        float height = user.getHeight();
        float bmi = (float) (weight / Math.pow(height / 100.0, 2));
        dto.setBmi(bmi);
        dto.setBmiStatus(getBMIStatus(bmi));

        // Tính toán BMR
        float bmr = (float) ((10 * weight) + (6.25 * height) - (5 * (user.getAge() != null ? user.getAge() : 0)));
        bmr = (user.getGender() != null && user.getGender()) ? bmr + 5 : bmr - 161;
        dto.setBmr(bmr);

        // Lấy mục tiêu
        UserGoal goal = userGoalRepo.findTopByUserIdOrderByIdDesc(userId).orElse(null);
        dto.setGoalType(goal != null ? goal.getGoalType() : "Duy trì");

        // Xử lý dữ liệu 4 thẻ bữa ăn hôm nay
        DailyMenu todayMenu = dailyMenuRepo.findByUserIdAndMenuDate(userId.intValue(), LocalDate.now()).orElse(null);
        float todayCalories = 0f;
        List<TodayMealDTO> todayMealList = new ArrayList<>();

        if (todayMenu != null) {
            dto.setMenuId(todayMenu.getMenuId());
            List<MenuDetail> details = menuDetailRepo.findByMenuId(todayMenu.getMenuId());

            // Gom nhóm món ăn theo mealTypeId
            Map<Integer, List<MenuDetail>> groupedMeals = details.stream()
                    .filter(d -> d.getMealTypeId() != null)
                    .collect(Collectors.groupingBy(MenuDetail::getMealTypeId));

            for (int typeId = 1; typeId <= 4; typeId++) {
                List<MenuDetail> items = groupedMeals.getOrDefault(typeId, new ArrayList<>());
                
                TodayMealDTO mealDto = new TodayMealDTO();
                mealDto.setMealTypeId(typeId);
                mealDto.setMealName(getMealName(typeId));

                // Map sang FoodDTO để lấy ảnh thumbnail cho Dashboard
                List<FoodDTO> foodInMeal = items.stream()
                        .map(i -> foodRepo.findById(i.getFoodId()).map(this::mapToFoodDTO).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                mealDto.setFoods(foodInMeal);

                // Tính tổng calo bữa này
                float mealTotalCalo = (float) foodInMeal.stream().mapToDouble(FoodDTO::getCalories).sum();
                mealDto.setTotalCalories(mealTotalCalo);

                // Trạng thái đã xác nhận ăn hay chưa
                boolean isConfirmed = items.stream().anyMatch(d -> Boolean.TRUE.equals(d.getIsConfirmed()));
                mealDto.setConfirmed(isConfirmed);

                todayMealList.add(mealDto);
            }
            todayCalories = (float) todayMenu.getTotalCalories();
        }

        // TDEE/Target Calo
        float targetCalories = bmr * 1.2f; 
        dto.setTodayCalories(todayCalories);
        dto.setTargetCalories(targetCalories);
        dto.setRemainCalories(targetCalories - todayCalories);
        dto.setTodayMeals(todayMealList);
        dto.setHomeSuggestions(recommendationService.getSuggestedFoodsWithMeta(userDTO));

        return dto;
    }

    /**
     * 2. Lấy thông tin thực đơn chi tiết của một ngày (Trang Meal Plan)
     */
    public MealPlanDTO getMealPlan(Integer userId, LocalDate date) {
        MealPlanDTO dto = new MealPlanDTO();

        HealthIndex hi = healthIndexRepo.findTopByUser_IdOrderByIdDesc(userId).orElse(null);
        dto.setTargetCalories(hi != null ? (double) hi.getTdee() : 2000.0);

        DailyMenu menu = dailyMenuRepo.findByUserIdAndMenuDate(userId, date)
                .orElseGet(() -> {
                    DailyMenu newMenu = new DailyMenu();
                    newMenu.setUserId(userId);
                    newMenu.setMenuDate(date);
                    newMenu.setTotalCalories(0.0);
                    newMenu.setStatus("IN_PROGRESS");
                    return dailyMenuRepo.save(newMenu);
                });

        dto.setMenuId(menu.getMenuId());
        dto.setTotalConsumedCalories(menu.getTotalCalories());

        List<MenuDetail> details = menuDetailRepo.findByMenuId(menu.getMenuId());
        List<MealSectionDTO> sections = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            final int currentMealTypeId = i;
            MealSectionDTO section = new MealSectionDTO();
            section.setMealTypeId(currentMealTypeId);
            
            List<MenuDetail> detailsInMeal = details.stream()
                .filter(d -> d.getMealTypeId() != null && d.getMealTypeId().intValue() == currentMealTypeId)
                .collect(Collectors.toList());

            List<FoodDTO> foodsInMeal = detailsInMeal.stream()
                .map(d -> foodRepo.findById(d.getFoodId()).map(this::mapToFoodDTO).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            section.setFoods(foodsInMeal);
            section.setConfirmed(detailsInMeal.stream().anyMatch(d -> Boolean.TRUE.equals(d.getIsConfirmed())));
            sections.add(section);
        }
        
        dto.setSections(sections);
        return dto;
    }

    /**
     * 3. Lưu kế hoạch ăn uống (Batch Save)
     */
    @Transactional
    public void saveMealSession(MealSaveRequest request) {
        menuDetailRepo.deleteByMenuIdAndMealTypeId(request.getMenuId(), request.getMealTypeId());

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<MenuDetail> details = request.getItems().stream().map(item -> {
                MenuDetail detail = new MenuDetail();
                detail.setMenuId(request.getMenuId());
                detail.setMealTypeId(request.getMealTypeId());
                detail.setFoodId(item.getFoodId());
                detail.setIsConfirmed(false);
                return detail;
            }).collect(Collectors.toList());
            
            menuDetailRepo.saveAll(details);
            menuDetailRepo.flush();
        }
    }

    /**
     * 4. Xác nhận đã ăn (Confirm Meal)
     */
    @Transactional
    public void confirmMeal(Integer userId, MealConfirmRequest request) {
        LocalDate selectedDate = LocalDate.parse(request.getSelectedDate());
        
        List<MenuDetail> details = menuDetailRepo.findByMenuIdAndMealTypeId(request.getMenuId(), request.getMealTypeId());
        double totalMealCalories = 0;

        for (MenuDetail detail : details) {
            if (Boolean.TRUE.equals(detail.getIsConfirmed())) continue;

            detail.setIsConfirmed(true);
            
            // Lưu lịch sử
            UserHistory history = new UserHistory();
            history.setUser(userRepo.findById(userId.longValue()).orElse(null));
            Food food = foodRepo.findById(detail.getFoodId()).orElse(null);
            history.setFood(food);
            history.setEatenAt(LocalDateTime.now());
            userHistoryRepo.save(history);

            if (food != null) totalMealCalories += food.getCalories();
        }
        menuDetailRepo.saveAll(details);

        DailyMenu menu = dailyMenuRepo.findById(request.getMenuId()).orElseThrow();
        menu.setTotalCalories(menu.getTotalCalories() + totalMealCalories);
        dailyMenuRepo.save(menu);
    }

    // --- CÁC HÀM BỔ TRỢ ---

    private String getMealName(int typeId) {
        return switch (typeId) {
            case 1 -> "Bữa sáng";
            case 2 -> "Bữa trưa";
            case 3 -> "Bữa tối";
            case 4 -> "Bữa phụ";
            default -> "Khác";
        };
    }

    private String getBMIStatus(double bmi) {
        if (bmi < 18.5) return "Gầy";
        if (bmi < 25) return "Bình thường";
        return "Thừa cân";
    }

    private FoodDTO mapToFoodDTO(Food f) {
        FoodDTO d = new FoodDTO();
        d.setFoodId(f.getFoodId());
        d.setFoodName(f.getFoodName());
        d.setImageUrl(f.getImageUrl());
        d.setCalories(f.getCalories());
        d.setProtein(f.getProtein());
        d.setFat(f.getFat());
        d.setCarbohydrate(f.getCarbohydrate());
        d.setFoodType(f.getFoodType());
        return d;
    }
}
package com.example.service;

import com.example.dto.HomeSummaryDTO;
import com.example.dto.UserDTO;
import com.example.entity.*;
import com.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.time.format.TextStyle;
import java.util.stream.Collectors;

@Service
public class MealPlanService {

    @Autowired private DailyMenuRepository dailyMenuRepo;
    @Autowired private MenuDetailRepository menuDetailRepo;
    @Autowired private FoodRepository foodRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private UserGoalRepository userGoalRepo;
    @Autowired private RecommendationService recommendationService;
    @Autowired private UserService userService;
    @Autowired private MealTypeRepository mealTypeRepo;

    // 1. Hàm thêm món ăn (Giữ nguyên logic của bạn)[cite: 9, 17]
    @Transactional
    public void addFoodToMenu(Long userId, LocalDate date, Integer mealTypeId, Integer foodId) {
        DailyMenu menu = dailyMenuRepo.findByUserIdAndMenuDate(userId.intValue(), date)
            .orElseGet(() -> {
                DailyMenu newMenu = new DailyMenu();
                newMenu.setUserId(userId.intValue());
                newMenu.setMenuDate(date);
                newMenu.setStatus("Pending");
                return dailyMenuRepo.save(newMenu);
            });

        MenuDetail detail = new MenuDetail();
        detail.setMenuId(menu.getMenuId());
        detail.setFoodId(foodId);
        detail.setMealTypeId(mealTypeId);
        menuDetailRepo.save(detail);

        updateTotalCalories(menu);
    }

    // 2. Hàm lấy dữ liệu trang chủ (home.js)[cite: 16]

    public HomeSummaryDTO getHomeDashboardData(Long userId) {

        User user = userRepo.findById(userId).orElse(null);
       
        UserDTO userDTO = userService.getUserFullProfile(user.getId());
        HomeSummaryDTO dto = new HomeSummaryDTO();

        if (user == null) {
            return dto;
        }

        dto.setName(user.getName());

        float bmi = (float) (user.getWeight() / Math.pow(user.getHeight() / 100.0, 2));
        dto.setBmi(bmi);
        dto.setBmiStatus(getBMIStatus(bmi));

        float bmr = (float) (
            (10 * user.getWeight())
            + (6.25 * user.getHeight())
            - (5 * user.getAge())
        );
  
        bmr = (user.getGender() != null && user.getGender())
                ? bmr + 5
                : bmr - 161;

        dto.setBmr(bmr);

        UserGoal goal = userGoalRepo
            .findTopByUserIdOrderByIdDesc(userId)
            .orElse(null);

        if (goal != null) {
            dto.setGoalType(goal.getGoalType());
        } else {
            dto.setGoalType("Duy trì");
      }

    // Calo hôm nay
         DailyMenu todayMenu = dailyMenuRepo
            .findByUserIdAndMenuDate(userId.intValue(), LocalDate.now())
            .orElse(null);

        float todayCalories = todayMenu != null
            ? (float) todayMenu.getTotalCalories()
            : 0f;

         float targetCalories = goal != null ? goal.getTargetCalories() : bmr * 1.2f;
        float remainCalories = Math.max(0f, targetCalories - todayCalories);

        dto.setTodayCalories(todayCalories);
        dto.setTargetCalories(targetCalories);
        dto.setRemainCalories(remainCalories);

    
        dto.setTodayMeals(buildTodayMeals(todayMenu));

        dto.setHomeSuggestions(
            recommendationService.getSuggestedFoodsWithMeta(userDTO)
        );

         return dto;
    }
    private List<com.example.dto.TodayMealDTO> buildTodayMeals(DailyMenu todayMenu) {
        if (todayMenu == null) {
            return new ArrayList<>();
        }

        List<MenuDetail> details = menuDetailRepo.findByMenuId(todayMenu.getMenuId());
        if (details.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, List<MenuDetail>> groupedByMealType = details.stream()
                .collect(Collectors.groupingBy(MenuDetail::getMealTypeId, TreeMap::new, Collectors.toList()));

        List<com.example.dto.TodayMealDTO> meals = new ArrayList<>();
        for (Map.Entry<Integer, List<MenuDetail>> entry : groupedByMealType.entrySet()) {
            com.example.dto.TodayMealDTO meal = new com.example.dto.TodayMealDTO();
            Integer mealTypeId = entry.getKey();
            List<MenuDetail> mealDetails = entry.getValue();

            meal.setMealName(resolveMealName(mealTypeId));
            meal.setTotalFoods(mealDetails.size());

            float totalCalories = 0f;
            for (MenuDetail detail : mealDetails) {
                totalCalories += foodRepo.findById(detail.getFoodId())
                        .map(food -> food.getCalories().floatValue())
                        .orElse(0f);
            }
            meal.setTotalCalories(totalCalories);
            meals.add(meal);
        }

        return meals;
    }

    private String resolveMealName(Integer mealTypeId) {
        return switch (mealTypeId) {
            case 1 -> "Bữa sáng";
            case 2 -> "Bữa trưa";
            case 3 -> "Bữa tối";
            case 4 -> "Bữa phụ";
            default -> "Bữa " + mealTypeId;
        };
    }
    // 3. Hàm lấy thực đơn chi tiết (meal_plan.js)
    public Map<String, Object> getDailyMealPlan(Long userId, String dateStr) {
         LocalDate date = parseSelectedDate(dateStr);
        DailyMenu menu = dailyMenuRepo.findByUserIdAndMenuDate(userId.intValue(), date).orElse(null);
        
        Map<String, Object> res = new HashMap<>();
        res.put("totalCalories", (menu != null) ? menu.getTotalCalories() : 0.0);
        res.put("canEdit", !date.isBefore(LocalDate.now()));
        
        res.put("weekSlider", buildWeekSlider(userId, date));
        
        // Trả về các bữa ăn (Sáng, Trưa, Tối...)[cite: 17]
        res.put("mealSections", getMealSections(menu));
        
        return res;
    }
     private LocalDate parseSelectedDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }
    public List<Map<String, Object>> getAllFoodsSimple() {
        return foodRepo.findAll().stream().map(food -> {
            Map<String, Object> f = new HashMap<>();
            f.put("id", food.getFoodId());
            f.put("name", food.getFoodName());
            f.put("calories", food.getCalories() == null ? 0.0 : food.getCalories());
            return f;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void removeMealDetail(Long userId, Integer detailId) {
        MenuDetail detail = menuDetailRepo.findById(detailId).orElse(null);
        if (detail == null) return;
        DailyMenu menu = dailyMenuRepo.findById(detail.getMenuId()).orElse(null);
        if (menu == null || menu.getUserId() != userId.intValue()) return;
        menuDetailRepo.deleteByDetailId(detailId);
        updateTotalCalories(menu);
    }

    // 4. Hàm điều chỉnh món ăn trong bữa (Nâng cao từ addFoodToMenu)[cite: 17]
    @Transactional
    public void updateUserMeal(Long userId, Map<String, Object> payload) {
        LocalDate date = LocalDate.parse((String) payload.get("date"));
        Integer mealTypeId = Integer.valueOf(payload.get("mealTypeId").toString());
        boolean mealTypeExists = mealTypeRepo.existsById(mealTypeId);
        if (!mealTypeExists) {
            throw new IllegalArgumentException("Meal type không tồn tại: " + mealTypeId);
        }
        List<Integer> foodIds = ((List<?>) payload.get("foodIds")).stream()
                .map(v -> Integer.valueOf(v.toString()))
                .collect(Collectors.toList());
        DailyMenu menu = dailyMenuRepo.findByUserIdAndMenuDate(userId.intValue(), date)
            .orElseGet(() -> {
                DailyMenu nm = new DailyMenu();
                nm.setUserId(userId.intValue());
                nm.setMenuDate(date);
                nm.setStatus("Pending");
                return dailyMenuRepo.save(nm);
            });

        // Xóa món cũ trong bữa đó để ghi đè món mới[cite: 17]
        menuDetailRepo.deleteByMenuIdAndMealTypeId(menu.getMenuId(), mealTypeId);

        for (Integer fId : foodIds) {
            MenuDetail d = new MenuDetail();
            d.setMenuId(menu.getMenuId());
            d.setFoodId(fId);
            d.setMealTypeId(mealTypeId);
            menuDetailRepo.save(d);
        }

        updateTotalCalories(menu);
    }
    private List<Map<String, Object>> buildWeekSlider(Long userId, LocalDate selectedDate) {
        LocalDate start = selectedDate.minusDays(3);
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.toString());
            item.put("dow", d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("vi", "VN")));
            item.put("day", d.getDayOfMonth());
            item.put("selected", d.equals(selectedDate));
            item.put("hasMeals", dailyMenuRepo.findByUserIdAndMenuDate(userId.intValue(), d).isPresent());
            days.add(item);
        }
        return days;
    }

    // --- HÀM BỔ TRỢ (PRIVATE) ---

    private void updateTotalCalories(DailyMenu menu) {
        List<MenuDetail> details = menuDetailRepo.findByMenuId(menu.getMenuId());
        double total = details.stream()
            .mapToDouble(d -> foodRepo.findById(d.getFoodId()).map(Food::getCalories).orElse(0.0))
            .sum();
        menu.setTotalCalories(total);
        dailyMenuRepo.save(menu);
    }

    private String getBMIStatus(double bmi) {
        if (bmi < 18.5) return "Gầy";
        if (bmi < 25) return "Bình thường";
        return "Thừa cân";
    }

    private List<Map<String, Object>> getMealSections(DailyMenu menu) {
        
        List<Map<String, Object>> sections = new ArrayList<>();
        List<MealType> mealTypes = mealTypeRepo.findAllByOrderByMealTypeIdAsc();
        Map<Integer, List<MenuDetail>> grouped = new HashMap<>();
        if (menu != null) {
            grouped = menuDetailRepo.findByMenuId(menu.getMenuId()).stream()
                    .collect(Collectors.groupingBy(MenuDetail::getMealTypeId));
        }

       for (MealType mt : mealTypes) {
            Integer mealTypeId = mt.getMealTypeId();
            List<MenuDetail> details = grouped.getOrDefault(mealTypeId, new ArrayList<>());
            List<Map<String, Object>> foods = new ArrayList<>();
            double usedCalories = 0.0;
            List<String> foodIds = new ArrayList<>();
            for (MenuDetail d : details) {
                Food f = foodRepo.findById(d.getFoodId()).orElse(null);
                if (f == null) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("detailId", d.getDetailId());
                item.put("foodId", f.getFoodId());
                item.put("foodName", f.getFoodName());
                item.put("imageUrl", f.getImageUrl());
                item.put("calories", f.getCalories() == null ? 0.0 : f.getCalories());
                foods.add(item);
                usedCalories += f.getCalories() == null ? 0.0 : f.getCalories();
                foodIds.add(String.valueOf(f.getFoodId()));
            }
            Map<String, Object> section = new HashMap<>();
            section.put("mealTypeId", mealTypeId);
            section.put("mealName", mt.getMealName());
            section.put("foods", foods);
            section.put("usedCalories", usedCalories);
           section.put("targetCalories", mt.getTargetCalories() == null ? 0.0 : mt.getTargetCalories());
            section.put("foodIdsString", String.join(",", foodIds));
            sections.add(section);
        }
        return sections;
    }
}
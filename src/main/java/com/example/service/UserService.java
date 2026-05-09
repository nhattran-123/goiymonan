package com.example.service;

import com.example.dto.*;
import com.example.entity.*;
import com.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private UserGoalRepository goalRepo;
    @Autowired private UserDiseaseRepository userDiseaseRepo;
    @Autowired private UserAllergyRepository userAllergyRepo;
    @Autowired private DiseaseRepository diseaseRepo;
    @Autowired private IngredientRepository ingredientRepo;
    @Autowired private WeightHeightHistoryRepository weightHistoryRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private UserHistoryRepository userHistoryRepo;
    @Autowired private DailyMenuRepository dailyMenuRepo;
    @Autowired private HealthIndexRepository healthIndexRepo;
    @Autowired private UserGoalRepository userGoalRepo;
    @Autowired private WeightHeightHistoryRepository historyRepo;

    /**
     * Đăng ký người dùng mới
     */
    @Transactional
    public boolean registerNewUser(UserDTO dto) {
        try {
            if (existsByEmail(dto.getEmail())) return false;

            User user = new User();
            user.setName(dto.getFullName());
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setGender("Nam".equalsIgnoreCase(dto.getGender()));
            user.setAge(dto.getAge());
            user.setWeight(dto.getWeight());
            user.setHeight(dto.getHeight());
            user.setDesiredWeight(dto.getDesiredWeight());
            user.setDesiredHeight(dto.getDesiredHeight());
            user.setRole("USER");
            user.setIsActivate(1);
            
            User savedUser = userRepo.save(user);

            updateUserDiseases(savedUser, dto.getDiseaseIds());
            updateUserAllergies(savedUser, dto.getAllergyIds());

            UserGoal goal = new UserGoal();
            goal.setUser(savedUser);
            goal.setGoalType(dto.getGoalType() != null ? dto.getGoalType() : "Duy trì");
            goal.setTargetCalories((float) estimateTargetCalories(dto));
            goalRepo.save(goal);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public UserDTO getUserFullProfile(Long userId){
        User user = userRepo.findById(userId)
            .orElseThrow(()-> new RuntimeException("Khong tim thay id"));
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setFullName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setGender(user.getGender() ? "Nam" : "Nữ");
            dto.setAge(user.getAge());
            dto.setWeight(user.getWeight());
            dto.setHeight(user.getHeight());
            dto.setDesiredWeight(user.getDesiredWeight());
            dto.setDesiredHeight(user.getDesiredHeight());
            dto.setDiseaseIds(userDiseaseRepo.findByUserId(userId).stream()
                    .map(ud -> (Integer) ud.getDisease().getDiseaseId())
                    .collect(Collectors.toList()));
            dto.setAllergyIds(userAllergyRepo.findByUserId(userId).stream()
                .map(ua -> (Integer) ua.getIngredient().getIngredientId()).collect(Collectors.toList()));

            return dto;
    }

    @Transactional
    public void updateFullProfile(Long userId, UserDTO dto) {
        User user = userRepo.findById(userId).orElseThrow();
        user.setName(dto.getFullName());
        user.setGender("Nam".equalsIgnoreCase(dto.getGender()));
        user.setAge(dto.getAge());
        user.setWeight(dto.getWeight());
        user.setHeight(dto.getHeight());
        user.setDesiredWeight(dto.getDesiredWeight());
        user.setDesiredHeight(dto.getDesiredHeight());
        userRepo.save(user);

        updateUserDiseases(user, dto.getDiseaseIds());
        updateUserAllergies(user, dto.getAllergyIds());
        
        updateWeightHeight(userId, dto.getWeight(), dto.getHeight());
    }

    public Map<String, Object> authenticate(LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepo.findByEmail(loginRequest.getEmail()).orElse(null);

        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            if (user.getIsActivate() != null && user.getIsActivate() == 0) {
                response.put("status", "INACTIVE");
                return response;
            }

            response.put("status", "SUCCESS");
            response.put("redirect", "ADMIN".equalsIgnoreCase(user.getRole()) ? "/admin/dashboard" : "/home");
            return response;
        }
        response.put("status", "ERROR");
        return response;
    }

    public ProgressDTO getProgressSummary(Long userId) {
    Integer userIdInt = userId.intValue(); 
    User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    LocalDate today = LocalDate.now();
    ProgressDTO dto = new ProgressDTO();

    // --- GIỮ NGUYÊN LOGIC CŨ CỦA THÀNH ---
    DailyMenu todayMenu = dailyMenuRepo.findByUserIdAndMenuDate(userIdInt, today).orElse(null);
    dto.setTodayCalories(todayMenu != null ? (float) todayMenu.getTotalCalories() : 0f);

    dto.setCurrentWeight(user.getWeight());
    dto.setCurrentHeight(user.getHeight());
    dto.setTargetWeight(user.getDesiredWeight());
    dto.setTargetHeight(user.getDesiredHeight());

    HealthIndex latestHealth = healthIndexRepo.findTopByUser_IdOrderByIdDesc(userIdInt).orElse(null);
    if (latestHealth != null) {
        dto.setBmi(latestHealth.getBmi());
    } else {
        float hMeter = user.getHeight() / 100f;
        dto.setBmi(user.getWeight() / (hMeter * hMeter));
    }

    if (user.getCreateAt() != null) {
        LocalDate registrationDate = user.getCreateAt();
        long days = java.time.temporal.ChronoUnit.DAYS.between(registrationDate, today);
        dto.setTotalDaysFollowed((int) days + 1);
    } else {
        dto.setTotalDaysFollowed(1);
    }

    UserGoal goal = userGoalRepo.findTopByUserIdOrderByIdDesc(userId).orElse(null);
    dto.setGoalLabel(goal != null ? goal.getGoalType() : "Duy trì");

    if (user.getDesiredWeight() > 0) {
        float weightPercent = (user.getWeight() / user.getDesiredWeight()) * 100f;
        dto.setWeightProgressPercent(Math.min(weightPercent, 100f));
    } else {
        dto.setWeightProgressPercent(0f);
    }

    if (user.getDesiredHeight() > 0) {
        float heightPercent = (user.getHeight() / user.getDesiredHeight()) * 100f;
        dto.setHeightProgressPercent(Math.min(heightPercent, 100f));
    } else {
        dto.setHeightProgressPercent(0f);
    }

    List<UserHistory> historyList = userHistoryRepo.findTop5ByUserIdOrderByEatenAtDesc(userId);
    List<HistoryDTO> historyDTOs = historyList.stream().map(h -> {
        HistoryDTO hDto = new HistoryDTO();
        if (h.getFood() != null) {
            hDto.setFoodName(h.getFood().getFoodName());
            hDto.setImageUrl(h.getFood().getImageUrl());
            hDto.setCalories(h.getFood().getCalories());
        }
        hDto.setEatenAt(h.getEatenAt());
        return hDto;
    }).collect(Collectors.toList());

    dto.setRecentHistory(historyDTOs);

    // --- PHẦN VIẾT THÊM CHO BIỂU ĐỒ ---

    // 1. Lấy dữ liệu lịch sử cân nặng chiều cao từ DB (Lấy 7 hoặc 10 bản ghi gần nhất)
    // Dùng hàm repository mà Thành đã sửa lỗi recordedAt
    List<WeightHeightHistory> bodyHistoryEntities = historyRepo.findByUserIdOrderByRecordedAtAsc(userId);

    // 2. Đổ vào list weightHistory trong DTO
    List<BodyHistoryDTO> weightHistoryDTOs = bodyHistoryEntities.stream().map(bh -> {
        BodyHistoryDTO bhDto = new BodyHistoryDTO();
        bhDto.setValue((float) bh.getWeight()); // Lấy cân nặng
        bhDto.setRecordedAt(bh.getRecordedAt());
        return bhDto;
    }).collect(Collectors.toList());
    dto.setWeightHistory(weightHistoryDTOs);

    // 3. Đổ vào list heightHistory trong DTO
    List<BodyHistoryDTO> heightHistoryDTOs = bodyHistoryEntities.stream().map(bh -> {
        BodyHistoryDTO bhDto = new BodyHistoryDTO();
        bhDto.setValue((float) bh.getHeight()); // Lấy chiều cao
        bhDto.setRecordedAt(bh.getRecordedAt());
        return bhDto;
    }).collect(Collectors.toList());
    dto.setHeightHistory(heightHistoryDTOs);

    return dto;
}
    /**
     * Đổi mật khẩu
     */
    @Transactional
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepo.findById(userId).orElse(null);

        if (user != null && passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepo.save(user);
            res.put("success", true);
            res.put("message", "Đổi mật khẩu thành công!");
        } else {
            res.put("success", false);
            res.put("message", "Mật khẩu cũ không chính xác!");
        }
        return res;
    }

    public Map<String, Object> getSettingsProfile(Long userId) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepo.findById(userId).orElse(null);
        UserGoal goal = goalRepo.findTopByUserIdOrderByIdDesc(userId).orElse(null);

        Map<String, Object> userData = new HashMap<>();
        Map<String, Object> goalData = new HashMap<>();

        if (user != null) {
            userData.put("fullName", user.getName());
            userData.put("email", user.getEmail());
            goalData.put("goalType", goal != null ? goal.getGoalType() : "Duy trì");
            goalData.put("targetCalories", goal != null ? goal.getTargetCalories() : 2000);
        }

        response.put("user", userData);
        response.put("goal", goalData);
        return response;
    }

    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    /**
     * Cập nhật chỉ số cơ thể hiện tại và lưu lịch sử
     */
    @Transactional
    public void updateWeightHeight(Long userId, float newWeight, float newHeight) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        user.setWeight(newWeight);
        user.setHeight(newHeight);
        userRepo.save(user);

        WeightHeightHistory history = new WeightHeightHistory();
        history.setUser(user);
        history.setWeight(newWeight);
        history.setHeight(newHeight);
        
        // Kiểm tra Entity WeightHeightHistory của Thành: 
        // Nếu biến là record_date thì sửa thành setRecord_date
        history.setRecordedAt(LocalDateTime.now()); 
        
        historyRepo.save(history);
    }

    /**
     * Hàm hỗ trợ cập nhật chỉ số từ Controller (Dùng DTO)
     */
    @Transactional
    public void updateBodyStats(Long userId, UpdateStatsRequest request) {
        updateWeightHeight(userId, request.getWeight(), request.getHeight());
    }

    private void updateUserDiseases(User user, List<Integer> diseaseIds) {
        userDiseaseRepo.deleteByUserId(user.getId());
        if (diseaseIds != null) {
            diseaseIds.forEach(id -> {
                UserDisease ud = new UserDisease();
                ud.setUser(user);
                diseaseRepo.findById(id).ifPresent(ud::setDisease);
                userDiseaseRepo.save(ud);
            });
        }
    }

    private void updateUserAllergies(User user, List<Integer> allergyIds) {
        userAllergyRepo.deleteByUserId(user.getId());
        if (allergyIds != null) {
            allergyIds.forEach(id -> {
                UserAllergy ua = new UserAllergy();
                ua.setUser(user);
                ingredientRepo.findById(id).ifPresent(ua::setIngredient);
                userAllergyRepo.save(ua);
            });
        }
    }

    private double estimateTargetCalories(UserDTO dto) {
        double bmr = "Nam".equalsIgnoreCase(dto.getGender()) 
            ? (10 * dto.getWeight() + 6.25 * dto.getHeight() - 5 * dto.getAge() + 5)
            : (10 * dto.getWeight() + 6.25 * dto.getHeight() - 5 * dto.getAge() - 161);
        double tdee = bmr * 1.4;
        if ("Giảm cân".equalsIgnoreCase(dto.getGoalType())) return Math.max(1200, tdee - 300);
        if ("Tăng cân".equalsIgnoreCase(dto.getGoalType())) return tdee + 300;
        return tdee;
    }

    @Transactional
    public Map<String, Object> updatePersonalSettings(Long userId, String fullName, String email) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepo.findById(userId).orElse(null);
        
        if (user != null) {
            user.setName(fullName);
            user.setEmail(email);
            userRepo.save(user);
            res.put("success", true);
            res.put("message", "Cập nhật thông tin thành công!");
        } else {
            res.put("success", false);
            res.put("message", "Không tìm thấy người dùng!");
        }
        return res;
    }

    @Transactional
    public Map<String, Object> updateNutritionGoal(Long userId, String goalType, Object targetCalories) {
        Map<String, Object> res = new HashMap<>();
        UserGoal goal = goalRepo.findTopByUserIdOrderByIdDesc(userId).orElse(new UserGoal());
        
        if (goal.getUser() == null) {
            User user = userRepo.findById(userId).orElse(null);
            goal.setUser(user);
        }
        
        goal.setGoalType(goalType);
        goal.setTargetCalories(Float.parseFloat(targetCalories.toString()));
        
        goalRepo.save(goal);

        res.put("success", true);
        res.put("message", "Cập nhật mục tiêu thành công!");
        return res;
    }

    public UserDTO getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getName());
        dto.setEmail(user.getEmail());

        return dto;
    }
}
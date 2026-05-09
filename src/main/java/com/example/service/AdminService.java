package com.example.service;

import com.example.repository.*;
import com.example.entity.*; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class AdminService {

    @Autowired private UserRepository userRepo;
    @Autowired private FoodRepository foodRepo;
    @Autowired private DailyMenuRepository dailyMenuRepo;
     @Autowired private UserLoginRepository loginRepo;
    @Autowired private UserFavoriteRepository userFavoriteRepo;
    @Autowired private UserGoalRepository userGoalRepo;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepo.count());
        stats.put("totalFoods", foodRepo.count());
        stats.put("totalMenus", dailyMenuRepo.count());
        
        stats.put("todayActivities", loginRepo.countByLoginDate(LocalDate.now()));
        return stats;
    }

    public void toggleUserStatus(Long userId, boolean activate) {
        userRepo.findById(userId).ifPresent(user -> {
            user.setIsActivate(activate ? 1 : 0); 
            userRepo.save(user);
        });
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        long totalUsers = userRepo.count();
        long totalFoods = foodRepo.count();
        long totalMenus = dailyMenuRepo.count();

        data.put("name", "Admin");
        data.put("totalUsers", totalUsers);
        data.put("totalFoods", totalFoods);
        data.put("totalMenus", totalMenus);
        data.put("todayActivities", loginRepo.countByLoginDate(LocalDate.now()));

        LocalDate today = LocalDate.now();
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = thisMonthStart.plusMonths(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);

        data.put("userGrowth", calcGrowthPercent(
                userRepo.countByCreateAtBetween(thisMonthStart, nextMonthStart.minusDays(1)),
                userRepo.countByCreateAtBetween(lastMonthStart, thisMonthStart.minusDays(1))));
        data.put("foodGrowth", calcGrowthPercent(
                foodRepo.countByCreateAtBetween(thisMonthStart, nextMonthStart.minusDays(1)),
                foodRepo.countByCreateAtBetween(lastMonthStart, thisMonthStart.minusDays(1))));
        data.put("menuGrowth", calcGrowthPercent(
                dailyMenuRepo.countByMenuDateBetween(thisMonthStart, nextMonthStart.minusDays(1)),
                dailyMenuRepo.countByMenuDateBetween(lastMonthStart, thisMonthStart.minusDays(1))));

        List<Integer> userChartData = new ArrayList<>(Collections.nCopies(12, 0));
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate yearEnd = yearStart.plusYears(1).minusDays(1);
        for (User user : userRepo.findByCreateAtBetween(yearStart, yearEnd)) {
            if (user.getCreateAt() == null) continue;
            int monthIndex = user.getCreateAt().getMonthValue() - 1;
            userChartData.set(monthIndex, userChartData.get(monthIndex) + 1);
        }
        data.put("userChartData", userChartData);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        Map<String, Long> loginStats = new LinkedHashMap<>();
        LocalDate tenDaysAgo = today.minusDays(9);
        for (int i = 9; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            loginStats.put(day.format(formatter), 0L);
        }
           Map<LocalDate, Set<Integer>> uniqueLoginsPerDay = new HashMap<>();
        for (UserLogin login : loginRepo.findByLoginDateBetween(tenDaysAgo, today)) {
            uniqueLoginsPerDay
                    .computeIfAbsent(login.getLoginDate(), k -> new HashSet<>())
                    .add(login.getUserId());
        }

        for (Map.Entry<LocalDate, Set<Integer>> entry : uniqueLoginsPerDay.entrySet()) {
            String dayLabel = entry.getKey().format(formatter);
            if (loginStats.containsKey(dayLabel)) {
                loginStats.put(dayLabel, (long) entry.getValue().size());
            }
        }
        data.put("chartLabels", new ArrayList<>(loginStats.keySet()));
        data.put("chartData", new ArrayList<>(loginStats.values()));

        LinkedHashMap<String, Integer> top3 = new LinkedHashMap<>();
        for (Object[] row : userFavoriteRepo.countTopFoods()) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            top3.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }
        data.put("topFoods", top3);

        Map<String, Double> popularGoals = new LinkedHashMap<>();
        List<Object[]> goals = userGoalRepo.countGoalsByType();
        double totalGoals = goals.stream().mapToDouble(r -> ((Number) r[1]).doubleValue()).sum();
        if (totalGoals > 0) {
            for (Object[] goal : goals) {
                if (goal[0] == null) {
                    continue;
                }
                String goalType = String.valueOf(goal[0]);
                double count = ((Number) goal[1]).doubleValue();
                popularGoals.put(goalType, (count * 100.0) / totalGoals);
            }
        }
        data.put("popularGoals", popularGoals);
        
        return data;
    }

    public Map<String, Object> getUsersPaged(int page, String keyword, String role) {
        Map<String, Object> result = new HashMap<>();
        result.put("users", userRepo.findAll()); 
        result.put("totalCount", userRepo.count());
        return result;
    }
 private double calcGrowthPercent(long thisMonth, long lastMonth) {
        if (lastMonth <= 0) {
            return thisMonth > 0 ? 100.0 : 0.0;
        }
        return ((double) (thisMonth - lastMonth) / lastMonth) * 100.0;
    }
}
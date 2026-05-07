package com.example.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
   public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return "redirect:/home";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    // User

    @GetMapping("/home")
    public String home() {
        return "views/home";
    }

    @GetMapping("/profile")
    public String profile() {
        return "views/profile";
    }

    @GetMapping("/foods")
    public String foodList() {
        return "views/food_list";
    }

    @GetMapping("/food_detail")
    public String foodDetail() {
        return "views/food_detail";
    }

    @GetMapping("/meal_plan")
    public String mealPlan() {
        return "views/meal_plan";
    }

    @GetMapping("/progress")
    public String progress() {
        return "views/progress";
    }

    @GetMapping("/favorites")
    public String favorites() {
        return "views/favorites";
    }

    @GetMapping("/search")
    public String search() {
        return "views/search";
    }

    @GetMapping("/customize_recipe")
    public String customizeRecipe() {
        return "views/customize_rec"; 
    }

    @GetMapping("/edit_food_user")
    public String editFoodUser() {
        return "views/edit_food_use"; 
    }

    @GetMapping("/settings")
    public String userSettings() {
        return "views/settings";
    }

    // admin

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/manage_food")
    public String adminManageFood() {
        return "admin/manage_food";
    }

    @GetMapping("/admin/add_food")
    public String adminAddFood() {
        return "admin/add_food";
    }

    @GetMapping("/admin/edit_food")
    public String adminEditFood() {
        return "admin/edit_food";
    }

    @GetMapping("/admin/manage_ingredient")
    public String adminManageIngredient() {
        return "admin/manage_ingredient"; 
    }

    @GetMapping("/admin/add_ingredient")
    public String adminAddIngredient() {
        return "admin/add_ingredient"; 
    }

    @GetMapping("/admin/edit_ingredient")
    public String adminEditIngredient() {
        return "admin/edit_ingredient"; 
    }

    @GetMapping("/admin/manage_disease")
    public String adminManageDisease() {
        return "admin/manage_disease"; 
    }

    @GetMapping("/admin/manage_users")
    public String adminManageUser() {
        return "admin/manage_users";
    }

    @GetMapping("/admin/settings")
    public String adminSettings() {
        return "admin/settings";
    }
}
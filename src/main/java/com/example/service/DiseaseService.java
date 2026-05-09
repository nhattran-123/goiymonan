package com.example.service;

import com.example.dto.DiseaseCompatibilityDTO;
import com.example.entity.Disease;
import com.example.entity.FoodDisease;
import com.example.repository.DiseaseRepository;
import com.example.repository.FoodDiseaseRepository;
import com.example.repository.FoodRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiseaseService {

    @Autowired private DiseaseRepository diseaseRepo;
    @Autowired private FoodDiseaseRepository foodDiseaseRepo;
    @Autowired private FoodRepository foodRepo;
     public Disease createDisease(Disease disease) {
        return diseaseRepo.save(disease);
    }

    public Disease updateDisease(Integer id, Disease payload) {
        Disease disease = diseaseRepo.findById(id).orElseThrow();
        disease.setDiseaseName(payload.getDiseaseName());
        disease.setDiseaseDescription(payload.getDiseaseDescription());
        disease.setDiseaseType(payload.getDiseaseType());
        return diseaseRepo.save(disease);
    }

    @Transactional
    public void deleteDisease(Integer id) {
        foodDiseaseRepo.deleteByDiseaseDiseaseId(id);
        diseaseRepo.deleteById(id);
    }

    @Transactional
    public ResponseEntity<?> addCompatibility(DiseaseCompatibilityDTO dto) {
        if (foodDiseaseRepo.existsByFoodFoodIdAndDiseaseDiseaseId(dto.getFoodId(), dto.getDiseaseId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Quy tắc tương thích đã tồn tại"));
    }
     FoodDisease fd = new FoodDisease();
        foodRepo.findById(dto.getFoodId()).ifPresent(fd::setFood);
        diseaseRepo.findById(dto.getDiseaseId()).ifPresent(fd::setDisease);
        fd.setRating(dto.getRating());
        foodDiseaseRepo.save(fd);
        return ResponseEntity.ok(Map.of("success", true));
    }

    public void deleteCompatibility(Integer id) {
        foodDiseaseRepo.deleteById(id);
    }
    public List<Disease> getAllDiseases() {
        return diseaseRepo.findAll();
    }
    public List<Map<String, Object>> getAllDiseasesForAdmin() {
        Map<Integer, Long> foodCountByDiseaseId = foodDiseaseRepo.findAll().stream()
                .filter(fd -> fd.getDisease() != null
                        && fd.getDisease().getDiseaseId() != null
                        && fd.getRating() != null
                        && fd.getRating() >= 3)
                .collect(Collectors.groupingBy(
                        fd -> fd.getDisease().getDiseaseId(),
                        Collectors.counting()
                ));

        return diseaseRepo.findAll().stream().map(disease -> {
            long foodCount = foodCountByDiseaseId.getOrDefault(disease.getDiseaseId(), 0L);

            return Map.<String, Object>of(
                    "diseaseId", disease.getDiseaseId(),
                    "diseaseName", Optional.ofNullable(disease.getDiseaseName()).orElse(""),
                    "diseaseDescription", Optional.ofNullable(disease.getDiseaseDescription()).orElse(""),
                    "foodCount", foodCount
            );
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllCompatibility() {
       return foodDiseaseRepo.findAll().stream().map(item -> Map.<String, Object>of(
    "id", item.getId(),
    "foodName", Optional.ofNullable(item.getFood()).map(f -> f.getFoodName()).orElse(""),
    "diseaseName", Optional.ofNullable(item.getDisease()).map(Disease::getDiseaseName).orElse(""),
    "rating", item.getRating() == null ? 0 : item.getRating()
)).collect(Collectors.toList());
    }
}

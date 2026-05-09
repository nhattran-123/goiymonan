package com.example.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "disease")
public class Disease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Disease_id" )
    private Integer diseaseId;

    @Column(name="Disease_name")
    private String diseaseName;

    @Column(name="disease_description")
    private String diseaseDescription;

    @Column(name="disease_type")
    private String diseaseType;
}

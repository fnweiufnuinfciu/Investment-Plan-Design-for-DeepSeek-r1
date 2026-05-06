package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "report_date", nullable = false)
    private java.time.LocalDate reportDate;

    @Column(name = "report_text", nullable = false, columnDefinition = "TEXT")
    private String reportText;

    @Column(length = 100)
    private String source;

    @Column(length = 50)
    private String sector;

    @Column(name = "analyst_recommendation", length = 20)
    private String analystRecommendation;

    @Column(name = "objective_ratio")
    private Double objectiveRatio = 0.5;

    @Column(name = "subjective_ratio")
    private Double subjectiveRatio = 0.5;

    @Column(name = "volatility_20d")
    private Double volatility20d = 0.3;

    @Column(name = "future_ar_60d")
    private Double futureAr60d;

    @Column(name = "daily_returns_60d", columnDefinition = "TEXT")
    private String dailyReturns60d;

    @Column(name = "quality_flags")
    private String qualityFlags;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}

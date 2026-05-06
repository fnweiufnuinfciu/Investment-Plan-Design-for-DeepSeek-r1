package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analysis_history")
public class AnalysisHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(length = 20)
    private String recommendation;

    @Column(name = "recommendation_score")
    private Integer recommendationScore;

    private Double confidence;
    @Column(name = "objective_ratio")
    private Double objectiveRatio;
    @Column(name = "subjective_ratio")
    private Double subjectiveRatio;
    @Column(name = "volatility_20d")
    private Double volatility20d;
    @Column(name = "signal_score")
    private Double signalScore;
    @Column(name = "target_weight")
    private Double targetWeight;
    @Column(length = 10)
    private String side;
    @Column(name = "plan_id", length = 50)
    private String planId;
    @Column(name = "input_source", length = 20)
    private String inputSource;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

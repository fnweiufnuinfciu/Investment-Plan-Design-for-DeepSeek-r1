package com.investment.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportRequest {
    @NotBlank
    private String ticker;
    private LocalDate reportDate;
    private String reportText;
    private String source;
    private String sector;
    private String analystRecommendation;
    private Double confidence;
    private Double objectiveRatio;
    private Double subjectiveRatio;
    private Double volatility20d;
    private Double futureAr60d;
    private double[] dailyReturns60d;
}

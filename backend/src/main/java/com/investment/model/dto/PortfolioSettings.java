package com.investment.model.dto;

import lombok.Data;

@Data
public class PortfolioSettings {
    private Double capital = 1_000_000.0;
    private String mode = "long_short";
    private Integer maxPositions = 12;
    private Double maxPositionWeight = 0.15;
    private Double minConfidence = 0.55;
    private Double minObjectiveRatio = 0.45;
    private Integer holdDays = 60;
    private Integer rebalanceDays = 20;
    private Double stopLossPct = 0.08;
    private Double takeProfitPct = 0.20;
}

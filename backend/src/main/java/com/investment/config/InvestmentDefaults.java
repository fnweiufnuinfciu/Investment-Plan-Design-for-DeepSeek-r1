package com.investment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "investment.defaults")
public class InvestmentDefaults {
    private double capital = 1_000_000;
    private String mode = "long_short";
    private int maxPositions = 12;
    private double maxPositionWeight = 0.15;
    private double minConfidence = 0.55;
    private double minObjectiveRatio = 0.45;
    private int holdDays = 60;
    private int rebalanceDays = 20;
    private double stopLossPct = 0.08;
    private double takeProfitPct = 0.20;
}

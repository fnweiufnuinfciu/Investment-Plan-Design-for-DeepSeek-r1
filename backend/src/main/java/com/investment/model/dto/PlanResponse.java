package com.investment.model.dto;

import com.investment.model.entity.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PlanResponse {
    private PlanMeta meta;
    private PlanSummary summary;
    private RiskRules riskRules;
    private List<String> executionChecklist;
    private List<PositionDTO> positions;
    private List<DroppedDTO> droppedSamples;
    private String markdown;

    // Analytics
    private DataQualityDTO dataQuality;
    private BacktestDTO backtest;
    private StressTestDTO stressTest;
    private SensitivityDTO sensitivity;
    private ExplainabilityDTO explainability;

    private DiagnosticsDTO diagnostics;

    @Data
    public static class PlanMeta {
        private String generatedAt;
        private String inputFile;
        private PortfolioSettings settings;
        private String methodology;
        private Map<String, Object> apiSummary;
    }

    @Data
    public static class PlanSummary {
        private int universeSize;
        private int selectedPositions;
        private int longPositions;
        private int shortPositions;
        private double grossExposure;
        private double netExposure;
        private double capital;
        private int filteredOut;
        private List<DroppedBreakdown> filteredBreakdown;
    }

    @Data
    public static class DroppedBreakdown {
        private String reason;
        private int count;
    }

    @Data
    public static class RiskRules {
        private int holdDays;
        private int rebalanceDays;
        private double stopLossPct;
        private double takeProfitPct;
        private double maxPositionWeight;
    }

    @Data
    public static class PositionDTO {
        private String ticker;
        private String reportDate;
        private String side;
        private String recommendation;
        private int recommendationScore;
        private double confidence;
        private double objectiveRatio;
        private double subjectiveRatio;
        private double subjectiveWeight;
        private double volatility20d;
        private double signalScore;
        private double qualityScore;
        private double targetWeight;
        private double targetDollar;
        private int holdDays;
        private double stopLossPct;
        private double takeProfitPct;
        private List<String> notes;
    }

    @Data
    public static class DroppedDTO {
        private String ticker;
        private String reason;
    }

    @Data
    public static class DataQualityDTO {
        private int totalRecords;
        private int missingRecommendation;
        private int duplicateTickerDate;
        private double qualityScore;
        private Map<String, Integer> recommendationDistribution;
        private StatsDTO confidenceStats;
        private StatsDTO objectiveRatioStats;
        private List<String> warnings;
    }

    @Data
    public static class StatsDTO {
        private int count;
        private Double mean;
        private Double std;
        private Double min;
        private Double max;
    }

    @Data
    public static class BacktestDTO {
        private Map<String, Object> assumptions;
        private Map<String, Integer> sourceBreakdown;
        private MetricsDTO metrics;
        private TradeStatsDTO tradeStats;
        private List<Double> cumulativeCurve;
        private List<Double> expectedCurve;
    }

    @Data
    public static class MetricsDTO {
        private int days;
        private Double cumulativeReturn;
        private Double annualizedReturn;
        private Double annualizedVolatility;
        private Double sharpe;
        private Double sortino;
        private Double maxDrawdown;
        private Double dailyWinRate;
        private Double var95;
        private Double cvar95;
    }

    @Data
    public static class TradeStatsDTO {
        private int positionCount;
        private int winnerCount;
        private int loserCount;
        private Double winRate;
        private Double avgPositionReturn;
        private Double avgHoldingDays;
    }

    @Data
    public static class StressTestDTO {
        private List<StressScenarioDTO> scenarios;
        private StressScenarioDTO worstCase;
    }

    @Data
    public static class StressScenarioDTO {
        private String scenario;
        private Double assumedMarketMove;
        private Double portfolioPnl;
        private Double portfolioReturn;
    }

    @Data
    public static class SensitivityDTO {
        private List<SensitivityGridDTO> grid;
        private SensitivityGridDTO bestConfiguration;
    }

    @Data
    public static class SensitivityGridDTO {
        private double minConfidence;
        private double minObjectiveRatio;
        private int selectedPositions;
        private int longPositions;
        private int shortPositions;
        private double grossExposure;
        private Double expectedReturn60d;
    }

    @Data
    public static class ExplainabilityDTO {
        private Map<String, Double> sideExposure;
        private List<ContributorDTO> topSignalContributors;
        private StatsDTO signalStats;
    }

    @Data
    public static class ContributorDTO {
        private String ticker;
        private String side;
        private double targetWeight;
        private double signalScore;
        private double contributionScore;
        private double confidence;
        private double objectiveRatio;
    }

    @Data
    public static class DiagnosticsDTO {
        private int inputRecords;
        private int candidateCount;
        private int positionCount;
        private Double qualityScore;
        private int apiErrors;
        private long totalApiLatencyMs;
    }
}

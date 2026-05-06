package com.investment.service;

import com.investment.model.dto.PlanResponse;
import com.investment.model.dto.PortfolioSettings;
import com.investment.util.SignalCalculator;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Strategy comparison: long_short vs long_only vs equal_weight benchmark.
 */
@Service
public class StrategyComparisonService {

    public record ComparisonResult(
            String strategyName,
            String strategyLabel,
            int positionCount,
            int longCount,
            int shortCount,
            double grossExposure,
            double netExposure,
            double expectedReturn60d,
            double annualizedReturn,
            double sharpe,
            double maxDrawdown,
            double dailyWinRate,
            List<String> topTickers,
            Map<String, Double> weightMap
    ) {}

    public record StrategyComparison(
            List<ComparisonResult> results,
            String bestStrategy,
            String bestReason
    ) {}

    /**
     * Compare strategies using the same candidate pool.
     */
    public StrategyComparison compare(List<PlanResponse> plans) {
        List<ComparisonResult> results = new ArrayList<>();

        for (PlanResponse plan : plans) {
            String mode = plan.getMeta().getSettings().getMode();
            results.add(buildResult(mode, plan));
        }

        // Pick best by expected return
        ComparisonResult best = results.stream()
                .max(Comparator.comparingDouble(ComparisonResult::expectedReturn60d))
                .orElse(results.get(0));

        return new StrategyComparison(results, best.strategyLabel,
                "预期收益最高 (" + String.format("%.2f%%", best.expectedReturn60d * 100) + ")");
    }

    private ComparisonResult buildResult(String mode, PlanResponse plan) {
        var positions = plan.getPositions();
        int longCount = (int) positions.stream().filter(p -> p.getTargetWeight() > 0).count();
        int shortCount = (int) positions.stream().filter(p -> p.getTargetWeight() < 0).count();
        double gross = positions.stream().mapToDouble(p -> Math.abs(p.getTargetWeight())).sum();
        double net = positions.stream().mapToDouble(PlanResponse.PositionDTO::getTargetWeight).sum();
        double expRet = positions.stream()
                .mapToDouble(p -> Math.abs(p.getSignalScore()) * 0.03).sum();

        var backtest = plan.getBacktest();
        var metrics = backtest != null ? backtest.getMetrics() : null;

        List<String> topTickers = positions.stream()
                .sorted(Comparator.comparingDouble((PlanResponse.PositionDTO p) ->
                        Math.abs(p.getSignalScore())).reversed())
                .limit(5)
                .map(p -> p.getTicker() + "(" + p.getSide() + ")")
                .toList();

        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (var p : positions) {
            weightMap.put(p.getTicker(), p.getTargetWeight());
        }

        String label = "long_short".equals(mode) ? "多空组合" :
                       "long_only".equals(mode) ? "仅做多" : mode;

        return new ComparisonResult(
                mode, label, positions.size(), longCount, shortCount,
                gross, net, expRet,
                metrics != null ? metrics.getAnnualizedReturn() : 0,
                metrics != null && metrics.getSharpe() != null ? metrics.getSharpe() : 0,
                metrics != null ? metrics.getMaxDrawdown() : 0,
                metrics != null ? metrics.getDailyWinRate() : 0,
                topTickers, weightMap
        );
    }
}

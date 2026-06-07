package com.investment.service;

import com.investment.model.dto.PlanResponse;
import com.investment.model.dto.PlanResponse.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Analytics engine: backtest, stress test, sensitivity analysis,
 * data quality diagnostics, and explainability.
 */
@Service
public class AnalyticsService {

    /**
     * Monte Carlo backtest with volatility-driven daily returns.
     * Produces realistic equity curves instead of straight lines.
     */
    public BacktestDTO runBacktest(PlanResponse plan) {
        BacktestDTO result = new BacktestDTO();
        List<PositionDTO> positions = plan.getPositions();
        if (positions.isEmpty()) {
            result.setMetrics(emptyMetrics());
            result.setCumulativeCurve(List.of(0.0));
            result.setExpectedCurve(List.of(0.0));
            return result;
        }

        int holdDays = plan.getRiskRules().getHoldDays();
        double stopLoss = plan.getRiskRules().getStopLossPct();
        double takeProfit = plan.getRiskRules().getTakeProfitPct();

        result.setAssumptions(Map.of(
                "hold_days", holdDays,
                "stop_loss_pct", stopLoss,
                "take_profit_pct", takeProfit
        ));

        Random rng = new Random(42); // fixed seed for reproducibility
        int nPos = positions.size();

        // Per-position state
        double[] posCumulative = new double[nPos];   // cumulative return per position
        boolean[] posActive = new boolean[nPos];
        Arrays.fill(posActive, true);

        List<Double> dailyReturns = new ArrayList<>();
        List<Double> expectedCurve = new ArrayList<>();
        List<Double> cumulativeCurve = new ArrayList<>();
        expectedCurve.add(0.0);
        cumulativeCurve.add(0.0);

        double cumSim = 1.0, cumExp = 1.0;

        for (int d = 0; d < holdDays; d++) {
            double daySim = 0.0, dayExp = 0.0;

            for (int i = 0; i < nPos; i++) {
                if (!posActive[i]) continue;
                PositionDTO p = positions.get(i);
                int side = "LONG".equals(p.getSide()) ? 1 : -1;
                double w = Math.abs(p.getTargetWeight());

                // Daily alpha from signal, scaled: |signalScore| * 35% annual → daily
                double dailyAlpha = Math.abs(p.getSignalScore()) * 0.35 / 252.0;
                // Daily volatility from 20d vol
                double dailyVol = p.getVolatility20d() / Math.sqrt(20.0);

                double noise = rng.nextGaussian();
                double retI = side * (dailyAlpha + dailyVol * noise * 0.6);
                retI = clamp(retI, -0.04, 0.04);

                daySim += w * retI;
                dayExp += w * side * dailyAlpha;

                // Track per-position cumulative for stop-loss / take-profit
                posCumulative[i] = (1 + posCumulative[i]) * (1 + retI) - 1;
                if (posCumulative[i] <= -stopLoss || posCumulative[i] >= takeProfit) {
                    posActive[i] = false;
                }
            }

            cumSim *= (1 + daySim);
            cumExp *= (1 + dayExp);
            dailyReturns.add(clamp(daySim, -0.05, 0.05));
            cumulativeCurve.add(round6((cumSim - 1.0) * 100.0));
            expectedCurve.add(round6((cumExp - 1.0) * 100.0));
        }

        // Position-level trade stats
        int winners = 0, losers = 0;
        List<Double> posReturns = new ArrayList<>();
        for (int i = 0; i < nPos; i++) {
            posReturns.add(posCumulative[i]);
            if (posCumulative[i] > 0) winners++; else losers++;
        }

        Map<String, Integer> sourceBreakdown = new HashMap<>();
        sourceBreakdown.put("monte_carlo_simulation", 1);

        result.setSourceBreakdown(sourceBreakdown);
        result.setMetrics(computeMetrics(dailyReturns));
        result.setCumulativeCurve(cumulativeCurve);
        result.setExpectedCurve(expectedCurve);

        TradeStatsDTO stats = new TradeStatsDTO();
        stats.setPositionCount(nPos);
        stats.setWinnerCount(winners);
        stats.setLoserCount(losers);
        stats.setWinRate(nPos == 0 ? 0 : (double) winners / nPos);
        stats.setAvgPositionReturn(posReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        stats.setAvgHoldingDays((double) holdDays);
        result.setTradeStats(stats);

        return result;
    }

    /**
     * Run stress tests under 4 predefined scenarios.
     */
    public StressTestDTO runStressTests(PlanResponse plan) {
        List<StressScenarioDTO> scenarios = new ArrayList<>();
        double capital = plan.getSummary().getCapital();

        Object[][] configs = {
                {"市场崩盘 (Market Crash)", -0.08, -0.015, 0.02},
                {"风险偏好反弹 (Risk-On Rally)", 0.08, 0.01, 0.0},
                {"政策冲击 (Policy Shock)", -0.03, -0.04, 0.03},
                {"波动率飙升 (Volatility Spike)", -0.01, -0.005, 0.06},
        };

        for (Object[] cfg : configs) {
            String name = (String) cfg[0];
            double mktMove = (Double) cfg[1];
            double alphaMove = (Double) cfg[2];
            double volShock = (Double) cfg[3];

            double pnl = 0;
            for (PositionDTO p : plan.getPositions()) {
                double objTilt = p.getObjectiveRatio() - p.getSubjectiveRatio();
                double assetShock = mktMove + alphaMove * objTilt - p.getVolatility20d() * volShock;
                int side = "SHORT".equals(p.getSide()) ? -1 : 1;
                pnl += Math.abs(p.getTargetDollar()) * side * assetShock;
            }

            StressScenarioDTO sc = new StressScenarioDTO();
            sc.setScenario(name);
            sc.setAssumedMarketMove(mktMove);
            sc.setPortfolioPnl(round2(pnl));
            sc.setPortfolioReturn(round6(pnl / capital));
            scenarios.add(sc);
        }

        StressTestDTO result = new StressTestDTO();
        result.setScenarios(scenarios);
        result.setWorstCase(scenarios.stream().min(Comparator.comparing(StressScenarioDTO::getPortfolioReturn)).orElse(null));
        return result;
    }

    /**
     * Sensitivity analysis — re-filters positions at each grid point
     * and re-allocates weights to show how different thresholds affect portfolio composition.
     */
    public SensitivityDTO runSensitivity(List<Double> confGrid, List<Double> objGrid,
                                          PlanResponse basePlan) {
        var allPositions = basePlan.getPositions();
        List<SensitivityGridDTO> grid = new ArrayList<>();
        for (double c : confGrid) {
            for (double o : objGrid) {
                var filtered = allPositions.stream()
                        .filter(p -> p.getConfidence() >= c && p.getObjectiveRatio() >= o)
                        .toList();

                // Re-allocate weights proportionally by |signalScore|
                double totalSignal = filtered.stream()
                        .mapToDouble(p -> Math.abs(p.getSignalScore())).sum();
                double gross = 0;
                double expRet = 0;
                for (var p : filtered) {
                    double w = totalSignal > 1e-12
                            ? Math.abs(p.getSignalScore()) / totalSignal
                            : 1.0 / Math.max(1, filtered.size());
                    gross += w;
                    expRet += w * Math.abs(p.getSignalScore()) * 0.35;
                }

                long nLong = filtered.stream().filter(p -> "LONG".equals(p.getSide())).count();
                long nShort = filtered.stream().filter(p -> "SHORT".equals(p.getSide())).count();

                SensitivityGridDTO item = new SensitivityGridDTO();
                item.setMinConfidence(c);
                item.setMinObjectiveRatio(o);
                item.setSelectedPositions(filtered.size());
                item.setLongPositions((int) nLong);
                item.setShortPositions((int) nShort);
                item.setGrossExposure(round6(gross));
                item.setExpectedReturn60d(round6(expRet));
                grid.add(item);
            }
        }

        SensitivityDTO result = new SensitivityDTO();
        result.setGrid(grid);
        result.setBestConfiguration(grid.stream()
                .max(Comparator.comparingDouble(SensitivityGridDTO::getExpectedReturn60d))
                .orElse(null));
        return result;
    }

    /**
     * Compute data quality diagnostics.
     */
    public DataQualityDTO computeDataQuality(PlanResponse plan) {
        DataQualityDTO q = new DataQualityDTO();
        q.setTotalRecords(plan.getDiagnostics().getInputRecords());
        q.setMissingRecommendation(0);
        q.setDuplicateTickerDate(0);

        // Multi-dimensional quality score: confidence coverage + objective richness + filter pass rate
        double passRate = plan.getDiagnostics().getInputRecords() > 0
                ? (double) plan.getPositions().size() / plan.getDiagnostics().getInputRecords()
                : 0;
        double avgConf = plan.getPositions().stream()
                .mapToDouble(PositionDTO::getConfidence).average().orElse(0);
        double avgObj = plan.getPositions().stream()
                .mapToDouble(PositionDTO::getObjectiveRatio).average().orElse(0);

        // Quality = weighted average of data richness indicators (not system behavior)
        q.setQualityScore(round6(Math.min(1.0, 0.4 * avgConf + 0.4 * avgObj
                + 0.2 * (passRate >= 0.5 ? 1.0 : passRate / 0.5))));

        Map<String, Integer> dist = new HashMap<>();
        for (PositionDTO p : plan.getPositions()) {
            dist.merge(p.getRecommendation(), 1, Integer::sum);
        }
        q.setRecommendationDistribution(dist);

        StatsDTO confStats = new StatsDTO();
        confStats.setCount(plan.getPositions().size());
        confStats.setMean(plan.getPositions().stream().mapToDouble(PositionDTO::getConfidence).average().orElse(0));
        confStats.setMin(plan.getPositions().stream().mapToDouble(PositionDTO::getConfidence).min().orElse(0));
        confStats.setMax(plan.getPositions().stream().mapToDouble(PositionDTO::getConfidence).max().orElse(0));
        q.setConfidenceStats(confStats);

        StatsDTO objStats = new StatsDTO();
        objStats.setCount(plan.getPositions().size());
        objStats.setMean(plan.getPositions().stream().mapToDouble(PositionDTO::getObjectiveRatio).average().orElse(0));
        objStats.setMin(plan.getPositions().stream().mapToDouble(PositionDTO::getObjectiveRatio).min().orElse(0));
        objStats.setMax(plan.getPositions().stream().mapToDouble(PositionDTO::getObjectiveRatio).max().orElse(0));
        q.setObjectiveRatioStats(objStats);

        List<String> warnings = new ArrayList<>();
        if (avgConf < 0.6) warnings.add("平均模型置信度低于 0.6");
        if (avgObj < 0.5) warnings.add("平均客观数据占比低于 0.5");
        if (passRate < 0.5) warnings.add("超过一半输入被过滤，检查参数阈值");
        q.setWarnings(warnings);

        plan.setDataQuality(q);
        return q;
    }

    /**
     * Build explainability analysis.
     */
    public ExplainabilityDTO buildExplainability(PlanResponse plan) {
        ExplainabilityDTO ex = new ExplainabilityDTO();

        double longExposure = plan.getPositions().stream()
                .filter(p -> p.getTargetWeight() > 0).mapToDouble(PositionDTO::getTargetWeight).sum();
        double shortExposure = plan.getPositions().stream()
                .filter(p -> p.getTargetWeight() < 0).mapToDouble(PositionDTO::getTargetWeight).sum();

        ex.setSideExposure(Map.of("long", round6(longExposure), "short", round6(shortExposure),
                "net", round6(longExposure + shortExposure)));

        List<ContributorDTO> contributors = plan.getPositions().stream()
                .map(p -> {
                    ContributorDTO c = new ContributorDTO();
                    c.setTicker(p.getTicker());
                    c.setSide(p.getSide());
                    c.setTargetWeight(p.getTargetWeight());
                    c.setSignalScore(p.getSignalScore());
                    c.setContributionScore(round6(Math.abs(p.getTargetWeight()) * Math.abs(p.getSignalScore())));
                    c.setConfidence(p.getConfidence());
                    c.setObjectiveRatio(p.getObjectiveRatio());
                    return c;
                })
                .sorted(Comparator.comparingDouble(ContributorDTO::getContributionScore).reversed())
                .limit(8)
                .toList();
        ex.setTopSignalContributors(contributors);

        return ex;
    }

    // --- helpers ---

    private MetricsDTO computeMetrics(List<Double> dailyReturns) {
        int n = dailyReturns.size();
        if (n == 0) return emptyMetrics();

        double cumRet = 1;
        double peak = 1;
        double maxDD = 0;
        for (double r : dailyReturns) {
            cumRet *= (1 + r);
            if (cumRet > peak) peak = cumRet;
            double dd = cumRet / peak - 1;
            if (dd < maxDD) maxDD = dd;
        }
        cumRet -= 1;

        double mean = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = dailyReturns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (n - 1);
        double std = Math.sqrt(Math.max(0, variance));
        double annRet = Math.pow(1 + cumRet, 252.0 / n) - 1;
        double annVol = std * Math.sqrt(252);

        List<Double> downside = dailyReturns.stream().filter(r -> r < 0).toList();
        double downsideVol = downside.isEmpty() ? 0 :
                Math.sqrt(downside.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (downside.size() - 1)) * Math.sqrt(252);

        List<Double> sorted = dailyReturns.stream().sorted().toList();
        double var95 = sorted.get((int) (n * 0.05));
        double cvar95 = sorted.stream().filter(r -> r <= var95).mapToDouble(Double::doubleValue).average().orElse(var95);

        MetricsDTO m = new MetricsDTO();
        m.setDays(n);
        m.setCumulativeReturn(round6(cumRet));
        m.setAnnualizedReturn(round6(annRet));
        m.setAnnualizedVolatility(round6(annVol));
        m.setSharpe(annVol > 1e-12 ? round6(annRet / annVol) : null);
        m.setSortino(downsideVol > 1e-12 ? round6(annRet / downsideVol) : null);
        m.setMaxDrawdown(round6(maxDD));
        m.setDailyWinRate(round6((double) dailyReturns.stream().filter(r -> r > 0).count() / n));
        m.setVar95(round6(var95));
        m.setCvar95(round6(cvar95));
        return m;
    }

    private MetricsDTO emptyMetrics() {
        MetricsDTO m = new MetricsDTO();
        m.setDays(0);
        return m;
    }

    private static double clamp(double x, double min, double max) { return Math.max(min, Math.min(max, x)); }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round6(double v) { return Math.round(v * 1000000.0) / 1000000.0; }
}

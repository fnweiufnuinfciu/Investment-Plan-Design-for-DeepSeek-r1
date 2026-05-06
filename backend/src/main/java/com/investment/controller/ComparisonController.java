package com.investment.controller;

import com.investment.model.dto.*;
import com.investment.service.AnalyticsService;
import com.investment.service.PortfolioService;
import com.investment.service.StrategyComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ComparisonController {

    private final PortfolioService portfolioService;
    private final AnalyticsService analyticsService;
    private final StrategyComparisonService comparisonService;

    public ComparisonController(PortfolioService portfolioService, AnalyticsService analyticsService,
                                StrategyComparisonService comparisonService) {
        this.portfolioService = portfolioService;
        this.analyticsService = analyticsService;
        this.comparisonService = comparisonService;
    }

    @PostMapping("/compare-strategies")
    public ResponseEntity<?> compareStrategies(@RequestBody PlanRequest request) {
        List<PlanResponse> plans = new ArrayList<>();

        // Long/Short mode
        PortfolioSettings lsSettings = copySettings(request.getSettings());
        lsSettings.setMode("long_short");
        PlanResponse lsPlan = portfolioService.generatePlanFromPrecomputed(
                request.getRecords(), lsSettings);
        attachAnalytics(lsPlan);
        plans.add(lsPlan);

        // Long Only mode
        PortfolioSettings loSettings = copySettings(request.getSettings());
        loSettings.setMode("long_only");
        PlanResponse loPlan = portfolioService.generatePlanFromPrecomputed(
                request.getRecords(), loSettings);
        attachAnalytics(loPlan);
        plans.add(loPlan);

        var comparison = comparisonService.compare(plans);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategies", comparison.results());
        result.put("best", comparison.bestStrategy());
        result.put("reason", comparison.bestReason());
        // Attach full positions for each strategy so frontend can render charts
        Map<String, List<PlanResponse.PositionDTO>> positions = new LinkedHashMap<>();
        positions.put("long_short", lsPlan.getPositions());
        positions.put("long_only", loPlan.getPositions());
        result.put("positions", positions);
        return ResponseEntity.ok(result);
    }

    private void attachAnalytics(PlanResponse plan) {
        plan.setBacktest(analyticsService.runBacktest(plan));
        plan.setStressTest(analyticsService.runStressTests(plan));
    }

    private PortfolioSettings copySettings(PortfolioSettings s) {
        PortfolioSettings ns = new PortfolioSettings();
        ns.setCapital(s.getCapital());
        ns.setMode(s.getMode());
        ns.setMaxPositions(s.getMaxPositions());
        ns.setMaxPositionWeight(s.getMaxPositionWeight());
        ns.setMinConfidence(s.getMinConfidence());
        ns.setMinObjectiveRatio(s.getMinObjectiveRatio());
        ns.setHoldDays(s.getHoldDays());
        ns.setRebalanceDays(s.getRebalanceDays());
        ns.setStopLossPct(s.getStopLossPct());
        ns.setTakeProfitPct(s.getTakeProfitPct());
        return ns;
    }
}

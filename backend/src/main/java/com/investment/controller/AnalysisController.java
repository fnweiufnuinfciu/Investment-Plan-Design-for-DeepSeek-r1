package com.investment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.config.DeepSeekConfig;
import com.investment.config.InvestmentDefaults;
import com.investment.model.dto.*;
import com.investment.service.AnalyticsService;
import com.investment.service.DeepSeekService;
import com.investment.service.PortfolioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);
    private final PortfolioService portfolioService;
    private final AnalyticsService analyticsService;
    private final DeepSeekService deepSeekService;
    private final DeepSeekConfig deepSeekConfig;
    private final InvestmentDefaults defaults;
    private final ObjectMapper mapper;

    public AnalysisController(PortfolioService portfolioService, AnalyticsService analyticsService,
                              DeepSeekService deepSeekService, DeepSeekConfig deepSeekConfig,
                              InvestmentDefaults defaults, ObjectMapper mapper) {
        this.portfolioService = portfolioService;
        this.analyticsService = analyticsService;
        this.deepSeekService = deepSeekService;
        this.deepSeekConfig = deepSeekConfig;
        this.defaults = defaults;
        this.mapper = mapper;
    }

    // ── Health / Config ──

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean hasKey = (System.getenv("DEEPSEEK_API_KEY") != null)
                || (deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isBlank());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "model", deepSeekConfig.getModel(),
                "api_configured", hasKey
        ));
    }

    @GetMapping("/defaults")
    public ResponseEntity<Map<String, Object>> getDefaults() {
        return ResponseEntity.ok(Map.of("defaults", defaults));
    }

    // ── Report Analysis ──

    @PostMapping("/analyze-report")
    public ResponseEntity<?> analyzeReport(@Valid @RequestBody ReportRequest request) {
        if (request.getReportText() == null || request.getReportText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "report_text is required"));
        }
        Map<String, Object> result = deepSeekService.analyzeReport(
                request.getReportText(),
                request.getTicker(),
                request.getReportDate() != null ? request.getReportDate().toString() : null);
        return ResponseEntity.ok(result);
    }

    // ── Plan from Pre-computed Data (original flow) ──

    @PostMapping("/plan")
    public ResponseEntity<?> generatePlan(@Valid @RequestBody PlanRequest request) {
        try {
            PlanResponse response = portfolioService.generatePlanFromPrecomputed(
                    request.getRecords(), request.getSettings());

            response.setDataQuality(analyticsService.computeDataQuality(response));
            response.setBacktest(analyticsService.runBacktest(response));
            response.setStressTest(analyticsService.runStressTests(response));

            double baseConf = defaults.getMinConfidence();
            double baseObj = defaults.getMinObjectiveRatio();
            response.setSensitivity(analyticsService.runSensitivity(
                    List.of(clamp(baseConf - 0.1, 0, 1), baseConf, clamp(baseConf + 0.1, 0, 1)),
                    List.of(clamp(baseObj - 0.1, 0, 1), baseObj, clamp(baseObj + 0.1, 0, 1)),
                    response));

            response.setExplainability(analyticsService.buildExplainability(response));

            if (response.getDiagnostics() != null) {
                response.getDiagnostics().setQualityScore(
                        response.getDataQuality() != null ? response.getDataQuality().getQualityScore() : null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Plan generation failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Plan from Raw Texts (DeepSeek API flow) ──

    @PostMapping("/plan-from-texts")
    public ResponseEntity<?> generatePlanFromTexts(@Valid @RequestBody PlanRequest request) {
        try {
            PlanResponse response = portfolioService.generatePlanFromTexts(
                    request.getRecords(), request.getSettings());

            response.setDataQuality(analyticsService.computeDataQuality(response));
            response.setBacktest(analyticsService.runBacktest(response));
            response.setStressTest(analyticsService.runStressTests(response));

            double baseConf = defaults.getMinConfidence();
            double baseObj = defaults.getMinObjectiveRatio();
            response.setSensitivity(analyticsService.runSensitivity(
                    List.of(clamp(baseConf - 0.1, 0, 1), baseConf, clamp(baseConf + 0.1, 0, 1)),
                    List.of(clamp(baseObj - 0.1, 0, 1), baseObj, clamp(baseObj + 0.1, 0, 1)),
                    response));

            response.setExplainability(analyticsService.buildExplainability(response));

            if (response.getDiagnostics() != null) {
                response.getDiagnostics().setQualityScore(
                        response.getDataQuality() != null ? response.getDataQuality().getQualityScore() : null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Plan generation from texts failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }
}

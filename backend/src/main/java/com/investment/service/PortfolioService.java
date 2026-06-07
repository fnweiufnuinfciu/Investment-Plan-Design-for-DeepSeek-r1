package com.investment.service;

import com.investment.config.InvestmentDefaults;
import com.investment.model.dto.*;
import com.investment.model.enums.PortfolioMode;
import com.investment.model.enums.Recommendation;
import com.investment.util.SignalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private final DeepSeekService deepSeekService;
    private final FinBertService finBertService;
    private final InvestmentDefaults defaults;

    public PortfolioService(DeepSeekService deepSeekService,
                            FinBertService finBertService,
                            InvestmentDefaults defaults) {
        this.deepSeekService = deepSeekService;
        this.finBertService = finBertService;
        this.defaults = defaults;
    }

    /**
     * Internal record for a normalized report ready for signal computation.
     */
    record NormalizedRecord(
            String ticker, String reportDate, int recommendationScore, String recommendationLabel,
            double confidence, double objectiveRatio, double subjectiveRatio, double subjectiveWeight,
            double volatility20d, Double futureAr60d, double[] dailyReturns60d,
            String analystRec, String rationale, String objSummary, String subjSummary,
            Map<String, Object> apiMeta, String apiError
    ) {}

    /**
     * Candidate after filtering + signal computation.
     */
    record Candidate(NormalizedRecord record, SignalCalculator.SignalResult score) {}

    // --- Public API ---

    public PlanResponse generatePlanFromTexts(List<ReportRequest> requests, PortfolioSettings settings) {
        PortfolioSettings resolved = resolveSettings(settings);
        List<NormalizedRecord> normalized = new ArrayList<>();

        for (ReportRequest req : requests) {
            Map<String, Object> dsResult = deepSeekService.analyzeReport(
                    req.getReportText(), req.getTicker(),
                    req.getReportDate() != null ? req.getReportDate().toString() : null);

            // If API fell back, use user-provided analyst recommendation from form
            String recommendation = (String) dsResult.get("recommendation");
            String apiError = (String) dsResult.get("_api_error");
            if (apiError != null && req.getAnalystRecommendation() != null
                    && !req.getAnalystRecommendation().isBlank()) {
                recommendation = req.getAnalystRecommendation();
                dsResult.put("recommendation", recommendation);
                // Fallback confidence: prefer user-provided confidence, then
                // estimate from objective ratio (higher objective → more reliable signal)
                double fallbackConf = req.getConfidence() != null
                        ? req.getConfidence()
                        : 0.4 + 0.4 * toDouble(req.getObjectiveRatio(), 0.5);
                dsResult.put("confidence", fallbackConf);
            }

            // FinBERT: compute objective/subjective ratios from report text.
            // Falls back to heuristic when Python / model unavailable.
            FinBertService.FinBertResult fbResult = finBertService.classify(
                    req.getReportText(), req.getTicker());

            // Use FinBERT ratios when available; otherwise keep form-provided values.
            double effectiveObjRatio = fbResult.fallback()
                    ? toDouble(req.getObjectiveRatio(), fbResult.objectiveRatio())
                    : fbResult.objectiveRatio();
            double effectiveSubjRatio = fbResult.fallback()
                    ? toDouble(req.getSubjectiveRatio(), fbResult.subjectiveRatio())
                    : fbResult.subjectiveRatio();

            log.debug("FinBERT {} → obj={}, subj={} (fallback={})",
                    req.getTicker(), effectiveObjRatio, effectiveSubjRatio, fbResult.fallback());

            normalized.add(new NormalizedRecord(
                    req.getTicker(),
                    req.getReportDate() != null ? req.getReportDate().toString() : null,
                    Recommendation.fromLabel((String) dsResult.get("recommendation")).getScore(),
                    (String) dsResult.get("recommendation"),
                    toDouble(dsResult.get("confidence"), 0.5),
                    effectiveObjRatio,
                    effectiveSubjRatio,
                    toDouble(dsResult.get("weight"), 0.5),
                    toDouble(req.getVolatility20d(), 0.3),
                    req.getFutureAr60d(),
                    req.getDailyReturns60d(),
                    req.getAnalystRecommendation(),
                    (String) dsResult.get("rationale"),
                    (String) dsResult.get("objective_summary"),
                    (String) dsResult.get("subjective_summary"),
                    Map.of(
                            "latency_ms", dsResult.getOrDefault("_api_latency_ms", 0L),
                            "model", dsResult.getOrDefault("_api_model", "unknown"),
                            "finbert_obj_ratio", fbResult.objectiveRatio(),
                            "finbert_subj_ratio", fbResult.subjectiveRatio(),
                            "finbert_fallback", fbResult.fallback()
                    ),
                    (String) dsResult.get("_api_error")
            ));
        }

        return buildPlanResponse(normalized, resolved, "text_input");
    }

    public PlanResponse generatePlanFromPrecomputed(List<ReportRequest> requests, PortfolioSettings settings) {
        PortfolioSettings resolved = resolveSettings(settings);
        List<NormalizedRecord> normalized = new ArrayList<>();

        for (ReportRequest req : requests) {
            double effectiveConfidence = req.getConfidence() != null
                    ? req.getConfidence()
                    : 0.4 + 0.4 * toDouble(req.getObjectiveRatio(), 0.5);
            normalized.add(new NormalizedRecord(
                    req.getTicker(), req.getReportDate() != null ? req.getReportDate().toString() : null,
                    Recommendation.fromLabel(req.getAnalystRecommendation()).getScore(),
                    req.getAnalystRecommendation(),
                    effectiveConfidence,
                    toDouble(req.getObjectiveRatio(), 0.5),
                    toDouble(req.getSubjectiveRatio(), 0.5),
                    0.5, // default weight
                    toDouble(req.getVolatility20d(), 0.3),
                    req.getFutureAr60d(),
                    req.getDailyReturns60d(),
                    req.getAnalystRecommendation(),
                    null, null, null,
                    Map.of("latency_ms", 0L, "model", "precomputed"),
                    null
            ));
        }

        return buildPlanResponse(normalized, resolved, "precomputed");
    }

    // --- Core Pipeline ---

    private PlanResponse buildPlanResponse(List<NormalizedRecord> records, PortfolioSettings settings, String inputSource) {
        // Step 1: Filter candidates
        List<Map<String, Object>> dropped = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();

        for (NormalizedRecord r : records) {
            if (r.recommendationScore == 3) {
                dropped.add(Map.of("ticker", r.ticker, "reason", "neutral_signal"));
                continue;
            }
            if (r.confidence < settings.getMinConfidence()) {
                dropped.add(Map.of("ticker", r.ticker, "reason", "low_confidence"));
                continue;
            }
            if (r.objectiveRatio < settings.getMinObjectiveRatio()) {
                dropped.add(Map.of("ticker", r.ticker, "reason", "low_objective_ratio"));
                continue;
            }

            SignalCalculator.SignalResult sig = SignalCalculator.compute(
                    r.recommendationScore, r.confidence,
                    r.objectiveRatio, r.subjectiveRatio,
                    r.subjectiveWeight, r.volatility20d);

            boolean isLongOnly = "long_only".equalsIgnoreCase(settings.getMode());
            if (isLongOnly && sig.finalScore() <= 0) {
                dropped.add(Map.of("ticker", r.ticker, "reason", "non_positive_signal_in_long_only"));
                continue;
            }

            candidates.add(new Candidate(r, sig));
        }

        // Step 2: Build portfolio
        List<PlanResponse.PositionDTO> positions = buildPortfolio(candidates, settings);

        // Step 3: Summarize
        PlanResponse response = new PlanResponse();

        // Meta
        PlanResponse.PlanMeta meta = new PlanResponse.PlanMeta();
        meta.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        meta.setInputFile(inputSource);
        meta.setSettings(settings);
        meta.setMethodology("Based on thesis: LLM ratings show incremental predictive power; objective-information-heavy reports receive higher weight.");
        meta.setApiSummary(Map.of(
                "total_reports", records.size(),
                "api_successes", records.stream().filter(r -> r.apiError == null).count(),
                "api_errors", records.stream().filter(r -> r.apiError != null).count(),
                "total_latency_ms", records.stream().mapToLong(r -> (long) toDouble(r.apiMeta.get("latency_ms"), 0)).sum()
        ));
        response.setMeta(meta);

        // Summary
        PlanResponse.PlanSummary summary = new PlanResponse.PlanSummary();
        summary.setUniverseSize(records.size());
        summary.setSelectedPositions(positions.size());
        summary.setLongPositions((int) positions.stream().filter(p -> "LONG".equals(p.getSide())).count());
        summary.setShortPositions((int) positions.stream().filter(p -> "SHORT".equals(p.getSide())).count());
        summary.setGrossExposure(positions.stream().mapToDouble(p -> Math.abs(p.getTargetWeight())).sum());
        summary.setNetExposure(positions.stream().mapToDouble(PlanResponse.PositionDTO::getTargetWeight).sum());
        summary.setCapital(settings.getCapital());
        summary.setFilteredOut(dropped.size());
        summary.setFilteredBreakdown(
                dropped.stream()
                        .collect(Collectors.groupingBy(d -> (String) d.get("reason"), Collectors.counting()))
                        .entrySet().stream()
                        .map(e -> { PlanResponse.DroppedBreakdown b = new PlanResponse.DroppedBreakdown(); b.setReason(e.getKey()); b.setCount(e.getValue().intValue()); return b; })
                        .toList()
        );
        response.setSummary(summary);

        // Risk rules
        PlanResponse.RiskRules rules = new PlanResponse.RiskRules();
        rules.setHoldDays(settings.getHoldDays());
        rules.setRebalanceDays(settings.getRebalanceDays());
        rules.setStopLossPct(settings.getStopLossPct());
        rules.setTakeProfitPct(settings.getTakeProfitPct());
        rules.setMaxPositionWeight(settings.getMaxPositionWeight());
        response.setRiskRules(rules);

        response.setExecutionChecklist(List.of(
                "Validate earnings/news blackout windows before execution.",
                "Execute at next market open after signal generation.",
                "Use TWAP/VWAP for illiquid names.",
                "Rebalance on schedule or when risk limits are breached.",
                "Close residual positions at hold period end if no risk trigger hit."
        ));

        response.setPositions(positions);
        response.setDroppedSamples(dropped.stream().map(d -> {
            PlanResponse.DroppedDTO dd = new PlanResponse.DroppedDTO();
            dd.setTicker((String) d.get("ticker"));
            dd.setReason((String) d.get("reason"));
            return dd;
        }).toList());

        // Markdown
        response.setMarkdown(generateMarkdown(response));

        // Diagnostics
        PlanResponse.DiagnosticsDTO diag = new PlanResponse.DiagnosticsDTO();
        diag.setInputRecords(records.size());
        diag.setCandidateCount(candidates.size());
        diag.setPositionCount(positions.size());
        diag.setApiErrors((int) records.stream().filter(r -> r.apiError != null).count());
        diag.setTotalApiLatencyMs(records.stream().mapToLong(r -> (long) toDouble(r.apiMeta.get("latency_ms"), 0)).sum());
        response.setDiagnostics(diag);

        return response;
    }

    private List<PlanResponse.PositionDTO> buildPortfolio(List<Candidate> candidates, PortfolioSettings settings) {
        List<Candidate> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble((Candidate c) -> Math.abs(c.score.finalScore())).reversed())
                .toList();

        boolean isLongOnly = "long_only".equalsIgnoreCase(settings.getMode());
        int maxPos = settings.getMaxPositions();

        if (isLongOnly) {
            List<Candidate> picks = sorted.stream()
                    .filter(c -> c.score.finalScore() > 0)
                    .limit(maxPos)
                    .toList();

            double[] raw = picks.stream().mapToDouble(c -> Math.abs(c.score.finalScore())).toArray();
            double[] weights = SignalCalculator.allocateWithCap(raw, 1.0, settings.getMaxPositionWeight());

            List<PlanResponse.PositionDTO> positions = new ArrayList<>();
            for (int i = 0; i < picks.size(); i++) {
                positions.add(toPositionDTO(picks.get(i), weights[i], "LONG", settings));
            }
            return positions;
        }

        // Long/Short mode
        int perSide = Math.max(1, maxPos / 2);
        List<Candidate> longs = sorted.stream().filter(c -> c.score.finalScore() > 0).limit(perSide).toList();
        List<Candidate> shorts = sorted.stream().filter(c -> c.score.finalScore() < 0).limit(perSide).toList();

        double longTarget = shorts.isEmpty() ? 1.0 : 0.5;
        double shortTarget = longs.isEmpty() ? 1.0 : 0.5;

        double[] longWeights = SignalCalculator.allocateWithCap(
                longs.stream().mapToDouble(c -> Math.abs(c.score.finalScore())).toArray(),
                longTarget, settings.getMaxPositionWeight());
        double[] shortWeights = SignalCalculator.allocateWithCap(
                shorts.stream().mapToDouble(c -> Math.abs(c.score.finalScore())).toArray(),
                shortTarget, settings.getMaxPositionWeight());

        List<PlanResponse.PositionDTO> positions = new ArrayList<>();
        for (int i = 0; i < longs.size(); i++) {
            positions.add(toPositionDTO(longs.get(i), longWeights[i], "LONG", settings));
        }
        for (int i = 0; i < shorts.size(); i++) {
            positions.add(toPositionDTO(shorts.get(i), -shortWeights[i], "SHORT", settings));
        }

        positions.sort(Comparator.comparingDouble(p -> -Math.abs(p.getTargetWeight())));
        return positions;
    }

    private PlanResponse.PositionDTO toPositionDTO(Candidate c, double weight, String side, PortfolioSettings settings) {
        PlanResponse.PositionDTO dto = new PlanResponse.PositionDTO();
        NormalizedRecord r = c.record;
        SignalCalculator.SignalResult s = c.score;

        dto.setTicker(r.ticker);
        dto.setReportDate(r.reportDate != null ? r.reportDate : "");
        dto.setSide(side);
        dto.setRecommendation(r.recommendationLabel);
        dto.setRecommendationScore(r.recommendationScore);
        dto.setConfidence(round4(r.confidence));
        dto.setObjectiveRatio(round4(r.objectiveRatio));
        dto.setSubjectiveRatio(round4(r.subjectiveRatio));
        dto.setSubjectiveWeight(round4(r.subjectiveWeight));
        dto.setVolatility20d(round4(r.volatility20d));
        dto.setSignalScore(round6(s.finalScore()));
        dto.setQualityScore(round6(s.qualityScore()));
        dto.setTargetWeight(round6(weight));
        dto.setTargetDollar(round2(weight * settings.getCapital()));
        dto.setHoldDays(settings.getHoldDays());
        dto.setStopLossPct(settings.getStopLossPct());
        dto.setTakeProfitPct(settings.getTakeProfitPct());

        List<String> notes = new ArrayList<>();
        if (r.objectiveRatio >= 0.5) notes.add("高客观含量");
        if (r.confidence >= 0.75) notes.add("高模型置信度");
        if (r.volatility20d >= 0.5) notes.add("高波动风险");
        dto.setNotes(notes);

        return dto;
    }

    private String generateMarkdown(PlanResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("# DeepSeek-R1 Investment Plan\n\n");
        sb.append("- Generated at: ").append(response.getMeta().getGeneratedAt()).append("\n");
        sb.append("- Mode: ").append(response.getMeta().getSettings().getMode()).append("\n");
        sb.append("- Capital: ").append(String.format("%.0f", response.getMeta().getSettings().getCapital())).append("\n\n");

        sb.append("## Summary\n");
        sb.append("- Universe: ").append(response.getSummary().getUniverseSize()).append("\n");
        sb.append("- Selected: ").append(response.getSummary().getSelectedPositions()).append("\n");
        sb.append("- Long/Short: ").append(response.getSummary().getLongPositions())
                .append("/").append(response.getSummary().getShortPositions()).append("\n");
        sb.append("- Gross: ").append(String.format("%.4f", response.getSummary().getGrossExposure())).append("\n");
        sb.append("- Net: ").append(String.format("%.4f", response.getSummary().getNetExposure())).append("\n\n");

        sb.append("## Positions\n");
        sb.append("| Ticker | Side | Rec | Conf | Obj | Weight | Dollar | Signal |\n");
        sb.append("|---|---|---:|---:|---:|---:|---:|---:|\n");
        for (PlanResponse.PositionDTO p : response.getPositions()) {
            sb.append("| ").append(p.getTicker())
                    .append(" | ").append(p.getSide())
                    .append(" | ").append(p.getRecommendationScore())
                    .append(" | ").append(String.format("%.3f", p.getConfidence()))
                    .append(" | ").append(String.format("%.3f", p.getObjectiveRatio()))
                    .append(" | ").append(String.format("%.4f", p.getTargetWeight()))
                    .append(" | ").append(String.format("%.2f", p.getTargetDollar()))
                    .append(" | ").append(String.format("%.4f", p.getSignalScore()))
                    .append(" |\n");
        }
        return sb.toString();
    }

    // --- Helpers ---

    private PortfolioSettings resolveSettings(PortfolioSettings s) {
        if (s == null) s = new PortfolioSettings();
        if (s.getCapital() == null) s.setCapital(defaults.getCapital());
        if (s.getMode() == null) s.setMode(defaults.getMode());
        if (s.getMaxPositions() == null) s.setMaxPositions(defaults.getMaxPositions());
        if (s.getMaxPositionWeight() == null) s.setMaxPositionWeight(defaults.getMaxPositionWeight());
        if (s.getMinConfidence() == null) s.setMinConfidence(defaults.getMinConfidence());
        if (s.getMinObjectiveRatio() == null) s.setMinObjectiveRatio(defaults.getMinObjectiveRatio());
        if (s.getHoldDays() == null) s.setHoldDays(defaults.getHoldDays());
        if (s.getRebalanceDays() == null) s.setRebalanceDays(defaults.getRebalanceDays());
        if (s.getStopLossPct() == null) s.setStopLossPct(defaults.getStopLossPct());
        if (s.getTakeProfitPct() == null) s.setTakeProfitPct(defaults.getTakeProfitPct());
        return s;
    }

    private double toDouble(Object v, double fallback) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
    private static double round6(double v) { return Math.round(v * 1000000.0) / 1000000.0; }
}

package com.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Calls the FinBERT Python classifier for sentence-level subjective/objective
 * classification. Communicates with a long-running Python process via
 * stdin/stdout JSON lines protocol.
 *
 * <p>Falls back to a built-in heuristic when the Python process is unavailable
 * — ensuring the pipeline never blocks on FinBERT availability.</p>
 */
@Service
public class FinBertService {

    private static final Logger log = LoggerFactory.getLogger(FinBertService.class);
    private final ObjectMapper mapper;

    private Process pythonProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private boolean available;
    private final String scriptPath;

    public FinBertService(ObjectMapper mapper,
                          @Value("${finbert.script-path:finbert/finbert_classifier.py}")
                          String scriptPath) {
        this.mapper = mapper;
        // Resolve relative to project root; fall back to workspace/finbert
        String projectRoot = System.getProperty("user.dir");
        this.scriptPath = new File(projectRoot, scriptPath).getAbsolutePath();
    }

    /**
     * Result of FinBERT classification.
     */
    public record FinBertResult(
            double objectiveRatio,
            double subjectiveRatio,
            double avgConfidence,
            int sentenceCount,
            boolean fallback,
            String note
    ) {
        /** Clamp ratios to [0, 1] and provide sensible defaults. */
        public static FinBertResult safe(double obj, double subj, double conf,
                                          int count, boolean fb, String note) {
            return new FinBertResult(
                    clamp(obj, 0, 1), clamp(subj, 0, 1), clamp(conf, 0, 1),
                    count, fb, note);
        }

        private static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    /**
     * Classify a single report text. Thread-safe — serializes access to the
     * shared Python process.
     */
    public synchronized FinBertResult classify(String reportText, String ticker) {
        if (reportText == null || reportText.isBlank()) {
            return FinBertResult.safe(0.5, 0.5, 0.5, 0, true, "Empty input");
        }

        // Try Python process first
        if (ensureProcess()) {
            try {
                return callPython(reportText, ticker);
            } catch (Exception e) {
                log.warn("FinBERT Python call failed for {}: {}", ticker, e.getMessage());
                available = false;
            }
        }

        // Fallback
        return heuristicClassify(reportText);
    }

    // --- Python process management ---

    private synchronized boolean ensureProcess() {
        if (available && pythonProcess != null && pythonProcess.isAlive()) {
            return true;
        }
        if (available && pythonProcess != null) {
            log.warn("FinBERT process died — restarting");
        }
        return startProcess();
    }

    private boolean startProcess() {
        // Try multiple Python paths (Windows compatibility)
        String[] pythonCandidates = {
            "python3", "python",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python312\\python.exe",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python314\\python.exe",
        };

        for (String python : pythonCandidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(python, "-u", scriptPath);
                pb.directory(new File(scriptPath).getParentFile());
                pb.redirectError(ProcessBuilder.Redirect.PIPE);

                pythonProcess = pb.start();
                writer = new BufferedWriter(
                        new OutputStreamWriter(pythonProcess.getOutputStream(), StandardCharsets.UTF_8));
                reader = new BufferedReader(
                        new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8));

                // Read startup status from stderr (first line is JSON status)
                BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(pythonProcess.getErrorStream(), StandardCharsets.UTF_8));
                String status = errReader.readLine();
                if (status != null && status.contains("ready")) {
                    log.info("FinBERT Python process started with {}: {}", python, status);
                    available = true;
                    return true;
                }

                // This Python worked but model didn't load — still usable (heuristic)
                log.info("FinBERT started with {} (fallback mode): {}", python, status);
                available = true;
                return true;

            } catch (IOException e) {
                // Try next candidate
                log.debug("Python candidate {} not available: {}", python, e.getMessage());
                if (pythonProcess != null && pythonProcess.isAlive()) {
                    pythonProcess.destroy();
                }
            }
        }

        log.warn("No Python found — FinBERT heuristic fallback will be used");
        available = false;
        return false;
    }

    private FinBertResult callPython(String text, String ticker)
            throws JsonProcessingException, IOException {

        // Sanitize: replace newlines in text so it stays on one JSON line
        String safeText = text.replace("\n", "\\n").replace("\r", "\\r");
        String request = mapper.writeValueAsString(
                java.util.Map.of("text", safeText, "ticker", ticker));
        writer.write(request);
        writer.newLine();
        writer.flush();

        String response = reader.readLine();
        if (response == null) {
            throw new IOException("FinBERT process returned EOF");
        }

        JsonNode root = mapper.readTree(response);
        if (root.has("error")) {
            throw new IOException("FinBERT error: " + root.get("error").asText());
        }

        boolean fb = root.has("fallback") && root.get("fallback").asBoolean();
        return FinBertResult.safe(
                root.path("objective_ratio").asDouble(0.5),
                root.path("subjective_ratio").asDouble(0.5),
                root.path("avg_confidence").asDouble(0.5),
                root.path("sentence_count").asInt(0),
                fb,
                root.has("note") ? root.get("note").asText() : null);
    }

    // --- Heuristic fallback (no Python required) ---

    private FinBertResult heuristicClassify(String text) {
        // Count data/fact keywords as proxy for objectivity
        String[] dataKeywords = {
                "增长", "下降", "亿元", "万元", "%", "同比", "环比",
                "收入", "利润", "毛利率", "净利率", "ROE", "EPS",
                "PE", "PB", "市值", "营收", "净利", "亿元", "万元",
                "revenue", "profit", "margin", "growth", "billion", "million",
                "Q1", "Q2", "Q3", "Q4", "H1", "H2"
        };
        String[] opinionKeywords = {
                "认为", "预计", "预计", "判断", "看好", "看空", "建议",
                "推荐", "值得", "有望", "有望", "可能", "或将", "或将",
                "believe", "expect", "anticipate", "suggest", "recommend",
                "likely", "potential", "outlook", "view"
        };

        int dataScore = countKeywords(text, dataKeywords);
        int opinionScore = countKeywords(text, opinionKeywords);
        int totalHits = dataScore + opinionScore;

        if (totalHits == 0) {
            return FinBertResult.safe(0.5, 0.5, 0.5, 0, true, "No keywords matched");
        }

        double objRatio = (double) dataScore / totalHits;
        double subjRatio = (double) opinionScore / totalHits;
        double conf = 0.4 + 0.3 * ((double) totalHits / Math.max(50, totalHits + 20));

        return FinBertResult.safe(objRatio, subjRatio, conf, 1, true,
                "Heuristic: " + dataScore + " data vs " + opinionScore + " opinion keywords");
    }

    private int countKeywords(String text, String[] keywords) {
        String lower = text.toLowerCase();
        int count = 0;
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) count++;
        }
        return count;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down FinBERT Python process");
        available = false;
        if (writer != null) {
            try { writer.close(); } catch (IOException ignored) {}
        }
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}

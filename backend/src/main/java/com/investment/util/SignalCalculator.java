package com.investment.util;

import com.investment.model.enums.Recommendation;

/**
 * Core signal computation engine. Ported from Node.js core.js,
 * implements the thesis signal formula with quality gating and risk penalties.
 */
public final class SignalCalculator {

    private SignalCalculator() {}

    /** Map recommendation label to 1-5 score */
    public static int recommendationToScore(String raw) {
        return Recommendation.fromLabel(raw).getScore();
    }

    /** Map score back to label */
    public static String scoreToLabel(int score) {
        return Recommendation.fromScore(score).getLabel();
    }

    /**
     * Compute the composite signal score.
     *
     * Signal = centeredRec × M_info × M_conf × P_subj × P_vol × (0.5 + Q_text)
     *
     * M_info: Information structure multiplier using thesis coefficients (see Section 4.3.2).
     *   - High objective (>0.5): coeff 1.469; Low objective: coeff 0.388
     *   - High subjective (>0.5): coeff 0.451; Low subjective: coeff 1.39
     *   Normalized by dividing the average by 0.849 (theoretical midpoint of the
     *   four coefficient combinations) to center the multiplier near 1.0.
     *
     * Q_text: Quality score based on objective dominance, NOT confidence.
     *   Confidence is already factored through M_conf; including it here would
     *   double-count and inflate high-confidence signals.
     */
    public static SignalResult compute(int recommendationScore, double confidence,
                                       double objectiveRatio, double subjectiveRatio,
                                       double subjectiveWeight, double volatility20d) {

        double centeredRec = recommendationScore - 3.0;

        // Information structure multiplier (from thesis coefficients, Table 4.3)
        double objCoef = objectiveRatio >= 0.5 ? 1.469 : 0.388;
        double subjCoef = subjectiveRatio >= 0.5 ? 0.451 : 1.39;
        // Divide by 0.849 = average of (1.469+0.451)/2 + (0.388+1.39)/2 to center near 1.0
        double infoMultiplier = ((objCoef + subjCoef) / 2.0) / 0.849;

        // Confidence multiplier: maps [0,1] confidence to [0.6, 1.4] range
        double confMultiplier = 0.6 + 0.8 * confidence;

        // Subjective penalty: penalize when subjective weight exceeds 0.6
        double subjPenalty = 1.0 - 0.2 * Math.max(0, subjectiveWeight - 0.6);

        // Volatility penalty: linearly decreases from 1.0 at vol=0 to floor at vol>=0.5
        // Floor at 0.6 ensures extreme volatility doesn't zero out the signal
        double volPenalty = Math.max(0.6, 1.0 - 0.8 * volatility20d);

        double rawSignal = centeredRec * infoMultiplier * confMultiplier * subjPenalty * volPenalty;

        // Quality score: based on objective dominance, avoiding confidence double-count
        // Rewards high objective-to-subjective ratio; penalizes subjective-heavy reports
        double qualityScore = clamp(
                objectiveRatio * (1.0 - 0.5 * Math.max(0, subjectiveRatio - objectiveRatio)),
                0, 1);

        double finalScore = rawSignal * (0.5 + qualityScore);

        return new SignalResult(centeredRec, infoMultiplier, confMultiplier,
                subjPenalty, volPenalty, rawSignal, qualityScore, finalScore);
    }

    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    /**
     * Allocate weights with per-position cap using iterative reflow.
     */
    public static double[] allocateWithCap(double[] values, double targetSum, double cap) {
        int n = values.length;
        if (n == 0) return new double[0];

        double total = 0;
        for (double v : values) total += v;
        if (total <= 0) return new double[n];

        double[] weights = new double[n];
        for (int i = 0; i < n; i++) weights[i] = (values[i] / total) * targetSum;

        for (int round = 0; round < 100; round++) {
            double excess = 0;
            int[] freeIdx = new int[n];
            int freeCount = 0;

            for (int i = 0; i < n; i++) {
                if (weights[i] > cap) {
                    excess += weights[i] - cap;
                    weights[i] = cap;
                } else {
                    freeIdx[freeCount++] = i;
                }
            }

            if (excess <= 1e-10 || freeCount == 0) break;

            double freeTotal = 0;
            for (int j = 0; j < freeCount; j++) freeTotal += weights[freeIdx[j]];

            if (freeTotal <= 1e-12) {
                double add = excess / freeCount;
                for (int j = 0; j < freeCount; j++) weights[freeIdx[j]] += add;
            } else {
                for (int j = 0; j < freeCount; j++) {
                    weights[freeIdx[j]] += excess * (weights[freeIdx[j]] / freeTotal);
                }
            }
        }
        return weights;
    }

    public record SignalResult(double centeredRec, double infoMultiplier, double confMultiplier,
                                double subjPenalty, double volPenalty, double rawSignal,
                                double qualityScore, double finalScore) {}
}

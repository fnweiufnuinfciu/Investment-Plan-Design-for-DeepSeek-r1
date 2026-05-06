package com.investment.model.enums;

public enum Recommendation {
    VERY_BULLISH("Very Bullish", 5),
    BULLISH("Bullish", 4),
    NEUTRAL("Neutral", 3),
    BEARISH("Bearish", 2),
    VERY_BEARISH("Very Bearish", 1);

    private final String label;
    private final int score;

    Recommendation(String label, int score) {
        this.label = label;
        this.score = score;
    }

    public String getLabel() { return label; }
    public int getScore() { return score; }

    public static Recommendation fromLabel(String label) {
        if (label == null) return NEUTRAL;
        String compact = label.trim().toLowerCase().replaceAll("[\\s_-]+", "");
        return switch (compact) {
            case "verybullish", "strongbuy", "buystrong" -> VERY_BULLISH;
            case "bullish", "buy" -> BULLISH;
            case "neutral", "hold" -> NEUTRAL;
            case "bearish", "sell" -> BEARISH;
            case "verybearish", "strongsell", "sellstrong" -> VERY_BEARISH;
            case "非常看涨" -> VERY_BULLISH;
            case "看涨" -> BULLISH;
            case "中性" -> NEUTRAL;
            case "看跌" -> BEARISH;
            case "非常看跌" -> VERY_BEARISH;
            default -> NEUTRAL;
        };
    }

    public static Recommendation fromScore(int score) {
        return switch (score) {
            case 5 -> VERY_BULLISH;
            case 4 -> BULLISH;
            case 3 -> NEUTRAL;
            case 2 -> BEARISH;
            case 1 -> VERY_BEARISH;
            default -> NEUTRAL;
        };
    }
}

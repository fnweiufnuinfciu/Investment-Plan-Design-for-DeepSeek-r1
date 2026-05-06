package com.investment.model.enums;

public enum PortfolioMode {
    LONG_SHORT("long_short"),
    LONG_ONLY("long_only");

    private final String value;

    PortfolioMode(String value) { this.value = value; }
    public String getValue() { return value; }

    public static PortfolioMode fromValue(String v) {
        if (v == null) return LONG_SHORT;
        return "long_only".equalsIgnoreCase(v.trim()) ? LONG_ONLY : LONG_SHORT;
    }
}

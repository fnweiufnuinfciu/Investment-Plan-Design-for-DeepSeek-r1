package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "backtest_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    private Integer holdDays;
    private Double stopLossPct;
    private Double takeProfitPct;

    private Double cumulativeReturn;
    private Double annualizedReturn;
    private Double annualizedVolatility;
    private Double sharpe;
    private Double sortino;
    private Double maxDrawdown;
    private Double dailyWinRate;
    private Double var95;
    private Double cvar95;

    private Integer positionCount;
    private Integer winnerCount;
    private Integer loserCount;
    private Double avgPositionReturn;
    private Double avgHoldingDays;

    @Column(columnDefinition = "TEXT")
    private String sourceBreakdown;

    @Column(columnDefinition = "TEXT")
    private String dailyReturns;

    @Column(columnDefinition = "TEXT")
    private String bySide;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = java.time.LocalDateTime.now(); }
}

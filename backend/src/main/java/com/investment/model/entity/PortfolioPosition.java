package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portfolio_positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(length = 20)
    private String recommendation;

    @Column(name = "recommendation_score")
    private Integer recommendationScore;

    private Double confidence;
    private Double objectiveRatio;
    private Double subjectiveRatio;
    private Double subjectiveWeight;
    private Double volatility20d;
    private Double signalScore;
    private Double qualityScore;
    private Double targetWeight;
    private Double targetDollar;
    private Integer holdDays;
    private Double stopLossPct;
    private Double takeProfitPct;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

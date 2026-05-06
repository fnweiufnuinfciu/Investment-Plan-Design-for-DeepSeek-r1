package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 20)
    private String recommendation;

    @Column(name = "recommendation_score", nullable = false)
    private Integer recommendationScore;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "objective_summary", columnDefinition = "TEXT")
    private String objectiveSummary;

    @Column(name = "subjective_summary", columnDefinition = "TEXT")
    private String subjectiveSummary;

    @Column(name = "key_evidence", columnDefinition = "TEXT")
    private String keyEvidence;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "signal_score")
    private Double signalScore;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "api_latency_ms")
    private Long apiLatencyMs;

    @Column(name = "api_model", length = 50)
    private String apiModel;

    @Column(name = "api_error", columnDefinition = "TEXT")
    private String apiError;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}

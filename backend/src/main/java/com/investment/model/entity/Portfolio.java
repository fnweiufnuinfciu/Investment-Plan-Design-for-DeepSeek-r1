package com.investment.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(nullable = false)
    private Double capital;

    @Column(name = "universe_size")
    private Integer universeSize;

    @Column(name = "selected_positions")
    private Integer selectedPositions;

    @Column(name = "long_positions")
    private Integer longPositions;

    @Column(name = "short_positions")
    private Integer shortPositions;

    @Column(name = "gross_exposure")
    private Double grossExposure;

    @Column(name = "net_exposure")
    private Double netExposure;

    @Column(columnDefinition = "TEXT")
    private String settings;

    @Column(name = "api_summary", columnDefinition = "TEXT")
    private String apiSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String methodology;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("targetWeight DESC")
    private List<PortfolioPosition> positions = new ArrayList<>();

    @Column(name = "generated_at")
    private java.time.LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = java.time.LocalDateTime.now();
    }
}

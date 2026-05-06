package com.investment.repository;

import com.investment.model.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByTickerOrderByCreatedAtDesc(String ticker);
    List<AnalysisResult> findByReportId(Long reportId);
}

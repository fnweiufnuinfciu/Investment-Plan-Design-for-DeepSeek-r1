package com.investment.repository;

import com.investment.model.entity.AnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    List<AnalysisHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AnalysisHistory> findByPlanIdOrderByCreatedAtDesc(String planId);
    List<AnalysisHistory> findByUserIdAndPlanIdOrderByCreatedAtDesc(Long userId, String planId);
}

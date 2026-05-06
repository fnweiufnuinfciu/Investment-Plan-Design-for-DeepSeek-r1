package com.investment.repository;

import com.investment.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByTickerOrderByReportDateDesc(String ticker);
    List<Report> findByTickerAndReportDate(String ticker, LocalDate reportDate);
    List<Report> findByReportDateBetweenOrderByReportDateDesc(LocalDate start, LocalDate end);
}

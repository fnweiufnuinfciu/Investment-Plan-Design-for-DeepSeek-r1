package com.investment.controller;

import com.investment.model.entity.Report;
import com.investment.repository.ReportRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public ResponseEntity<List<Report>> list() {
        return ResponseEntity.ok(reportRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return reportRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<List<Report>> getByTicker(@PathVariable String ticker) {
        return ResponseEntity.ok(reportRepository.findByTickerOrderByReportDateDesc(ticker));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody Report report) {
        Report saved = reportRepository.save(report);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Report report) {
        if (!reportRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        report.setId(id);
        return ResponseEntity.ok(reportRepository.save(report));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!reportRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        reportRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @DeleteMapping("/batch")
    @Transactional
    public ResponseEntity<?> deleteBatch(@RequestBody List<Long> ids) {
        reportRepository.deleteAllById(ids);
        return ResponseEntity.ok(Map.of("message", "批量删除成功", "count", ids.size()));
    }
}

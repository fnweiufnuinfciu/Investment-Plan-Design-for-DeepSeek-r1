package com.investment.controller;

import com.investment.model.entity.AnalysisHistory;
import com.investment.repository.AnalysisHistoryRepository;
import com.investment.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final AnalysisHistoryRepository historyRepo;
    private final UserRepository userRepo;

    public HistoryController(AnalysisHistoryRepository historyRepo, UserRepository userRepo) {
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<List<AnalysisHistory>> listByUser(Principal principal) {
        if (principal == null) return ResponseEntity.ok(List.of());
        var user = userRepo.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(historyRepo.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<AnalysisHistory>> listByPlan(@PathVariable String planId) {
        return ResponseEntity.ok(historyRepo.findByPlanIdOrderByCreatedAtDesc(planId));
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody List<AnalysisHistory> records, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        var user = userRepo.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }
        String planId = "PLAN-" + System.currentTimeMillis();
        for (AnalysisHistory r : records) {
            r.setUserId(user.getId());
            r.setPlanId(planId);
        }
        historyRepo.saveAll(records);
        return ResponseEntity.ok(Map.of("planId", planId, "count", records.size()));
    }
}

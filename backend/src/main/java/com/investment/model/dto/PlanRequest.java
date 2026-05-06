package com.investment.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class PlanRequest {
    @NotEmpty
    private List<@Valid ReportRequest> records;
    private PortfolioSettings settings = new PortfolioSettings();
    private String inputFile;
}

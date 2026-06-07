package com.investment.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PortfolioSettings {
    @Positive(message = "资金必须大于0")
    private Double capital = 1_000_000.0;

    @Pattern(regexp = "long_short|long_only", message = "模式必须是 long_short 或 long_only")
    private String mode = "long_short";

    @Min(value = 1, message = "最大持仓数至少为1") @Max(value = 100, message = "最大持仓数不超过100")
    private Integer maxPositions = 12;

    @DecimalMin(value = "0.01", message = "单票上限至少为0.01")
    @DecimalMax(value = "1.0", message = "单票上限不超过1.0")
    private Double maxPositionWeight = 0.15;

    @DecimalMin(value = "0.0", message = "最低信心不低于0") @DecimalMax(value = "1.0", message = "最低信心不超过1")
    private Double minConfidence = 0.55;

    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private Double minObjectiveRatio = 0.45;

    @Min(value = 1, message = "持有天数至少为1") @Max(value = 365, message = "持有天数不超过365")
    private Integer holdDays = 60;

    @Min(value = 1) @Max(value = 365)
    private Integer rebalanceDays = 20;

    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0", message = "止损比例不超过1.0")
    private Double stopLossPct = 0.08;

    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0", message = "止盈比例不超过5.0")
    private Double takeProfitPct = 0.20;
}

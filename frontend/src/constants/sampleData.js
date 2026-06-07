/**
 * 示例数据和研报文本（预计算模式 + 文本模式）
 */
export const SAMPLE_DATA = [
  { ticker:"AAPL", reportDate:"2026-04-15", analystRecommendation:"Bullish",
    confidence:0.72, objectiveRatio:0.68, subjectiveRatio:0.22, volatility20d:0.24, futureAr60d:0.0784 },
  { ticker:"MSFT", reportDate:"2026-04-15", analystRecommendation:"Very Bullish",
    confidence:0.85, objectiveRatio:0.72, subjectiveRatio:0.18, volatility20d:0.21, futureAr60d:0.0912 },
  { ticker:"TSLA", reportDate:"2026-04-13", analystRecommendation:"Bearish",
    confidence:0.65, objectiveRatio:0.45, subjectiveRatio:0.48, volatility20d:0.52, futureAr60d:-0.0345 },
  { ticker:"NVDA", reportDate:"2026-04-15", analystRecommendation:"Very Bullish",
    confidence:0.88, objectiveRatio:0.75, subjectiveRatio:0.15, volatility20d:0.38, futureAr60d:0.1240 },
  { ticker:"META", reportDate:"2026-04-12", analystRecommendation:"Bullish",
    confidence:0.70, objectiveRatio:0.55, subjectiveRatio:0.35, volatility20d:0.29, futureAr60d:0.0567 },
  { ticker:"INTC", reportDate:"2026-04-10", analystRecommendation:"Bearish",
    confidence:0.58, objectiveRatio:0.40, subjectiveRatio:0.50, volatility20d:0.44, futureAr60d:-0.0210 },
]

export const REPORT_TEXTS = {
  AAPL: "Apple Inc. Q1 FY2026 earnings report: Revenue reached $125B, beating consensus estimates by 4%. " +
    "Services segment revenue grew 18% YoY to $28B, driven by App Store and Apple Music subscriptions. " +
    "iPhone revenue remained flat at $55B, with higher ASP offsetting unit declines. " +
    "Gross margins expanded 120bps to 46.2%, above management guidance. " +
    "The company authorized an additional $90B share buyback program. " +
    "Greater China revenue declined 8% YoY to $20.8B amid competitive pressure from Huawei. " +
    "EU Digital Markets Act compliance costs estimated at $500M annually. " +
    "Wearables revenue up 5% YoY. Forward P/E of 28x reflects premium valuation. " +
    "Cash position of $165B provides significant strategic flexibility.",
  MSFT: "Microsoft Corporation Q3 FY2026 analysis: Total revenue of $68B, up 15% YoY, driven by Azure cloud growth. " +
    "Azure revenue accelerated to 33% YoY growth, outpacing AWS and GCP. " +
    "Microsoft 365 commercial seats grew 8%, with E5 premium mix improving ARPU. " +
    "AI Copilot adoption reached 60% of Fortune 500 enterprises, contributing $3.2B incremental revenue. " +
    "Operating margins expanded 50bps to 48.5%. Gaming revenue declined 4% due to hardware cycle maturity. " +
    "LinkedIn revenue grew 10% YoY. Capital expenditures of $14B reflect AI infrastructure investment. " +
    "Commercial cloud backlog of $260B provides multi-year visibility. " +
    "Net cash of $55B. Forward P/E 32x reflects AI growth premium.",
  TSLA: "Tesla Inc. Q1 2026 report: Total revenue of $28B missed consensus by 3%. " +
    "Automotive gross margin declined to 16.2% from 18.5% year-over-year, reflecting price cuts in China and Europe. " +
    "Operating margin compressed to 7.1% from 11.4%. Cybertruck production ramp progressing slower than initial guidance, " +
    "with 45K units delivered in Q1. Energy storage deployed 12GWh, up 90% YoY, with Megapack backlog extending into 2027. " +
    "FSD take rate declined to 12% from 18% globally. China market share dropped from 8.5% to 6.2%. " +
    "Competition from BYD and Xpeng intensifying across price segments. " +
    "Berlin and Austin factories operating at 65% utilization. Cash reserves of $25B. " +
    "Forward P/E of 58x remains elevated relative to auto peers.",
  NVDA: "NVIDIA Corporation Q1 FY2027 analysis: Revenue of $35B exceeded guidance by 8%, driven by Blackwell GPU demand. " +
    "Data Center segment revenue grew 200% YoY to $28B, as hyperscale customers accelerate AI infrastructure buildout. " +
    "Gaming revenue of $3.5B, up 25% YoY on RTX 50 series refresh cycle. " +
    "Gross margins reached 78.4%, expanding 300bps sequentially on product mix shift to higher-end configurations. " +
    "Enterprise AI adoption driving demand for DGX systems and Omniverse platform. " +
    "Supply constraints on HBM3e memory expected to ease in H2 2026. " +
    "Announced $25B share repurchase authorization. Forward P/E of 35x on consensus estimates. " +
    "Competitive moat remains wide with CUDA ecosystem locked in across 4M+ developers. " +
    "Automotive revenue of $400M, up 15% YoY. Main risk: potential export controls tightening.",
  META: "Meta Platforms Inc. Q1 2026 report: Revenue of $45B, up 18% YoY, driven by AI-powered ad targeting improvements. " +
    "Family daily active people reached 3.4B, up 6% YoY. Ad impressions increased 12% with average price per ad up 5%. " +
    "Reality Labs operating loss of $5.2B, with revenue of $800M from Quest 4 and Ray-Ban Meta smart glasses. " +
    "Operating margins of 42% on improved cost discipline. AI content recommendation drove 8% increase in Reels engagement. " +
    "WhatsApp Business monetization reached $2B annual run rate. Capital expenditures of $12B focused on AI training clusters. " +
    "Threads MAU of 350M. Cash reserves of $65B. Forward P/E of 22x reasonable relative to growth rate. " +
    "Main risks: TikTok regulatory uncertainty creates competitive opening but also regulatory precedent risk.",
  INTC: "Intel Corporation Q1 2026 analysis: Revenue of $14.2B, flat YoY, missing expectations by 2%. " +
    "Data Center revenue declined 8% YoY as Gaudi 3 AI accelerator failed to gain meaningful traction against NVIDIA. " +
    "Client Computing revenue of $7.2B, down 3% YoY on PC market weakness. " +
    "Foundry Services (IFS) revenue of $400M, with external customer pipeline of $5B but slow conversion. " +
    "Gross margins of 38.5%, well below historical 55%+ levels. Operating loss of $800M reflects heavy R&D investment. " +
    "18A process node delayed to late 2026, extending timeline for competitive parity with TSMC. " +
    "Announced $10B cost reduction program including 5% workforce reduction. " +
    "Dividend suspended to preserve cash for foundry investments. Net debt position of $8B. " +
    "Forward P/E NM. Main risk: sustained market share loss in both client and data center segments.",
}

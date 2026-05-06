# DeepSeek-R1 智能投资决策系统

基于 SpringBoot + Vue3 + MySQL 的全栈重构版本。

## 项目结构

```
workspace/
├── backend/          # SpringBoot 3.2 后端
│   ├── src/main/java/com/investment/
│   │   ├── config/        # 配置类 (DeepSeek, CORS, Defaults)
│   │   ├── controller/    # REST API 控制器
│   │   ├── service/       # 核心服务 (DeepSeek, Portfolio, Analytics)
│   │   ├── model/
│   │   │   ├── entity/    # JPA 实体
│   │   │   ├── dto/       # 数据传输对象
│   │   │   └── enums/     # 枚举
│   │   ├── repository/    # Spring Data JPA Repository
│   │   └── util/          # 工具类 (SignalCalculator)
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/  # Flyway 数据库迁移
├── frontend/         # Vue3 + Element Plus 前端
│   └── src/
│       ├── views/         # 页面视图
│       ├── router/        # 路由配置
│       ├── api/           # API 调用层
│       └── stores/        # Pinia 状态管理
└── README.md
```

## 快速启动

### 1. 启动后端

```bash
cd backend

# 开发模式 (H2 内存数据库，无需 MySQL)
mvn spring-boot:run

# 或设置 API Key
set DEEPSEEK_API_KEY=sk-xxx
mvn spring-boot:run
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173

## API 文档

### 核心端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| GET | `/api/defaults` | 获取默认参数 |
| POST | `/api/analyze-report` | 单篇报告 DeepSeek 分析 |
| POST | `/api/plan` | 预计算数据 → 投资方案 |
| POST | `/api/plan-from-texts` | 原始文本 → API 分析 → 投资方案 |

### POST /api/plan 请求体

```json
{
  "records": [
    {
      "ticker": "AAPL",
      "report_date": "2026-04-15",
      "deepseek": {
        "recommendation": "Bullish",
        "confidence": 0.82,
        "weight": 0.36
      },
      "finbert": {
        "objective_ratio": 0.68,
        "subjective_ratio": 0.22
      },
      "market": {
        "volatility_20d": 0.24
      }
    }
  ],
  "settings": {
    "mode": "long_short",
    "capital": 1000000,
    "minConfidence": 0.55
  }
}
```

### 响应包含

- `plan` — 投资方案（持仓、规则、摘要）
- `analytics.backtest` — 回测指标（Sharpe, Sortino, MaxDD, VaR95）
- `analytics.stressTest` — 压力测试（4种场景）
- `analytics.sensitivity` — 参数敏感性网格
- `analytics.dataQuality` — 数据质量诊断
- `analytics.explainability` — 可解释性分析

## 技术栈

**后端**: Spring Boot 3.2 / Spring Data JPA / Flyway / MySQL / H2 / Java 17
**前端**: Vue 3.4 / Vite 5 / Element Plus / Pinia / Axios / ECharts
**AI**: DeepSeek Chat API (deepseek-chat)

## 生产部署

```bash
# 后端
mvn clean package -DskipTests
java -jar target/deepseek-investment-system-1.0.0.jar --spring.profiles.active=prod

# 前端
npm run build
# 将 dist/ 部署到 Nginx
```

## 与 Node.js 原型的对应关系

| Node.js | SpringBoot |
|---------|------------|
| `src/core.js` → `SignalCalculator.java` | 信号计算引擎 |
| `src/core.js` → `PortfolioService.java` | 组合构建 |
| `src/analytics.js` → `AnalyticsService.java` | 回测/压力/敏感性 |
| `src/deepseek-client.js` → `DeepSeekService.java` | API 客户端 |
| `server.js` → `AnalysisController.java` | HTTP API |
| `web/` → `frontend/` | 前端 UI |
| JSON 文件 → MySQL + JPA | 数据持久化 |

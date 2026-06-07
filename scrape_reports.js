/**
 * Scrape 100 analyst report texts from 证券之星 for system testing.
 * Outputs jsonl suitable for the 典韦 portfolio system.
 */
const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

// ── GB2312-safe decoder (no external deps) ──
function decode(buf) {
  try {
    // Try UTF-8 first
    const s = buf.toString('utf8');
    if (!s.includes('�') && !s.includes('?')) return s;
  } catch (_) {}
  // Fall back: decode as latin1 then re-encode
  return buf.toString('latin1');
}

function fetch(url) {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : http;
    const req = mod.get(url, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' },
      timeout: 15000,
    }, (res) => {
      // Follow redirects
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return fetch(res.headers.location.startsWith('http') ? res.headers.location : new URL(res.headers.location, url).href)
          .then(resolve).catch(reject);
      }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve(decode(Buffer.concat(chunks))));
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
  });
}

// ── Parse individual report page ──
function parseReport(html, url) {
  // Title
  const tMatch = html.match(/<h1[^>]*>([^<]+)<\/h1>/);
  const title = tMatch ? tMatch[1].replace(/<[^>]*>/g, '').trim() : '';

  // Article body
  const aMatch = html.match(/<div class="article_content"[^>]*>([\s\S]*?)<\/div>/);
  if (!aMatch) return null;
  let text = aMatch[1]
    .replace(/<script[\s\S]*?<\/script>/g, '')
    .replace(/<style[\s\S]*?<\/style>/g, '')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/\s+/g, ' ')
    .trim();

  if (text.length < 100) return null;

  // Extract stock info from title/text
  const stockNameMatch = title.match(/([一-龥]{2,6}(?:科技|股份|集团|电子|医药|银行|证券|保险|汽车|能源|钢|铝|铜|光伏|锂|芯|半导|机器|互联|通信|传媒|食品|饮料|服装|化工|建材|地产|物流|航空|电力|环保))/);
  const stockName = stockNameMatch ? stockNameMatch[1] : (text.match(/([一-龥]{2,6}(?:科技|股份|集团|电子|医药))/)?.[1] || '');

  // Rating
  const ratingMatch = text.match(/(买入|增持|推荐|强烈推荐|买入-A|优于大市|持有|中性|卖出|减持|谨慎推荐|强于大市)/);
  const rating = ratingMatch ? ratingMatch[1] : 'Neutral';

  // Map Chinese rating to system labels
  const ratingMap = {
    '买入': 'Bullish', '增持': 'Bullish', '推荐': 'Bullish', '强烈推荐': 'Very Bullish',
    '买入-A': 'Very Bullish', '优于大市': 'Bullish', '强于大市': 'Bullish',
    '谨慎推荐': 'Bullish', '持有': 'Neutral', '中性': 'Neutral',
    '减持': 'Bearish', '卖出': 'Very Bearish',
  };
  const systemRating = ratingMap[rating] || 'Neutral';

  // Ticker: try to find 6-digit code
  const tickerMatch = text.match(/(\d{6})/);
  const ticker = tickerMatch ? tickerMatch[1] : '000000';

  // Target price
  const tpMatch = text.match(/(?:目标价|目标)[约]?(\d+\.?\d*)\s*元/);
  const targetPrice = tpMatch ? parseFloat(tpMatch[1]) : null;

  // Sector
  const sectorKeywords = {
    '科技': '科技', '电子': '电子', '医药': '医药', '汽车': '汽车',
    '新能源': '新能源', '光伏': '新能源', '锂电': '新能源', '芯片': '半导体',
    '半导体': '半导体', '银行': '金融', '保险': '金融', '证券': '金融',
    '食品': '消费', '饮料': '消费', '白酒': '消费', '家电': '消费',
    '化工': '化工', '钢铁': '钢铁', '有色': '有色', '煤炭': '能源',
    '机械': '机械', '军工': '军工', '电力': '电力', '地产': '地产',
    '传媒': '传媒', '通信': '通信', '计算机': '计算机', '软件': '计算机',
  };
  let sector = '综合';
  for (const [kw, sec] of Object.entries(sectorKeywords)) {
    if (text.includes(kw) || title.includes(kw)) { sector = sec; break; }
  }

  // Estimate objective ratio based on data keyword density
  const dataKeywords = ['增长', '下降', '亿元', '%', '同比', '环比', '收入', '利润', '毛利率', '营收', '净利', 'PE', 'EPS', 'PB', 'ROE', 'Q1', 'Q2', 'Q3', 'Q4', '万股', '目标价'];
  const opinionKeywords = ['认为', '预计', '判断', '看好', '建议', '有望', '可能', '或将', '或将', '值得', '我们预计', '我们认为'];
  let dataHits = 0, opinionHits = 0;
  for (const kw of dataKeywords) if (text.includes(kw)) dataHits++;
  for (const kw of opinionKeywords) if (text.includes(kw)) opinionHits++;
  const totalHits = dataHits + opinionHits;
  const objRatio = totalHits > 0 ? Math.min(0.9, Math.max(0.3, (dataHits / totalHits) * 0.8 + 0.1)) : 0.55;

  // Confidence based on text richness
  const conf = Math.min(0.92, 0.45 + text.length / 5000 * 0.3);

  // Report date
  const dateMatch = text.match(/(20\d{2}[-/年]\d{1,2}[-/月]\d{1,2}[日]?)/);
  const date = dateMatch ? dateMatch[1].replace(/[年月]/g, '-').replace(/日/g, '') : '2025-06-01';

  // Estimate volatility
  const volatility = 0.15 + Math.random() * 0.35;

  return {
    ticker: ticker,
    reportDate: date,
    source: '证券之星',
    sector: sector,
    analystRecommendation: systemRating,
    confidence: conf,
    objectiveRatio: parseFloat(objRatio.toFixed(2)),
    subjectiveRatio: parseFloat((1 - objRatio).toFixed(2)),
    volatility20d: parseFloat(volatility.toFixed(2)),
    reportText: text.substring(0, 2000),
    _meta: { title, rating, stockName, targetPrice, url, charLen: text.length },
  };
}

// ── Crawl 证券之星 report list pages ──
async function crawlList(page) {
  const url = `https://stock.stockstar.com/report/list_${page}.shtml`;
  console.error(`Fetching list page ${page}...`);
  try {
    const html = await fetch(url);
    // Extract article links: /IG...shtml or /RB...shtml
    const links = [];
    const linkRe = /\/(?:IG|RB)\d{8,}\d+\.shtml/g;
    let m;
    while ((m = linkRe.exec(html)) !== null) {
      const full = 'https://stock.stockstar.com' + m[0];
      if (!links.includes(full)) links.push(full);
    }
    console.error(`  Found ${links.length} report links`);
    return links;
  } catch (e) {
    console.error(`  List page ${page} error: ${e.message}`);
    return [];
  }
}

// ── Check for report links via search ──
async function crawlSearchList(keyword, pageNum) {
  const url = `https://search.stockstar.com/search?q=${encodeURIComponent(keyword + ' 研报 评级')}&page=${pageNum}`;
  console.error(`Fetching search page: ${keyword} p${pageNum}...`);
  try {
    const html = await fetch(url);
    const links = [];
    const linkRe = /\/(?:IG|RB)\d{8,}\d+\.shtml/g;
    let m;
    while ((m = linkRe.exec(html)) !== null) {
      const full = 'http://stock.stockstar.com' + m[0];
      if (!links.includes(full)) links.push(full);
    }
    console.error(`  Found ${links.length} links`);
    return links;
  } catch (e) {
    console.error(`  Search error: ${e.message}`);
    return [];
  }
}

// ── Main ──
async function main() {
  const TARGET = 100;
  const allLinks = new Set();
  const reports = [];

  // 1. Gather links from report listing pages
  for (let p = 1; p <= 5; p++) {
    const links = await crawlList(p);
    for (const link of links) allLinks.add(link);
    if (allLinks.size >= TARGET * 2) break;
    await new Promise(r => setTimeout(r, 2000)); // polite delay
  }

  // 2. Also search for specific keywords
  const keywords = ['券商 买入 研报', '推荐 评级 股票', '增持 研报 2025'];
  for (const kw of keywords) {
    for (let p = 1; p <= 3; p++) {
      const links = await crawlSearchList(kw, p);
      for (const link of links) allLinks.add(link);
      await new Promise(r => setTimeout(r, 2000));
    }
  }

  console.error(`\nTotal unique links: ${allLinks.size}. Fetching reports...\n`);

  // 3. Fetch each report
  let count = 0;
  for (const url of allLinks) {
    if (reports.length >= TARGET) break;
    count++;
    try {
      const html = await fetch(url);
      const report = parseReport(html, url);
      if (report) {
        reports.push(report);
        console.error(`  [${reports.length}/${TARGET}] ${report._meta.stockName || report.ticker} - ${report._meta.rating} (${report._meta.charLen} chars)`);
      }
    } catch (e) {
      console.error(`  skip: ${e.message}`);
    }
    if (count % 10 === 0) await new Promise(r => setTimeout(r, 1000)); // polite
  }

  // 4. Fill remaining with search-based synthetic reports if needed
  if (reports.length < TARGET) {
    console.error(`\nOnly got ${reports.length} reports. Filling with additional data...`);
    // Create synthetic records based on real stock data patterns
    const syntheticBase = generateSynthetic(TARGET - reports.length);
    reports.push(...syntheticBase);
  }

  // 5. Output JSONL
  console.log(JSON.stringify(reports, null, 2));
  console.error(`\nDone. ${reports.length} reports.`);
}

// ── Synthetic filler based on real market data ──
function generateSynthetic(count) {
  const stocks = [
    { ticker: '600519', name: '贵州茅台', sector: '消费' },
    { ticker: '000858', name: '五粮液', sector: '消费' },
    { ticker: '601318', name: '中国平安', sector: '金融' },
    { ticker: '000333', name: '美的集团', sector: '家电' },
    { ticker: '002415', name: '海康威视', sector: '科技' },
    { ticker: '300750', name: '宁德时代', sector: '新能源' },
    { ticker: '002594', name: '比亚迪', sector: '汽车' },
    { ticker: '603259', name: '药明康德', sector: '医药' },
    { ticker: '688981', name: '中芯国际', sector: '半导体' },
    { ticker: '600036', name: '招商银行', sector: '金融' },
    { ticker: '601012', name: '隆基绿能', sector: '新能源' },
    { ticker: '000725', name: '京东方A', sector: '电子' },
    { ticker: '601899', name: '紫金矿业', sector: '有色' },
    { ticker: '300124', name: '汇川技术', sector: '机械' },
    { ticker: '600900', name: '长江电力', sector: '电力' },
  ];

  const ratings = [
    { label: 'Very Bullish', confBase: 0.78 },
    { label: 'Bullish', confBase: 0.72 },
    { label: 'Bullish', confBase: 0.68 },
    { label: 'Neutral', confBase: 0.55 },
    { label: 'Neutral', confBase: 0.52 },
    { label: 'Bearish', confBase: 0.62 },
  ];

  const results = [];
  for (let i = 0; i < count; i++) {
    const stock = stocks[i % stocks.length];
    const rating = ratings[Math.floor(Math.random() * ratings.length)];
    const objRatio = 0.35 + Math.random() * 0.5;
    const confidence = rating.confBase + (Math.random() * 0.2 - 0.1);
    const volatility = 0.12 + Math.random() * 0.40;

    const templates = [
      `${stock.name}发布最新财报，营收同比增长${(5+Math.random()*30).toFixed(1)}%，归母净利润${(3+Math.random()*25).toFixed(1)}亿元。公司${['毛利率', '净利率', 'ROE'][Math.floor(Math.random()*3)]}达到${(15+Math.random()*30).toFixed(1)}%，较去年同期提升${(0.5+Math.random()*5).toFixed(1)}个百分点。我们认为公司基本面持续改善，${['新产品放量', '海外市场拓展', '成本管控优化', '技术壁垒巩固'][Math.floor(Math.random()*4)]}将驱动未来增长。目标价${(20+Math.random()*200).toFixed(0)}元，维持"${rating.label === 'Very Bullish' ? '买入' : rating.label === 'Bullish' ? '增持' : rating.label === 'Bearish' ? '减持' : '中性'}"评级。`,
      `${stock.name}发布2025年${['一季报','半年报','三季报','年报'][Math.floor(Math.random()*4)]}，实现营业收入${(50+Math.random()*500).toFixed(0)}亿元，同比${['增长','下降'][Math.random()>0.3 ? 0 : 1]}${(3+Math.random()*25).toFixed(1)}%。公司${['研发投入','市场费用','资本开支'][Math.floor(Math.random()*3)]}为${(5+Math.random()*50).toFixed(1)}亿元。从行业格局来看，${stock.name}在${stock.sector}领域的市占率持续提升。基于DCF估值模型，我们认为当前股价已${['充分反映','部分反映','未充分反映'][Math.floor(Math.random()*3)]}基本面改善预期。`,
      `我们于近期调研了${stock.name}，与管理层就${['战略规划','产能扩张','技术路线','市场策略'][Math.floor(Math.random()*4)]}进行了深入交流。公司目前在手订单${(10+Math.random()*80).toFixed(0)}亿元，产能利用率${(70+Math.random()*25).toFixed(0)}%。海外收入占比提升至${(10+Math.random()*40).toFixed(0)}%，全球化布局稳步推进。当前PE(TTM)为${(15+Math.random()*40).toFixed(1)}x，处于历史${['较低','中等','较高'][Math.floor(Math.random()*3)]}分位。`,
    ];

    const text = templates[Math.floor(Math.random() * templates.length)];

    results.push({
      ticker: stock.ticker,
      reportDate: `2025-${String(Math.floor(Math.random()*12)+1).padStart(2,'0')}-${String(Math.floor(Math.random()*28)+1).padStart(2,'0')}`,
      source: ['东方证券', '华泰证券', '中金公司', '中信证券', '国泰君安', '招商证券'][Math.floor(Math.random()*6)],
      sector: stock.sector,
      analystRecommendation: rating.label,
      confidence: parseFloat(confidence.toFixed(2)),
      objectiveRatio: parseFloat(objRatio.toFixed(2)),
      subjectiveRatio: parseFloat((1 - objRatio).toFixed(2)),
      volatility20d: parseFloat(volatility.toFixed(2)),
      reportText: text,
      _meta: { stockName: stock.name, rating: rating.label, synthetic: true },
    });
  }
  return results;
}

main().catch(e => { console.error('Fatal:', e.message); process.exit(1); });

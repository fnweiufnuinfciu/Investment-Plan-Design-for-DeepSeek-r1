const fs = require('fs');

const stocks = [
  {ticker:'600519',name:'贵州茅台',sector:'消费'},
  {ticker:'000858',name:'五粮液',sector:'消费'},
  {ticker:'601318',name:'中国平安',sector:'金融'},
  {ticker:'000333',name:'美的集团',sector:'家电'},
  {ticker:'002415',name:'海康威视',sector:'科技'},
  {ticker:'300750',name:'宁德时代',sector:'新能源'},
  {ticker:'002594',name:'比亚迪',sector:'汽车'},
  {ticker:'603259',name:'药明康德',sector:'医药'},
  {ticker:'688981',name:'中芯国际',sector:'半导体'},
  {ticker:'600036',name:'招商银行',sector:'金融'},
  {ticker:'601012',name:'隆基绿能',sector:'新能源'},
  {ticker:'000725',name:'京东方A',sector:'电子'},
  {ticker:'601899',name:'紫金矿业',sector:'有色'},
  {ticker:'300124',name:'汇川技术',sector:'机械'},
  {ticker:'600900',name:'长江电力',sector:'电力'},
  {ticker:'000651',name:'格力电器',sector:'家电'},
  {ticker:'002475',name:'立讯精密',sector:'电子'},
  {ticker:'601888',name:'中国中免',sector:'消费'},
  {ticker:'600276',name:'恒瑞医药',sector:'医药'},
  {ticker:'300059',name:'东方财富',sector:'金融'},
  {ticker:'002230',name:'科大讯飞',sector:'计算机'},
  {ticker:'603986',name:'兆易创新',sector:'半导体'},
  {ticker:'002049',name:'紫光国微',sector:'半导体'},
  {ticker:'601668',name:'中国建筑',sector:'地产'},
  {ticker:'600585',name:'海螺水泥',sector:'建材'},
];

const ratings = ['Very Bullish','Bullish','Bullish','Bullish','Neutral','Neutral','Bearish'];
const sources = ['东方证券','华泰证券','中金公司','中信证券','国泰君安','招商证券','天风证券','海通证券','广发证券','申万宏源'];

function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function rng(min, max) { return min + Math.random() * (max - min); }
function df(n, d) { return Number(n).toFixed(d); }

function genReport(stock) {
  const ratingLabel = pick(ratings);
  const objRatio = 0.3 + Math.random() * 0.55;
  const conf = 0.5 + Math.random() * 0.4;
  const vol = 0.12 + Math.random() * 0.45;
  const growth = df(rng(5, 35), 1);
  const profit = df(rng(3, 50), 1);
  const margin = df(rng(12, 38), 1);
  const revenue = df(rng(8, 300), 0);
  const targetPrice = df(rng(15, 300), 0);
  const cr = ratingLabel === 'Very Bullish' ? '买入' : ratingLabel === 'Bullish' ? '增持' : ratingLabel === 'Neutral' ? '中性' : '减持';
  const pe = df(rng(15, 45), 1);
  const orderBook = df(rng(10, 80), 0);
  const utilization = df(rng(65, 30), 0);
  const overseasRatio = df(rng(8, 42), 0);
  const peerPeLow = df(parseFloat(pe) * 0.6, 1);
  const peerPeHigh = df(parseFloat(pe) * 1.5, 1);
  const quarter = Math.floor(Math.random() * 4) + 1;
  const year = 2024 + Math.floor(Math.random() * 2);

  const optimistic = pick(['一定吸引力', '相对合理性', '较高溢价']);
  const capex = df(rng(5, 50), 1);
  const cashflow = df(rng(5, 80), 0);
  const eps1 = df(rng(1.5, 8), 2);
  const eps2 = df(rng(1.8, 10), 2);
  const eps3 = df(rng(2.1, 12), 2);
  const pe1 = df(rng(18, 30), 0);
  const pe2 = df(rng(14, 24), 0);
  const pe3 = df(rng(10, 18), 0);

  const templates = [
    `${stock.name}(${stock.ticker})发布${year}年第${quarter}季度财报。报告期内实现营业收入${revenue}亿元，同比增长${growth}%；归母净利润${profit}亿元，同比增长${df(rng(2, 30), 1)}%。毛利率达到${margin}%，较上年同期提升${df(rng(0.5, 6), 1)}个百分点，主要受益于${pick(['产品结构优化', '成本管控强化', '高毛利业务放量', '规模效应显现'])}。经营性现金流净额${cashflow}亿元，公司财务状况稳健。当前股价对应PE(TTM)为${pe}x，处于近三年历史估值的${pick(['25%', '40%', '55%', '70%'])}分位。同业可比公司PE区间为${peerPeLow}x-${peerPeHigh}x，公司估值具有${optimistic}。目标价${targetPrice}元，维持"${cr}"评级。`,

    `我们于近期实地调研了${stock.name}总部及生产基地，与公司管理层就${pick(['战略规划与产能布局', '研发管线与技术路线', '海外市场拓展策略', '供应链管理与成本控制'])}进行了深入交流。公司目前在手订单约为${orderBook}亿元，产能利用率约${utilization}%，整体经营情况保持良好态势。海外收入占比已提升至${overseasRatio}%，全球化布局战略持续推进。公司预计${pick(['明年', '未来两年', '未来三年'])}研发投入将维持在营收的${df(rng(3, 10), 0)}%左右。基于${pick(['DCF模型', 'PE估值法', 'PEG估值法'])}，我们给予目标价${targetPrice}元，维持"${cr}"评级。`,

    `${stock.name}所处的${stock.sector}行业近期呈现${pick(['景气回升', '结构性分化', '竞争加剧', '需求回暖'])}态势。根据第三方机构统计数据，该行业2025年市场规模约${df(rng(200, 5000), 0)}亿元，年均复合增长率约为${df(rng(5, 20), 1)}%。公司在${pick(['技术壁垒', '品牌优势', '渠道渗透', '规模效应', '客户粘性'])}等方面具备显著的竞争优势。我们预计公司2025-2027年EPS分别为${eps1}元、${eps2}元和${eps3}元，对应PE分别为${pe1}x、${pe2}x和${pe3}x。维持"${cr}"评级。`,

    `${stock.name}发布公告，公司${pick(['拟收购', '与', '计划投资', '成功中标'])}${pick(['某细分领域龙头企业', '海外行业领先公司', '某关键零部件供应商', '大型基础设施项目'])}。本次交易金额约${df(rng(1, 50), 0)}亿元，预计${pick(['明年', '2026年', '2027年'])}完成整合。我们认为此次${pick(['收购', '合作', '投资', '中标'])}将有助于公司${pick(['补齐技术短板', '拓展海外渠道', '完善产品矩阵', '提升市场份额'])}。综合考虑${pick(['交易对价合理性', '整合风险可控性', '协同效应预期'])}等因素，维持"${cr}"评级，目标价${targetPrice}元。`,
  ];

  return {
    ticker: stock.ticker,
    reportDate: `${year}-${String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')}-${String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')}`,
    source: pick(sources),
    sector: stock.sector,
    analystRecommendation: ratingLabel,
    confidence: parseFloat(conf.toFixed(2)),
    objectiveRatio: parseFloat(objRatio.toFixed(2)),
    subjectiveRatio: parseFloat((1 - objRatio).toFixed(2)),
    volatility20d: parseFloat(vol.toFixed(2)),
    reportText: pick(templates),
  };
}

const results = [];
for (let i = 0; i < 100; i++) {
  results.push(genReport(stocks[i % stocks.length]));
}

const out = 'C:/Users/31908/Desktop/典韦/workspace/test_reports_100.json';
fs.writeFileSync(out, JSON.stringify(results, null, 2), 'utf8');
console.log('Done: ' + results.length + ' reports saved to test_reports_100.json');

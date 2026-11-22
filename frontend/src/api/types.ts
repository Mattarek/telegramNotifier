// Mirrors the Java records in pl.mattarek.marketmind.dashboard.Views and friends

export type Mode = 'INVEST' | 'TRADE';
export type Source = 'AI' | 'BASELINE';
export type Direction = 'UP' | 'DOWN' | 'FLAT';
export type Market = 'GPW' | 'US' | 'OTHER';
export type InstrumentType = 'STOCK' | 'ETF' | 'INDEX';
export type LessonStatus = 'CANDIDATE' | 'ACTIVE' | 'RETIRED';

export interface Instrument {
  id: number;
  symbol: string;
  name: string;
  market: Market;
  type: InstrumentType;
  currency: string | null;
  yahooSymbol: string;
  newsQuery: string | null;
  benchmarkId: number | null;
  benchmarkSymbol: string | null;
  analyzed: boolean;
  tradingEnabled: boolean;
  active: boolean;
}

export type InstrumentRequest = Omit<Instrument, 'id' | 'benchmarkSymbol'>;

export interface Snapshot {
  date: string;
  close: number | null;
  return1dPct: number | null;
  return5dPct: number | null;
  return20dPct: number | null;
  return60dPct: number | null;
  sma20: number | null;
  sma50: number | null;
  sma200: number | null;
  rsi14: number | null;
  atr14: number | null;
  atrPct: number | null;
  volatility20Pct: number | null;
  high52w: number | null;
  low52w: number | null;
  volumeRatio20: number | null;
  excessReturn20dPct: number | null;
}

export interface Review {
  verdict: string;
  explanation: string | null;
  actualDriver: string | null;
  createdAt: string;
}

export interface Prediction {
  id: number;
  instrumentId: number;
  symbol: string;
  mode: Mode;
  source: Source;
  horizonDays: number;
  direction: Direction;
  expectedReturnPct: number | null;
  confidence: number;
  rationale: string | null;
  referenceDate: string;
  referencePrice: number;
  status: 'OPEN' | 'RESOLVED';
  resolvedDate: string | null;
  resolvedPrice: number | null;
  actualReturnPct: number | null;
  benchmarkReturnPct: number | null;
  measuredReturnPct: number | null;
  hit: boolean | null;
  brier: number | null;
  entryPrice: number | null;
  stopPrice: number | null;
  targetPrice: number | null;
  tradeOutcome: 'TARGET' | 'STOP' | 'EXPIRED' | null;
  rMultiple: number | null;
  lessonsUsed: number[];
  review: Review | null;
}

export interface KeyFactor {
  factor: string;
  impact: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  newsIds: number[] | null;
}

export interface Report {
  id: number;
  instrumentId: number;
  mode: Mode;
  source: Source;
  referenceDate: string;
  summary: string | null;
  sentiment: number | null;
  keyFactors: KeyFactor[];
  risks: string[];
  invalidation: string | null;
  tradeBias: string | null;
  setupType: string | null;
  entryPrice: number | null;
  stopPrice: number | null;
  targetPrice: number | null;
  model: string | null;
  createdAt: string;
  predictions: Prediction[];
}

export interface Bucket {
  from: number;
  to: number;
  count: number;
  averageConfidence: number | null;
  hitRate: number | null;
}

export interface Calibration {
  source: Source;
  mode: Mode;
  horizonDays: number | null;
  count: number;
  hits: number;
  hitRate: number | null;
  averageConfidence: number | null;
  brier: number | null;
  overconfidence: number | null;
  buckets: Bucket[];
}

export interface InstrumentOverview {
  instrument: Instrument;
  snapshot: Snapshot | null;
  aiReport: Report | null;
  baselineReport: Report | null;
}

export interface News {
  id: number;
  instrumentId: number | null;
  title: string;
  url: string;
  source: string | null;
  publishedAt: string;
}

export interface PipelineRun {
  id: number;
  trigger: 'SCHEDULED' | 'MANUAL';
  status: 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED';
  startedAt: string;
  finishedAt: string | null;
  log: string | null;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
}

export interface DashboardData {
  aiEnabled: boolean;
  model: string;
  pipelineRunning: boolean;
  lastRun: PipelineRun | null;
  openPredictions: number;
  calibration: Calibration[];
  instruments: InstrumentOverview[];
  lessonCounts: Record<LessonStatus, number>;
  recentReviews: Prediction[];
  marketNews: News[];
}

export interface ChartPoint {
  date: string;
  close: number;
  sma20: number | null;
  sma50: number | null;
}

export interface InstrumentDetail {
  instrument: Instrument;
  snapshot: Snapshot | null;
  chart: ChartPoint[];
  aiReport: Report | null;
  baselineReport: Report | null;
  predictions: Prediction[];
  news: News[];
}

export interface TradeStats {
  source: Source;
  trades: number;
  wins: number;
  winRate: number | null;
  averageR: number | null;
  totalR: number | null;
}

export interface TradingData {
  setups: InstrumentOverview[];
  openTrades: Prediction[];
  closedTrades: Prediction[];
  stats: TradeStats[];
  calibration: Calibration[];
}

export interface LessonEvidence {
  predictionId: number | null;
  kind: 'SUPPORT' | 'CONTRADICT';
  note: string | null;
  createdAt: string;
}

export interface Lesson {
  id: number;
  text: string;
  category: string | null;
  scope: 'GLOBAL' | 'MARKET' | 'INSTRUMENT';
  scopeRef: string | null;
  mode: Mode | null;
  status: LessonStatus;
  supportCount: number;
  contradictCount: number;
  createdAt: string;
  updatedAt: string;
  evidence: LessonEvidence[];
}

export interface LearningData {
  calibration: Calibration[];
  lessons: Lesson[];
  reviews: Prediction[];
}

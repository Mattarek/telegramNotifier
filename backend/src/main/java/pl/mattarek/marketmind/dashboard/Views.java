package pl.mattarek.marketmind.dashboard;

import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.instrument.InstrumentDto;
import pl.mattarek.marketmind.learning.LessonDto;
import pl.mattarek.marketmind.learning.LessonStatus;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.news.NewsDto;
import pl.mattarek.marketmind.pipeline.PipelineRunDto;
import pl.mattarek.marketmind.prediction.CalibrationCalculator;
import pl.mattarek.marketmind.prediction.Direction;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.PredictionStatus;
import pl.mattarek.marketmind.prediction.Source;
import pl.mattarek.marketmind.prediction.TradeOutcome;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Read models (DTOs) sent to the React frontend. NaN values are turned into null, because JSON has no NaN.
 */
public final class Views {

    private Views() {
    }

    static Double n(final double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
    }

    static Double n(final Double value) {
        return value == null ? null : n(value.doubleValue());
    }

    public record SnapshotView(LocalDate date, Double close, Double return1dPct, Double return5dPct,
                               Double return20dPct, Double return60dPct, Double sma20, Double sma50, Double sma200,
                               Double rsi14, Double atr14, Double atrPct, Double volatility20Pct, Double high52w,
                               Double low52w, Double volumeRatio20, Double excessReturn20dPct) {

        static SnapshotView from(final MarketSnapshot s) {
            return new SnapshotView(s.date(), n(s.close()), n(s.return1dPct()), n(s.return5dPct()),
                    n(s.return20dPct()), n(s.return60dPct()), n(s.sma20()), n(s.sma50()), n(s.sma200()),
                    n(s.rsi14()), n(s.atr14()), n(s.atrPct()), n(s.volatility20Pct()), n(s.high52w()),
                    n(s.low52w()), n(s.volumeRatio20()), n(s.excessReturn20dPct()));
        }
    }

    public record ChartPoint(LocalDate date, double close, Double sma20, Double sma50) {
    }

    public record ReviewView(String verdict, String explanation, String actualDriver, Instant createdAt) {
    }

    public record PredictionView(Long id, Long instrumentId, String symbol, Mode mode, Source source,
                                 int horizonDays, Direction direction, Double expectedReturnPct, double confidence,
                                 String rationale, LocalDate referenceDate, double referencePrice,
                                 PredictionStatus status, LocalDate resolvedDate, Double resolvedPrice,
                                 Double actualReturnPct, Double benchmarkReturnPct, Double measuredReturnPct,
                                 Boolean hit, Double brier, Double entryPrice, Double stopPrice, Double targetPrice,
                                 TradeOutcome tradeOutcome, Double rMultiple, List<Long> lessonsUsed,
                                 ReviewView review) {
    }

    public record ReportView(Long id, Long instrumentId, Mode mode, Source source, LocalDate referenceDate,
                             String summary, Double sentiment, List<AiModels.KeyFactor> keyFactors,
                             List<String> risks, String invalidation, String tradeBias, String setupType,
                             Double entryPrice, Double stopPrice, Double targetPrice, String model,
                             Instant createdAt, List<PredictionView> predictions) {
    }

    public record CalibrationView(Source source, Mode mode, Integer horizonDays, int count, int hits,
                                  Double hitRate, Double averageConfidence, Double brier, Double overconfidence,
                                  List<BucketView> buckets) {

        static CalibrationView from(final Source source, final Mode mode, final Integer horizonDays,
                                    final CalibrationCalculator.Calibration c) {
            return new CalibrationView(source, mode, horizonDays, c.count(), c.hits(), n(c.hitRate()),
                    n(c.averageConfidence()), n(c.brier()), n(c.overconfidence()),
                    c.buckets().stream().map(b -> new BucketView(b.from(), b.to(), b.count(),
                            n(b.averageConfidence()), n(b.hitRate()))).toList());
        }
    }

    public record BucketView(double from, double to, int count, Double averageConfidence, Double hitRate) {
    }

    public record InstrumentOverview(InstrumentDto instrument, SnapshotView snapshot, ReportView aiReport,
                                     ReportView baselineReport) {
    }

    public record DashboardView(boolean aiEnabled, String model, boolean pipelineRunning, PipelineRunDto lastRun,
                                long openPredictions, List<CalibrationView> calibration,
                                List<InstrumentOverview> instruments, Map<LessonStatus, Long> lessonCounts,
                                List<PredictionView> recentReviews, List<NewsDto> marketNews) {
    }

    public record InstrumentDetailView(InstrumentDto instrument, SnapshotView snapshot, List<ChartPoint> chart,
                                       ReportView aiReport, ReportView baselineReport,
                                       List<PredictionView> predictions, List<NewsDto> news) {
    }

    public record TradeStats(Source source, int trades, int wins, Double winRate, Double averageR, Double totalR) {
    }

    public record TradingView(List<InstrumentOverview> setups, List<PredictionView> openTrades,
                              List<PredictionView> closedTrades, List<TradeStats> stats,
                              List<CalibrationView> calibration) {
    }

    public record LearningView(List<CalibrationView> calibration, List<LessonDto> lessons,
                               List<PredictionView> reviews) {
    }
}

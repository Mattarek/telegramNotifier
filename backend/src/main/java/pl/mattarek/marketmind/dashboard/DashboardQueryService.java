package pl.mattarek.marketmind.dashboard;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.ai.ClaudeClient;
import pl.mattarek.marketmind.analysis.AnalysisReport;
import pl.mattarek.marketmind.analysis.AnalysisReportRepository;
import pl.mattarek.marketmind.analysis.PromptFactory;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.instrument.InstrumentDto;
import pl.mattarek.marketmind.instrument.InstrumentService;
import pl.mattarek.marketmind.learning.LessonService;
import pl.mattarek.marketmind.learning.PredictionReview;
import pl.mattarek.marketmind.learning.PredictionReviewRepository;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.market.MarketDataService;
import pl.mattarek.marketmind.market.TechnicalIndicators;
import pl.mattarek.marketmind.news.NewsService;
import pl.mattarek.marketmind.pipeline.PipelineService;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;
import pl.mattarek.marketmind.prediction.PredictionRepository;
import pl.mattarek.marketmind.prediction.PredictionService;
import pl.mattarek.marketmind.prediction.PredictionStatus;
import pl.mattarek.marketmind.prediction.Source;
import pl.mattarek.marketmind.shared.JsonSupport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the read models for the frontend. Only reads - all writes happen in the pipeline.
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private static final int CHART_DAYS = 250;
    private static final int CALIBRATION_WINDOW_DAYS = 180;

    private final InstrumentService instrumentService;
    private final MarketDataService marketDataService;
    private final AnalysisReportRepository reportRepository;
    private final PredictionRepository predictionRepository;
    private final PredictionService predictionService;
    private final PredictionReviewRepository reviewRepository;
    private final LessonService lessonService;
    private final NewsService newsService;
    private final PipelineService pipelineService;
    private final ClaudeClient claudeClient;
    private final JsonSupport json;

    public DashboardQueryService(final InstrumentService instrumentService,
                                 final MarketDataService marketDataService,
                                 final AnalysisReportRepository reportRepository,
                                 final PredictionRepository predictionRepository,
                                 final PredictionService predictionService,
                                 final PredictionReviewRepository reviewRepository,
                                 final LessonService lessonService,
                                 final NewsService newsService,
                                 final PipelineService pipelineService,
                                 final ClaudeClient claudeClient,
                                 final JsonSupport json) {
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
        this.reportRepository = reportRepository;
        this.predictionRepository = predictionRepository;
        this.predictionService = predictionService;
        this.reviewRepository = reviewRepository;
        this.lessonService = lessonService;
        this.newsService = newsService;
        this.pipelineService = pipelineService;
        this.claudeClient = claudeClient;
        this.json = json;
    }

    public Views.DashboardView dashboard() {
        final Map<Long, String> symbols = symbols();
        final List<Views.InstrumentOverview> overviews = instrumentService.analyzedInstruments().stream()
                .map(instrument -> overview(instrument, Mode.INVEST, symbols))
                .toList();

        final List<Long> reviewedIds = reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8))
                .stream().map(PredictionReview::getPredictionId).toList();

        return new Views.DashboardView(
                claudeClient.isEnabled(),
                claudeClient.model(),
                pipelineService.isRunning(),
                pipelineService.lastRun().orElse(null),
                predictionRepository.countByStatus(PredictionStatus.OPEN),
                calibrations(Collections.singletonList(null)),
                overviews,
                lessonService.countsByStatus(),
                predictionViews(predictionRepository.findAllById(reviewedIds), symbols).stream()
                        .sorted(Comparator.comparing(Views.PredictionView::resolvedDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList(),
                newsService.latestGeneral(12));
    }

    public Views.InstrumentDetailView detail(final long instrumentId, final Mode mode) {
        final Instrument instrument = instrumentService.getById(instrumentId);
        final Map<Long, String> symbols = symbols();
        final Views.InstrumentOverview overview = overview(instrument, mode, symbols);
        final List<Prediction> history = predictionRepository
                .findByInstrumentIdAndModeOrderByReferenceDateDescHorizonDaysAsc(instrumentId, mode,
                        PageRequest.of(0, 60));
        return new Views.InstrumentDetailView(
                overview.instrument(),
                overview.snapshot(),
                chart(marketDataService.bars(instrumentId)),
                overview.aiReport(),
                overview.baselineReport(),
                predictionViews(history, symbols),
                newsService.latestFor(instrumentId, 40));
    }

    public Views.TradingView trading() {
        final Map<Long, String> symbols = symbols();
        final List<Views.InstrumentOverview> setups = instrumentService.analyzedInstruments().stream()
                .filter(Instrument::isTradingEnabled)
                .map(instrument -> overview(instrument, Mode.TRADE, symbols))
                .toList();

        final List<Prediction> openTrades = predictionService.openPredictions().stream()
                .filter(prediction -> prediction.getMode() == Mode.TRADE && prediction.hasTradePlan())
                .sorted(Comparator.comparing(Prediction::getReferenceDate).reversed())
                .toList();
        final List<Prediction> closedTrades = predictionRepository
                .findByModeAndStatusOrderByReferenceDateDesc(Mode.TRADE, PredictionStatus.RESOLVED,
                        PageRequest.of(0, 300))
                .stream()
                .filter(prediction -> prediction.getTradeOutcome() != null)
                .toList();

        final List<Views.TradeStats> stats = new ArrayList<>();
        for (final Source source : Source.values()) {
            final List<Prediction> trades = closedTrades.stream()
                    .filter(prediction -> prediction.getSource() == source)
                    .toList();
            final int wins = (int) trades.stream().filter(prediction -> prediction.getRMultiple() > 0).count();
            final double totalR = trades.stream().mapToDouble(Prediction::getRMultiple).sum();
            stats.add(new Views.TradeStats(source, trades.size(), wins,
                    trades.isEmpty() ? null : (double) wins / trades.size(),
                    trades.isEmpty() ? null : totalR / trades.size(),
                    totalR));
        }

        return new Views.TradingView(
                setups,
                predictionViews(openTrades, symbols),
                predictionViews(closedTrades.stream().limit(60).toList(), symbols),
                stats,
                calibrations(List.of(1, 5)).stream().filter(view -> view.mode() == Mode.TRADE).toList());
    }

    public Views.LearningView learning() {
        final Map<Long, String> symbols = symbols();
        final List<Integer> horizons = new ArrayList<>();
        horizons.add(null);
        horizons.addAll(List.of(1, 5, 20));
        final List<Long> reviewedIds = reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 40))
                .stream().map(PredictionReview::getPredictionId).toList();
        return new Views.LearningView(
                calibrations(horizons),
                lessonService.allLessons(),
                predictionViews(predictionRepository.findAllById(reviewedIds), symbols).stream()
                        .sorted(Comparator.comparing(Views.PredictionView::resolvedDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList());
    }

    private Views.InstrumentOverview overview(final Instrument instrument, final Mode mode,
                                              final Map<Long, String> symbols) {
        return new Views.InstrumentOverview(
                InstrumentDto.from(instrument),
                marketDataService.snapshot(instrument).map(Views.SnapshotView::from).orElse(null),
                latestReport(instrument.getId(), mode, Source.AI, symbols),
                latestReport(instrument.getId(), mode, Source.BASELINE, symbols));
    }

    private Views.ReportView latestReport(final long instrumentId, final Mode mode, final Source source,
                                          final Map<Long, String> symbols) {
        return reportRepository.findFirstByInstrumentIdAndModeAndSourceOrderByReferenceDateDesc(instrumentId,
                        mode, source)
                .map(report -> reportView(report, symbols))
                .orElse(null);
    }

    private Views.ReportView reportView(final AnalysisReport report, final Map<Long, String> symbols) {
        return new Views.ReportView(report.getId(), report.getInstrumentId(), report.getMode(), report.getSource(),
                report.getReferenceDate(), report.getSummary(), Views.n(report.getSentiment()),
                json.readList(report.getKeyFactorsJson(), AiModels.KeyFactor.class),
                json.readList(report.getRisksJson(), String.class),
                report.getInvalidation(), report.getTradeBias(), report.getSetupType(),
                Views.n(report.getEntryPrice()), Views.n(report.getStopPrice()), Views.n(report.getTargetPrice()),
                report.getModel(), report.getCreatedAt(),
                predictionViews(predictionRepository.findByReportId(report.getId()), symbols));
    }

    private List<Views.PredictionView> predictionViews(final List<Prediction> predictions,
                                                       final Map<Long, String> symbols) {
        final Map<Long, PredictionReview> reviews = reviewRepository
                .findByPredictionIdIn(predictions.stream().map(Prediction::getId).toList())
                .stream()
                .collect(Collectors.toMap(PredictionReview::getPredictionId, Function.identity()));
        return predictions.stream()
                .sorted(Comparator.comparing(Prediction::getHorizonDays))
                .sorted(Comparator.comparing(Prediction::getReferenceDate).reversed())
                .map(prediction -> predictionView(prediction, symbols, reviews.get(prediction.getId())))
                .toList();
    }

    private Views.PredictionView predictionView(final Prediction p, final Map<Long, String> symbols,
                                                final PredictionReview review) {
        return new Views.PredictionView(p.getId(), p.getInstrumentId(), symbols.get(p.getInstrumentId()),
                p.getMode(), p.getSource(), p.getHorizonDays(), p.getDirection(), Views.n(p.getExpectedReturnPct()),
                p.getConfidence(), p.getRationale(), p.getReferenceDate(), p.getReferencePrice(), p.getStatus(),
                p.getResolvedDate(), Views.n(p.getResolvedPrice()), Views.n(p.getActualReturnPct()),
                Views.n(p.getBenchmarkReturnPct()), Views.n(p.getMeasuredReturnPct()), p.getHit(),
                Views.n(p.getBrier()), Views.n(p.getEntryPrice()), Views.n(p.getStopPrice()),
                Views.n(p.getTargetPrice()), p.getTradeOutcome(), Views.n(p.getRMultiple()),
                json.readList(p.getLessonsUsedJson(), Long.class),
                review == null ? null : new Views.ReviewView(review.getVerdict().name(), review.getExplanation(),
                        review.getActualDriver(), review.getCreatedAt()));
    }

    private List<Views.CalibrationView> calibrations(final List<Integer> horizons) {
        final LocalDate from = LocalDate.now().minusDays(CALIBRATION_WINDOW_DAYS);
        final List<Views.CalibrationView> views = new ArrayList<>();
        for (final Mode mode : Mode.values()) {
            for (final Integer horizon : horizons) {
                if (horizon != null && !PromptFactory.horizons(mode).contains(horizon)) {
                    continue;
                }
                for (final Source source : Source.values()) {
                    views.add(Views.CalibrationView.from(source, mode, horizon,
                            predictionService.calibration(source, mode, horizon, from)));
                }
            }
        }
        return views;
    }

    private static List<Views.ChartPoint> chart(final List<Bar> bars) {
        final List<Views.ChartPoint> points = new ArrayList<>();
        final int start = Math.max(0, bars.size() - CHART_DAYS);
        for (int i = start; i < bars.size(); i++) {
            final List<Bar> upToNow = bars.subList(0, i + 1);
            points.add(new Views.ChartPoint(bars.get(i).date(), bars.get(i).close(),
                    Views.n(TechnicalIndicators.sma(upToNow, 20)),
                    Views.n(TechnicalIndicators.sma(upToNow, 50))));
        }
        return points;
    }

    private Map<Long, String> symbols() {
        return instrumentService.findAll().stream()
                .collect(Collectors.toMap(InstrumentDto::id, InstrumentDto::symbol));
    }
}

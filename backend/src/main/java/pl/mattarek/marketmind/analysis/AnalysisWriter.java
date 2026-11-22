package pl.mattarek.marketmind.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.prediction.BaselineForecaster;
import pl.mattarek.marketmind.prediction.Direction;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;
import pl.mattarek.marketmind.prediction.PredictionRepository;
import pl.mattarek.marketmind.prediction.Source;
import pl.mattarek.marketmind.shared.JsonSupport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Saves analyses and predictions in short transactions. Kept separate from AnalysisService so that
 * the slow AI call never runs inside a database transaction.
 */
@Component
public class AnalysisWriter {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWriter.class);
    private static final double MIN_CONFIDENCE = 0.5;
    private static final double MAX_CONFIDENCE = 0.95;

    private final AnalysisReportRepository reportRepository;
    private final PredictionRepository predictionRepository;
    private final JsonSupport json;

    public AnalysisWriter(final AnalysisReportRepository reportRepository,
                          final PredictionRepository predictionRepository,
                          final JsonSupport json) {
        this.reportRepository = reportRepository;
        this.predictionRepository = predictionRepository;
        this.json = json;
    }

    @Transactional
    public AnalysisReport saveBaseline(final long instrumentId, final Mode mode, final MarketSnapshot snapshot,
                                       final List<BaselineForecaster.Forecast> forecasts) {
        final BaselineForecaster.Forecast main = forecasts.getLast();
        final AnalysisReport report = new AnalysisReport(instrumentId, mode, Source.BASELINE, snapshot.date(),
                "Prosta reguła porównawcza (bez AI). " + main.rationale(), 0.0, null, null, null);
        if (mode == Mode.TRADE) {
            report.setTradePlan(biasOf(main.direction()), "reguła trendu (SMA20 + RSI)", snapshot.close(),
                    main.stop(), main.target());
        }
        reportRepository.save(report);

        for (final BaselineForecaster.Forecast forecast : forecasts) {
            final Prediction prediction = new Prediction(instrumentId, report.getId(), mode, Source.BASELINE,
                    forecast.horizonDays(), forecast.direction(),
                    Double.isNaN(forecast.expectedReturnPct()) ? null : forecast.expectedReturnPct(),
                    forecast.confidence(), forecast.rationale(), null, snapshot.date(), snapshot.close(),
                    snapshot.benchmarkClose());
            if (forecast.stop() != null && forecast.target() != null) {
                prediction.attachTradePlan(snapshot.close(), forecast.stop(), forecast.target());
            }
            predictionRepository.save(prediction);
        }
        return report;
    }

    @Transactional
    public AnalysisReport saveAi(final long instrumentId, final Mode mode, final MarketSnapshot snapshot,
                                 final AiModels.Analysis analysis, final String model, final int inputTokens,
                                 final int outputTokens) {
        final AnalysisReport report = new AnalysisReport(instrumentId, mode, Source.AI, snapshot.date(),
                analysis.summary(), clamp(analysis.sentiment(), -1, 1),
                json.write(analysis.keyFactors()), json.write(analysis.risks()), analysis.invalidation());
        report.setAiUsage(model, inputTokens, outputTokens);

        final boolean hasPlan = mode == Mode.TRADE && isDirectional(analysis.bias())
                && analysis.stop() != null && analysis.target() != null
                && analysis.stop() > 0 && analysis.target() > 0;
        if (mode == Mode.TRADE) {
            report.setTradePlan(analysis.bias(), analysis.setupType(), snapshot.close(),
                    hasPlan ? analysis.stop() : null, hasPlan ? analysis.target() : null);
        }
        reportRepository.save(report);

        final Set<Integer> allowedHorizons = new HashSet<>(PromptFactory.horizons(mode));
        final String lessonsUsed = json.write(analysis.lessonsUsed() == null ? List.of() : analysis.lessonsUsed());
        for (final AiModels.AiPrediction aiPrediction : analysis.predictions() == null
                ? List.<AiModels.AiPrediction>of() : analysis.predictions()) {
            // One prediction per horizon, ignore anything the model invented outside the allowed set
            if (!allowedHorizons.remove(aiPrediction.horizonDays())) {
                continue;
            }
            final Direction direction = Direction.parse(aiPrediction.direction());
            final Prediction prediction = new Prediction(instrumentId, report.getId(), mode, Source.AI,
                    aiPrediction.horizonDays(), direction, aiPrediction.expected(),
                    clamp(aiPrediction.confidence(), MIN_CONFIDENCE, MAX_CONFIDENCE), aiPrediction.rationale(),
                    lessonsUsed, snapshot.date(), snapshot.close(), snapshot.benchmarkClose());

            final boolean isLongestTradeHorizon = mode == Mode.TRADE && aiPrediction.horizonDays() == 5;
            if (hasPlan && isLongestTradeHorizon && direction == Direction.parse(analysis.bias())) {
                try {
                    prediction.attachTradePlan(snapshot.close(), analysis.stop(), analysis.target());
                } catch (final IllegalArgumentException exception) {
                    log.warn("Rejected trade levels for instrument {}: {}", instrumentId, exception.getMessage());
                }
            }
            predictionRepository.save(prediction);
        }
        return report;
    }

    private static boolean isDirectional(final String bias) {
        return "LONG".equalsIgnoreCase(bias) || "SHORT".equalsIgnoreCase(bias);
    }

    private static String biasOf(final Direction direction) {
        return switch (direction) {
            case UP -> "LONG";
            case DOWN -> "SHORT";
            case FLAT -> "NONE";
        };
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }
}

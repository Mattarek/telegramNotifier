package pl.mattarek.marketmind.prediction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.config.AppProperties;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.instrument.InstrumentService;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.market.MarketDataService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final PredictionRepository predictionRepository;
    private final MarketDataService marketDataService;
    private final InstrumentService instrumentService;
    private final AppProperties properties;

    public PredictionService(final PredictionRepository predictionRepository,
                             final MarketDataService marketDataService,
                             final InstrumentService instrumentService,
                             final AppProperties properties) {
        this.predictionRepository = predictionRepository;
        this.marketDataService = marketDataService;
        this.instrumentService = instrumentService;
        this.properties = properties;
    }

    /**
     * Checks every OPEN prediction and resolves those whose horizon has passed.
     * The horizon is counted in trading days (bars), not calendar days - weekends and holidays don't count.
     *
     * @return number of predictions resolved in this call
     */
    @Transactional
    public int resolveDue() {
        final Map<Long, Instrument> instruments = new HashMap<>();
        int resolved = 0;
        for (final Prediction prediction : predictionRepository.findByStatus(PredictionStatus.OPEN)) {
            final Instrument instrument = instruments.computeIfAbsent(prediction.getInstrumentId(),
                    instrumentService::getById);
            try {
                if (tryResolve(prediction, instrument)) {
                    resolved++;
                }
            } catch (final RuntimeException exception) {
                log.warn("Cannot resolve prediction {}: {}", prediction.getId(), exception.getMessage());
            }
        }
        return resolved;
    }

    private boolean tryResolve(final Prediction prediction, final Instrument instrument) {
        final List<Bar> barsAfter = marketDataService.barsAfter(instrument.getId(), prediction.getReferenceDate());
        if (barsAfter.size() < prediction.getHorizonDays()) {
            return false;
        }
        final Bar resolutionBar = barsAfter.get(prediction.getHorizonDays() - 1);
        final double actualReturn = PredictionScorer.returnPct(prediction.getReferencePrice(), resolutionBar.close());

        Double benchmarkPrice = null;
        Double benchmarkReturn = null;
        if (instrument.getBenchmark() != null && prediction.getBenchmarkReferencePrice() != null) {
            final Optional<Bar> benchmarkBar = marketDataService.barOnOrBefore(instrument.getBenchmark().getId(),
                    resolutionBar.date());
            if (benchmarkBar.isPresent()) {
                benchmarkPrice = benchmarkBar.get().close();
                benchmarkReturn = PredictionScorer.returnPct(prediction.getBenchmarkReferencePrice(), benchmarkPrice);
            }
        }

        // INVEST is judged on beating the benchmark, TRADE on the absolute move
        final double measured = prediction.getMode() == Mode.INVEST && benchmarkReturn != null
                ? actualReturn - benchmarkReturn
                : actualReturn;
        final PredictionScorer.Score score = PredictionScorer.score(prediction.getDirection(),
                prediction.getConfidence(), measured, properties.scoring().flatBandFor(prediction.getHorizonDays()));

        prediction.resolve(resolutionBar.date(), resolutionBar.close(), benchmarkPrice, actualReturn,
                benchmarkReturn, measured, score);

        if (prediction.hasTradePlan() && prediction.getDirection() != Direction.FLAT) {
            prediction.recordTrade(TradeOutcomeEvaluator.evaluate(prediction.getDirection(),
                    prediction.getStopPrice(), prediction.getTargetPrice(), barsAfter, prediction.getHorizonDays()));
        }
        return true;
    }

    @Transactional(readOnly = true)
    public CalibrationCalculator.Calibration calibration(final Source source, final Mode mode,
                                                         final Integer horizonDays, final LocalDate from) {
        final List<CalibrationCalculator.Outcome> outcomes = resolvedSince(from).stream()
                .filter(prediction -> prediction.getSource() == source && prediction.getMode() == mode)
                .filter(prediction -> horizonDays == null || prediction.getHorizonDays() == horizonDays)
                .map(prediction -> new CalibrationCalculator.Outcome(prediction.getConfidence(),
                        Boolean.TRUE.equals(prediction.getHit())))
                .toList();
        return CalibrationCalculator.calculate(outcomes);
    }

    @Transactional(readOnly = true)
    public List<Prediction> resolvedSince(final LocalDate from) {
        return predictionRepository.findByStatusAndResolvedDateGreaterThanEqual(PredictionStatus.RESOLVED, from);
    }

    @Transactional(readOnly = true)
    public List<Prediction> openPredictions() {
        return predictionRepository.findByStatus(PredictionStatus.OPEN);
    }
}

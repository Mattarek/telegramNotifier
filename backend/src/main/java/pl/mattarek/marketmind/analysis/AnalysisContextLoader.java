package pl.mattarek.marketmind.analysis;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.learning.LessonService;
import pl.mattarek.marketmind.learning.PredictionReview;
import pl.mattarek.marketmind.learning.PredictionReviewRepository;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.market.MarketDataService;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.news.NewsService;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;
import pl.mattarek.marketmind.prediction.PredictionRepository;
import pl.mattarek.marketmind.prediction.PredictionService;
import pl.mattarek.marketmind.prediction.PredictionStatus;
import pl.mattarek.marketmind.prediction.Source;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collects the AI's "working memory" for one analysis.
 */
@Component
public class AnalysisContextLoader {

    private static final int TRACK_RECORD_SIZE = 12;
    private static final int RECENT_BARS = 15;
    private static final int MAX_NEWS = 25;
    private static final int MAX_GENERAL_NEWS = 10;

    private final MarketDataService marketDataService;
    private final NewsService newsService;
    private final PredictionRepository predictionRepository;
    private final PredictionService predictionService;
    private final PredictionReviewRepository reviewRepository;
    private final LessonService lessonService;

    public AnalysisContextLoader(final MarketDataService marketDataService,
                                 final NewsService newsService,
                                 final PredictionRepository predictionRepository,
                                 final PredictionService predictionService,
                                 final PredictionReviewRepository reviewRepository,
                                 final LessonService lessonService) {
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.predictionRepository = predictionRepository;
        this.predictionService = predictionService;
        this.reviewRepository = reviewRepository;
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public AnalysisContext load(final Instrument instrument, final Mode mode, final MarketSnapshot snapshot) {
        final List<Bar> bars = marketDataService.bars(instrument.getId());
        final List<Bar> recentBars = bars.subList(Math.max(0, bars.size() - RECENT_BARS), bars.size());

        final List<Prediction> resolved = predictionRepository
                .findByInstrumentIdAndModeAndSourceAndStatusOrderByResolvedDateDesc(instrument.getId(), mode,
                        Source.AI, PredictionStatus.RESOLVED, PageRequest.of(0, TRACK_RECORD_SIZE));
        final Map<Long, PredictionReview> reviews = reviewRepository
                .findByPredictionIdIn(resolved.stream().map(Prediction::getId).toList())
                .stream()
                .collect(Collectors.toMap(PredictionReview::getPredictionId, Function.identity()));
        final List<AnalysisContext.TrackRecordEntry> trackRecord = resolved.stream()
                .map(prediction -> new AnalysisContext.TrackRecordEntry(prediction, reviews.get(prediction.getId())))
                .toList();

        final List<Prediction> open = predictionService.openPredictions().stream()
                .filter(prediction -> prediction.getInstrumentId().equals(instrument.getId()))
                .filter(prediction -> prediction.getMode() == mode && prediction.getSource() == Source.AI)
                .toList();

        final LocalDate calibrationFrom = LocalDate.now().minusDays(180);
        return new AnalysisContext(
                instrument,
                mode,
                snapshot,
                recentBars,
                limit(newsService.recentFor(instrument.getId()), MAX_NEWS),
                limit(newsService.recentGeneral(), MAX_GENERAL_NEWS),
                trackRecord,
                open,
                predictionService.calibration(Source.AI, mode, null, calibrationFrom),
                predictionService.calibration(Source.BASELINE, mode, null, calibrationFrom),
                lessonService.lessonsFor(instrument.getMarket().name(), instrument.getSymbol(), mode));
    }

    private static <T> List<T> limit(final List<T> list, final int max) {
        return list.size() <= max ? list : list.subList(0, max);
    }
}

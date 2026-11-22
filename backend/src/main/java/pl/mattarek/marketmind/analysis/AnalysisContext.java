package pl.mattarek.marketmind.analysis;

import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.learning.Lesson;
import pl.mattarek.marketmind.learning.PredictionReview;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.news.NewsItem;
import pl.mattarek.marketmind.prediction.CalibrationCalculator;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;

import java.util.List;

/**
 * Everything the AI gets to see when analysing one instrument - including its own memory:
 * past predictions with outcomes and post-mortems, calibration stats and lessons.
 */
public record AnalysisContext(Instrument instrument,
                              Mode mode,
                              MarketSnapshot snapshot,
                              List<Bar> recentBars,
                              List<NewsItem> news,
                              List<NewsItem> generalNews,
                              List<TrackRecordEntry> trackRecord,
                              List<Prediction> openPredictions,
                              CalibrationCalculator.Calibration aiCalibration,
                              CalibrationCalculator.Calibration baselineCalibration,
                              List<Lesson> lessons) {

    public record TrackRecordEntry(Prediction prediction, PredictionReview review) {
    }
}

package pl.mattarek.marketmind.prediction;

import java.util.ArrayList;
import java.util.List;

/**
 * Calibration = "when I say 70%, am I right ~70% of the time?". Pure logic.
 *
 * <p>Predictions are grouped into confidence buckets (50-60%, 60-70%, ...). For a well calibrated
 * forecaster the hit rate in each bucket is close to the bucket's average confidence.
 */
public final class CalibrationCalculator {

    private static final double[] BUCKET_EDGES = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0001};

    private CalibrationCalculator() {
    }

    public static Calibration calculate(final List<Outcome> outcomes) {
        final List<Bucket> buckets = new ArrayList<>();
        for (int b = 0; b < BUCKET_EDGES.length - 1; b++) {
            final double from = BUCKET_EDGES[b];
            final double to = BUCKET_EDGES[b + 1];
            final List<Outcome> inBucket = outcomes.stream()
                    .filter(outcome -> outcome.confidence() >= from && outcome.confidence() < to)
                    .toList();
            buckets.add(new Bucket(from, Math.min(to, 1.0), inBucket.size(),
                    average(inBucket.stream().mapToDouble(Outcome::confidence).toArray()),
                    hitRate(inBucket)));
        }
        return new Calibration(
                outcomes.size(),
                (int) outcomes.stream().filter(Outcome::hit).count(),
                hitRate(outcomes),
                average(outcomes.stream().mapToDouble(Outcome::confidence).toArray()),
                average(outcomes.stream()
                        .mapToDouble(outcome -> Math.pow(outcome.confidence() - (outcome.hit() ? 1 : 0), 2))
                        .toArray()),
                buckets);
    }

    private static double hitRate(final List<Outcome> outcomes) {
        if (outcomes.isEmpty()) {
            return Double.NaN;
        }
        return (double) outcomes.stream().filter(Outcome::hit).count() / outcomes.size();
    }

    private static double average(final double[] values) {
        if (values.length == 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (final double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public record Outcome(double confidence, boolean hit) {
    }

    public record Bucket(double from, double to, int count, double averageConfidence, double hitRate) {
    }

    /**
     * @param overconfidence positive = says more than it delivers (e.g. 0.12 -> 12 pp too confident)
     */
    public record Calibration(int count, int hits, double hitRate, double averageConfidence, double brier,
                              List<Bucket> buckets) {

        public double overconfidence() {
            return count == 0 ? Double.NaN : averageConfidence - hitRate;
        }
    }
}

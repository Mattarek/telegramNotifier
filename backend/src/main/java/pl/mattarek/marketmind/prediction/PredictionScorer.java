package pl.mattarek.marketmind.prediction;

/**
 * Decides whether a prediction was right and how good its confidence was. Pure logic.
 *
 * <p>Brier score = (confidence - outcome)^2, where outcome is 1 for a hit and 0 for a miss.
 * 0 is perfect, 0.25 is what you get by always saying "50%". Lower is better.
 */
public final class PredictionScorer {

    private PredictionScorer() {
    }

    /**
     * @param measuredReturnPct return that the prediction is judged on (excess vs benchmark for INVEST,
     *                          absolute for TRADE)
     * @param flatBandPct       moves inside +/- this band count as FLAT
     */
    public static Score score(final Direction direction, final double confidence, final double measuredReturnPct,
                              final double flatBandPct) {
        final Direction actual = classify(measuredReturnPct, flatBandPct);
        final boolean hit = actual == direction;
        final double outcome = hit ? 1.0 : 0.0;
        final double brier = (confidence - outcome) * (confidence - outcome);
        return new Score(actual, hit, brier);
    }

    public static Direction classify(final double returnPct, final double flatBandPct) {
        if (returnPct > flatBandPct) {
            return Direction.UP;
        }
        if (returnPct < -flatBandPct) {
            return Direction.DOWN;
        }
        return Direction.FLAT;
    }

    public static double returnPct(final double from, final double to) {
        return (to / from - 1) * 100;
    }

    public record Score(Direction actualDirection, boolean hit, double brier) {
    }
}

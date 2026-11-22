package pl.mattarek.marketmind.prediction;

import pl.mattarek.marketmind.market.Bar;

import java.util.List;

/**
 * Simulates a swing trade on daily bars: enter at the next session's open, then walk forward
 * day by day and check whether the stop or the target was touched first. Pure logic.
 *
 * <p>Result is expressed in R (multiples of the risk): +2R means "won twice what was risked",
 * -1R means "lost exactly the planned risk".
 */
public final class TradeOutcomeEvaluator {

    private TradeOutcomeEvaluator() {
    }

    /**
     * @param barsAfterSignal bars after the day the setup was created (the first one is the entry day)
     */
    public static Result evaluate(final Direction direction, final double stop, final double target,
                                  final List<Bar> barsAfterSignal, final int horizonDays) {
        if (direction == Direction.FLAT) {
            throw new IllegalArgumentException("A trade needs a direction");
        }
        if (barsAfterSignal.size() < horizonDays) {
            throw new IllegalArgumentException("Not enough bars to evaluate the trade");
        }
        final boolean isLong = direction == Direction.UP;
        final double entry = barsAfterSignal.getFirst().open();
        final double risk = Math.abs(entry - stop);

        for (int i = 0; i < horizonDays; i++) {
            final Bar bar = barsAfterSignal.get(i);
            final boolean stopHit = isLong ? bar.low() <= stop : bar.high() >= stop;
            final boolean targetHit = isLong ? bar.high() >= target : bar.low() <= target;
            if (stopHit) {
                // If the day opened beyond the stop (gap), we exit at the open, not at the stop price
                final double exit = isLong ? Math.min(bar.open(), stop) : Math.max(bar.open(), stop);
                return result(TradeOutcome.STOP, entry, exit, risk, isLong, bar);
            }
            if (targetHit) {
                final double exit = isLong ? Math.max(bar.open(), target) : Math.min(bar.open(), target);
                return result(TradeOutcome.TARGET, entry, exit, risk, isLong, bar);
            }
        }
        final Bar last = barsAfterSignal.get(horizonDays - 1);
        return result(TradeOutcome.EXPIRED, entry, last.close(), risk, isLong, last);
    }

    private static Result result(final TradeOutcome outcome, final double entry, final double exit,
                                 final double risk, final boolean isLong, final Bar exitBar) {
        final double profitPerShare = isLong ? exit - entry : entry - exit;
        final double rMultiple = risk == 0 ? 0 : profitPerShare / risk;
        return new Result(outcome, entry, exit, rMultiple, exitBar.date());
    }

    public record Result(TradeOutcome outcome, double entry, double exit, double rMultiple,
                         java.time.LocalDate exitDate) {
    }
}

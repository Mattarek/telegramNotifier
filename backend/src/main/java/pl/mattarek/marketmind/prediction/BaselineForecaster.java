package pl.mattarek.marketmind.prediction;

import pl.mattarek.marketmind.market.MarketSnapshot;

import java.util.List;

/**
 * Deliberately simple rules that the AI has to beat. Pure logic.
 *
 * <ul>
 *   <li>INVEST: relative momentum - what beat the benchmark over 20 days keeps beating it.</li>
 *   <li>TRADE: trend following - above SMA20 and not overbought = long, below and not oversold = short,
 *       stop 1.5 ATR away, target 3 ATR away (risk/reward 1:2).</li>
 * </ul>
 */
public final class BaselineForecaster {

    static final double CONFIDENCE = 0.55;

    private BaselineForecaster() {
    }

    public static List<Forecast> invest(final MarketSnapshot snapshot) {
        final double momentum = snapshot.excessReturn20dPct();
        final Direction direction = Double.isNaN(momentum) || Math.abs(momentum) < 1.0
                ? Direction.FLAT
                : momentum > 0 ? Direction.UP : Direction.DOWN;
        final String rationale = "Momentum względem benchmarku (20 dni): %.2f%%".formatted(momentum);
        return List.of(
                new Forecast(5, direction, momentum / 4, CONFIDENCE, rationale, null, null),
                new Forecast(20, direction, momentum, CONFIDENCE, rationale, null, null));
    }

    public static List<Forecast> trade(final MarketSnapshot snapshot) {
        final double close = snapshot.close();
        final double atr = snapshot.atr14();
        final Direction direction;
        if (close > snapshot.sma20() && snapshot.rsi14() < 70) {
            direction = Direction.UP;
        } else if (close < snapshot.sma20() && snapshot.rsi14() > 30) {
            direction = Direction.DOWN;
        } else {
            direction = Direction.FLAT;
        }
        final String rationale = "Kurs %.2f vs SMA20 %.2f, RSI %.1f".formatted(close, snapshot.sma20(),
                snapshot.rsi14());

        Double stop = null;
        Double target = null;
        if (direction != Direction.FLAT && !Double.isNaN(atr)) {
            final int sign = direction == Direction.UP ? 1 : -1;
            stop = close - sign * 1.5 * atr;
            target = close + sign * 3.0 * atr;
        }
        final double expected = direction == Direction.FLAT ? 0 : (direction == Direction.UP ? 1 : -1)
                * snapshot.atrPct();
        return List.of(
                new Forecast(1, direction, expected / 2, CONFIDENCE, rationale, null, null),
                new Forecast(5, direction, expected, CONFIDENCE, rationale, stop, target));
    }

    public record Forecast(int horizonDays, Direction direction, double expectedReturnPct, double confidence,
                           String rationale, Double stop, Double target) {
    }
}

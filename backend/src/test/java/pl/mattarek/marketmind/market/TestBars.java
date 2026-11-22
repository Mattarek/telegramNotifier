package pl.mattarek.marketmind.market;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to build fake price series for tests.
 */
public final class TestBars {

    private TestBars() {
    }

    /**
     * Bars where open = high = low = close, starting at 2026-01-01, one per day.
     */
    public static List<Bar> fromCloses(final double... closes) {
        final List<Bar> bars = new ArrayList<>();
        LocalDate date = LocalDate.of(2026, 1, 1);
        for (final double close : closes) {
            bars.add(new Bar(date, close, close, close, close, 1000));
            date = date.plusDays(1);
        }
        return bars;
    }

    public static Bar bar(final int day, final double open, final double high, final double low, final double close) {
        return new Bar(LocalDate.of(2026, 1, day), open, high, low, close, 1000);
    }

    /**
     * Steadily rising series: 100, 101, 102, ...
     */
    public static List<Bar> rising(final int count) {
        final double[] closes = new double[count];
        for (int i = 0; i < count; i++) {
            closes[i] = 100 + i;
        }
        return fromCloses(closes);
    }
}

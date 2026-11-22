package pl.mattarek.marketmind.market;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Everything the AI and the baseline need to know about the price action on a given day.
 * Built from bars only, so it is fully testable without a database.
 */
public record MarketSnapshot(LocalDate date,
                             double close,
                             double return1dPct,
                             double return5dPct,
                             double return20dPct,
                             double return60dPct,
                             double sma20,
                             double sma50,
                             double sma200,
                             double rsi14,
                             double atr14,
                             double atrPct,
                             double volatility20Pct,
                             double high52w,
                             double low52w,
                             double volumeRatio20,
                             Double benchmarkClose,
                             double benchmarkReturn5dPct,
                             double benchmarkReturn20dPct) {

    public static MarketSnapshot of(final List<Bar> bars, final List<Bar> benchmarkBars) {
        if (bars.isEmpty()) {
            throw new IllegalArgumentException("Cannot build a snapshot without bars");
        }
        final Bar last = bars.getLast();
        final double atr = TechnicalIndicators.atr(bars, 14);
        final List<Bar> alignedBenchmark = benchmarkBars == null ? List.of() : upTo(benchmarkBars, last.date());
        return new MarketSnapshot(
                last.date(),
                last.close(),
                TechnicalIndicators.returnPct(bars, 1),
                TechnicalIndicators.returnPct(bars, 5),
                TechnicalIndicators.returnPct(bars, 20),
                TechnicalIndicators.returnPct(bars, 60),
                TechnicalIndicators.sma(bars, 20),
                TechnicalIndicators.sma(bars, 50),
                TechnicalIndicators.sma(bars, 200),
                TechnicalIndicators.rsi(bars, 14),
                atr,
                atr / last.close() * 100,
                TechnicalIndicators.annualizedVolatilityPct(bars, 20),
                TechnicalIndicators.highestHigh(bars, 252),
                TechnicalIndicators.lowestLow(bars, 252),
                TechnicalIndicators.volumeRatio(bars, 20),
                alignedBenchmark.isEmpty() ? null : alignedBenchmark.getLast().close(),
                TechnicalIndicators.returnPct(alignedBenchmark, 5),
                TechnicalIndicators.returnPct(alignedBenchmark, 20));
    }

    /**
     * Excess return vs the benchmark over 20 days - "did it beat the market lately?".
     */
    public double excessReturn20dPct() {
        return return20dPct - benchmarkReturn20dPct;
    }

    public double distanceFromHighPct() {
        return (close / high52w - 1) * 100;
    }

    /**
     * Compact, human/AI-readable description used in prompts.
     */
    public String describe() {
        return String.join("\n",
                "date: " + date,
                "close: " + fmt(close),
                "return 1d / 5d / 20d / 60d (%): " + fmt(return1dPct) + " / " + fmt(return5dPct) + " / "
                        + fmt(return20dPct) + " / " + fmt(return60dPct),
                "benchmark return 5d / 20d (%): " + fmt(benchmarkReturn5dPct) + " / " + fmt(benchmarkReturn20dPct),
                "excess return vs benchmark 20d (%): " + fmt(excessReturn20dPct()),
                "SMA20 / SMA50 / SMA200: " + fmt(sma20) + " / " + fmt(sma50) + " / " + fmt(sma200),
                "RSI14: " + fmt(rsi14),
                "ATR14: " + fmt(atr14) + " (" + fmt(atrPct) + "% of price)",
                "annualized volatility 20d (%): " + fmt(volatility20Pct),
                "52w high / low: " + fmt(high52w) + " / " + fmt(low52w)
                        + " (distance from high: " + fmt(distanceFromHighPct()) + "%)",
                "last volume vs 20d average: " + fmt(volumeRatio20) + "x");
    }

    private static List<Bar> upTo(final List<Bar> bars, final LocalDate date) {
        int end = bars.size();
        while (end > 0 && bars.get(end - 1).date().isAfter(date)) {
            end--;
        }
        return bars.subList(0, end);
    }

    private static String fmt(final double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.2f", value);
    }
}

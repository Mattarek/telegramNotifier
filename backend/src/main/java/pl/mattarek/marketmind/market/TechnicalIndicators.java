package pl.mattarek.marketmind.market;

import java.util.List;

/**
 * Classic indicators computed from daily bars. Pure functions - no Spring, no database.
 * Every method returns NaN when there is not enough data.
 */
public final class TechnicalIndicators {

    private TechnicalIndicators() {
    }

    /**
     * Simple moving average of the last {@code period} closes.
     */
    public static double sma(final List<Bar> bars, final int period) {
        if (bars.size() < period || period <= 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = bars.size() - period; i < bars.size(); i++) {
            sum += bars.get(i).close();
        }
        return sum / period;
    }

    /**
     * Percentage change of the close over the last {@code days} bars.
     */
    public static double returnPct(final List<Bar> bars, final int days) {
        if (bars.size() <= days || days <= 0) {
            return Double.NaN;
        }
        final double now = bars.getLast().close();
        final double then = bars.get(bars.size() - 1 - days).close();
        return then == 0 ? Double.NaN : (now / then - 1) * 100;
    }

    /**
     * Relative Strength Index with Wilder's smoothing. Above 70 = overbought, below 30 = oversold.
     */
    public static double rsi(final List<Bar> bars, final int period) {
        if (bars.size() <= period) {
            return Double.NaN;
        }
        double averageGain = 0;
        double averageLoss = 0;
        for (int i = 1; i <= period; i++) {
            final double change = bars.get(i).close() - bars.get(i - 1).close();
            averageGain += Math.max(change, 0);
            averageLoss += Math.max(-change, 0);
        }
        averageGain /= period;
        averageLoss /= period;

        for (int i = period + 1; i < bars.size(); i++) {
            final double change = bars.get(i).close() - bars.get(i - 1).close();
            averageGain = (averageGain * (period - 1) + Math.max(change, 0)) / period;
            averageLoss = (averageLoss * (period - 1) + Math.max(-change, 0)) / period;
        }
        if (averageLoss == 0) {
            return 100;
        }
        final double relativeStrength = averageGain / averageLoss;
        return 100 - 100 / (1 + relativeStrength);
    }

    /**
     * Average True Range - the typical daily price range. Used to size stops and to judge "big" moves.
     */
    public static double atr(final List<Bar> bars, final int period) {
        if (bars.size() <= period) {
            return Double.NaN;
        }
        double atr = 0;
        for (int i = 1; i <= period; i++) {
            atr += trueRange(bars.get(i), bars.get(i - 1));
        }
        atr /= period;
        for (int i = period + 1; i < bars.size(); i++) {
            atr = (atr * (period - 1) + trueRange(bars.get(i), bars.get(i - 1))) / period;
        }
        return atr;
    }

    /**
     * Annualized volatility (in %) from the standard deviation of daily returns.
     */
    public static double annualizedVolatilityPct(final List<Bar> bars, final int period) {
        if (bars.size() <= period) {
            return Double.NaN;
        }
        final double[] returns = new double[period];
        double mean = 0;
        for (int i = 0; i < period; i++) {
            final int index = bars.size() - period + i;
            returns[i] = Math.log(bars.get(index).close() / bars.get(index - 1).close());
            mean += returns[i];
        }
        mean /= period;
        double variance = 0;
        for (final double value : returns) {
            variance += (value - mean) * (value - mean);
        }
        variance /= (period - 1);
        return Math.sqrt(variance) * Math.sqrt(252) * 100;
    }

    public static double highestHigh(final List<Bar> bars, final int period) {
        if (bars.isEmpty()) {
            return Double.NaN;
        }
        double max = Double.NEGATIVE_INFINITY;
        for (int i = Math.max(0, bars.size() - period); i < bars.size(); i++) {
            max = Math.max(max, bars.get(i).high());
        }
        return max;
    }

    public static double lowestLow(final List<Bar> bars, final int period) {
        if (bars.isEmpty()) {
            return Double.NaN;
        }
        double min = Double.POSITIVE_INFINITY;
        for (int i = Math.max(0, bars.size() - period); i < bars.size(); i++) {
            min = Math.min(min, bars.get(i).low());
        }
        return min;
    }

    /**
     * Last volume divided by the average volume of the previous {@code period} bars.
     * 2.0 means "twice the usual volume" - often a sign that news really moved the stock.
     */
    public static double volumeRatio(final List<Bar> bars, final int period) {
        if (bars.size() <= period) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = bars.size() - 1 - period; i < bars.size() - 1; i++) {
            sum += bars.get(i).volume();
        }
        final double average = sum / period;
        return average == 0 ? Double.NaN : bars.getLast().volume() / average;
    }

    private static double trueRange(final Bar current, final Bar previous) {
        final double highLow = current.high() - current.low();
        final double highClose = Math.abs(current.high() - previous.close());
        final double lowClose = Math.abs(current.low() - previous.close());
        return Math.max(highLow, Math.max(highClose, lowClose));
    }
}

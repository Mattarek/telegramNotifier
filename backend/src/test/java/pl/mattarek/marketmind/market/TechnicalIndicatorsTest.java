package pl.mattarek.marketmind.market;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicalIndicatorsTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldCalculateSimpleMovingAverageOfLastCloses() {
        final List<Bar> bars = TestBars.fromCloses(1, 2, 3, 4, 5);

        assertEquals(4.0, TechnicalIndicators.sma(bars, 3), EPSILON);
    }

    @Test
    void shouldReturnNaNWhenNotEnoughBars() {
        final List<Bar> bars = TestBars.fromCloses(1, 2);

        assertTrue(Double.isNaN(TechnicalIndicators.sma(bars, 3)));
        assertTrue(Double.isNaN(TechnicalIndicators.rsi(bars, 14)));
    }

    @Test
    void shouldCalculateReturnInPercent() {
        final List<Bar> bars = TestBars.fromCloses(100, 105, 110);

        assertEquals(10.0, TechnicalIndicators.returnPct(bars, 2), EPSILON);
    }

    @Test
    void shouldGiveRsiOf100WhenPriceOnlyRises() {
        assertEquals(100.0, TechnicalIndicators.rsi(TestBars.rising(30), 14), EPSILON);
    }

    @Test
    void shouldUseGapsInAverageTrueRange() {
        // Every day closes 2 higher with no intraday range -> true range = 2 (gap from previous close)
        final List<Bar> bars = TestBars.fromCloses(10, 12, 14, 16, 18, 20);

        assertEquals(2.0, TechnicalIndicators.atr(bars, 3), EPSILON);
    }

    @Test
    void shouldCompareLastVolumeWithAverage() {
        final List<Bar> bars = List.of(
                new Bar(java.time.LocalDate.of(2026, 1, 1), 1, 1, 1, 1, 100),
                new Bar(java.time.LocalDate.of(2026, 1, 2), 1, 1, 1, 1, 100),
                new Bar(java.time.LocalDate.of(2026, 1, 3), 1, 1, 1, 1, 300));

        assertEquals(3.0, TechnicalIndicators.volumeRatio(bars, 2), EPSILON);
    }
}

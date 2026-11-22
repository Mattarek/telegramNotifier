package pl.mattarek.marketmind.prediction;

import org.junit.jupiter.api.Test;
import pl.mattarek.marketmind.market.Bar;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.mattarek.marketmind.market.TestBars.bar;

class TradeOutcomeEvaluatorTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldHitTargetOnLongTrade() {
        // entry at open 100, stop 95 (risk 5), target 110
        final List<Bar> bars = List.of(
                bar(2, 100, 104, 99, 103),
                bar(3, 103, 111, 102, 109));

        final TradeOutcomeEvaluator.Result result =
                TradeOutcomeEvaluator.evaluate(Direction.UP, 95, 110, bars, 2);

        assertEquals(TradeOutcome.TARGET, result.outcome());
        assertEquals(2.0, result.rMultiple(), EPSILON);
    }

    @Test
    void shouldAssumeStopWhenBothLevelsTouchedSameDay() {
        final List<Bar> bars = List.of(bar(2, 100, 111, 94, 100));

        final TradeOutcomeEvaluator.Result result =
                TradeOutcomeEvaluator.evaluate(Direction.UP, 95, 110, bars, 1);

        assertEquals(TradeOutcome.STOP, result.outcome());
        assertEquals(-1.0, result.rMultiple(), EPSILON);
    }

    @Test
    void shouldExitAtOpenWhenPriceGapsThroughStop() {
        // Short: entry 100, stop 105 (risk 5). Next day opens at 108 -> exit at 108 = -1.6R
        final List<Bar> bars = List.of(
                bar(2, 100, 102, 99, 101),
                bar(3, 108, 109, 107, 108));

        final TradeOutcomeEvaluator.Result result =
                TradeOutcomeEvaluator.evaluate(Direction.DOWN, 105, 90, bars, 2);

        assertEquals(TradeOutcome.STOP, result.outcome());
        assertEquals(-1.6, result.rMultiple(), EPSILON);
    }

    @Test
    void shouldCloseAtLastCloseWhenNothingHit() {
        final List<Bar> bars = List.of(
                bar(2, 100, 102, 98, 101),
                bar(3, 101, 103, 99, 102.5));

        final TradeOutcomeEvaluator.Result result =
                TradeOutcomeEvaluator.evaluate(Direction.UP, 95, 110, bars, 2);

        assertEquals(TradeOutcome.EXPIRED, result.outcome());
        assertEquals(0.5, result.rMultiple(), EPSILON);
    }

    @Test
    void shouldRejectFlatTrade() {
        assertThrows(IllegalArgumentException.class, () -> TradeOutcomeEvaluator.evaluate(Direction.FLAT, 95, 110,
                List.of(bar(2, 100, 101, 99, 100)), 1));
    }
}

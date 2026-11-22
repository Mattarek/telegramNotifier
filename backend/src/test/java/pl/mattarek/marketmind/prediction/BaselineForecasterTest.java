package pl.mattarek.marketmind.prediction;

import org.junit.jupiter.api.Test;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.market.TestBars;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineForecasterTest {

    @Test
    void shouldPredictOutperformanceForStockBeatingBenchmark() {
        final List<Bar> stock = TestBars.rising(60);
        final double[] flat = new double[60];
        java.util.Arrays.fill(flat, 100);
        final List<Bar> benchmark = TestBars.fromCloses(flat);

        final List<BaselineForecaster.Forecast> forecasts =
                BaselineForecaster.invest(MarketSnapshot.of(stock, benchmark));

        assertEquals(2, forecasts.size());
        assertTrue(forecasts.stream().allMatch(forecast -> forecast.direction() == Direction.UP));
    }

    @Test
    void shouldPlaceStopBelowAndTargetAboveForLongTrade() {
        final double[] closes = new double[40];
        for (int i = 0; i < closes.length; i++) {
            // rising with pullbacks so RSI stays below 70
            closes[i] = 100 + i * 0.3 + (i % 2 == 0 ? 0 : -1.5);
        }
        final MarketSnapshot snapshot = MarketSnapshot.of(TestBars.fromCloses(closes), List.of());

        final BaselineForecaster.Forecast fiveDay = BaselineForecaster.trade(snapshot).getLast();

        assertEquals(Direction.UP, fiveDay.direction());
        assertTrue(fiveDay.stop() < snapshot.close());
        assertTrue(fiveDay.target() > snapshot.close());
    }
}

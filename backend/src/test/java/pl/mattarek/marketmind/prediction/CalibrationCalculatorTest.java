package pl.mattarek.marketmind.prediction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalibrationCalculatorTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldDetectOverconfidence() {
        // Says 80% but is right only half of the time
        final List<CalibrationCalculator.Outcome> outcomes = List.of(
                new CalibrationCalculator.Outcome(0.8, true),
                new CalibrationCalculator.Outcome(0.8, false),
                new CalibrationCalculator.Outcome(0.8, true),
                new CalibrationCalculator.Outcome(0.8, false));

        final CalibrationCalculator.Calibration calibration = CalibrationCalculator.calculate(outcomes);

        assertEquals(4, calibration.count());
        assertEquals(0.5, calibration.hitRate(), EPSILON);
        assertEquals(0.3, calibration.overconfidence(), EPSILON);
        // (0.2^2 + 0.8^2) / 2 = 0.34
        assertEquals(0.34, calibration.brier(), EPSILON);
    }

    @Test
    void shouldPutOutcomesIntoBuckets() {
        final List<CalibrationCalculator.Outcome> outcomes = List.of(
                new CalibrationCalculator.Outcome(0.55, true),
                new CalibrationCalculator.Outcome(0.62, false),
                new CalibrationCalculator.Outcome(0.95, true));

        final CalibrationCalculator.Calibration calibration = CalibrationCalculator.calculate(outcomes);

        assertEquals(5, calibration.buckets().size());
        assertEquals(1, calibration.buckets().get(0).count());
        assertEquals(1, calibration.buckets().get(1).count());
        // 0.95 must land in the last bucket (90-100%)
        assertEquals(1, calibration.buckets().get(4).count());
    }

    @Test
    void shouldReturnNaNForEmptyInput() {
        final CalibrationCalculator.Calibration calibration = CalibrationCalculator.calculate(List.of());

        assertEquals(0, calibration.count());
        assertTrue(Double.isNaN(calibration.hitRate()));
    }
}

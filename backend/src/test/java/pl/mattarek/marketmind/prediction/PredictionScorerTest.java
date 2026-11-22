package pl.mattarek.marketmind.prediction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredictionScorerTest {

    private static final double BAND = 1.0;
    private static final double EPSILON = 1e-9;

    @Test
    void shouldCountUpAsHitWhenMoveIsAboveBand() {
        final PredictionScorer.Score score = PredictionScorer.score(Direction.UP, 0.7, 2.5, BAND);

        assertTrue(score.hit());
        assertEquals(Direction.UP, score.actualDirection());
        assertEquals(0.09, score.brier(), EPSILON);
    }

    @Test
    void shouldCountSmallMoveAsFlat() {
        final PredictionScorer.Score up = PredictionScorer.score(Direction.UP, 0.6, 0.4, BAND);
        final PredictionScorer.Score flat = PredictionScorer.score(Direction.FLAT, 0.6, 0.4, BAND);

        assertFalse(up.hit());
        assertTrue(flat.hit());
    }

    @Test
    void shouldPunishConfidentMissMoreThanCautiousMiss() {
        final PredictionScorer.Score confident = PredictionScorer.score(Direction.DOWN, 0.9, 3.0, BAND);
        final PredictionScorer.Score cautious = PredictionScorer.score(Direction.DOWN, 0.55, 3.0, BAND);

        assertTrue(confident.brier() > cautious.brier());
    }

    @Test
    void shouldCalculateReturnPercent() {
        assertEquals(-20.0, PredictionScorer.returnPct(50, 40), EPSILON);
    }
}

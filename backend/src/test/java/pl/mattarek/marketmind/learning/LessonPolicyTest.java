package pl.mattarek.marketmind.learning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonPolicyTest {

    @Test
    void shouldStayCandidateWithTooLittleEvidence() {
        assertEquals(LessonStatus.CANDIDATE, LessonPolicy.nextStatus(LessonStatus.CANDIDATE, 2, 0));
    }

    @Test
    void shouldActivateAfterRepeatedSupport() {
        assertEquals(LessonStatus.ACTIVE, LessonPolicy.nextStatus(LessonStatus.CANDIDATE, 3, 1));
    }

    @Test
    void shouldRetireWhenContradictedMoreOftenThanSupported() {
        assertEquals(LessonStatus.RETIRED, LessonPolicy.nextStatus(LessonStatus.ACTIVE, 2, 4));
    }

    @Test
    void shouldDemoteActiveLessonWhenItBecomesShaky() {
        // 3 support / 2 contradict = 60% < 65% -> back to testing, but not retired yet
        assertEquals(LessonStatus.CANDIDATE, LessonPolicy.nextStatus(LessonStatus.ACTIVE, 3, 2));
    }

    @Test
    void shouldNeverReviveRetiredLesson() {
        assertEquals(LessonStatus.RETIRED, LessonPolicy.nextStatus(LessonStatus.RETIRED, 10, 0));
    }
}

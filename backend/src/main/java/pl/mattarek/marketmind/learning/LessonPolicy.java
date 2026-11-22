package pl.mattarek.marketmind.learning;

/**
 * The "slow learning" rules. A lesson has to earn trust with repeated evidence and loses it when
 * the evidence turns against it. Pure logic - easy to tune and unit test.
 */
public final class LessonPolicy {

    static final int MIN_SUPPORT_TO_ACTIVATE = 3;
    static final double MIN_RATIO_TO_ACTIVATE = 0.65;
    static final int MIN_CONTRADICTIONS_TO_RETIRE = 3;
    static final double MAX_RATIO_TO_RETIRE = 0.40;

    private LessonPolicy() {
    }

    public static LessonStatus nextStatus(final LessonStatus current, final int support, final int contradict) {
        if (current == LessonStatus.RETIRED) {
            // Retired lessons stay retired - a new, better worded lesson can be created instead
            return LessonStatus.RETIRED;
        }
        final double ratio = supportRatio(support, contradict);
        if (contradict >= MIN_CONTRADICTIONS_TO_RETIRE && ratio < MAX_RATIO_TO_RETIRE) {
            return LessonStatus.RETIRED;
        }
        if (support >= MIN_SUPPORT_TO_ACTIVATE && ratio >= MIN_RATIO_TO_ACTIVATE) {
            return LessonStatus.ACTIVE;
        }
        if (current == LessonStatus.ACTIVE && ratio < MIN_RATIO_TO_ACTIVATE) {
            // Shaky again - back to testing
            return LessonStatus.CANDIDATE;
        }
        return current;
    }

    public static double supportRatio(final int support, final int contradict) {
        final int total = support + contradict;
        return total == 0 ? 0 : (double) support / total;
    }
}

package pl.mattarek.marketmind.learning;

/**
 * Result of a post-mortem. The important distinction is "right for the right reasons" vs "lucky",
 * and "wrong because of bad reasoning" vs "wrong because nobody could know" or plain noise.
 * Only verdicts that say something about the reasoning are allowed to change lessons.
 */
public enum Verdict {
    CORRECT_FOR_RIGHT_REASONS(true),
    CORRECT_BY_LUCK(true),
    WRONG_BAD_REASONING(true),
    WRONG_UNFORESEEABLE(false),
    NOISE(false);

    private final boolean informative;

    Verdict(final boolean informative) {
        this.informative = informative;
    }

    /**
     * Whether this outcome may be used as evidence for or against a lesson.
     */
    public boolean isInformative() {
        return informative;
    }

    public static Verdict parse(final String value) {
        if (value == null) {
            return NOISE;
        }
        try {
            return valueOf(value.strip().toUpperCase());
        } catch (final IllegalArgumentException exception) {
            return NOISE;
        }
    }
}

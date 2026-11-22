package pl.mattarek.marketmind.prediction;

public enum TradeOutcome {
    /** Target reached before the stop. */
    TARGET,
    /** Stop loss hit first (or both on the same day - we assume the worse case). */
    STOP,
    /** Neither level touched within the horizon - closed at the last close. */
    EXPIRED
}

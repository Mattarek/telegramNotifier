package pl.mattarek.marketmind.prediction;

/**
 * INVEST - "akcje" tab: medium horizon, measured against the benchmark (did it beat WIG20 / S&P 500?).
 * TRADE  - "trading" tab: short horizon, measured on the absolute price move, with entry/stop/target.
 */
public enum Mode {
    INVEST,
    TRADE
}

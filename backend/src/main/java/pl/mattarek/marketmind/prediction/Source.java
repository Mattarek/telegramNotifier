package pl.mattarek.marketmind.prediction;

/**
 * Who made the prediction. BASELINE is a dumb rule (momentum / trend) that runs on the same
 * instruments and days - if the AI cannot beat it over time, its "insights" are not worth much.
 */
public enum Source {
    AI,
    BASELINE
}

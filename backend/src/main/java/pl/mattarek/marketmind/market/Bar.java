package pl.mattarek.marketmind.market;

import java.time.LocalDate;

/**
 * Plain daily OHLCV bar used by the pure calculation code (no JPA here, easy to unit test).
 */
public record Bar(LocalDate date, double open, double high, double low, double close, long volume) {
}

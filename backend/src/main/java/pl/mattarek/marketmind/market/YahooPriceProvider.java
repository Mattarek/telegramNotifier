package pl.mattarek.marketmind.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses the unofficial Yahoo Finance chart endpoint (v8). It needs no key, but it is not an
 * official API, so it can change without notice - that is why it sits behind PriceProvider.
 */
@Component
public class YahooPriceProvider implements PriceProvider {

    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}";

    private final RestClient restClient;

    public YahooPriceProvider(@Qualifier("marketRestClient") final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Bar> fetchDaily(final String providerSymbol, final String range) {
        final URI uri = UriComponentsBuilder.fromUriString(CHART_URL)
                .queryParam("range", range)
                .queryParam("interval", "1d")
                .queryParam("includePrePost", "false")
                .buildAndExpand(providerSymbol)
                .encode()
                .toUri();

        final ChartResponse response = restClient.get().uri(uri).retrieve().body(ChartResponse.class);
        if (response == null || response.chart() == null || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            throw new IllegalStateException("Yahoo returned no data for " + providerSymbol);
        }
        return toBars(response.chart().result().getFirst());
    }

    private static List<Bar> toBars(final ChartResult result) {
        final List<Long> timestamps = result.timestamp();
        if (timestamps == null || result.indicators() == null || result.indicators().quote() == null
                || result.indicators().quote().isEmpty()) {
            return List.of();
        }
        final ZoneId zone = result.meta() != null && result.meta().exchangeTimezoneName() != null
                ? ZoneId.of(result.meta().exchangeTimezoneName())
                : ZoneId.of("UTC");
        final Quote quote = result.indicators().quote().getFirst();

        final List<Bar> bars = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            final Double close = valueAt(quote.close(), i);
            if (close == null) {
                // Yahoo puts nulls for days without trading - skip them
                continue;
            }
            final LocalDate date = Instant.ofEpochSecond(timestamps.get(i)).atZone(zone).toLocalDate();
            final Double open = valueAt(quote.open(), i);
            final Double high = valueAt(quote.high(), i);
            final Double low = valueAt(quote.low(), i);
            final Long volume = valueAt(quote.volume(), i);
            bars.add(new Bar(date,
                    open == null ? close : open,
                    high == null ? close : high,
                    low == null ? close : low,
                    close,
                    volume == null ? 0 : volume));
        }
        return deduplicateByDate(bars);
    }

    /**
     * During the session Yahoo sometimes returns the current day twice - keep the last one.
     */
    private static List<Bar> deduplicateByDate(final List<Bar> bars) {
        final List<Bar> result = new ArrayList<>();
        for (final Bar bar : bars) {
            if (!result.isEmpty() && result.getLast().date().equals(bar.date())) {
                result.removeLast();
            }
            result.add(bar);
        }
        return result;
    }

    private static <T> T valueAt(final List<T> values, final int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChartResponse(Chart chart) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Chart(List<ChartResult> result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChartResult(Meta meta, List<Long> timestamp, Indicators indicators) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Meta(String currency, String symbol, String exchangeTimezoneName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Indicators(List<Quote> quote) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Quote(List<Double> open, List<Double> high, List<Double> low, List<Double> close, List<Long> volume) {
    }
}

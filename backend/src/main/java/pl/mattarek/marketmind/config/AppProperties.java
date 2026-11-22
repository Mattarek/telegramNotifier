package pl.mattarek.marketmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * All "app.*" settings from application.yml in one typed object.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Ai ai, Market market, News news, Pipeline pipeline, Scoring scoring) {

    public record Ai(String apiKey,
                     String baseUrl,
                     String model,
                     int maxTokens,
                     double inputPricePerMtok,
                     double outputPricePerMtok) {

        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Market(String historyRange, String userAgent) {
    }

    public record News(int maxItemsPerFeed, int lookbackHours, List<Feed> generalFeeds) {
    }

    public record Feed(String name, String url) {
    }

    public record Pipeline(String cron, String zone, boolean enabled) {
    }

    public record Scoring(Map<Integer, Double> flatBandPct) {

        /**
         * Moves smaller than this band are treated as "FLAT" for the given horizon.
         */
        public double flatBandFor(final int horizonDays) {
            final Double configured = flatBandPct == null ? null : flatBandPct.get(horizonDays);
            if (configured != null) {
                return configured;
            }
            // Fallback: band grows with the square root of time, like volatility does
            return 0.45 * Math.sqrt(horizonDays);
        }
    }
}

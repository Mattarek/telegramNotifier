package pl.mattarek.marketmind.market;

import java.util.List;

/**
 * Source of daily prices. Yahoo is the default; another provider can be plugged in later
 * (e.g. Stooq, which since 2026 needs an API key) without touching the rest of the app.
 */
public interface PriceProvider {

    /**
     * @param providerSymbol symbol in the provider's own format, e.g. "CBF.WA" or "NVDA"
     * @param range          how far back, e.g. "1y", "2y"
     * @return bars sorted from the oldest to the newest
     */
    List<Bar> fetchDaily(String providerSymbol, String range);
}

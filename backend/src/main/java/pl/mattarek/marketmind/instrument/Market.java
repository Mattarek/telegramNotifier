package pl.mattarek.marketmind.instrument;

/**
 * Market an instrument is listed on. Decides the language/region of news searches.
 */
public enum Market {
    GPW("pl", "PL", "PL:pl"),
    US("en-US", "US", "US:en"),
    OTHER("en-US", "US", "US:en");

    private final String newsLanguage;
    private final String newsCountry;
    private final String newsEdition;

    Market(final String newsLanguage, final String newsCountry, final String newsEdition) {
        this.newsLanguage = newsLanguage;
        this.newsCountry = newsCountry;
        this.newsEdition = newsEdition;
    }

    public String newsLanguage() {
        return newsLanguage;
    }

    public String newsCountry() {
        return newsCountry;
    }

    public String newsEdition() {
        return newsEdition;
    }
}

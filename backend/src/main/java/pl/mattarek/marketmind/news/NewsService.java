package pl.mattarek.marketmind.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.config.AppProperties;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.instrument.Market;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final String GOOGLE_NEWS_SEARCH =
            "https://news.google.com/rss/search?q=%s&hl=%s&gl=%s&ceid=%s";
    private static final String YAHOO_HEADLINES =
            "https://feeds.finance.yahoo.com/rss/2.0/headline?s=%s&region=US&lang=en-US";

    private final RssClient rssClient;
    private final NewsItemRepository newsItemRepository;
    private final AppProperties properties;

    public NewsService(final RssClient rssClient, final NewsItemRepository newsItemRepository,
                       final AppProperties properties) {
        this.rssClient = rssClient;
        this.newsItemRepository = newsItemRepository;
        this.properties = properties;
    }

    /**
     * Fetches news for one instrument. Returns the number of new items.
     */
    @Transactional
    public int fetchFor(final Instrument instrument) {
        int added = 0;
        for (final Feed feed : feedsFor(instrument)) {
            added += fetchFeed(instrument.getId(), feed);
        }
        return added;
    }

    @Transactional
    public int fetchGeneral() {
        int added = 0;
        for (final AppProperties.Feed feed : properties.news().generalFeeds()) {
            added += fetchFeed(null, new Feed(feed.name(), feed.url()));
        }
        return added;
    }

    @Transactional(readOnly = true)
    public List<NewsItem> recentFor(final long instrumentId) {
        return newsItemRepository.findByInstrumentIdAndPublishedAtAfterOrderByPublishedAtDesc(instrumentId,
                lookbackStart());
    }

    @Transactional(readOnly = true)
    public List<NewsItem> recentGeneral() {
        return newsItemRepository.findByInstrumentIdIsNullAndPublishedAtAfterOrderByPublishedAtDesc(lookbackStart());
    }

    /**
     * News that appeared while a prediction was "running" - used in post-mortems.
     */
    @Transactional(readOnly = true)
    public List<NewsItem> between(final long instrumentId, final Instant from, final Instant to) {
        return newsItemRepository.findByInstrumentIdAndPublishedAtBetweenOrderByPublishedAtDesc(instrumentId, from, to);
    }

    @Transactional(readOnly = true)
    public List<NewsDto> latestFor(final long instrumentId, final int limit) {
        return newsItemRepository.findByInstrumentIdOrderByPublishedAtDesc(instrumentId, PageRequest.of(0, limit))
                .stream().map(NewsDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NewsDto> latestGeneral(final int limit) {
        return newsItemRepository.findByInstrumentIdIsNullOrderByPublishedAtDesc(PageRequest.of(0, limit))
                .stream().map(NewsDto::from).toList();
    }

    private int fetchFeed(final Long instrumentId, final Feed feed) {
        final List<RssClient.RssEntry> entries;
        try {
            entries = rssClient.fetch(feed.url(), properties.news().maxItemsPerFeed());
        } catch (final Exception exception) {
            // One broken feed must not stop the whole daily run
            log.warn("Feed '{}' failed: {}", feed.name(), exception.getMessage());
            return 0;
        }
        int added = 0;
        for (final RssClient.RssEntry entry : entries) {
            if (entry.link().length() > 2000 || newsItemRepository.existsByUrl(entry.link())) {
                continue;
            }
            final String title = entry.title().length() > 1000 ? entry.title().substring(0, 1000) : entry.title();
            newsItemRepository.save(new NewsItem(instrumentId, title, entry.link(), entry.source(), feed.name(),
                    entry.publishedAt()));
            added++;
        }
        return added;
    }

    private List<Feed> feedsFor(final Instrument instrument) {
        final List<Feed> feeds = new ArrayList<>();
        if (instrument.getNewsQuery() != null && !instrument.getNewsQuery().isBlank()) {
            final Market market = instrument.getMarket();
            final String query = URLEncoder.encode(instrument.getNewsQuery(), StandardCharsets.UTF_8);
            feeds.add(new Feed("Google News: " + instrument.getNewsQuery(),
                    GOOGLE_NEWS_SEARCH.formatted(query, market.newsLanguage(), market.newsCountry(),
                            market.newsEdition())));
        }
        if (instrument.getMarket() == Market.US && !instrument.getYahooSymbol().contains(".")) {
            final String symbol = URLEncoder.encode(instrument.getYahooSymbol(), StandardCharsets.UTF_8);
            feeds.add(new Feed("Yahoo Finance: " + instrument.getSymbol(), YAHOO_HEADLINES.formatted(symbol)));
        }
        return feeds;
    }

    private Instant lookbackStart() {
        return Instant.now().minus(Duration.ofHours(properties.news().lookbackHours()));
    }

    private record Feed(String name, String url) {
    }
}

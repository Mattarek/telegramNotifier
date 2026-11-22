package pl.mattarek.marketmind.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "news_item")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * null = general market news, not tied to one instrument.
     */
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String url;

    private String source;

    @Column(name = "feed_name")
    private String feedName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected NewsItem() {
        // for JPA
    }

    public NewsItem(final Long instrumentId, final String title, final String url, final String source,
                    final String feedName, final Instant publishedAt) {
        this.instrumentId = instrumentId;
        this.title = title;
        this.url = url;
        this.source = source;
        this.feedName = feedName;
        this.publishedAt = publishedAt;
        this.fetchedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public String getFeedName() {
        return feedName;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}

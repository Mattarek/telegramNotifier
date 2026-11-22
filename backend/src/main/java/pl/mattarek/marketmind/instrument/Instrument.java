package pl.mattarek.marketmind.instrument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A stock, ETF or index we follow. Benchmarks (WIG20, S&P 500) are instruments with analyzed = false.
 */
@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Market market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstrumentType type;

    private String currency;

    @Column(name = "yahoo_symbol", nullable = false)
    private String yahooSymbol;

    @Column(name = "news_query")
    private String newsQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benchmark_id")
    private Instrument benchmark;

    @Column(nullable = false)
    private boolean analyzed;

    @Column(name = "trading_enabled", nullable = false)
    private boolean tradingEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Instrument() {
        // for JPA
    }

    public Instrument(final String symbol, final String name, final Market market, final InstrumentType type,
                      final String yahooSymbol) {
        this.symbol = symbol;
        this.name = name;
        this.market = market;
        this.type = type;
        this.yahooSymbol = yahooSymbol;
        this.analyzed = true;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void update(final String name, final Market market, final InstrumentType type, final String currency,
                       final String yahooSymbol, final String newsQuery, final Instrument benchmark,
                       final boolean analyzed, final boolean tradingEnabled, final boolean active) {
        if (benchmark != null && benchmark.getId() != null && benchmark.getId().equals(id)) {
            throw new IllegalArgumentException("Instrument cannot be its own benchmark");
        }
        this.name = name;
        this.market = market;
        this.type = type;
        this.currency = currency;
        this.yahooSymbol = yahooSymbol;
        this.newsQuery = newsQuery;
        this.benchmark = benchmark;
        this.analyzed = analyzed;
        this.tradingEnabled = tradingEnabled;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Market getMarket() {
        return market;
    }

    public InstrumentType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public String getYahooSymbol() {
        return yahooSymbol;
    }

    public String getNewsQuery() {
        return newsQuery;
    }

    public Instrument getBenchmark() {
        return benchmark;
    }

    public boolean isAnalyzed() {
        return analyzed;
    }

    public boolean isTradingEnabled() {
        return tradingEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

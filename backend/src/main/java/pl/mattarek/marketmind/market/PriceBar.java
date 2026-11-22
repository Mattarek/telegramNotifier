package pl.mattarek.marketmind.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "price_bar")
public class PriceBar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "bar_date", nullable = false)
    private LocalDate barDate;

    private Double open;
    private Double high;
    private Double low;

    @Column(nullable = false)
    private double close;

    private Long volume;

    protected PriceBar() {
        // for JPA
    }

    public PriceBar(final Long instrumentId, final Bar bar) {
        this.instrumentId = instrumentId;
        this.barDate = bar.date();
        apply(bar);
    }

    /**
     * The most recent bar can change during the session, so we overwrite it on every sync.
     */
    public void apply(final Bar bar) {
        this.open = bar.open();
        this.high = bar.high();
        this.low = bar.low();
        this.close = bar.close();
        this.volume = bar.volume();
    }

    public Bar toBar() {
        return new Bar(barDate,
                open == null ? close : open,
                high == null ? close : high,
                low == null ? close : low,
                close,
                volume == null ? 0 : volume);
    }

    public Long getId() {
        return id;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public LocalDate getBarDate() {
        return barDate;
    }

    public double getClose() {
        return close;
    }
}

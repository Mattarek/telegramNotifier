package pl.mattarek.marketmind.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.config.AppProperties;
import pl.mattarek.marketmind.instrument.Instrument;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final PriceProvider priceProvider;
    private final PriceBarRepository priceBarRepository;
    private final AppProperties properties;

    public MarketDataService(final PriceProvider priceProvider,
                             final PriceBarRepository priceBarRepository,
                             final AppProperties properties) {
        this.priceProvider = priceProvider;
        this.priceBarRepository = priceBarRepository;
        this.properties = properties;
    }

    /**
     * Downloads daily bars and upserts them. Returns how many bars were inserted or updated.
     */
    @Transactional
    public int sync(final Instrument instrument) {
        final List<Bar> fetched = priceProvider.fetchDaily(instrument.getYahooSymbol(),
                properties.market().historyRange());
        if (fetched.isEmpty()) {
            log.warn("No bars for {}", instrument.getSymbol());
            return 0;
        }
        final Map<LocalDate, PriceBar> existing = priceBarRepository
                .findByInstrumentIdAndBarDateGreaterThanEqualOrderByBarDateAsc(instrument.getId(),
                        fetched.getFirst().date())
                .stream()
                .collect(Collectors.toMap(PriceBar::getBarDate, Function.identity()));

        int changed = 0;
        for (final Bar bar : fetched) {
            final PriceBar stored = existing.get(bar.date());
            if (stored == null) {
                priceBarRepository.save(new PriceBar(instrument.getId(), bar));
                changed++;
            } else if (stored.getClose() != bar.close()) {
                stored.apply(bar);
                changed++;
            }
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public List<Bar> bars(final long instrumentId) {
        return priceBarRepository.findByInstrumentIdOrderByBarDateAsc(instrumentId).stream()
                .map(PriceBar::toBar)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Bar> barsAfter(final long instrumentId, final LocalDate after) {
        return priceBarRepository.findByInstrumentIdAndBarDateAfterOrderByBarDateAsc(instrumentId, after).stream()
                .map(PriceBar::toBar)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Bar> lastBar(final long instrumentId) {
        return priceBarRepository.findFirstByInstrumentIdOrderByBarDateDesc(instrumentId).map(PriceBar::toBar);
    }

    @Transactional(readOnly = true)
    public Optional<Bar> barOnOrBefore(final long instrumentId, final LocalDate date) {
        return priceBarRepository.findFirstByInstrumentIdAndBarDateLessThanEqualOrderByBarDateDesc(instrumentId, date)
                .map(PriceBar::toBar);
    }

    @Transactional(readOnly = true)
    public Optional<MarketSnapshot> snapshot(final Instrument instrument) {
        final List<Bar> bars = bars(instrument.getId());
        if (bars.size() < 30) {
            return Optional.empty();
        }
        final List<Bar> benchmarkBars = instrument.getBenchmark() == null
                ? List.of()
                : bars(instrument.getBenchmark().getId());
        return Optional.of(MarketSnapshot.of(bars, benchmarkBars));
    }
}

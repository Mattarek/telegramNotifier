package pl.mattarek.marketmind.instrument;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.shared.NotFoundException;

import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(final InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public List<InstrumentDto> findAll() {
        return instrumentRepository.findAllByOrderBySymbol().stream()
                .map(InstrumentDto::from)
                .toList();
    }

    /**
     * Everything we download prices for (watchlist + benchmarks).
     */
    @Transactional(readOnly = true)
    public List<Instrument> activeInstruments() {
        return instrumentRepository.findByActiveTrueOrderBySymbol();
    }

    /**
     * Everything the AI analyses every day (benchmarks excluded).
     */
    @Transactional(readOnly = true)
    public List<Instrument> analyzedInstruments() {
        return instrumentRepository.findByActiveTrueAndAnalyzedTrueOrderBySymbol();
    }

    @Transactional(readOnly = true)
    public Instrument getById(final long id) {
        return instrumentRepository.findWithBenchmarkById(id)
                .orElseThrow(() -> new NotFoundException("Instrument " + id + " not found"));
    }

    @Transactional
    public InstrumentDto create(final InstrumentRequest request) {
        final String symbol = request.symbol().toUpperCase();
        if (instrumentRepository.existsBySymbol(symbol)) {
            throw new IllegalArgumentException("Instrument " + symbol + " already exists");
        }
        final Instrument instrument = new Instrument(symbol, request.name(), request.market(), request.type(),
                request.yahooSymbol());
        applyRequest(instrument, request);
        return InstrumentDto.from(instrumentRepository.save(instrument));
    }

    @Transactional
    public InstrumentDto update(final long id, final InstrumentRequest request) {
        final Instrument instrument = getById(id);
        applyRequest(instrument, request);
        return InstrumentDto.from(instrument);
    }

    private void applyRequest(final Instrument instrument, final InstrumentRequest request) {
        final Instrument benchmark = request.benchmarkId() == null ? null : getById(request.benchmarkId());
        instrument.update(request.name(), request.market(), request.type(), request.currency(),
                request.yahooSymbol(), request.newsQuery(), benchmark, request.analyzed(),
                request.tradingEnabled(), request.active());
    }
}

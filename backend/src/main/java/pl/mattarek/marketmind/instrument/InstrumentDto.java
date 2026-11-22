package pl.mattarek.marketmind.instrument;

public record InstrumentDto(Long id,
                            String symbol,
                            String name,
                            Market market,
                            InstrumentType type,
                            String currency,
                            String yahooSymbol,
                            String newsQuery,
                            Long benchmarkId,
                            String benchmarkSymbol,
                            boolean analyzed,
                            boolean tradingEnabled,
                            boolean active) {

    public static InstrumentDto from(final Instrument instrument) {
        final Instrument benchmark = instrument.getBenchmark();
        return new InstrumentDto(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getMarket(),
                instrument.getType(),
                instrument.getCurrency(),
                instrument.getYahooSymbol(),
                instrument.getNewsQuery(),
                benchmark == null ? null : benchmark.getId(),
                benchmark == null ? null : benchmark.getSymbol(),
                instrument.isAnalyzed(),
                instrument.isTradingEnabled(),
                instrument.isActive());
    }
}

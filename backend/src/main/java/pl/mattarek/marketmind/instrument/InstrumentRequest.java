package pl.mattarek.marketmind.instrument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstrumentRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._^-]+") String symbol,
        @NotBlank @Size(max = 200) String name,
        @NotNull Market market,
        @NotNull InstrumentType type,
        @Size(max = 8) String currency,
        @NotBlank @Size(max = 32) String yahooSymbol,
        @Size(max = 200) String newsQuery,
        Long benchmarkId,
        boolean analyzed,
        boolean tradingEnabled,
        boolean active) {
}

package pl.mattarek.marketmind.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceBarRepository extends JpaRepository<PriceBar, Long> {

    List<PriceBar> findByInstrumentIdOrderByBarDateAsc(Long instrumentId);

    List<PriceBar> findByInstrumentIdAndBarDateGreaterThanEqualOrderByBarDateAsc(Long instrumentId, LocalDate from);

    List<PriceBar> findByInstrumentIdAndBarDateAfterOrderByBarDateAsc(Long instrumentId, LocalDate after);

    Optional<PriceBar> findFirstByInstrumentIdOrderByBarDateDesc(Long instrumentId);

    Optional<PriceBar> findFirstByInstrumentIdAndBarDateLessThanEqualOrderByBarDateDesc(Long instrumentId,
                                                                                       LocalDate date);
}

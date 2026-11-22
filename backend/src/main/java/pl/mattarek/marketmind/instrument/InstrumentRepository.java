package pl.mattarek.marketmind.instrument;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * The benchmark is loaded together with the instrument (EntityGraph), because almost every
 * caller needs it and we work with open-in-view disabled (no lazy loading outside transactions).
 */
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    @EntityGraph(attributePaths = "benchmark")
    List<Instrument> findByActiveTrueOrderBySymbol();

    @EntityGraph(attributePaths = "benchmark")
    List<Instrument> findByActiveTrueAndAnalyzedTrueOrderBySymbol();

    @EntityGraph(attributePaths = "benchmark")
    List<Instrument> findAllByOrderBySymbol();

    @EntityGraph(attributePaths = "benchmark")
    Optional<Instrument> findWithBenchmarkById(Long id);

    boolean existsBySymbol(String symbol);
}

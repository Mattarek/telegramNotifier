package pl.mattarek.marketmind.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Source;

import java.time.LocalDate;
import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    boolean existsByInstrumentIdAndModeAndSourceAndReferenceDate(Long instrumentId, Mode mode, Source source,
                                                                 LocalDate referenceDate);

    Optional<AnalysisReport> findFirstByInstrumentIdAndModeAndSourceOrderByReferenceDateDesc(Long instrumentId,
                                                                                            Mode mode,
                                                                                            Source source);
}

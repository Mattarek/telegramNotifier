package pl.mattarek.marketmind.prediction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByStatus(PredictionStatus status);

    List<Prediction> findByStatusAndSourceAndReviewedFalse(PredictionStatus status, Source source);

    List<Prediction> findByStatusAndResolvedDateGreaterThanEqual(PredictionStatus status, LocalDate from);

    List<Prediction> findByInstrumentIdAndModeOrderByReferenceDateDescHorizonDaysAsc(Long instrumentId, Mode mode,
                                                                                    Pageable pageable);

    List<Prediction> findByInstrumentIdAndModeAndSourceAndStatusOrderByResolvedDateDesc(
            Long instrumentId, Mode mode, Source source, PredictionStatus status, Pageable pageable);

    List<Prediction> findByModeAndStatusOrderByReferenceDateDesc(Mode mode, PredictionStatus status,
                                                                 Pageable pageable);

    List<Prediction> findByReportId(Long reportId);

    long countByStatus(PredictionStatus status);
}

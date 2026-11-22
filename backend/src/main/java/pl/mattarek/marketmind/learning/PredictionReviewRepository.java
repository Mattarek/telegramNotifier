package pl.mattarek.marketmind.learning;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PredictionReviewRepository extends JpaRepository<PredictionReview, Long> {

    List<PredictionReview> findByPredictionIdIn(Collection<Long> predictionIds);

    List<PredictionReview> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

package pl.mattarek.marketmind.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonEvidenceRepository extends JpaRepository<LessonEvidence, Long> {

    List<LessonEvidence> findByLessonIdOrderByCreatedAtDesc(Long lessonId);

    boolean existsByLessonIdAndPredictionId(Long lessonId, Long predictionId);
}

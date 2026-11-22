package pl.mattarek.marketmind.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByStatusInOrderByUpdatedAtDesc(Collection<LessonStatus> statuses);

    List<Lesson> findAllByOrderByStatusAscUpdatedAtDesc();

    long countByStatus(LessonStatus status);
}

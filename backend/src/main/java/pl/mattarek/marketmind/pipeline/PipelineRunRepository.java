package pl.mattarek.marketmind.pipeline;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    List<PipelineRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<PipelineRun> findFirstByOrderByStartedAtDesc();
}

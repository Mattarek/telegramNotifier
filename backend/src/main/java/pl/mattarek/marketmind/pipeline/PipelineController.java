package pl.mattarek.marketmind.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(final PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping("/run")
    public ResponseEntity<PipelineRunDto> run() {
        return pipelineService.startAsync(PipelineRun.Trigger.MANUAL)
                .map(run -> ResponseEntity.status(HttpStatus.ACCEPTED).body(run))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @GetMapping("/runs")
    public List<PipelineRunDto> runs() {
        return pipelineService.recentRuns(20);
    }
}

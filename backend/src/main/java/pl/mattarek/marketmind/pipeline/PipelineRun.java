package pl.mattarek.marketmind.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pipeline_run")
public class PipelineRun {

    public enum Trigger { SCHEDULED, MANUAL }

    public enum Status { RUNNING, SUCCESS, PARTIAL, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private Trigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    private String log;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    protected PipelineRun() {
        // for JPA
    }

    public PipelineRun(final Trigger trigger) {
        this.trigger = trigger;
        this.status = Status.RUNNING;
        this.startedAt = Instant.now();
        this.log = "";
    }

    public void appendLog(final String line) {
        log = log + line + "\n";
    }

    public void finish(final Status status, final long inputTokens, final long outputTokens) {
        this.status = status;
        this.finishedAt = Instant.now();
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public Long getId() {
        return id;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getLog() {
        return log;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }
}

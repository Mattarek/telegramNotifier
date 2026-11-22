package pl.mattarek.marketmind.pipeline;

import java.time.Instant;

public record PipelineRunDto(Long id,
                             PipelineRun.Trigger trigger,
                             PipelineRun.Status status,
                             Instant startedAt,
                             Instant finishedAt,
                             String log,
                             long inputTokens,
                             long outputTokens,
                             double estimatedCostUsd) {

    public static PipelineRunDto from(final PipelineRun run, final double inputPricePerMtok,
                                      final double outputPricePerMtok) {
        final double cost = run.getInputTokens() / 1_000_000.0 * inputPricePerMtok
                + run.getOutputTokens() / 1_000_000.0 * outputPricePerMtok;
        return new PipelineRunDto(run.getId(), run.getTrigger(), run.getStatus(), run.getStartedAt(),
                run.getFinishedAt(), run.getLog(), run.getInputTokens(), run.getOutputTokens(), cost);
    }
}

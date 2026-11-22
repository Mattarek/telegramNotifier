package pl.mattarek.marketmind.ai;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts tokens used by the Claude API, so every pipeline run can show what it cost.
 */
@Component
public class AiUsageMeter {

    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    public void record(final long input, final long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }

    public Usage snapshot() {
        return new Usage(inputTokens.get(), outputTokens.get());
    }

    public record Usage(long inputTokens, long outputTokens) {

        public Usage minus(final Usage other) {
            return new Usage(inputTokens - other.inputTokens, outputTokens - other.outputTokens);
        }
    }
}

package pl.mattarek.marketmind.learning;

/**
 * CANDIDATE - a hypothesis from a single post-mortem, shown to the AI as "under test".
 * ACTIVE    - confirmed several times, used as an established rule.
 * RETIRED   - contradicted by the evidence, no longer given to the AI (kept for history).
 */
public enum LessonStatus {
    CANDIDATE,
    ACTIVE,
    RETIRED
}

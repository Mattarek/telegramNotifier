package pl.mattarek.marketmind.learning;

import pl.mattarek.marketmind.prediction.Mode;

import java.time.Instant;
import java.util.List;

public record LessonDto(Long id,
                        String text,
                        String category,
                        LessonScope scope,
                        String scopeRef,
                        Mode mode,
                        LessonStatus status,
                        int supportCount,
                        int contradictCount,
                        Instant createdAt,
                        Instant updatedAt,
                        List<EvidenceDto> evidence) {

    public static LessonDto from(final Lesson lesson, final List<LessonEvidence> evidence) {
        return new LessonDto(lesson.getId(), lesson.getText(), lesson.getCategory(), lesson.getScope(),
                lesson.getScopeRef(), lesson.getMode(), lesson.getStatus(), lesson.getSupportCount(),
                lesson.getContradictCount(), lesson.getCreatedAt(), lesson.getUpdatedAt(),
                evidence.stream().map(EvidenceDto::from).toList());
    }

    public record EvidenceDto(Long predictionId, EvidenceKind kind, String note, Instant createdAt) {

        static EvidenceDto from(final LessonEvidence evidence) {
            return new EvidenceDto(evidence.getPredictionId(), evidence.getKind(), evidence.getNote(),
                    evidence.getCreatedAt());
        }
    }
}

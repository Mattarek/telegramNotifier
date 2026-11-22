package pl.mattarek.marketmind.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Why a lesson's counters changed - links the lesson to the prediction that confirmed or contradicted it.
 */
@Entity
@Table(name = "lesson_evidence")
public class LessonEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "prediction_id")
    private Long predictionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceKind kind;

    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LessonEvidence() {
        // for JPA
    }

    public LessonEvidence(final Long lessonId, final Long predictionId, final EvidenceKind kind, final String note) {
        this.lessonId = lessonId;
        this.predictionId = predictionId;
        this.kind = kind;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Long getPredictionId() {
        return predictionId;
    }

    public EvidenceKind getKind() {
        return kind;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

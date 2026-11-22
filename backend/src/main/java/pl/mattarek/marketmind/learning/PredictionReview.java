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
 * AI post-mortem of one resolved prediction: why it worked or didn't.
 */
@Entity
@Table(name = "prediction_review")
public class PredictionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prediction_id", nullable = false, unique = true)
    private Long predictionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verdict verdict;

    private String explanation;

    @Column(name = "actual_driver")
    private String actualDriver;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PredictionReview() {
        // for JPA
    }

    public PredictionReview(final Long predictionId, final Verdict verdict, final String explanation,
                            final String actualDriver) {
        this.predictionId = predictionId;
        this.verdict = verdict;
        this.explanation = explanation;
        this.actualDriver = actualDriver;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPredictionId() {
        return predictionId;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getActualDriver() {
        return actualDriver;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

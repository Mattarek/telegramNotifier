package pl.mattarek.marketmind.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One checkable claim: "in N trading days X will go UP/DOWN/FLAT, I am P% sure".
 * It stays OPEN until enough bars exist, then gets RESOLVED with the real outcome.
 */
@Entity
@Table(name = "prediction")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "report_id")
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "horizon_days", nullable = false)
    private int horizonDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(name = "expected_return_pct")
    private Double expectedReturnPct;

    @Column(nullable = false)
    private double confidence;

    private String rationale;

    @Column(name = "lessons_used_json")
    private String lessonsUsedJson;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "reference_price", nullable = false)
    private double referencePrice;

    @Column(name = "benchmark_reference_price")
    private Double benchmarkReferencePrice;

    @Column(name = "entry_price")
    private Double entryPrice;

    @Column(name = "stop_price")
    private Double stopPrice;

    @Column(name = "target_price")
    private Double targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionStatus status;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "resolved_price")
    private Double resolvedPrice;

    @Column(name = "benchmark_resolved_price")
    private Double benchmarkResolvedPrice;

    @Column(name = "actual_return_pct")
    private Double actualReturnPct;

    @Column(name = "benchmark_return_pct")
    private Double benchmarkReturnPct;

    @Column(name = "measured_return_pct")
    private Double measuredReturnPct;

    private Boolean hit;

    private Double brier;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_outcome")
    private TradeOutcome tradeOutcome;

    @Column(name = "r_multiple")
    private Double rMultiple;

    @Column(nullable = false)
    private boolean reviewed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Prediction() {
        // for JPA
    }

    public Prediction(final Long instrumentId, final Long reportId, final Mode mode, final Source source,
                      final int horizonDays, final Direction direction, final Double expectedReturnPct,
                      final double confidence, final String rationale, final String lessonsUsedJson,
                      final LocalDate referenceDate, final double referencePrice,
                      final Double benchmarkReferencePrice) {
        if (confidence < 0.5 || confidence > 0.95) {
            throw new IllegalArgumentException("Confidence must be between 0.5 and 0.95, was " + confidence);
        }
        this.instrumentId = instrumentId;
        this.reportId = reportId;
        this.mode = mode;
        this.source = source;
        this.horizonDays = horizonDays;
        this.direction = direction;
        this.expectedReturnPct = expectedReturnPct;
        this.confidence = confidence;
        this.rationale = rationale;
        this.lessonsUsedJson = lessonsUsedJson;
        this.referenceDate = referenceDate;
        this.referencePrice = referencePrice;
        this.benchmarkReferencePrice = benchmarkReferencePrice;
        this.status = PredictionStatus.OPEN;
        // Baseline predictions are never reviewed by the AI
        this.reviewed = source == Source.BASELINE;
        this.createdAt = Instant.now();
    }

    /**
     * Attaches a swing-trade plan. Levels must make sense for the direction, otherwise they are rejected.
     */
    public void attachTradePlan(final Double entry, final double stop, final double target) {
        final boolean validLong = direction == Direction.UP && stop < referencePrice && target > referencePrice;
        final boolean validShort = direction == Direction.DOWN && stop > referencePrice && target < referencePrice;
        if (!validLong && !validShort) {
            throw new IllegalArgumentException("Trade levels do not match direction " + direction);
        }
        this.entryPrice = entry;
        this.stopPrice = stop;
        this.targetPrice = target;
    }

    public boolean hasTradePlan() {
        return stopPrice != null && targetPrice != null;
    }

    public void resolve(final LocalDate resolvedDate, final double resolvedPrice, final Double benchmarkResolvedPrice,
                        final double actualReturnPct, final Double benchmarkReturnPct,
                        final double measuredReturnPct, final PredictionScorer.Score score) {
        if (status == PredictionStatus.RESOLVED) {
            throw new IllegalStateException("Prediction " + id + " is already resolved");
        }
        this.status = PredictionStatus.RESOLVED;
        this.resolvedDate = resolvedDate;
        this.resolvedPrice = resolvedPrice;
        this.benchmarkResolvedPrice = benchmarkResolvedPrice;
        this.actualReturnPct = actualReturnPct;
        this.benchmarkReturnPct = benchmarkReturnPct;
        this.measuredReturnPct = measuredReturnPct;
        this.hit = score.hit();
        this.brier = score.brier();
    }

    public void recordTrade(final TradeOutcomeEvaluator.Result result) {
        this.entryPrice = result.entry();
        this.tradeOutcome = result.outcome();
        this.rMultiple = result.rMultiple();
    }

    public void markReviewed() {
        this.reviewed = true;
    }

    public Long getId() {
        return id;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public Long getReportId() {
        return reportId;
    }

    public Mode getMode() {
        return mode;
    }

    public Source getSource() {
        return source;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public Direction getDirection() {
        return direction;
    }

    public Double getExpectedReturnPct() {
        return expectedReturnPct;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getRationale() {
        return rationale;
    }

    public String getLessonsUsedJson() {
        return lessonsUsedJson;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public double getReferencePrice() {
        return referencePrice;
    }

    public Double getBenchmarkReferencePrice() {
        return benchmarkReferencePrice;
    }

    public Double getEntryPrice() {
        return entryPrice;
    }

    public Double getStopPrice() {
        return stopPrice;
    }

    public Double getTargetPrice() {
        return targetPrice;
    }

    public PredictionStatus getStatus() {
        return status;
    }

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public Double getResolvedPrice() {
        return resolvedPrice;
    }

    public Double getActualReturnPct() {
        return actualReturnPct;
    }

    public Double getBenchmarkReturnPct() {
        return benchmarkReturnPct;
    }

    public Double getMeasuredReturnPct() {
        return measuredReturnPct;
    }

    public Boolean getHit() {
        return hit;
    }

    public Double getBrier() {
        return brier;
    }

    public TradeOutcome getTradeOutcome() {
        return tradeOutcome;
    }

    public Double getRMultiple() {
        return rMultiple;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

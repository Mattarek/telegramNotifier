package pl.mattarek.marketmind.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Source;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The written analysis of one instrument for one day. Predictions point to it via report_id.
 */
@Entity
@Table(name = "analysis_report")
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    private String summary;

    private Double sentiment;

    @Column(name = "key_factors_json")
    private String keyFactorsJson;

    @Column(name = "risks_json")
    private String risksJson;

    private String invalidation;

    @Column(name = "trade_bias")
    private String tradeBias;

    @Column(name = "setup_type")
    private String setupType;

    @Column(name = "entry_price")
    private Double entryPrice;

    @Column(name = "stop_price")
    private Double stopPrice;

    @Column(name = "target_price")
    private Double targetPrice;

    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnalysisReport() {
        // for JPA
    }

    public AnalysisReport(final Long instrumentId, final Mode mode, final Source source,
                          final LocalDate referenceDate, final String summary, final Double sentiment,
                          final String keyFactorsJson, final String risksJson, final String invalidation) {
        this.instrumentId = instrumentId;
        this.mode = mode;
        this.source = source;
        this.referenceDate = referenceDate;
        this.summary = summary;
        this.sentiment = sentiment;
        this.keyFactorsJson = keyFactorsJson;
        this.risksJson = risksJson;
        this.invalidation = invalidation;
        this.createdAt = Instant.now();
    }

    public void setTradePlan(final String bias, final String setupType, final Double entry, final Double stop,
                             final Double target) {
        this.tradeBias = bias;
        this.setupType = setupType;
        this.entryPrice = entry;
        this.stopPrice = stop;
        this.targetPrice = target;
    }

    public void setAiUsage(final String model, final int inputTokens, final int outputTokens) {
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public Long getId() {
        return id;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public Mode getMode() {
        return mode;
    }

    public Source getSource() {
        return source;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public String getSummary() {
        return summary;
    }

    public Double getSentiment() {
        return sentiment;
    }

    public String getKeyFactorsJson() {
        return keyFactorsJson;
    }

    public String getRisksJson() {
        return risksJson;
    }

    public String getInvalidation() {
        return invalidation;
    }

    public String getTradeBias() {
        return tradeBias;
    }

    public String getSetupType() {
        return setupType;
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

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

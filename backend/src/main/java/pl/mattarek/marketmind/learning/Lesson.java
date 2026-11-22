package pl.mattarek.marketmind.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.mattarek.marketmind.prediction.Mode;

import java.time.Instant;

@Entity
@Table(name = "lesson")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonScope scope;

    /**
     * Market name or instrument symbol, depending on the scope. null for GLOBAL.
     */
    @Column(name = "scope_ref")
    private String scopeRef;

    /**
     * null = applies to both INVEST and TRADE.
     */
    @Enumerated(EnumType.STRING)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status;

    @Column(name = "support_count", nullable = false)
    private int supportCount;

    @Column(name = "contradict_count", nullable = false)
    private int contradictCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
        // for JPA
    }

    public Lesson(final String text, final String category, final LessonScope scope, final String scopeRef,
                  final Mode mode) {
        this.text = text;
        this.category = category;
        this.scope = scope;
        this.scopeRef = scope == LessonScope.GLOBAL ? null : scopeRef;
        this.mode = mode;
        this.status = LessonStatus.CANDIDATE;
        this.supportCount = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addEvidence(final EvidenceKind kind) {
        if (kind == EvidenceKind.SUPPORT) {
            supportCount++;
        } else {
            contradictCount++;
        }
        status = LessonPolicy.nextStatus(status, supportCount, contradictCount);
        updatedAt = Instant.now();
    }

    public boolean appliesTo(final String market, final String symbol, final Mode targetMode) {
        if (status == LessonStatus.RETIRED) {
            return false;
        }
        if (mode != null && mode != targetMode) {
            return false;
        }
        return switch (scope) {
            case GLOBAL -> true;
            case MARKET -> market.equalsIgnoreCase(scopeRef);
            case INSTRUMENT -> symbol.equalsIgnoreCase(scopeRef);
        };
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    public LessonScope getScope() {
        return scope;
    }

    public String getScopeRef() {
        return scopeRef;
    }

    public Mode getMode() {
        return mode;
    }

    public LessonStatus getStatus() {
        return status;
    }

    public int getSupportCount() {
        return supportCount;
    }

    public int getContradictCount() {
        return contradictCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

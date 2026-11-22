package pl.mattarek.marketmind.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;
import pl.mattarek.marketmind.prediction.PredictionRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the long-term memory: post-mortems and lessons, and the rules for changing them.
 */
@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);
    private static final int MAX_LESSONS_IN_PROMPT = 20;
    private static final int MAX_NEW_LESSONS_PER_REVIEW = 2;

    private final LessonRepository lessonRepository;
    private final LessonEvidenceRepository evidenceRepository;
    private final PredictionReviewRepository reviewRepository;
    private final PredictionRepository predictionRepository;

    public LessonService(final LessonRepository lessonRepository,
                         final LessonEvidenceRepository evidenceRepository,
                         final PredictionReviewRepository reviewRepository,
                         final PredictionRepository predictionRepository) {
        this.lessonRepository = lessonRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewRepository = reviewRepository;
        this.predictionRepository = predictionRepository;
    }

    /**
     * Lessons the AI should consider for this instrument: established ones first, then candidates.
     */
    @Transactional(readOnly = true)
    public List<Lesson> lessonsFor(final String market, final String symbol, final Mode mode) {
        return lessonRepository
                .findByStatusInOrderByUpdatedAtDesc(List.of(LessonStatus.ACTIVE, LessonStatus.CANDIDATE))
                .stream()
                .filter(lesson -> lesson.appliesTo(market, symbol, mode))
                .sorted(Comparator.comparing((Lesson lesson) -> lesson.getStatus() != LessonStatus.ACTIVE)
                        .thenComparing(Lesson::getSupportCount, Comparator.reverseOrder()))
                .limit(MAX_LESSONS_IN_PROMPT)
                .toList();
    }

    /**
     * Saves the AI post-mortem and updates lesson counters - but only with "informative" outcomes.
     * Noise and unforeseeable events never strengthen or weaken a lesson.
     */
    @Transactional
    public ApplyResult applyPostMortem(final AiModels.PostMortem postMortem,
                                       final Map<Long, PredictionContext> contexts) {
        final Map<Long, Verdict> verdicts = new HashMap<>();
        int reviews = 0;
        for (final AiModels.Review review : safe(postMortem.reviews())) {
            final PredictionContext context = contexts.get(review.predictionId());
            if (context == null || verdicts.containsKey(review.predictionId())) {
                continue;
            }
            final Verdict verdict = Verdict.parse(review.verdict());
            verdicts.put(review.predictionId(), verdict);
            reviewRepository.save(new PredictionReview(review.predictionId(), verdict, review.explanation(),
                    review.actualDriver()));
            reviews++;
        }

        final int supported = applyEvidence(postMortem.supportedLessons(), EvidenceKind.SUPPORT, verdicts);
        final int contradicted = applyEvidence(postMortem.contradictedLessons(), EvidenceKind.CONTRADICT, verdicts);

        int created = 0;
        for (final AiModels.NewLesson newLesson : safe(postMortem.newLessons())) {
            if (created >= MAX_NEW_LESSONS_PER_REVIEW) {
                break;
            }
            final PredictionContext context = contexts.get(newLesson.predictionId());
            final Verdict verdict = verdicts.get(newLesson.predictionId());
            if (context == null || verdict == null || !verdict.isInformative()
                    || newLesson.text() == null || newLesson.text().isBlank()) {
                continue;
            }
            final LessonScope scope = parseScope(newLesson.scope());
            final String scopeRef = switch (scope) {
                case GLOBAL -> null;
                case MARKET -> context.market();
                case INSTRUMENT -> context.symbol();
            };
            final Mode mode = parseMode(newLesson.mode());
            final Lesson lesson = lessonRepository.save(new Lesson(newLesson.text().strip(), newLesson.category(),
                    scope, scopeRef, mode));
            evidenceRepository.save(new LessonEvidence(lesson.getId(), newLesson.predictionId(),
                    EvidenceKind.SUPPORT, "Lekcja utworzona na podstawie tej predykcji"));
            created++;
        }

        // Every prediction we sent gets marked, even if the AI skipped it - we don't retry forever
        for (final PredictionContext context : contexts.values()) {
            context.prediction().markReviewed();
            predictionRepository.save(context.prediction());
        }
        return new ApplyResult(reviews, supported, contradicted, created);
    }

    @Transactional(readOnly = true)
    public List<LessonDto> allLessons() {
        return lessonRepository.findAllByOrderByStatusAscUpdatedAtDesc().stream()
                .map(lesson -> LessonDto.from(lesson, evidenceRepository.findByLessonIdOrderByCreatedAtDesc(
                        lesson.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<LessonStatus, Long> countsByStatus() {
        final Map<LessonStatus, Long> counts = new HashMap<>();
        for (final LessonStatus status : LessonStatus.values()) {
            counts.put(status, lessonRepository.countByStatus(status));
        }
        return counts;
    }

    private int applyEvidence(final List<AiModels.LessonEvidenceItem> items, final EvidenceKind kind,
                              final Map<Long, Verdict> verdicts) {
        int applied = 0;
        for (final AiModels.LessonEvidenceItem item : safe(items)) {
            final Verdict verdict = verdicts.get(item.predictionId());
            if (verdict == null || !verdict.isInformative()) {
                continue;
            }
            if (evidenceRepository.existsByLessonIdAndPredictionId(item.lessonId(), item.predictionId())) {
                continue;
            }
            final Optional<Lesson> lesson = lessonRepository.findById(item.lessonId());
            if (lesson.isEmpty() || lesson.get().getStatus() == LessonStatus.RETIRED) {
                log.debug("Skipping evidence for unknown or retired lesson {}", item.lessonId());
                continue;
            }
            lesson.get().addEvidence(kind);
            evidenceRepository.save(new LessonEvidence(item.lessonId(), item.predictionId(), kind, item.note()));
            applied++;
        }
        return applied;
    }

    private static LessonScope parseScope(final String value) {
        try {
            return value == null ? LessonScope.GLOBAL : LessonScope.valueOf(value.toUpperCase());
        } catch (final IllegalArgumentException exception) {
            return LessonScope.GLOBAL;
        }
    }

    /**
     * null means "applies to both tabs".
     */
    private static Mode parseMode(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return Mode.valueOf(value.toUpperCase());
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private static <T> List<T> safe(final List<T> list) {
        return list == null ? List.of() : list;
    }

    public record PredictionContext(Prediction prediction, String market, String symbol) {
    }

    public record ApplyResult(int reviews, int supported, int contradicted, int created) {
    }
}

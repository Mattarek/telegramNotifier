package pl.mattarek.marketmind.analysis;

import org.springframework.stereotype.Service;
import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.ai.AiTools;
import pl.mattarek.marketmind.ai.ClaudeClient;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.instrument.InstrumentService;
import pl.mattarek.marketmind.learning.Lesson;
import pl.mattarek.marketmind.learning.LessonService;
import pl.mattarek.marketmind.news.NewsItem;
import pl.mattarek.marketmind.news.NewsService;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;
import pl.mattarek.marketmind.prediction.PredictionRepository;
import pl.mattarek.marketmind.prediction.PredictionStatus;
import pl.mattarek.marketmind.prediction.Source;
import pl.mattarek.marketmind.shared.JsonSupport;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Next day" learning: the AI looks at its resolved predictions, explains why they worked or not,
 * and proposes small changes to the lesson memory (which LessonService then applies with strict rules).
 */
@Service
public class PostMortemService {

    private static final int MAX_PREDICTIONS_PER_CALL = 10;
    private static final int MAX_PREDICTIONS_PER_RUN = 40;
    private static final int MAX_NEWS = 30;

    private final PredictionRepository predictionRepository;
    private final AnalysisReportRepository reportRepository;
    private final InstrumentService instrumentService;
    private final NewsService newsService;
    private final LessonService lessonService;
    private final PromptFactory promptFactory;
    private final ClaudeClient claudeClient;
    private final JsonSupport json;

    public PostMortemService(final PredictionRepository predictionRepository,
                             final AnalysisReportRepository reportRepository,
                             final InstrumentService instrumentService,
                             final NewsService newsService,
                             final LessonService lessonService,
                             final PromptFactory promptFactory,
                             final ClaudeClient claudeClient,
                             final JsonSupport json) {
        this.predictionRepository = predictionRepository;
        this.reportRepository = reportRepository;
        this.instrumentService = instrumentService;
        this.newsService = newsService;
        this.lessonService = lessonService;
        this.promptFactory = promptFactory;
        this.claudeClient = claudeClient;
        this.json = json;
    }

    public List<String> reviewPending() {
        final List<String> log = new ArrayList<>();
        if (!claudeClient.isEnabled()) {
            log.add("Post-mortem pominięty (brak ANTHROPIC_API_KEY)");
            return log;
        }
        final List<Prediction> pending = predictionRepository
                .findByStatusAndSourceAndReviewedFalse(PredictionStatus.RESOLVED, Source.AI).stream()
                .sorted(Comparator.comparing(Prediction::getResolvedDate))
                .limit(MAX_PREDICTIONS_PER_RUN)
                .toList();
        if (pending.isEmpty()) {
            log.add("Brak nowych rozliczonych predykcji do przeglądu");
            return log;
        }

        final Map<Long, List<Prediction>> byInstrument = pending.stream()
                .collect(Collectors.groupingBy(Prediction::getInstrumentId, LinkedHashMap::new, Collectors.toList()));
        for (final Map.Entry<Long, List<Prediction>> entry : byInstrument.entrySet()) {
            final Instrument instrument = instrumentService.getById(entry.getKey());
            final List<Prediction> predictions = entry.getValue();
            for (int from = 0; from < predictions.size(); from += MAX_PREDICTIONS_PER_CALL) {
                final List<Prediction> batch = predictions.subList(from,
                        Math.min(predictions.size(), from + MAX_PREDICTIONS_PER_CALL));
                try {
                    log.add(reviewBatch(instrument, batch));
                } catch (final RuntimeException exception) {
                    log.add(instrument.getSymbol() + ": post-mortem nieudany - " + exception.getMessage());
                }
            }
        }
        return log;
    }

    private String reviewBatch(final Instrument instrument, final List<Prediction> batch) {
        final List<PromptFactory.PostMortemItem> items = batch.stream()
                .map(prediction -> new PromptFactory.PostMortemItem(prediction, prediction.getReportId() == null
                        ? null
                        : reportRepository.findById(prediction.getReportId())
                                .map(AnalysisReport::getSummary).orElse(null)))
                .toList();

        final LocalDate start = batch.stream().map(Prediction::getReferenceDate)
                .min(Comparator.naturalOrder()).orElseThrow();
        final LocalDate end = batch.stream().map(Prediction::getResolvedDate)
                .max(Comparator.naturalOrder()).orElseThrow();
        final List<NewsItem> news = newsService.between(instrument.getId(),
                start.atStartOfDay().toInstant(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        final Map<Long, Lesson> lessons = new LinkedHashMap<>();
        for (final Mode mode : Mode.values()) {
            lessonService.lessonsFor(instrument.getMarket().name(), instrument.getSymbol(), mode)
                    .forEach(lesson -> lessons.putIfAbsent(lesson.getId(), lesson));
        }

        final ClaudeClient.ToolCall call = claudeClient.callTool(
                promptFactory.postMortemSystemPrompt(),
                promptFactory.postMortemUserPrompt(instrument, items,
                        news.size() > MAX_NEWS ? news.subList(0, MAX_NEWS) : news,
                        List.copyOf(lessons.values())),
                AiTools.POST_MORTEM);
        final AiModels.PostMortem postMortem = json.convert(call.input(), AiModels.PostMortem.class);

        final Map<Long, LessonService.PredictionContext> contexts = new LinkedHashMap<>();
        for (final Prediction prediction : batch) {
            contexts.put(prediction.getId(), new LessonService.PredictionContext(prediction,
                    instrument.getMarket().name(), instrument.getSymbol()));
        }
        final LessonService.ApplyResult result = lessonService.applyPostMortem(postMortem, contexts);
        return "%s: przegląd %d predykcji, lekcje +%d potwierdzeń / %d zaprzeczeń / %d nowych".formatted(
                instrument.getSymbol(), result.reviews(), result.supported(), result.contradicted(),
                result.created());
    }
}

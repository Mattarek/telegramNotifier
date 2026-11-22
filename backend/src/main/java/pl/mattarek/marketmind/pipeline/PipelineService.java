package pl.mattarek.marketmind.pipeline;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.mattarek.marketmind.ai.AiUsageMeter;
import pl.mattarek.marketmind.analysis.AnalysisService;
import pl.mattarek.marketmind.analysis.PostMortemService;
import pl.mattarek.marketmind.config.AppProperties;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.instrument.InstrumentService;
import pl.mattarek.marketmind.market.MarketDataService;
import pl.mattarek.marketmind.news.NewsService;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.PredictionService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The daily loop:
 * 1. download prices, 2. download news, 3. resolve predictions whose horizon passed,
 * 4. AI post-mortems + lesson updates ("learn from yesterday"), 5. new analyses for today.
 *
 * <p>Every step is isolated per instrument - one failing feed or API call does not stop the rest.
 */
@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final InstrumentService instrumentService;
    private final MarketDataService marketDataService;
    private final NewsService newsService;
    private final PredictionService predictionService;
    private final PostMortemService postMortemService;
    private final AnalysisService analysisService;
    private final PipelineRunRepository runRepository;
    private final AiUsageMeter usageMeter;
    private final AppProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "pipeline"));

    public PipelineService(final InstrumentService instrumentService,
                           final MarketDataService marketDataService,
                           final NewsService newsService,
                           final PredictionService predictionService,
                           final PostMortemService postMortemService,
                           final AnalysisService analysisService,
                           final PipelineRunRepository runRepository,
                           final AiUsageMeter usageMeter,
                           final AppProperties properties) {
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.predictionService = predictionService;
        this.postMortemService = postMortemService;
        this.analysisService = analysisService;
        this.runRepository = runRepository;
        this.usageMeter = usageMeter;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.pipeline.cron}", zone = "${app.pipeline.zone}")
    void scheduledRun() {
        if (!properties.pipeline().enabled()) {
            return;
        }
        startAsync(PipelineRun.Trigger.SCHEDULED);
    }

    /**
     * Starts a run in the background. Returns empty if a run is already in progress.
     */
    public Optional<PipelineRunDto> startAsync(final PipelineRun.Trigger trigger) {
        if (!running.compareAndSet(false, true)) {
            return Optional.empty();
        }
        final PipelineRun run = runRepository.save(new PipelineRun(trigger));
        executor.submit(() -> {
            try {
                execute(run);
            } finally {
                running.set(false);
            }
        });
        return Optional.of(toDto(run));
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<PipelineRunDto> recentRuns(final int limit) {
        return runRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<PipelineRunDto> lastRun() {
        return runRepository.findFirstByOrderByStartedAtDesc().map(this::toDto);
    }

    private void execute(final PipelineRun run) {
        final AiUsageMeter.Usage usageBefore = usageMeter.snapshot();
        int failures = 0;
        try {
            step(run, "1/5 Notowania");
            for (final Instrument instrument : instrumentService.activeInstruments()) {
                try {
                    final int changed = marketDataService.sync(instrument);
                    run.appendLog("  " + instrument.getSymbol() + ": " + changed + " nowych/zmienionych świec");
                } catch (final RuntimeException exception) {
                    failures++;
                    run.appendLog("  " + instrument.getSymbol() + ": BŁĄD notowań - " + exception.getMessage());
                }
            }
            save(run);

            step(run, "2/5 Newsy");
            final List<Instrument> analyzed = instrumentService.analyzedInstruments();
            for (final Instrument instrument : analyzed) {
                try {
                    run.appendLog("  " + instrument.getSymbol() + ": " + newsService.fetchFor(instrument)
                            + " nowych");
                } catch (final RuntimeException exception) {
                    failures++;
                    run.appendLog("  " + instrument.getSymbol() + ": BŁĄD newsów - " + exception.getMessage());
                }
            }
            run.appendLog("  ogólne: " + newsService.fetchGeneral() + " nowych");
            save(run);

            step(run, "3/5 Rozliczanie predykcji");
            run.appendLog("  rozliczono: " + predictionService.resolveDue());
            save(run);

            step(run, "4/5 Nauka - przegląd wczorajszych wniosków");
            postMortemService.reviewPending().forEach(line -> run.appendLog("  " + line));
            save(run);

            step(run, "5/5 Nowe analizy");
            for (final Instrument instrument : analyzed) {
                failures += analyzeSafely(run, instrument, Mode.INVEST);
                if (instrument.isTradingEnabled()) {
                    failures += analyzeSafely(run, instrument, Mode.TRADE);
                }
                save(run);
            }

            finish(run, failures == 0 ? PipelineRun.Status.SUCCESS : PipelineRun.Status.PARTIAL, usageBefore);
        } catch (final RuntimeException exception) {
            log.error("Pipeline run {} failed", run.getId(), exception);
            run.appendLog("BŁĄD KRYTYCZNY: " + exception.getMessage());
            finish(run, PipelineRun.Status.FAILED, usageBefore);
        }
    }

    private int analyzeSafely(final PipelineRun run, final Instrument instrument, final Mode mode) {
        try {
            analysisService.analyze(instrument, mode).forEach(line -> run.appendLog("  " + line));
            return 0;
        } catch (final RuntimeException exception) {
            log.warn("Analysis {} {} failed", instrument.getSymbol(), mode, exception);
            run.appendLog("  " + instrument.getSymbol() + " " + mode + ": BŁĄD analizy - " + exception.getMessage());
            return 1;
        }
    }

    private void step(final PipelineRun run, final String name) {
        log.info("Pipeline {}: {}", run.getId(), name);
        run.appendLog(name);
    }

    private void finish(final PipelineRun run, final PipelineRun.Status status,
                        final AiUsageMeter.Usage usageBefore) {
        final AiUsageMeter.Usage used = usageMeter.snapshot().minus(usageBefore);
        run.finish(status, used.inputTokens(), used.outputTokens());
        save(run);
    }

    private void save(final PipelineRun run) {
        runRepository.save(run);
    }

    private PipelineRunDto toDto(final PipelineRun run) {
        return PipelineRunDto.from(run, properties.ai().inputPricePerMtok(), properties.ai().outputPricePerMtok());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

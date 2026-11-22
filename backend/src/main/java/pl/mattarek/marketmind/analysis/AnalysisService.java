package pl.mattarek.marketmind.analysis;

import org.springframework.stereotype.Service;
import pl.mattarek.marketmind.ai.AiModels;
import pl.mattarek.marketmind.ai.AiTools;
import pl.mattarek.marketmind.ai.ClaudeClient;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.market.MarketDataService;
import pl.mattarek.marketmind.market.MarketSnapshot;
import pl.mattarek.marketmind.prediction.BaselineForecaster;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Source;
import pl.mattarek.marketmind.shared.JsonSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Daily analysis of one instrument: the baseline rule always, the AI when an API key is configured.
 * Runs at most once per instrument, mode and trading day.
 */
@Service
public class AnalysisService {

    private final MarketDataService marketDataService;
    private final AnalysisReportRepository reportRepository;
    private final AnalysisContextLoader contextLoader;
    private final PromptFactory promptFactory;
    private final ClaudeClient claudeClient;
    private final AnalysisWriter writer;
    private final JsonSupport json;

    public AnalysisService(final MarketDataService marketDataService,
                           final AnalysisReportRepository reportRepository,
                           final AnalysisContextLoader contextLoader,
                           final PromptFactory promptFactory,
                           final ClaudeClient claudeClient,
                           final AnalysisWriter writer,
                           final JsonSupport json) {
        this.marketDataService = marketDataService;
        this.reportRepository = reportRepository;
        this.contextLoader = contextLoader;
        this.promptFactory = promptFactory;
        this.claudeClient = claudeClient;
        this.writer = writer;
        this.json = json;
    }

    /**
     * @return human readable log lines describing what was done
     */
    public List<String> analyze(final Instrument instrument, final Mode mode) {
        final List<String> log = new ArrayList<>();
        final Optional<MarketSnapshot> snapshotOptional = marketDataService.snapshot(instrument);
        if (snapshotOptional.isEmpty()) {
            log.add(instrument.getSymbol() + " " + mode + ": za mało notowań do analizy");
            return log;
        }
        final MarketSnapshot snapshot = snapshotOptional.get();

        if (!exists(instrument, mode, Source.BASELINE, snapshot)) {
            final List<BaselineForecaster.Forecast> forecasts = mode == Mode.INVEST
                    ? BaselineForecaster.invest(snapshot)
                    : BaselineForecaster.trade(snapshot);
            writer.saveBaseline(instrument.getId(), mode, snapshot, forecasts);
            log.add(instrument.getSymbol() + " " + mode + ": baseline zapisany");
        }

        if (!claudeClient.isEnabled()) {
            log.add(instrument.getSymbol() + " " + mode + ": AI pominięte (brak ANTHROPIC_API_KEY)");
            return log;
        }
        if (exists(instrument, mode, Source.AI, snapshot)) {
            log.add(instrument.getSymbol() + " " + mode + ": analiza AI na " + snapshot.date() + " już istnieje");
            return log;
        }

        final AnalysisContext context = contextLoader.load(instrument, mode, snapshot);
        final ClaudeClient.ToolCall call = claudeClient.callTool(
                promptFactory.analysisSystemPrompt(mode),
                promptFactory.analysisUserPrompt(context),
                mode == Mode.INVEST ? AiTools.INVEST_ANALYSIS : AiTools.TRADE_SETUP);
        final AiModels.Analysis analysis = json.convert(call.input(), AiModels.Analysis.class);
        writer.saveAi(instrument.getId(), mode, snapshot, analysis, claudeClient.model(), call.inputTokens(),
                call.outputTokens());
        log.add(instrument.getSymbol() + " " + mode + ": analiza AI zapisana (" + call.inputTokens() + " in / "
                + call.outputTokens() + " out tokenów)");
        return log;
    }

    private boolean exists(final Instrument instrument, final Mode mode, final Source source,
                           final MarketSnapshot snapshot) {
        return reportRepository.existsByInstrumentIdAndModeAndSourceAndReferenceDate(instrument.getId(), mode,
                source, snapshot.date());
    }
}

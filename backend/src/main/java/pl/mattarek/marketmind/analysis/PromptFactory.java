package pl.mattarek.marketmind.analysis;

import org.springframework.stereotype.Component;
import pl.mattarek.marketmind.config.AppProperties;
import pl.mattarek.marketmind.instrument.Instrument;
import pl.mattarek.marketmind.learning.Lesson;
import pl.mattarek.marketmind.market.Bar;
import pl.mattarek.marketmind.news.NewsItem;
import pl.mattarek.marketmind.prediction.CalibrationCalculator;
import pl.mattarek.marketmind.prediction.Mode;
import pl.mattarek.marketmind.prediction.Prediction;

import java.util.List;
import java.util.Locale;

/**
 * Builds the prompts. Instructions are in English (models follow them most reliably),
 * but every text the user will read must come back in Polish.
 */
@Component
public class PromptFactory {

    private static final String COMMON_RULES = """
            Ground rules (follow them strictly):
            - Markets are noisy and mostly efficient; most public news is priced in within hours. Default \
            confidence should be 0.50-0.60. Go above 0.70 only with strong, specific, not-yet-priced evidence. \
            Never exceed 0.85 unless the evidence is exceptional.
            - Use ONLY the data in this message. If news is thin or irrelevant, say so and stay near FLAT \
            with low confidence. Do not invent numbers, events or quotes.
            - Separate facts (what the news says) from interpretation (what you think it means).
            - Your track record and calibration are shown below. If you were overconfident (average confidence \
            above hit rate), lower your confidence now. If the simple baseline beats you, be humble.
            - Lessons: ACTIVE ones were confirmed by several outcomes; CANDIDATE ones are hypotheses still \
            being tested - use them carefully. Apply a lesson only if it is relevant today and list its id \
            in lessons_used.
            - This is a research companion for a private investor, not investment advice. Describe scenarios \
            and probabilities, never give orders like "buy now".
            - Write every human-readable text field in Polish.
            """;

    private final AppProperties properties;

    public PromptFactory(final AppProperties properties) {
        this.properties = properties;
    }

    public String analysisSystemPrompt(final Mode mode) {
        if (mode == Mode.INVEST) {
            return """
                    You are a disciplined equity analyst and the personal research companion of one private \
                    investor from Poland (GPW and US markets). Each request covers ONE instrument.
                    Your job: interpret the news and price data, then estimate how the instrument will perform \
                    RELATIVE TO ITS BENCHMARK over 5 and 20 trading days. UP = beats the benchmark by more \
                    than the flat band, DOWN = lags by more than the flat band, FLAT = within the band.
                    Give exactly one prediction for 5 days and one for 20 days.
                    """ + COMMON_RULES;
        }
        return """
                You are a disciplined swing trader and the personal research companion of one private \
                trader from Poland. Each request covers ONE instrument. Horizon: 1 and 5 trading days.
                Predict the ABSOLUTE price direction (UP / DOWN / FLAT within the flat band) for 1 day and \
                for 5 days - exactly one prediction for each.
                Then decide if there is a tradable setup for the next 1-5 sessions:
                - bias LONG or SHORT only when there is a clear edge; otherwise NONE (that is a valid, \
                often the best answer).
                - stop must be placed where the idea is clearly wrong (structure or ~1-2 ATR); target must give \
                a reward/risk of at least 1.5. Entry is assumed at the next session's open.
                - If bias is LONG the 5-day prediction must be UP, if SHORT it must be DOWN.
                """ + COMMON_RULES;
    }

    public String analysisUserPrompt(final AnalysisContext context) {
        final Instrument instrument = context.instrument();
        final StringBuilder prompt = new StringBuilder();

        prompt.append("## Instrument\n")
                .append(instrument.getSymbol()).append(" - ").append(instrument.getName())
                .append(" | market: ").append(instrument.getMarket())
                .append(" | type: ").append(instrument.getType())
                .append(" | currency: ").append(nullToDash(instrument.getCurrency()))
                .append(" | benchmark: ")
                .append(instrument.getBenchmark() == null ? "none" : instrument.getBenchmark().getName())
                .append("\n\n");

        prompt.append("## Flat band (moves inside +/- this count as FLAT)\n");
        for (final int horizon : horizons(context.mode())) {
            prompt.append(horizon).append(" days: ").append(fmt(properties.scoring().flatBandFor(horizon)))
                    .append("%\n");
        }

        prompt.append("\n## Price data\n").append(context.snapshot().describe()).append("\n\n");

        prompt.append("## Last closes\n");
        for (final Bar bar : context.recentBars()) {
            prompt.append(bar.date()).append(": ").append(fmt(bar.close()))
                    .append(" (H ").append(fmt(bar.high())).append(" L ").append(fmt(bar.low()))
                    .append(" V ").append(bar.volume()).append(")\n");
        }

        prompt.append("\n## News about this instrument (newest first)\n");
        appendNews(prompt, context.news());
        prompt.append("\n## General market news\n");
        appendNews(prompt, context.generalNews());

        prompt.append("\n## Your calibration in this mode (all instruments, last 180 days)\n");
        prompt.append("You (AI): ").append(describe(context.aiCalibration())).append("\n");
        prompt.append("Simple baseline rule: ").append(describe(context.baselineCalibration())).append("\n");

        prompt.append("\n## Your track record on this instrument (resolved, newest first)\n");
        if (context.trackRecord().isEmpty()) {
            prompt.append("No resolved predictions yet.\n");
        }
        for (final AnalysisContext.TrackRecordEntry entry : context.trackRecord()) {
            final Prediction p = entry.prediction();
            prompt.append("- ").append(p.getReferenceDate()).append(" ").append(p.getHorizonDays()).append("d ")
                    .append(p.getDirection()).append(" @").append(fmt(p.getConfidence()))
                    .append(" -> measured ").append(fmt(p.getMeasuredReturnPct())).append("% ")
                    .append(Boolean.TRUE.equals(p.getHit()) ? "HIT" : "MISS");
            if (entry.review() != null) {
                prompt.append(" | review: ").append(entry.review().getVerdict()).append(" - ")
                        .append(entry.review().getExplanation());
            }
            prompt.append("\n");
        }

        prompt.append("\n## Your predictions still running on this instrument\n");
        if (context.openPredictions().isEmpty()) {
            prompt.append("None.\n");
        }
        for (final Prediction p : context.openPredictions()) {
            prompt.append("- made ").append(p.getReferenceDate()).append(" ").append(p.getHorizonDays())
                    .append("d ").append(p.getDirection()).append(" @").append(fmt(p.getConfidence()))
                    .append(": ").append(nullToDash(p.getRationale())).append("\n");
        }

        prompt.append("\n## Lessons from memory\n");
        appendLessons(prompt, context.lessons());

        prompt.append("\nNow call the tool with your analysis.");
        return prompt.toString();
    }

    public String postMortemSystemPrompt() {
        return """
                You review your OWN past market predictions, honestly and without excuses, to slowly improve.
                For each resolved prediction decide:
                - CORRECT_FOR_RIGHT_REASONS: the move happened and was driven by what you predicted.
                - CORRECT_BY_LUCK: direction was right but for different reasons than you gave.
                - WRONG_BAD_REASONING: your reasoning had a flaw that was knowable at the time.
                - WRONG_UNFORESEEABLE: a genuinely new, unforeseeable event moved the price.
                - NOISE: the move was small/random relative to normal volatility (~1 ATR) with no clear driver.
                Be strict: one outcome is weak evidence. Most short-term moves are NOISE.
                Lesson memory rules:
                - Support or contradict an EXISTING lesson only when it was clearly relevant to the prediction.
                - Propose at most 2 new lessons, only if they are general, testable and not duplicates. \
                Prefer refining your process (e.g. calibration, which news types matter) over market folklore.
                - NOISE and WRONG_UNFORESEEABLE results must not be used as evidence for lessons.
                Write all human-readable text in Polish.
                """;
    }

    public String postMortemUserPrompt(final Instrument instrument, final List<PostMortemItem> items,
                                       final List<NewsItem> newsDuringPeriod, final List<Lesson> lessons) {
        final StringBuilder prompt = new StringBuilder();
        prompt.append("## Instrument\n").append(instrument.getSymbol()).append(" - ").append(instrument.getName())
                .append(" (").append(instrument.getMarket()).append(")\n\n");

        prompt.append("## Resolved predictions\n");
        for (final PostMortemItem item : items) {
            final Prediction p = item.prediction();
            prompt.append("### prediction_id ").append(p.getId()).append("\n")
                    .append("mode: ").append(p.getMode()).append(", horizon: ").append(p.getHorizonDays())
                    .append(" trading days, made on ").append(p.getReferenceDate())
                    .append(", resolved on ").append(p.getResolvedDate()).append("\n")
                    .append("predicted: ").append(p.getDirection()).append(" with confidence ")
                    .append(fmt(p.getConfidence())).append(", expected ").append(fmt(p.getExpectedReturnPct()))
                    .append("%\n")
                    .append("rationale: ").append(nullToDash(p.getRationale())).append("\n")
                    .append("price: ").append(fmt(p.getReferencePrice())).append(" -> ")
                    .append(fmt(p.getResolvedPrice())).append(" (").append(fmt(p.getActualReturnPct()))
                    .append("%), benchmark: ").append(fmt(p.getBenchmarkReturnPct())).append("%\n")
                    .append("measured return (what the prediction is judged on): ")
                    .append(fmt(p.getMeasuredReturnPct())).append("%, flat band: ")
                    .append(fmt(properties.scoring().flatBandFor(p.getHorizonDays()))).append("%\n")
                    .append("result: ").append(Boolean.TRUE.equals(p.getHit()) ? "HIT" : "MISS");
            if (p.getTradeOutcome() != null) {
                prompt.append(", trade: ").append(p.getTradeOutcome()).append(" ")
                        .append(fmt(p.getRMultiple())).append("R");
            }
            prompt.append("\n");
            if (item.reportSummary() != null) {
                prompt.append("original analysis: ").append(item.reportSummary()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("## News published while the predictions were running\n");
        appendNews(prompt, newsDuringPeriod);

        prompt.append("\n## Current lesson memory\n");
        appendLessons(prompt, lessons);

        prompt.append("\nNow call the tool with your post-mortem.");
        return prompt.toString();
    }

    public static List<Integer> horizons(final Mode mode) {
        return mode == Mode.INVEST ? List.of(5, 20) : List.of(1, 5);
    }

    private static void appendNews(final StringBuilder prompt, final List<NewsItem> news) {
        if (news.isEmpty()) {
            prompt.append("No news.\n");
            return;
        }
        for (final NewsItem item : news) {
            prompt.append("[").append(item.getId()).append("] ").append(item.getPublishedAt())
                    .append(" | ").append(nullToDash(item.getSource())).append(" | ").append(item.getTitle())
                    .append("\n");
        }
    }

    private static void appendLessons(final StringBuilder prompt, final List<Lesson> lessons) {
        if (lessons.isEmpty()) {
            prompt.append("No lessons yet.\n");
            return;
        }
        for (final Lesson lesson : lessons) {
            prompt.append("[").append(lesson.getId()).append("] ").append(lesson.getStatus())
                    .append(" (+").append(lesson.getSupportCount()).append("/-").append(lesson.getContradictCount())
                    .append(", ").append(lesson.getScope())
                    .append(lesson.getScopeRef() == null ? "" : " " + lesson.getScopeRef())
                    .append(") ").append(lesson.getText()).append("\n");
        }
    }

    private static String describe(final CalibrationCalculator.Calibration calibration) {
        if (calibration.count() == 0) {
            return "no resolved predictions yet";
        }
        final StringBuilder text = new StringBuilder()
                .append(calibration.count()).append(" resolved, hit rate ").append(pct(calibration.hitRate()))
                .append(", average confidence ").append(pct(calibration.averageConfidence()))
                .append(", Brier ").append(fmt(calibration.brier())).append(". Buckets: ");
        for (final CalibrationCalculator.Bucket bucket : calibration.buckets()) {
            if (bucket.count() > 0) {
                text.append(pct(bucket.from())).append("-").append(pct(bucket.to())).append(": ")
                        .append(pct(bucket.hitRate())).append(" of ").append(bucket.count()).append("; ");
            }
        }
        return text.toString();
    }

    private static String pct(final double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.0f%%", value * 100);
    }

    private static String fmt(final Double value) {
        return value == null || value.isNaN() ? "n/a" : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String nullToDash(final String value) {
        return value == null ? "-" : value;
    }

    public record PostMortemItem(Prediction prediction, String reportSummary) {
    }
}

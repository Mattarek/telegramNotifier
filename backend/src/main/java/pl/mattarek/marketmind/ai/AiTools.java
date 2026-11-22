package pl.mattarek.marketmind.ai;

import java.util.Map;

import static pl.mattarek.marketmind.ai.JsonSchema.array;
import static pl.mattarek.marketmind.ai.JsonSchema.integer;
import static pl.mattarek.marketmind.ai.JsonSchema.integerEnum;
import static pl.mattarek.marketmind.ai.JsonSchema.number;
import static pl.mattarek.marketmind.ai.JsonSchema.object;
import static pl.mattarek.marketmind.ai.JsonSchema.props;
import static pl.mattarek.marketmind.ai.JsonSchema.string;
import static pl.mattarek.marketmind.ai.JsonSchema.stringEnum;

/**
 * The JSON formats the AI must answer in. Field names here must match the records in {@link AiModels}.
 */
public final class AiTools {

    private AiTools() {
    }

    private static Map<String, Object> keyFactors() {
        return array("Most important factors behind the view, strongest first (max 6)",
                object(props(
                                "factor", string("Short description of the factor, in Polish"),
                                "impact", stringEnum("Effect on the price", "POSITIVE", "NEGATIVE", "NEUTRAL"),
                                "news_ids", array("Ids of the news items this factor comes from (may be empty)",
                                        integer("news id"))),
                        "factor", "impact", "news_ids"));
    }

    private static Map<String, Object> commonFields(final Map<String, Object> predictionSchema) {
        return props(
                "summary", string("3-6 sentences in Polish: what is going on and what it means"),
                "sentiment", number("-1 very negative ... 0 neutral ... +1 very positive", -1, 1),
                "key_factors", keyFactors(),
                "risks", array("Main risks to this view, in Polish (max 5)", string("risk")),
                "invalidation", string("In Polish: what concrete event or price level would prove this view wrong"),
                "predictions", array("Exactly one prediction per allowed horizon", predictionSchema),
                "lessons_used", array("Ids of lessons from memory that influenced this analysis",
                        integer("lesson id")));
    }

    public static final ClaudeClient.ToolDefinition INVEST_ANALYSIS = new ClaudeClient.ToolDefinition(
            "submit_investment_analysis",
            "Submit the medium-term analysis of one instrument with checkable predictions vs its benchmark.",
            object(commonFields(object(props(
                                    "horizon_days", integerEnum("Horizon in trading days", 5, 20),
                                    "direction", stringEnum("Expected move RELATIVE TO THE BENCHMARK",
                                            "UP", "DOWN", "FLAT"),
                                    "expected_excess_return_pct", number("Expected return minus benchmark return, %"),
                                    "confidence", number("Probability that the direction is right", 0.5, 0.95),
                                    "rationale", string("1-3 sentences in Polish")),
                            "horizon_days", "direction", "expected_excess_return_pct", "confidence", "rationale")),
                    "summary", "sentiment", "key_factors", "risks", "invalidation", "predictions",
                    "lessons_used"));

    public static final ClaudeClient.ToolDefinition TRADE_SETUP;

    static {
        final Map<String, Object> fields = commonFields(object(props(
                        "horizon_days", integerEnum("Horizon in trading days", 1, 5),
                        "direction", stringEnum("Expected ABSOLUTE price move", "UP", "DOWN", "FLAT"),
                        "expected_return_pct", number("Expected price change, %"),
                        "confidence", number("Probability that the direction is right", 0.5, 0.95),
                        "rationale", string("1-3 sentences in Polish")),
                "horizon_days", "direction", "expected_return_pct", "confidence", "rationale"));
        fields.put("bias", stringEnum("Trade idea for the next 1-5 sessions. NONE if there is no clear edge",
                "LONG", "SHORT", "NONE"));
        fields.put("setup_type", string("Name of the setup in Polish, e.g. 'wybicie z konsolidacji', "
                + "'odbicie od wsparcia', 'brak setupu'"));
        fields.put("stop", number("Stop loss price (required unless bias is NONE, use 0 then)"));
        fields.put("target", number("Take profit price (required unless bias is NONE, use 0 then)"));
        TRADE_SETUP = new ClaudeClient.ToolDefinition(
                "submit_trade_setup",
                "Submit a short-term (swing, 1-5 sessions) trading view with levels and checkable predictions.",
                object(fields, "summary", "sentiment", "key_factors", "risks", "invalidation", "predictions",
                        "lessons_used", "bias", "setup_type", "stop", "target"));
    }

    public static final ClaudeClient.ToolDefinition POST_MORTEM = new ClaudeClient.ToolDefinition(
            "submit_post_mortem",
            "Submit an honest review of resolved predictions and how the lesson memory should change.",
            object(props(
                            "reviews", array("One review per resolved prediction", object(props(
                                            "prediction_id", integer("Id of the reviewed prediction"),
                                            "verdict", stringEnum("Classification of the result",
                                                    "CORRECT_FOR_RIGHT_REASONS", "CORRECT_BY_LUCK",
                                                    "WRONG_BAD_REASONING", "WRONG_UNFORESEEABLE", "NOISE"),
                                            "explanation", string("2-4 sentences in Polish: why it worked or not"),
                                            "actual_driver", string("In Polish: what really moved the price")),
                                    "prediction_id", "verdict", "explanation", "actual_driver")),
                            "supported_lessons", array("Existing lessons confirmed by these results",
                                    lessonEvidence()),
                            "contradicted_lessons", array("Existing lessons contradicted by these results",
                                    lessonEvidence()),
                            "new_lessons", array("New candidate lessons (max 2, only if clearly general and "
                                            + "not a duplicate of an existing lesson)",
                                    object(props(
                                                    "text", string("The lesson as a short, testable rule in Polish"),
                                                    "category", string("e.g. 'wyniki finansowe', 'makro', "
                                                            + "'sentyment', 'analiza techniczna', 'kalibracja'"),
                                                    "scope", stringEnum("Where it applies",
                                                            "GLOBAL", "MARKET", "INSTRUMENT"),
                                                    "mode", stringEnum("Which tab it applies to",
                                                            "INVEST", "TRADE", "BOTH"),
                                                    "prediction_id", integer("Prediction that taught this lesson")),
                                            "text", "category", "scope", "mode", "prediction_id"))),
                    "reviews", "supported_lessons", "contradicted_lessons", "new_lessons"));

    private static Map<String, Object> lessonEvidence() {
        return object(props(
                        "lesson_id", integer("Lesson id"),
                        "prediction_id", integer("Prediction that is the evidence"),
                        "note", string("Short justification in Polish")),
                "lesson_id", "prediction_id", "note");
    }
}

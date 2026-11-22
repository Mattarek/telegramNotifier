package pl.mattarek.marketmind.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Java shapes of the AI answers defined in {@link AiTools}.
 */
public final class AiModels {

    private AiModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyFactor(String factor, String impact, @JsonProperty("news_ids") List<Long> newsIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiPrediction(@JsonProperty("horizon_days") int horizonDays,
                               String direction,
                               @JsonProperty("expected_excess_return_pct") Double expectedExcessReturnPct,
                               @JsonProperty("expected_return_pct") Double expectedReturnPct,
                               double confidence,
                               String rationale) {

        public Double expected() {
            return expectedExcessReturnPct != null ? expectedExcessReturnPct : expectedReturnPct;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Analysis(String summary,
                           double sentiment,
                           @JsonProperty("key_factors") List<KeyFactor> keyFactors,
                           List<String> risks,
                           String invalidation,
                           List<AiPrediction> predictions,
                           @JsonProperty("lessons_used") List<Long> lessonsUsed,
                           String bias,
                           @JsonProperty("setup_type") String setupType,
                           Double stop,
                           Double target) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(@JsonProperty("prediction_id") long predictionId,
                         String verdict,
                         String explanation,
                         @JsonProperty("actual_driver") String actualDriver) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LessonEvidenceItem(@JsonProperty("lesson_id") long lessonId,
                                     @JsonProperty("prediction_id") long predictionId,
                                     String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewLesson(String text, String category, String scope, String mode,
                            @JsonProperty("prediction_id") long predictionId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PostMortem(List<Review> reviews,
                             @JsonProperty("supported_lessons") List<LessonEvidenceItem> supportedLessons,
                             @JsonProperty("contradicted_lessons") List<LessonEvidenceItem> contradictedLessons,
                             @JsonProperty("new_lessons") List<NewLesson> newLessons) {
    }
}

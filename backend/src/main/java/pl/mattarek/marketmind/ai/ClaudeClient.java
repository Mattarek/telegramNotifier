package pl.mattarek.marketmind.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pl.mattarek.marketmind.config.AppProperties;

import java.util.List;
import java.util.Map;

/**
 * Thin client for the Claude Messages API.
 *
 * <p>We always force the model to answer by calling one "tool" whose input schema is our JSON format.
 * That way the answer is structured JSON we can save, instead of free text we would have to parse.
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final RestClient restClient;
    private final AppProperties.Ai settings;
    private final AiUsageMeter usageMeter;

    public ClaudeClient(@Qualifier("claudeRestClient") final RestClient restClient,
                        final AppProperties properties,
                        final AiUsageMeter usageMeter) {
        this.restClient = restClient;
        this.settings = properties.ai();
        this.usageMeter = usageMeter;
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public String model() {
        return settings.model();
    }

    public ToolCall callTool(final String systemPrompt, final String userPrompt, final ToolDefinition tool) {
        if (!isEnabled()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not set");
        }
        final MessagesRequest request = new MessagesRequest(
                settings.model(),
                settings.maxTokens(),
                systemPrompt,
                List.of(new Message("user", userPrompt)),
                List.of(tool),
                new ToolChoice("tool", tool.name()));

        final MessagesResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", settings.apiKey())
                    .body(request)
                    .retrieve()
                    .body(MessagesResponse.class);
        } catch (final RestClientResponseException exception) {
            log.error("Claude API error {}: {}", exception.getStatusCode(), exception.getResponseBodyAsString());
            throw exception;
        }
        if (response == null || response.content() == null) {
            throw new IllegalStateException("Empty response from Claude API");
        }
        if (response.usage() != null) {
            usageMeter.record(response.usage().inputTokens(), response.usage().outputTokens());
        }
        if ("max_tokens".equals(response.stopReason())) {
            throw new IllegalStateException("Claude answer was cut off (max_tokens) - increase app.ai.max-tokens");
        }
        return response.content().stream()
                .filter(block -> "tool_use".equals(block.type()) && tool.name().equals(block.name()))
                .findFirst()
                .map(block -> new ToolCall(block.input(),
                        response.usage() == null ? 0 : response.usage().inputTokens(),
                        response.usage() == null ? 0 : response.usage().outputTokens()))
                .orElseThrow(() -> new IllegalStateException("Claude did not call tool " + tool.name()));
    }

    public record ToolCall(Map<String, Object> input, int inputTokens, int outputTokens) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolDefinition(String name,
                                 String description,
                                 @JsonProperty("input_schema") Map<String, Object> inputSchema) {
    }

    record MessagesRequest(String model,
                           @JsonProperty("max_tokens") int maxTokens,
                           String system,
                           List<Message> messages,
                           List<ToolDefinition> tools,
                           @JsonProperty("tool_choice") ToolChoice toolChoice) {
    }

    record Message(String role, String content) {
    }

    record ToolChoice(String type, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessagesResponse(List<ContentBlock> content,
                            @JsonProperty("stop_reason") String stopReason,
                            Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text, String name, Map<String, Object> input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("input_tokens") int inputTokens,
                 @JsonProperty("output_tokens") int outputTokens) {
    }
}

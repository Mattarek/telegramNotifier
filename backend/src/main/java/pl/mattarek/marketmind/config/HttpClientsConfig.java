package pl.mattarek.marketmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Two HTTP clients: one for public market/news data, one for the Claude API.
 */
@Configuration
public class HttpClientsConfig {

    @Bean
    RestClient marketRestClient(final AppProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(Duration.ofSeconds(20)))
                .defaultHeader("User-Agent", properties.market().userAgent())
                .build();
    }

    @Bean
    RestClient claudeRestClient(final AppProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.ai().baseUrl())
                // AI answers can take a while - generous read timeout
                .requestFactory(requestFactory(Duration.ofSeconds(180)))
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    private static JdkClientHttpRequestFactory requestFactory(final Duration readTimeout) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}

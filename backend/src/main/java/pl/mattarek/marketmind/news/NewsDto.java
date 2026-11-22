package pl.mattarek.marketmind.news;

import java.time.Instant;

public record NewsDto(Long id, Long instrumentId, String title, String url, String source, Instant publishedAt) {

    public static NewsDto from(final NewsItem item) {
        return new NewsDto(item.getId(), item.getInstrumentId(), item.getTitle(), item.getUrl(), item.getSource(),
                item.getPublishedAt());
    }
}

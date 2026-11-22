package pl.mattarek.marketmind.news;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsByUrl(String url);

    List<NewsItem> findByInstrumentIdAndPublishedAtAfterOrderByPublishedAtDesc(Long instrumentId, Instant after);

    List<NewsItem> findByInstrumentIdIsNullAndPublishedAtAfterOrderByPublishedAtDesc(Instant after);

    List<NewsItem> findByInstrumentIdAndPublishedAtBetweenOrderByPublishedAtDesc(Long instrumentId,
                                                                                 Instant from, Instant to);

    List<NewsItem> findByInstrumentIdOrderByPublishedAtDesc(Long instrumentId, Pageable pageable);

    List<NewsItem> findByInstrumentIdIsNullOrderByPublishedAtDesc(Pageable pageable);
}

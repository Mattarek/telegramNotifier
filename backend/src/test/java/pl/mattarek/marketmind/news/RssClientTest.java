package pl.mattarek.marketmind.news;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RssClientTest {

    private static final String FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Google News</title>
                <item>
                  <title>cyber_Folks podnosi prognozy - Bankier.pl</title>
                  <link>https://example.com/a</link>
                  <pubDate>Fri, 20 Nov 2026 09:15:00 GMT</pubDate>
                  <source url="https://www.bankier.pl">Bankier.pl</source>
                </item>
                <item>
                  <title>Item without link</title>
                </item>
                <item>
                  <title>Second</title>
                  <link>https://example.com/b</link>
                </item>
              </channel>
            </rss>
            """;

    @Test
    void shouldParseItemsAndSkipBrokenOnes() {
        final List<RssClient.RssEntry> entries = RssClient.parse(FEED.strip().getBytes(StandardCharsets.UTF_8), 10);

        assertEquals(2, entries.size());
        assertEquals("cyber_Folks podnosi prognozy - Bankier.pl", entries.getFirst().title());
        assertEquals("Bankier.pl", entries.getFirst().source());
        assertEquals(Instant.parse("2026-11-20T09:15:00Z"), entries.getFirst().publishedAt());
    }

    @Test
    void shouldRespectLimit() {
        assertEquals(1, RssClient.parse(FEED.strip().getBytes(StandardCharsets.UTF_8), 1).size());
    }

    @Test
    void shouldRejectDoctypeToPreventXxe() {
        final String malicious = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss><channel><item><title>&xxe;</title><link>x</link></item></channel></rss>
                """;

        assertThrows(IllegalStateException.class,
                () -> RssClient.parse(malicious.strip().getBytes(StandardCharsets.UTF_8), 10));
    }
}

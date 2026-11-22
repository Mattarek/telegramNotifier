package pl.mattarek.marketmind.news;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RSS 2.0 reader built on the JDK XML parser (no extra dependency).
 */
@Component
public class RssClient {

    private final RestClient restClient;

    public RssClient(@Qualifier("marketRestClient") final RestClient restClient) {
        this.restClient = restClient;
    }

    public List<RssEntry> fetch(final String url, final int limit) {
        final byte[] body = restClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
        if (body == null || body.length == 0) {
            return List.of();
        }
        return parse(body, limit);
    }

    static List<RssEntry> parse(final byte[] xml, final int limit) {
        try {
            final Document document = secureBuilder().parse(new ByteArrayInputStream(xml));
            final NodeList items = document.getElementsByTagName("item");
            final List<RssEntry> entries = new ArrayList<>();
            for (int i = 0; i < items.getLength() && entries.size() < limit; i++) {
                final Element item = (Element) items.item(i);
                final String title = text(item, "title");
                final String link = text(item, "link");
                if (title == null || link == null) {
                    continue;
                }
                entries.add(new RssEntry(title.strip(), link.strip(), text(item, "source"),
                        parseDate(text(item, "pubDate"))));
            }
            return entries;
        } catch (final Exception exception) {
            throw new IllegalStateException("Cannot parse RSS feed", exception);
        }
    }

    private static DocumentBuilder secureBuilder() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Protect against XXE - feeds come from the internet
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private static String text(final Element parent, final String tag) {
        final NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        final Node node = nodes.item(0);
        final String value = node.getTextContent();
        return value == null || value.isBlank() ? null : value;
    }

    private static Instant parseDate(final String value) {
        if (value == null) {
            return Instant.now();
        }
        try {
            return ZonedDateTime.parse(value.strip(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (final DateTimeParseException exception) {
            return Instant.now();
        }
    }

    public record RssEntry(String title, String link, String source, Instant publishedAt) {
    }
}

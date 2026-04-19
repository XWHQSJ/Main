package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HackerNewsScraper tests")
class HackerNewsScraperTest {

    @Mock
    private HttpFetcher fetcher;

    private HackerNewsScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new HackerNewsScraper(3);
    }

    @Test
    @DisplayName("scrape fetches top stories from JSON API")
    void scrape_validResponse_returnsStories() throws Exception {
        // Arrange: top stories endpoint returns 3 IDs
        when(fetcher.fetch(eq("https://hacker-news.firebaseio.com/v0/topstories.json")))
                .thenReturn("[100, 200, 300]");

        // Arrange: each story item
        when(fetcher.fetch(contains("/item/100.json")))
                .thenReturn("""
                        {"id":100,"title":"Show HN: A cool project","url":"https://example.com/cool",
                         "score":42,"descendants":10,"by":"alice","type":"story"}
                        """);
        when(fetcher.fetch(contains("/item/200.json")))
                .thenReturn("""
                        {"id":200,"title":"Ask HN: Best books?","text":"What are your favorites?",
                         "score":85,"descendants":50,"by":"bob","type":"story"}
                        """);
        when(fetcher.fetch(contains("/item/300.json")))
                .thenReturn("""
                        {"id":300,"title":"Rust is fast","url":"https://rust-lang.org",
                         "score":120,"descendants":75,"by":"carol","type":"story"}
                        """);

        // Act
        var stories = scraper.scrape("https://news.ycombinator.com", fetcher);

        // Assert
        assertThat(stories).hasSize(3);

        assertThat(stories.get(0).title()).isEqualTo("Show HN: A cool project");
        assertThat(stories.get(0).url()).contains("item?id=100");
        assertThat(stories.get(0).description()).contains("Score: 42");
        assertThat(stories.get(0).answers()).isEmpty();

        // Ask HN has text content -> mapped as first "answer"
        assertThat(stories.get(1).title()).isEqualTo("Ask HN: Best books?");
        assertThat(stories.get(1).answers()).hasSize(1);
        assertThat(stories.get(1).answers().get(0)).contains("favorites");
    }

    @Test
    @DisplayName("scrape returns empty list on network failure")
    void scrape_networkFailure_returnsEmptyList() throws Exception {
        when(fetcher.fetch(eq("https://hacker-news.firebaseio.com/v0/topstories.json")))
                .thenThrow(new IOException("connection refused"));

        var stories = scraper.scrape("https://news.ycombinator.com", fetcher);

        assertThat(stories).isEmpty();
    }

    @Test
    @DisplayName("scrape skips individual story failures gracefully")
    void scrape_partialFailure_returnsSuccessful() throws Exception {
        when(fetcher.fetch(eq("https://hacker-news.firebaseio.com/v0/topstories.json")))
                .thenReturn("[100, 200]");

        when(fetcher.fetch(contains("/item/100.json")))
                .thenThrow(new IOException("timeout"));

        when(fetcher.fetch(contains("/item/200.json")))
                .thenReturn("""
                        {"id":200,"title":"Working story","url":"https://example.com",
                         "score":10,"descendants":5,"by":"dan","type":"story"}
                        """);

        var stories = scraper.scrape("https://news.ycombinator.com", fetcher);

        assertThat(stories).hasSize(1);
        assertThat(stories.get(0).title()).isEqualTo("Working story");
    }

    @Test
    @DisplayName("fetchStory maps HN item fields to Question record")
    void fetchStory_fullItem_mapsCorrectly() throws Exception {
        when(fetcher.fetch(contains("/item/42.json")))
                .thenReturn("""
                        {"id":42,"title":"Test Title","url":"https://example.com/test",
                         "score":100,"descendants":25,"by":"testuser","type":"story"}
                        """);

        Question q = scraper.fetchStory(42, fetcher);

        assertThat(q).isNotNull();
        assertThat(q.title()).isEqualTo("Test Title");
        assertThat(q.url()).isEqualTo("https://news.ycombinator.com/item?id=42");
        assertThat(q.description()).contains("Link: https://example.com/test");
        assertThat(q.description()).contains("Score: 100");
        assertThat(q.description()).contains("Comments: 25");
        assertThat(q.description()).contains("By: testuser");
    }

    @Test
    @DisplayName("limit is clamped between 1 and 500")
    void constructor_limitClamped() throws Exception {
        when(fetcher.fetch(eq("https://hacker-news.firebaseio.com/v0/topstories.json")))
                .thenReturn("[1, 2, 3, 4, 5]");

        // Limit 0 -> clamped to 1
        var scraperMin = new HackerNewsScraper(0);
        when(fetcher.fetch(contains("/item/1.json")))
                .thenReturn("{\"id\":1,\"title\":\"Only one\",\"score\":1,\"descendants\":0,\"by\":\"a\"}");

        var result = scraperMin.scrape("https://news.ycombinator.com", fetcher);
        assertThat(result).hasSize(1);
    }
}

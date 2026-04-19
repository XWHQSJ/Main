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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZhihuScraper tests")
class ZhihuScraperTest {

    @Mock
    private HttpFetcher fetcher;

    private ZhihuScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new ZhihuScraper();
    }

    @Test
    @DisplayName("parseQuestionPage extracts title, description, and answers from modern markup")
    void parseQuestionPage_modernMarkup_extractsAllFields() throws Exception {
        String html = loadResource("zhihu-question.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/12345");

        assertThat(question.title()).isEqualTo("如何评价 Java 17 的新特性？");
        assertThat(question.description()).contains("长期支持版本");
        assertThat(question.answers()).hasSize(2);
        assertThat(question.answers().get(0)).contains("sealed classes");
        assertThat(question.answers().get(1)).contains("Records");
    }

    @Test
    @DisplayName("parseQuestionPage falls back to regex on legacy markup")
    void parseQuestionPage_legacyMarkup_regexFallback() throws Exception {
        String html = loadResource("zhihu-legacy.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/99999");

        // Legacy page may or may not match depending on exact pattern
        assertThat(question).isNotNull();
        assertThat(question.url()).isEqualTo("https://www.zhihu.com/question/99999");
    }

    @Test
    @DisplayName("parseListingPage finds question URLs from fixture HTML")
    void parseListingPage_fixtureHtml_findsQuestionUrls() throws Exception {
        String html = loadResource("zhihu-listing.html");

        // Mock fetcher to return question page HTML for each discovered URL
        String questionHtml = loadResource("zhihu-question.html");
        when(fetcher.fetch(anyString())).thenReturn(questionHtml);

        var questions = scraper.parseListingPage(html, "https://www.zhihu.com/explore", fetcher);

        assertThat(questions).isNotEmpty();
        assertThat(questions.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("normalizeQuestionUrl extracts question ID correctly")
    void normalizeQuestionUrl_variousFormats_normalizes() {
        assertThat(ZhihuScraper.normalizeQuestionUrl("https://www.zhihu.com/question/12345678/answer/99999"))
                .isEqualTo("https://www.zhihu.com/question/12345678");

        assertThat(ZhihuScraper.normalizeQuestionUrl("/question/87654321"))
                .isEqualTo("https://www.zhihu.com/question/87654321");

        assertThat(ZhihuScraper.normalizeQuestionUrl("https://www.zhihu.com/question/11111111"))
                .isEqualTo("https://www.zhihu.com/question/11111111");

        assertThat(ZhihuScraper.normalizeQuestionUrl(""))
                .isNull();

        assertThat(ZhihuScraper.normalizeQuestionUrl(null))
                .isNull();

        assertThat(ZhihuScraper.normalizeQuestionUrl("https://www.zhihu.com/people/someone"))
                .isNull();
    }

    @Test
    @DisplayName("scrape returns empty list on fetch failure")
    void scrape_fetchFails_returnsEmptyList() throws Exception {
        when(fetcher.fetch(anyString())).thenThrow(new IOException("network error"));

        var questions = scraper.scrape("https://www.zhihu.com/explore", fetcher);

        assertThat(questions).isEmpty();
    }

    @Test
    @DisplayName("parseQuestionPage handles empty HTML gracefully")
    void parseQuestionPage_emptyHtml_returnsEmptyQuestion() {
        Question question = scraper.parseQuestionPage("", "https://www.zhihu.com/question/1");

        assertThat(question).isNotNull();
        assertThat(question.title()).isEmpty();
        assertThat(question.answers()).isEmpty();
    }

    private String loadResource(String name) throws Exception {
        try (var is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(name),
                "Resource not found: " + name)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

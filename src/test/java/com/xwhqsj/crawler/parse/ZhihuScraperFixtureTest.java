package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixture-based tests for ZhihuScraper.
 *
 * <p>Uses a captured HTML snapshot from {@code src/test/resources/fixtures/zhihu_snapshot.html}
 * to verify CSS selectors against realistic Zhihu SSR markup.</p>
 *
 * <p>Fixture captured: 2024-06-15 (synthetic, based on known patterns).
 * The scraper is best-effort for modern Zhihu — selectors may need updating
 * as Zhihu changes their markup.</p>
 */
@DisplayName("ZhihuScraper fixture-based tests")
class ZhihuScraperFixtureTest {

    private ZhihuScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new ZhihuScraper();
    }

    @Test
    @DisplayName("parses title from fixture snapshot")
    void parseSnapshot_extractsTitle() throws Exception {
        String html = loadFixture("fixtures/zhihu_snapshot.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/654321");

        assertThat(question.title()).isEqualTo("如何看待 2024 年 Java 生态的发展？");
    }

    @Test
    @DisplayName("parses description from fixture snapshot")
    void parseSnapshot_extractsDescription() throws Exception {
        String html = loadFixture("fixtures/zhihu_snapshot.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/654321");

        assertThat(question.description()).contains("Java 21");
        assertThat(question.description()).contains("虚拟线程");
        assertThat(question.description()).contains("GraalVM");
    }

    @Test
    @DisplayName("parses answers from fixture snapshot")
    void parseSnapshot_extractsAnswers() throws Exception {
        String html = loadFixture("fixtures/zhihu_snapshot.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/654321");

        assertThat(question.answers()).hasSize(3);
        assertThat(question.answers().get(0)).contains("虚拟线程");
        assertThat(question.answers().get(0)).contains("Project Loom");
        assertThat(question.answers().get(1)).contains("GraalVM");
        assertThat(question.answers().get(2)).contains("模式匹配");
    }

    @Test
    @DisplayName("question URL is preserved from input")
    void parseSnapshot_preservesUrl() throws Exception {
        String html = loadFixture("fixtures/zhihu_snapshot.html");

        Question question = scraper.parseQuestionPage(html, "https://www.zhihu.com/question/654321");

        assertThat(question.url()).isEqualTo("https://www.zhihu.com/question/654321");
    }

    private String loadFixture(String name) throws Exception {
        try (var is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(name),
                "Fixture not found: " + name)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

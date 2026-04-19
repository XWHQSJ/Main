package com.xwhqsj.crawler.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Model record tests")
class ModelTest {

    @Test
    @DisplayName("Question record handles null fields gracefully")
    void question_nullFields_defaultsToEmpty() {
        var q = new Question(null, null, null, null);

        assertThat(q.title()).isEmpty();
        assertThat(q.description()).isEmpty();
        assertThat(q.url()).isEmpty();
        assertThat(q.answers()).isEmpty();
    }

    @Test
    @DisplayName("Question record creates defensive copy of answers")
    void question_defensiveCopy() {
        var answers = new java.util.ArrayList<>(List.of("a", "b"));
        var q = new Question("title", "desc", "url", answers);

        // Modifying original list should not affect the record
        answers.add("c");
        assertThat(q.answers()).hasSize(2);
    }

    @Test
    @DisplayName("ScrapeOutcome.Success holds questions")
    void scrapeOutcome_success_holdsQuestions() {
        var questions = List.of(new Question("t", "d", "u", List.of()));
        var success = new ScrapeOutcome.Success(questions);

        assertThat(success.questions()).hasSize(1);
    }

    @Test
    @DisplayName("ScrapeOutcome sealed type instanceof pattern matching works")
    void scrapeOutcome_patternMatching() {
        ScrapeOutcome outcome = new ScrapeOutcome.Blocked(429, "Rate limited");

        String result;
        if (outcome instanceof ScrapeOutcome.Success s) {
            result = "ok: " + s.questions().size();
        } else if (outcome instanceof ScrapeOutcome.Blocked b) {
            result = "blocked: " + b.statusCode();
        } else if (outcome instanceof ScrapeOutcome.Error e) {
            result = "error: " + e.message();
        } else {
            result = "unknown";
        }

        assertThat(result).isEqualTo("blocked: 429");
    }

    @Test
    @DisplayName("CrawlResult record stores all fields")
    void crawlResult_storesAllFields() {
        var outcome = new ScrapeOutcome.Success(List.of());
        var result = new CrawlResult("https://example.com", 200, outcome, 1500L);

        assertThat(result.url()).isEqualTo("https://example.com");
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.durationMs()).isEqualTo(1500L);
    }
}

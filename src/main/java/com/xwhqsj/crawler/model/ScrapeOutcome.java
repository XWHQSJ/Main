package com.xwhqsj.crawler.model;

/**
 * Sealed interface representing the outcome of a scrape operation.
 */
public sealed interface ScrapeOutcome permits ScrapeOutcome.Success, ScrapeOutcome.Blocked, ScrapeOutcome.Error {

    record Success(java.util.List<Question> questions) implements ScrapeOutcome {
        public Success {
            questions = questions != null ? java.util.List.copyOf(questions) : java.util.List.of();
        }
    }

    record Blocked(int statusCode, String reason) implements ScrapeOutcome {}

    record Error(String message, Throwable cause) implements ScrapeOutcome {}
}

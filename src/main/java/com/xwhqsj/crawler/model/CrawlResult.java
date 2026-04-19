package com.xwhqsj.crawler.model;

/**
 * Represents a crawl result with status information.
 */
public record CrawlResult(
        String url,
        int statusCode,
        ScrapeOutcome outcome,
        long durationMs
) {}

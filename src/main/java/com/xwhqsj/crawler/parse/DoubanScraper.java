package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Stub scraper for Douban (豆瓣).
 *
 * TODO: Implement Douban scraping:
 * - Parse Douban movie/book pages
 * - Extract ratings, reviews, and metadata
 * - Handle pagination
 * - Respect Douban's rate limits and robots.txt
 */
public class DoubanScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(DoubanScraper.class);

    @Override
    public List<Question> scrape(String url, HttpFetcher fetcher) {
        log.warn("DoubanScraper is a stub — not yet implemented");
        // TODO: Implement Douban scraping logic
        return List.of();
    }
}

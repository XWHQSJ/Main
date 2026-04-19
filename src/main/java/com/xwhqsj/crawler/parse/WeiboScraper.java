package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Stub scraper for Weibo (微博).
 *
 * TODO: Implement Weibo scraping:
 * - Parse Weibo hot search page
 * - Extract trending topics and post content
 * - Handle Weibo's authentication requirements
 * - Respect Weibo's rate limits and robots.txt
 */
public class WeiboScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(WeiboScraper.class);

    @Override
    public List<Question> scrape(String url, HttpFetcher fetcher) {
        log.warn("WeiboScraper is a stub — not yet implemented");
        // TODO: Implement Weibo scraping logic
        return List.of();
    }
}

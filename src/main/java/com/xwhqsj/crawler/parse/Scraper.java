package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;

import java.util.List;

/**
 * Interface for scraping questions from a web page.
 */
public interface Scraper {

    /**
     * Scrapes questions from the given URL.
     *
     * @param url     the page URL to scrape
     * @param fetcher the HTTP fetcher to use
     * @return list of scraped questions
     */
    List<Question> scrape(String url, HttpFetcher fetcher);
}

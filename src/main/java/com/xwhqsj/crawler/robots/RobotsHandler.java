package com.xwhqsj.crawler.robots;

import com.xwhqsj.crawler.http.HttpFetcher;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles robots.txt compliance for crawled domains.
 * Caches parsed rules per domain.
 */
public class RobotsHandler {

    private static final Logger log = LoggerFactory.getLogger(RobotsHandler.class);
    private static final String CRAWLER_AGENT = "CrawlerEducation";

    private final HttpFetcher fetcher;
    private final Map<String, SimpleRobotRules> rulesCache = new ConcurrentHashMap<>();
    private final boolean enabled;

    public RobotsHandler(HttpFetcher fetcher, boolean enabled) {
        this.fetcher = fetcher;
        this.enabled = enabled;
    }

    /**
     * Checks whether the given URL is allowed by robots.txt.
     */
    public boolean isAllowed(String url) {
        if (!enabled) return true;

        try {
            var uri = URI.create(url);
            String domain = uri.getScheme() + "://" + uri.getHost();
            var rules = rulesCache.computeIfAbsent(domain, this::fetchRules);
            boolean allowed = rules.isAllowed(url);
            if (!allowed) {
                log.info("robots.txt disallows: {}", url);
            }
            return allowed;
        } catch (Exception e) {
            log.warn("Failed to check robots.txt for {}, allowing by default", url, e);
            return true;
        }
    }

    /**
     * Returns the crawl delay specified in robots.txt for the domain (in milliseconds).
     * Returns -1 if no delay is specified.
     */
    public long getCrawlDelayMs(String url) {
        if (!enabled) return -1;

        try {
            var uri = URI.create(url);
            String domain = uri.getScheme() + "://" + uri.getHost();
            var rules = rulesCache.computeIfAbsent(domain, this::fetchRules);
            long delayMs = rules.getCrawlDelay() * 1000L;
            return delayMs > 0 ? delayMs : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private SimpleRobotRules fetchRules(String domain) {
        String robotsUrl = domain + "/robots.txt";
        try {
            var result = fetcher.fetchWithStatus(robotsUrl);
            if (result.statusCode() == 200) {
                var parser = new SimpleRobotRulesParser();
                var rules = parser.parseContent(
                        robotsUrl,
                        result.body().getBytes(),
                        "text/plain",
                        CRAWLER_AGENT
                );
                log.info("Loaded robots.txt from {} ({} rules)", domain, rules.getRobotRules().size());
                return rules;
            }
            log.debug("No robots.txt at {} (status {})", robotsUrl, result.statusCode());
        } catch (Exception e) {
            log.debug("Failed to fetch robots.txt from {}", domain, e);
        }
        // Allow everything if robots.txt is not available
        return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
    }
}

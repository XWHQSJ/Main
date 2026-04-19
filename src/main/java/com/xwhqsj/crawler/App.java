package com.xwhqsj.crawler;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import com.xwhqsj.crawler.output.CsvWriter;
import com.xwhqsj.crawler.output.JsonWriter;
import com.xwhqsj.crawler.output.OutputWriter;
import com.xwhqsj.crawler.output.TextWriter;
import com.xwhqsj.crawler.parse.HackerNewsScraper;
import com.xwhqsj.crawler.parse.Scraper;
import com.xwhqsj.crawler.parse.ZhihuScraper;
import com.xwhqsj.crawler.robots.RobotsHandler;
import com.xwhqsj.crawler.state.SqliteStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the Zhihu crawler.
 *
 * <p>Usage:</p>
 * <pre>
 * ./gradlew run --args='zhihu --url https://www.zhihu.com/explore/recommendations --output out.json --format json'
 * </pre>
 */
@Command(
        name = "crawler",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Web crawler for Chinese Q&A and social platforms",
        subcommands = {App.ZhihuCommand.class, App.HackerNewsCommand.class}
)
public class App implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Command(name = "zhihu", description = "Crawl Zhihu (知乎) pages")
    static class ZhihuCommand implements Runnable {

        @Option(names = {"--url", "-u"}, required = true,
                description = "URL to crawl (e.g., https://www.zhihu.com/explore/recommendations)")
        private String url;

        @Option(names = {"--output", "-o"}, defaultValue = "output.txt",
                description = "Output file path (default: output.txt)")
        private String output;

        @Option(names = {"--format", "-f"}, defaultValue = "text",
                description = "Output format: text, json, csv (default: text)")
        private String format;

        @Option(names = {"--rate", "-r"}, defaultValue = "2000",
                description = "Delay between requests in ms (default: 2000)")
        private long rateMs;

        @Option(names = {"--threads", "-t"}, defaultValue = "3",
                description = "Number of concurrent threads (default: 3)")
        private int threads;

        @Option(names = {"--respect-robots"}, defaultValue = "true",
                description = "Respect robots.txt (default: true)")
        private boolean respectRobots;

        @Option(names = {"--db"}, defaultValue = "crawler_state.db",
                description = "SQLite database for state tracking (default: crawler_state.db)")
        private String dbPath;

        @Override
        public void run() {
            log.info("Starting Zhihu crawl: url={}, format={}, rate={}ms, threads={}",
                    url, format, rateMs, threads);

            var fetcher = new HttpFetcher();
            var robotsHandler = new RobotsHandler(fetcher, respectRobots);

            // Check robots.txt
            if (!robotsHandler.isAllowed(url)) {
                log.error("URL is disallowed by robots.txt: {}", url);
                return;
            }

            // Use robots.txt crawl delay if present and larger than configured rate
            long robotsDelay = robotsHandler.getCrawlDelayMs(url);
            long effectiveRate = robotsDelay > rateMs ? robotsDelay : rateMs;
            if (robotsDelay > rateMs) {
                log.info("Using robots.txt crawl delay: {}ms (overrides --rate {}ms)", robotsDelay, rateMs);
            }

            try (var stateStore = new SqliteStateStore(dbPath)) {
                // Check if URL already visited
                if (stateStore.isVisited(url)) {
                    log.info("URL already crawled (in state DB): {}", url);
                    log.info("Re-crawling anyway...");
                }

                Scraper scraper = new ZhihuScraper();
                List<Question> questions = scraper.scrape(url, fetcher);

                // Mark as visited
                stateStore.markVisited(url, 200, "");

                if (questions.isEmpty()) {
                    log.warn("No questions found. Zhihu may require JS rendering for this page.");
                    log.info("The crawler infrastructure supports jsoup-based scraping on " +
                            "server-side rendered pages. For JS-rendered content, consider " +
                            "using a headless browser.");
                    return;
                }

                log.info("Scraped {} questions", questions.size());

                // Write output
                OutputWriter writer = selectWriter(format);
                writer.write(questions, output);

                log.info("Done. Output written to: {}", output);
                log.info("State DB: {} total visited URLs", stateStore.getVisitedCount());

            } catch (Exception e) {
                log.error("Crawl failed", e);
            }
        }

        private OutputWriter selectWriter(String fmt) {
            return switch (fmt.toLowerCase()) {
                case "json" -> new JsonWriter();
                case "csv" -> new CsvWriter();
                default -> new TextWriter();
            };
        }
    }

    @Command(name = "hackernews", description = "Fetch top stories from Hacker News (guaranteed-working JSON API)")
    static class HackerNewsCommand implements Runnable {

        @Option(names = {"--output", "-o"}, defaultValue = "hn_output.json",
                description = "Output file path (default: hn_output.json)")
        private String output;

        @Option(names = {"--format", "-f"}, defaultValue = "json",
                description = "Output format: text, json, csv (default: json)")
        private String format;

        @Option(names = {"--limit", "-n"}, defaultValue = "10",
                description = "Number of top stories to fetch (default: 10, max: 500)")
        private int limit;

        @Override
        public void run() {
            log.info("Starting Hacker News fetch: limit={}, format={}", limit, format);

            var fetcher = new HttpFetcher();
            Scraper scraper = new HackerNewsScraper(limit);

            List<Question> stories = scraper.scrape("https://news.ycombinator.com", fetcher);

            if (stories.isEmpty()) {
                log.warn("No stories fetched from Hacker News.");
                return;
            }

            log.info("Fetched {} stories from Hacker News", stories.size());

            try {
                OutputWriter writer = selectWriter(format);
                writer.write(stories, output);
                log.info("Done. Output written to: {}", output);
            } catch (Exception e) {
                log.error("Failed to write output", e);
            }
        }

        private OutputWriter selectWriter(String fmt) {
            return switch (fmt.toLowerCase()) {
                case "json" -> new JsonWriter();
                case "csv" -> new CsvWriter();
                default -> new TextWriter();
            };
        }
    }
}

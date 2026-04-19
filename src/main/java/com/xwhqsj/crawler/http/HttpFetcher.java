package com.xwhqsj.crawler.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP fetcher using Java 11+ HttpClient with HTTPS, User-Agent, redirects,
 * and exponential backoff retry on 429/503.
 */
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; CrawlerEducation/1.0)";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client;

    public HttpFetcher() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Fetches the content at the given URL as a String.
     *
     * @param url the URL to fetch
     * @return the response body as a string
     * @throws IOException if the fetch fails after all retries
     */
    public String fetch(String url) throws IOException {
        var uri = URI.create(url);
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .timeout(TIMEOUT)
                .GET()
                .build();

        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    log.debug("Successfully fetched {} ({} chars)", url, response.body().length());
                    return response.body();
                }

                if (statusCode == 429 || statusCode == 503) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << attempt);
                    log.warn("Got {} for {}, retrying in {}ms (attempt {}/{})",
                            statusCode, url, backoff, attempt + 1, MAX_RETRIES);
                    Thread.sleep(backoff);
                    continue;
                }

                throw new IOException("HTTP " + statusCode + " for " + url);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while fetching " + url, e);
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << attempt);
                    log.warn("IOException for {}: {}, retrying in {}ms (attempt {}/{})",
                            url, e.getMessage(), backoff, attempt + 1, MAX_RETRIES);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while retrying " + url, ie);
                    }
                }
            }
        }
        throw new IOException("Failed to fetch " + url + " after " + MAX_RETRIES + " attempts", lastException);
    }

    /**
     * Fetches and returns both the status code and the body.
     */
    public FetchResult fetchWithStatus(String url) {
        try {
            var uri = URI.create(url);
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", USER_AGENT)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new FetchResult(response.statusCode(), response.body(), null);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new FetchResult(-1, "", e);
        }
    }

    public record FetchResult(int statusCode, String body, Throwable error) {}
}

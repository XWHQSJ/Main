package com.xwhqsj.crawler.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper for Hacker News using the public Firebase JSON API.
 *
 * <p>This is a guaranteed-working scraper that requires no authentication
 * and is not blocked by anti-bot measures. It fetches the top stories
 * and their details via the HN API.</p>
 *
 * <p>API docs: <a href="https://github.com/HackerNews/API">HN API</a></p>
 */
public class HackerNewsScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(HackerNewsScraper.class);
    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_URL_TEMPLATE = "https://hacker-news.firebaseio.com/v0/item/%d.json";
    private static final int DEFAULT_LIMIT = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int limit;

    public HackerNewsScraper() {
        this(DEFAULT_LIMIT);
    }

    public HackerNewsScraper(int limit) {
        this.limit = Math.max(1, Math.min(limit, 500));
    }

    @Override
    public List<Question> scrape(String url, HttpFetcher fetcher) {
        try {
            String topStoriesJson = fetcher.fetch(TOP_STORIES_URL);
            List<Long> storyIds = MAPPER.readValue(topStoriesJson, new TypeReference<>() {});

            int fetchCount = Math.min(limit, storyIds.size());
            log.info("Fetching top {} stories from Hacker News ({} total available)", fetchCount, storyIds.size());

            var questions = new ArrayList<Question>();
            for (int i = 0; i < fetchCount; i++) {
                long id = storyIds.get(i);
                try {
                    var question = fetchStory(id, fetcher);
                    if (question != null) {
                        questions.add(question);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch HN story {}: {}", id, e.getMessage());
                }
            }

            log.info("Successfully fetched {} Hacker News stories", questions.size());
            return List.copyOf(questions);
        } catch (IOException e) {
            log.error("Failed to fetch Hacker News top stories", e);
            return List.of();
        }
    }

    /**
     * Fetches a single HN story and maps it to a Question record.
     */
    Question fetchStory(long id, HttpFetcher fetcher) throws IOException {
        String itemUrl = String.format(ITEM_URL_TEMPLATE, id);
        String json = fetcher.fetch(itemUrl);
        JsonNode node = MAPPER.readTree(json);

        if (node == null || node.isNull()) {
            return null;
        }

        String title = textOrEmpty(node, "title");
        String storyUrl = textOrEmpty(node, "url");
        String text = textOrEmpty(node, "text");
        int score = node.has("score") ? node.get("score").asInt() : 0;
        int descendants = node.has("descendants") ? node.get("descendants").asInt() : 0;
        String by = textOrEmpty(node, "by");

        String hnUrl = "https://news.ycombinator.com/item?id=" + id;
        String description = buildDescription(storyUrl, score, descendants, by);

        // Use the story text (for Ask HN / Show HN) as the first "answer"
        var answers = new ArrayList<String>();
        if (!text.isEmpty()) {
            answers.add(text);
        }

        return new Question(title, description, hnUrl, answers);
    }

    private static String buildDescription(String url, int score, int descendants, String by) {
        var sb = new StringBuilder();
        if (!url.isEmpty()) {
            sb.append("Link: ").append(url).append(" | ");
        }
        sb.append("Score: ").append(score);
        sb.append(" | Comments: ").append(descendants);
        sb.append(" | By: ").append(by);
        return sb.toString();
    }

    private static String textOrEmpty(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText("");
        }
        return "";
    }
}

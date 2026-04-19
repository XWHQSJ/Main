package com.xwhqsj.crawler.parse;

import com.xwhqsj.crawler.http.HttpFetcher;
import com.xwhqsj.crawler.model.Question;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper for Zhihu (知乎) pages using jsoup CSS selectors.
 *
 * <p>NOTE: Modern Zhihu is a JavaScript SPA. Server-side rendered pages may have
 * limited content. The CSS selectors target known SSR markup patterns. If jsoup
 * returns empty results, a regex fallback is attempted with a warning logged.</p>
 *
 * <p>TODO: For full JS-rendered pages, consider using a headless browser
 * (Playwright/Selenium) or Zhihu's API endpoints.</p>
 */
public class ZhihuScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(ZhihuScraper.class);

    /**
     * CSS selectors for modern Zhihu SSR markup (multiple selectors for resilience).
     */
    private static final String[] QUESTION_LINK_SELECTORS = {
            "a[href*=/question/]",
            "h2 a[data-za-detail-view-path-module]",
            ".ExploreCollectionCard a[href*=/question/]",
            ".QuestionItem a[href*=/question/]",
            ".ContentItem a[href*=/question/]",
    };

    private static final String[] TITLE_SELECTORS = {
            "h1.QuestionHeader-title",
            "h1[class*=QuestionTitle]",
            "div.QuestionPage h1",
            "h2.QuestionHeader-title",
    };

    private static final String[] DESCRIPTION_SELECTORS = {
            "div.QuestionRichText span.RichText",
            "div.QuestionHeader-detail span.RichText",
            "div[class*=QuestionDetail] span.RichText",
    };

    private static final String[] ANSWER_SELECTORS = {
            "div.RichContent-inner span.RichText",
            "div.AnswerItem span.RichText",
            "div[class*=AnswerCard] span.RichText",
    };

    // Legacy regex patterns as fallback
    private static final Pattern LEGACY_LINK_PATTERN = Pattern.compile(
            "<h2>.+?question_link.+?href=\"(.+?)\".+?</h2>"
    );
    private static final Pattern LEGACY_TITLE_PATTERN = Pattern.compile(
            "zh-question-title.+?<h2.+?>(.+?)</h2>"
    );
    private static final Pattern LEGACY_DESC_PATTERN = Pattern.compile(
            "zh-question-detail.+?<div.+?>(.*?)</div>"
    );
    private static final Pattern LEGACY_ANSWER_PATTERN = Pattern.compile(
            "/answer/content.+?<div.+?>(.*?)</div>"
    );

    @Override
    public List<Question> scrape(String url, HttpFetcher fetcher) {
        try {
            String html = fetcher.fetch(url);
            return parseListingPage(html, url, fetcher);
        } catch (IOException e) {
            log.error("Failed to fetch listing page: {}", url, e);
            return List.of();
        }
    }

    /**
     * Parses a Zhihu listing page (e.g., /explore/recommendations) to extract
     * question URLs, then fetches each question page for details.
     */
    List<Question> parseListingPage(String html, String baseUrl, HttpFetcher fetcher) {
        Document doc = Jsoup.parse(html, baseUrl);
        var questionUrls = new ArrayList<String>();

        // Try jsoup selectors first
        for (String selector : QUESTION_LINK_SELECTORS) {
            for (Element link : doc.select(selector)) {
                String href = link.absUrl("href");
                if (href.isEmpty()) {
                    href = link.attr("href");
                }
                var normalized = normalizeQuestionUrl(href);
                if (normalized != null && !questionUrls.contains(normalized)) {
                    questionUrls.add(normalized);
                }
            }
        }

        // Regex fallback if jsoup found nothing
        if (questionUrls.isEmpty()) {
            log.warn("jsoup found no question links on {}; trying regex fallback", baseUrl);
            Matcher matcher = LEGACY_LINK_PATTERN.matcher(html);
            while (matcher.find()) {
                var normalized = normalizeQuestionUrl(matcher.group(1));
                if (normalized != null && !questionUrls.contains(normalized)) {
                    questionUrls.add(normalized);
                }
            }
        }

        log.info("Found {} question URLs on {}", questionUrls.size(), baseUrl);

        var questions = new ArrayList<Question>();
        for (String qUrl : questionUrls) {
            try {
                var question = fetchQuestionDetail(qUrl, fetcher);
                if (question != null) {
                    questions.add(question);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch question detail: {}", qUrl, e);
            }
        }

        return questions;
    }

    /**
     * Fetches and parses a single Zhihu question page.
     */
    Question fetchQuestionDetail(String url, HttpFetcher fetcher) throws IOException {
        log.info("Fetching question: {}", url);
        String html = fetcher.fetch(url);
        return parseQuestionPage(html, url);
    }

    /**
     * Parses a Zhihu question detail page into a Question record.
     * Visible for testing.
     */
    public Question parseQuestionPage(String html, String url) {
        Document doc = Jsoup.parse(html, url);

        // Title
        String title = selectFirst(doc, TITLE_SELECTORS);
        if (title.isEmpty()) {
            Matcher m = LEGACY_TITLE_PATTERN.matcher(html);
            if (m.find()) {
                title = Jsoup.parse(m.group(1)).text();
                log.debug("Title found via regex fallback");
            }
        }

        // Description
        String description = selectFirst(doc, DESCRIPTION_SELECTORS);
        if (description.isEmpty()) {
            Matcher m = LEGACY_DESC_PATTERN.matcher(html);
            if (m.find()) {
                description = Jsoup.parse(m.group(1)).text();
                log.debug("Description found via regex fallback");
            }
        }

        // Answers
        var answers = new ArrayList<String>();
        for (String selector : ANSWER_SELECTORS) {
            for (Element el : doc.select(selector)) {
                String text = el.text().trim();
                if (!text.isEmpty() && !answers.contains(text)) {
                    answers.add(text);
                }
            }
            if (!answers.isEmpty()) break;
        }

        if (answers.isEmpty()) {
            Matcher m = LEGACY_ANSWER_PATTERN.matcher(html);
            while (m.find()) {
                String text = Jsoup.parse(m.group(1)).text().trim();
                if (!text.isEmpty()) {
                    answers.add(text);
                }
            }
            if (!answers.isEmpty()) {
                log.debug("Answers found via regex fallback");
            }
        }

        if (title.isEmpty() && answers.isEmpty()) {
            log.warn("No content extracted from {}. Page may require JS rendering.", url);
        }

        return new Question(title, description, url, answers);
    }

    /**
     * Normalizes a Zhihu question URL to its canonical form.
     */
    static String normalizeQuestionUrl(String href) {
        if (href == null || href.isEmpty()) return null;

        // Extract question ID
        Pattern pattern = Pattern.compile("question/(\\d+)");
        Matcher matcher = pattern.matcher(href);
        if (matcher.find()) {
            return "https://www.zhihu.com/question/" + matcher.group(1);
        }
        return null;
    }

    private String selectFirst(Document doc, String[] selectors) {
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String text = el.text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }
}

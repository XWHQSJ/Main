package com.xwhqsj.crawler.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpFetcher tests")
class HttpFetcherTest {

    private final HttpFetcher fetcher = new HttpFetcher();

    @Test
    @DisplayName("fetchWithStatus returns status code and body for valid URL")
    void fetchWithStatus_validUrl_returnsStatusAndBody() {
        // Use a known reliable URL for testing
        var result = fetcher.fetchWithStatus("https://httpbin.org/get");

        // httpbin might be down — if so, just verify the result structure
        assertThat(result).isNotNull();
        if (result.error() == null) {
            assertThat(result.statusCode()).isEqualTo(200);
            assertThat(result.body()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("fetchWithStatus handles invalid URL gracefully")
    void fetchWithStatus_invalidUrl_returnsError() {
        var result = fetcher.fetchWithStatus("https://this-domain-does-not-exist-12345.example.com");

        assertThat(result).isNotNull();
        assertThat(result.error()).isNotNull();
        assertThat(result.statusCode()).isEqualTo(-1);
    }

    @Test
    @DisplayName("fetch throws IOException for unreachable URL after retries")
    void fetch_unreachableUrl_throwsAfterRetries() {
        assertThatThrownBy(() -> fetcher.fetch("https://this-domain-does-not-exist-12345.example.com"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to fetch");
    }
}

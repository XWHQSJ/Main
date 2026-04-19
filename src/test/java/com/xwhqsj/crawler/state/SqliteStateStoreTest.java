package com.xwhqsj.crawler.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqliteStateStore tests")
class SqliteStateStoreTest {

    @TempDir
    Path tempDir;

    private SqliteStateStore store;

    @BeforeEach
    void setUp() throws SQLException {
        store = new SqliteStateStore(tempDir.resolve("test.db").toString());
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    @DisplayName("isVisited returns false for unvisited URL")
    void isVisited_newUrl_returnsFalse() {
        assertThat(store.isVisited("https://example.com")).isFalse();
    }

    @Test
    @DisplayName("markVisited then isVisited returns true")
    void markVisited_thenIsVisited_returnsTrue() {
        store.markVisited("https://example.com", 200, "content");

        assertThat(store.isVisited("https://example.com")).isTrue();
    }

    @Test
    @DisplayName("getVisitedCount tracks visited URLs")
    void getVisitedCount_afterInserts_returnsCorrectCount() {
        assertThat(store.getVisitedCount()).isEqualTo(0);

        store.markVisited("https://example.com/1", 200, "a");
        store.markVisited("https://example.com/2", 200, "b");
        store.markVisited("https://example.com/3", 404, "");

        assertThat(store.getVisitedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("markVisited with same URL updates instead of failing")
    void markVisited_duplicateUrl_updates() {
        store.markVisited("https://example.com", 200, "first");
        store.markVisited("https://example.com", 200, "second");

        assertThat(store.getVisitedCount()).isEqualTo(1);
        assertThat(store.isVisited("https://example.com")).isTrue();
    }
}

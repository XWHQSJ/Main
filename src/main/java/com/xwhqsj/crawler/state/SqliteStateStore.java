package com.xwhqsj.crawler.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * SQLite-based state store for tracking visited URLs.
 * Table: visited(url TEXT PRIMARY KEY, crawled_at INTEGER, status INTEGER, content_hash TEXT)
 */
public class SqliteStateStore implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SqliteStateStore.class);
    private final Connection connection;

    public SqliteStateStore(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initTable();
    }

    private void initTable() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS visited (
                        url TEXT PRIMARY KEY,
                        crawled_at INTEGER NOT NULL,
                        status INTEGER NOT NULL,
                        content_hash TEXT
                    )
                    """);
        }
    }

    /**
     * Checks if a URL has already been visited.
     */
    public boolean isVisited(String url) {
        try (var stmt = connection.prepareStatement("SELECT 1 FROM visited WHERE url = ?")) {
            stmt.setString(1, url);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            log.warn("Failed to check visited status for {}", url, e);
            return false;
        }
    }

    /**
     * Marks a URL as visited with its status and content hash.
     */
    public void markVisited(String url, int status, String content) {
        var hash = hashContent(content);
        try (var stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO visited (url, crawled_at, status, content_hash) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, url);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setInt(3, status);
            stmt.setString(4, hash);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to mark visited: {}", url, e);
        }
    }

    /**
     * Returns the total number of visited URLs.
     */
    public int getVisitedCount() {
        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM visited");
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.warn("Failed to count visited URLs", e);
            return 0;
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("Failed to close SQLite connection", e);
        }
    }

    private static String hashContent(String content) {
        if (content == null || content.isEmpty()) return "";
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(content.getBytes());
            var sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}

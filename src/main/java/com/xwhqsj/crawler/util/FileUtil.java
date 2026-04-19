package com.xwhqsj.crawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File utility methods. Cleaned version of legacy FileReaderWriter.
 *
 * Bug fixes from legacy code:
 * - createNewFile now returns proper boolean success (was always returning false)
 * - writeToFile returns proper boolean success (was always returning false)
 */
public final class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
        // utility class
    }

    /**
     * Creates a file and its parent directories.
     *
     * @param filePath the path of the file to create
     * @return true if the file was created or already exists
     */
    public static boolean createNewFile(String filePath) {
        try {
            var path = Path.of(filePath);
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to create file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Writes content to a file.
     *
     * @param content  the content to write
     * @param filePath the path to write to
     * @param append   whether to append or overwrite
     * @return true if the write succeeded
     */
    public static boolean writeToFile(String content, String filePath, boolean append) {
        try {
            var path = Path.of(filePath);
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (append) {
                Files.writeString(path, content, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, StandardCharsets.UTF_8);
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to write to file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Reads a file's contents as a string.
     *
     * @param filePath the file to read
     * @return the file contents, or empty string on error
     */
    public static String readFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            return "";
        }
    }
}

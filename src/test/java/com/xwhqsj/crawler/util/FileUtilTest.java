package com.xwhqsj.crawler.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileUtil tests")
class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("createNewFile creates file and parent directories")
    void createNewFile_createsFileAndDirs() {
        String path = tempDir.resolve("a/b/test.txt").toString();

        boolean result = FileUtil.createNewFile(path);

        assertThat(result).isTrue();
        assertThat(Files.exists(Path.of(path))).isTrue();
    }

    @Test
    @DisplayName("writeToFile writes content correctly")
    void writeToFile_writesContent() {
        String path = tempDir.resolve("write.txt").toString();

        boolean result = FileUtil.writeToFile("hello world", path, false);

        assertThat(result).isTrue();
        assertThat(FileUtil.readFile(path)).isEqualTo("hello world");
    }

    @Test
    @DisplayName("writeToFile with append adds to existing content")
    void writeToFile_appendMode_addsContent() {
        String path = tempDir.resolve("append.txt").toString();

        FileUtil.writeToFile("first", path, false);
        FileUtil.writeToFile(" second", path, true);

        assertThat(FileUtil.readFile(path)).isEqualTo("first second");
    }

    @Test
    @DisplayName("readFile returns empty string for nonexistent file")
    void readFile_nonexistent_returnsEmpty() {
        String result = FileUtil.readFile(tempDir.resolve("nonexistent.txt").toString());

        assertThat(result).isEmpty();
    }
}

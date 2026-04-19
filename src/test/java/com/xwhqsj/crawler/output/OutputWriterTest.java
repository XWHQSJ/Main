package com.xwhqsj.crawler.output;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwhqsj.crawler.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutputWriter tests")
class OutputWriterTest {

    @TempDir
    Path tempDir;

    private final List<Question> sampleQuestions = List.of(
            new Question("Test Question 1", "Description 1", "https://zhihu.com/q/1",
                    List.of("Answer 1a", "Answer 1b")),
            new Question("Test Question 2", "Description 2", "https://zhihu.com/q/2",
                    List.of("Answer 2a"))
    );

    @Test
    @DisplayName("TextWriter produces readable output")
    void textWriter_writesReadableText() throws Exception {
        var writer = new TextWriter();
        var filePath = tempDir.resolve("output.txt").toString();

        writer.write(sampleQuestions, filePath);

        String content = Files.readString(Path.of(filePath));
        assertThat(content).contains("Test Question 1");
        assertThat(content).contains("Description 1");
        assertThat(content).contains("Answer 1a");
        assertThat(content).contains("Test Question 2");
    }

    @Test
    @DisplayName("JsonWriter produces valid JSON that round-trips")
    void jsonWriter_roundTrips() throws Exception {
        var writer = new JsonWriter();
        var filePath = tempDir.resolve("output.json").toString();

        writer.write(sampleQuestions, filePath);

        // Read back and verify
        var mapper = new ObjectMapper();
        List<Question> readBack = mapper.readValue(
                Path.of(filePath).toFile(),
                new TypeReference<>() {}
        );

        assertThat(readBack).hasSize(2);
        assertThat(readBack.get(0).title()).isEqualTo("Test Question 1");
        assertThat(readBack.get(0).answers()).containsExactly("Answer 1a", "Answer 1b");
        assertThat(readBack.get(1).title()).isEqualTo("Test Question 2");
    }

    @Test
    @DisplayName("CsvWriter produces valid CSV with header")
    void csvWriter_producesValidCsv() throws Exception {
        var writer = new CsvWriter();
        var filePath = tempDir.resolve("output.csv").toString();

        writer.write(sampleQuestions, filePath);

        List<String> lines = Files.readAllLines(Path.of(filePath));
        assertThat(lines).hasSizeGreaterThanOrEqualTo(3); // header + 2 data rows
        assertThat(lines.get(0)).contains("title");
        assertThat(lines.get(0)).contains("description");
        assertThat(lines.get(0)).contains("url");
        assertThat(lines.get(0)).contains("answers");
    }

    @Test
    @DisplayName("TextWriter creates parent directories if needed")
    void textWriter_createsParentDirs() throws Exception {
        var writer = new TextWriter();
        var filePath = tempDir.resolve("subdir/nested/output.txt").toString();

        writer.write(sampleQuestions, filePath);

        assertThat(Files.exists(Path.of(filePath))).isTrue();
    }

    @Test
    @DisplayName("Writers handle empty question list")
    void writers_handleEmptyList() throws Exception {
        var textPath = tempDir.resolve("empty.txt").toString();
        var jsonPath = tempDir.resolve("empty.json").toString();
        var csvPath = tempDir.resolve("empty.csv").toString();

        new TextWriter().write(List.of(), textPath);
        new JsonWriter().write(List.of(), jsonPath);
        new CsvWriter().write(List.of(), csvPath);

        assertThat(Files.exists(Path.of(textPath))).isTrue();
        assertThat(Files.exists(Path.of(jsonPath))).isTrue();
        assertThat(Files.exists(Path.of(csvPath))).isTrue();
    }
}

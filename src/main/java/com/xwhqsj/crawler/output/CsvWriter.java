package com.xwhqsj.crawler.output;

import com.opencsv.CSVWriter;
import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes questions to a CSV file using OpenCSV.
 */
public class CsvWriter implements OutputWriter {

    private static final Logger log = LoggerFactory.getLogger(CsvWriter.class);

    @Override
    public void write(List<Question> questions, String filePath) throws IOException {
        var path = Path.of(filePath);
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (var writer = new CSVWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8))) {
            // Header
            writer.writeNext(new String[]{"title", "description", "url", "answers"});

            for (var q : questions) {
                String answersJoined = String.join(" | ", q.answers());
                writer.writeNext(new String[]{q.title(), q.description(), q.url(), answersJoined});
            }
        }

        log.info("Wrote {} questions as CSV to {}", questions.size(), filePath);
    }
}

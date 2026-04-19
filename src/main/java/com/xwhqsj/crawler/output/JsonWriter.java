package com.xwhqsj.crawler.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes questions to a JSON file using Jackson.
 */
public class JsonWriter implements OutputWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonWriter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void write(List<Question> questions, String filePath) throws IOException {
        var path = Path.of(filePath);
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MAPPER.writeValue(path.toFile(), questions);
        log.info("Wrote {} questions as JSON to {}", questions.size(), filePath);
    }
}

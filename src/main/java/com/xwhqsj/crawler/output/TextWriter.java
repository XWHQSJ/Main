package com.xwhqsj.crawler.output;

import com.xwhqsj.crawler.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes questions to a plain text file.
 */
public class TextWriter implements OutputWriter {

    private static final Logger log = LoggerFactory.getLogger(TextWriter.class);

    @Override
    public void write(List<Question> questions, String filePath) throws IOException {
        var sb = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            var q = questions.get(i);
            sb.append("问题：").append(q.title()).append("\n");
            sb.append("描述：").append(q.description()).append("\n");
            sb.append("链接：").append(q.url()).append("\n");

            for (int j = 0; j < q.answers().size(); j++) {
                sb.append("回答").append(j + 1).append("：").append(q.answers().get(j)).append("\n\n");
            }

            if (i < questions.size() - 1) {
                sb.append("\n").append("=".repeat(60)).append("\n\n");
            }
        }

        var path = Path.of(filePath);
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        log.info("Wrote {} questions to {}", questions.size(), filePath);
    }
}

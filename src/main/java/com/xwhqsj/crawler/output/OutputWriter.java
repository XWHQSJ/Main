package com.xwhqsj.crawler.output;

import com.xwhqsj.crawler.model.Question;

import java.io.IOException;
import java.util.List;

/**
 * Interface for writing scraped questions to various output formats.
 */
public interface OutputWriter {

    /**
     * Writes the list of questions to the specified file path.
     *
     * @param questions the questions to write
     * @param filePath  the output file path
     * @throws IOException if writing fails
     */
    void write(List<Question> questions, String filePath) throws IOException;
}

package com.xwhqsj.crawler.model;

import java.util.List;

/**
 * Represents a Zhihu question with its metadata and answers.
 */
public record Question(
        String title,
        String description,
        String url,
        List<String> answers
) {
    public Question {
        title = title != null ? title : "";
        description = description != null ? description : "";
        url = url != null ? url : "";
        answers = answers != null ? List.copyOf(answers) : List.of();
    }
}

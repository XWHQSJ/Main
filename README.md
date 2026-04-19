# Main — Java 小爬虫

![Java](https://img.shields.io/badge/Java-8%2B-orange) ![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)

A small Java web crawler that scrapes Zhihu (知乎) editor-recommended questions, extracts titles, descriptions, and answers, then saves the results to a local text file.

一个 Java 小爬虫，抓取知乎编辑推荐页面的问题、描述和回答，并保存到本地文本文件。

## Features

- Crawls the Zhihu "Editor's Picks" page (`/explore/recommendations`)
- Extracts question URLs via regex matching on the recommendation listing
- For each question, fetches the detail page and parses:
  - Question title
  - Question description
  - All answers
- Writes structured output to a local `.txt` file
- Pure Java standard library — no external dependencies

## Project Structure

```
src/
├── Main.java              # Entry point — kicks off the crawl
├── Spider.java            # HTTP GET requests and HTML regex parsing
├── ZhiHu.java            # Zhihu question model — fetches and stores Q&A data
└── FileReaderWriter.java  # File creation and write helpers
```

## Requirements

- Java 8 or later

## Build and Run

```bash
# Compile
javac -d out src/*.java

# Run
java -cp out Main
```

Or open the project in **IntelliJ IDEA** and run the `Main` class directly.

> **Note**: The default output path in `Main.java` is `D:/知乎-编辑推荐.txt`. Change this to a valid path on your system before running.

## Dependencies

None. The crawler uses only the Java standard library:

- `java.net.URL` / `java.net.URLConnection` — HTTP requests
- `java.util.regex` — HTML parsing via regex
- `java.io` — file I/O

## Disclaimer

This is an educational project. Please respect target websites' `robots.txt` and terms of service. The Zhihu page structure may have changed since 2016, so the regex patterns may no longer match current HTML.

本项目仅供学习用途，请遵守目标网站的 `robots.txt` 及服务条款。知乎页面结构自 2016 年以来可能已发生变化，正则匹配可能不再适用于当前 HTML。

## License

[MIT](LICENSE)

# Zhihu Crawler (知乎爬虫)

![Java](https://img.shields.io/badge/Java-17-orange) ![Gradle](https://img.shields.io/badge/Gradle-8.7-blue) ![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

A Java web crawler that scrapes Zhihu (知乎) and other Chinese Q&A platforms, extracts questions, descriptions, and answers, then saves results in text, JSON, or CSV format.

Java 网络爬虫，抓取知乎等中文问答平台的问题、描述和回答，支持输出为文本、JSON 或 CSV 格式。

## Disclaimer / 免责声明

> **This is an educational project.** Please respect target websites' `robots.txt` and terms of service. The crawler includes robots.txt compliance by default. Modern Zhihu uses client-side JavaScript rendering; the crawler works on server-side rendered pages but may return limited results on JS-rendered pages.
>
> **本项目仅供学习使用。** 请遵守目标网站的 `robots.txt` 及服务条款。爬虫默认遵循 robots.txt。现代知乎使用客户端 JavaScript 渲染，爬虫在服务端渲染页面上有效，但 JS 渲染页面可能返回有限结果。

## Architecture / 架构

```
com.xwhqsj.crawler
├── App.java                    # CLI entry (picocli)
├── http/
│   └── HttpFetcher.java        # Java 11 HttpClient (HTTPS, retry, backoff)
├── parse/
│   ├── Scraper.java            # Scraper interface
│   ├── ZhihuScraper.java       # jsoup + regex fallback
│   ├── WeiboScraper.java       # stub (TODO)
│   └── DoubanScraper.java      # stub (TODO)
├── model/
│   ├── Question.java           # record
│   ├── CrawlResult.java        # record
│   └── ScrapeOutcome.java      # sealed interface (Success/Blocked/Error)
├── output/
│   ├── OutputWriter.java       # interface
│   ├── TextWriter.java
│   ├── JsonWriter.java         # Jackson
│   └── CsvWriter.java          # OpenCSV
├── util/
│   └── FileUtil.java           # cleaned legacy I/O
├── state/
│   └── SqliteStateStore.java   # SQLite visited-URL tracking
└── robots/
    └── RobotsHandler.java      # robots.txt compliance (crawler-commons)
```

## Requirements / 环境要求

- Java 17+ (build verified on OpenJDK 17)
- Gradle 8+ (wrapper included)

## Build / 构建

```bash
./gradlew build
```

## Run / 运行

```bash
# Basic usage
./gradlew run --args='zhihu --url https://www.zhihu.com/explore/recommendations --output out.json --format json'

# With rate limiting and threads
./gradlew run --args='zhihu --url https://www.zhihu.com/explore/recommendations --output out.csv --format csv --rate 2000 --threads 3'

# Show help
./gradlew run --args='--help'
./gradlew run --args='zhihu --help'
```

### CLI Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--url`, `-u` | (required) | URL to crawl |
| `--output`, `-o` | `output.txt` | Output file path |
| `--format`, `-f` | `text` | Output format: `text`, `json`, `csv` |
| `--rate`, `-r` | `2000` | Delay between requests (ms) |
| `--threads`, `-t` | `3` | Concurrent fetch threads |
| `--respect-robots` | `true` | Respect robots.txt |
| `--db` | `crawler_state.db` | SQLite state database path |

## Test / 测试

```bash
./gradlew test
```

## Docker

```bash
# Build
docker build -t zhihu-crawler .

# Run
docker run -v $PWD/out:/out zhihu-crawler zhihu \
  --url https://www.zhihu.com/explore/recommendations \
  --output /out/output.json \
  --format json

# Docker Compose
docker-compose up
```

## Features / 特性

- **jsoup HTML parsing** with CSS selectors + regex fallback
- **Java 11 HttpClient** with HTTPS, User-Agent, redirect following
- **Exponential backoff** retry on 429/503 (3 attempts, starting 2s)
- **Multiple output formats**: text, JSON (Jackson), CSV (OpenCSV)
- **robots.txt compliance** via crawler-commons
- **SQLite state tracking** to skip already-crawled URLs
- **SLF4J + Logback** logging (set level via `-Dlog.level=DEBUG`)
- **picocli CLI** with `--help` and subcommands
- **Java 17 features**: records, sealed interfaces, switch expressions, text blocks, `var`

## Known Limitations / 已知限制

- Modern Zhihu pages are JavaScript SPAs; jsoup cannot execute JS. CSS selectors may not match dynamically rendered content. For full scraping, a headless browser (Playwright/Selenium) or Zhihu API would be needed.
- CSS selectors are tuned for known Zhihu SSR patterns as of 2024. Zhihu may change their markup at any time.
- Weibo and Douban scrapers are stubs; implement based on the `Scraper` interface.

## Legacy Code / 旧代码

Original 2016 source files are preserved in `legacy/` for reference:
- `Main.java` — original entry point
- `Spider.java` — URLConnection + regex parsing
- `ZhiHu.java` — question model (has writeString double-append bug)
- `FileReaderWriter.java` — file I/O (return values always false)

## License

[MIT](LICENSE)

# Zhihu Crawler (知乎爬虫)

[![CI](https://github.com/XWHQSJ/Main/actions/workflows/codeql.yml/badge.svg)](https://github.com/XWHQSJ/Main/actions/workflows/codeql.yml)
[![Scheduled Crawl](https://github.com/XWHQSJ/Main/actions/workflows/scheduled-crawl.yml/badge.svg)](https://github.com/XWHQSJ/Main/actions/workflows/scheduled-crawl.yml)
[![Docker](https://github.com/XWHQSJ/Main/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/XWHQSJ/Main/actions/workflows/docker-publish.yml)
[![Release](https://github.com/XWHQSJ/Main/actions/workflows/release.yml/badge.svg)](https://github.com/XWHQSJ/Main/actions/workflows/release.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Gradle](https://img.shields.io/badge/Gradle-8.7-blue)
![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

A Java web crawler that scrapes Hacker News, Zhihu (知乎), and other platforms, then saves results in JSON, CSV, or text format. Includes automated daily scraping via GitHub Actions.

Java 网络爬虫，抓取 Hacker News、知乎等平台内容，支持 JSON、CSV、文本格式输出。含 GitHub Actions 自动化每日抓取。

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
│   ├── HackerNewsScraper.java  # Hacker News JSON API (guaranteed-working)
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
# Hacker News — guaranteed-working, fetches top stories via JSON API
./gradlew run --args='hackernews --output hn.json --format json --limit 20'

# Zhihu — best-effort (modern Zhihu may require JS rendering)
./gradlew run --args='zhihu --url https://www.zhihu.com/explore/recommendations --output out.json --format json'

# With rate limiting and threads
./gradlew run --args='zhihu --url https://www.zhihu.com/explore/recommendations --output out.csv --format csv --rate 2000 --threads 3'

# Show help
./gradlew run --args='--help'
./gradlew run --args='hackernews --help'
./gradlew run --args='zhihu --help'
```

### CLI Flags — hackernews

| Flag | Default | Description |
|------|---------|-------------|
| `--output`, `-o` | `hn_output.json` | Output file path |
| `--format`, `-f` | `json` | Output format: `text`, `json`, `csv` |
| `--limit`, `-n` | `10` | Number of top stories (max 500) |

### CLI Flags — zhihu

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
# Build locally
docker build -t zhihu-crawler .

# Run Hacker News scraper
docker run -v $PWD/out:/out zhihu-crawler hackernews \
  --output /out/hn.json --format json --limit 20

# Run Zhihu scraper (best-effort)
docker run -v $PWD/out:/out zhihu-crawler zhihu \
  --url https://www.zhihu.com/explore/recommendations \
  --output /out/output.json \
  --format json

# Docker Compose
docker-compose up

# Pull from GitHub Container Registry
docker pull ghcr.io/xwhqsj/main:master
```

## Run via Fat JAR

Download the latest release JAR from [Releases](https://github.com/XWHQSJ/Main/releases):

```bash
java -jar zhihu-crawler-1.0.0.jar hackernews --output hn.json --format json
java -jar zhihu-crawler-1.0.0.jar zhihu --url https://www.zhihu.com/explore/recommendations --output out.json --format json
```

## Automated Daily Scrape / 自动每日抓取

A GitHub Actions workflow runs daily:

- **Hacker News** (daily 02:00 UTC): Fetches top 30 stories via JSON API. Always succeeds.
- **Zhihu** (weekly Monday 03:00 UTC): Best-effort crawl. If Zhihu blocks (JS rendering), a placeholder JSON with `status: blocked` is committed instead.

Results are stored in the [`data/`](data/) directory and auto-committed to the repo.

You can also trigger manually: Actions tab -> "Scheduled Crawl" -> Run workflow.

## Features / 特性

- **Hacker News JSON API scraper** — guaranteed-working, no auth required
- **jsoup HTML parsing** with CSS selectors + regex fallback (Zhihu)
- **Java 11 HttpClient** with HTTPS, User-Agent, redirect following
- **Exponential backoff** retry on 429/503 (3 attempts, starting 2s)
- **Multiple output formats**: text, JSON (Jackson), CSV (OpenCSV)
- **robots.txt compliance** via crawler-commons
- **SQLite state tracking** to skip already-crawled URLs
- **SLF4J + Logback** logging (set level via `-Dlog.level=DEBUG`)
- **picocli CLI** with `--help` and subcommands
- **Java 17 features**: records, sealed interfaces, switch expressions, text blocks, `var`
- **Shadow JAR**: single executable fat JAR via `./gradlew shadowJar`
- **CI/CD**: GitHub Actions for scheduled crawling, Docker publish, release binaries
- **Security**: CodeQL analysis + Dependabot for dependency updates

## Known Limitations / 已知限制

- Modern Zhihu pages are JavaScript SPAs; jsoup cannot execute JS. CSS selectors may not match dynamically rendered content. For full scraping, a headless browser (Playwright/Selenium) or Zhihu API would be needed. The Zhihu scraper is **best-effort** — use HackerNewsScraper for a guaranteed-working demo.
- CSS selectors are tuned for known Zhihu SSR patterns. Fixtures in `src/test/resources/fixtures/` were captured 2024-06-15; Zhihu may change their markup at any time.
- Weibo and Douban scrapers are stubs; implement based on the `Scraper` interface.

## Zhihu ToS Disclaimer / 知乎服务条款声明

> **Education only.** This project is for learning web scraping techniques. Do not use it to violate Zhihu's Terms of Service. The crawler respects `robots.txt` by default. The Hacker News scraper uses the official public API, which is explicitly provided for programmatic access.
>
> **仅供学习。** 本项目用于学习爬虫技术。请勿用于违反知乎服务条款。爬虫默认遵守 `robots.txt`。Hacker News 爬虫使用官方公开 API。

## Legacy Code / 旧代码

Original 2016 source files are preserved in `legacy/` for reference:
- `Main.java` — original entry point
- `Spider.java` — URLConnection + regex parsing
- `ZhiHu.java` — question model (has writeString double-append bug)
- `FileReaderWriter.java` — file I/O (return values always false)

## License

[MIT](LICENSE)

# InsightAgent

An AI data analyst built entirely in Java. Upload a CSV or Excel file, ask questions in plain English, and an agent writes SQL, runs it, fixes its own errors, and explains the result. You watch every step live.

**Stack:** Vaadin 24 · Spring Boot 3.5 · Spring AI 1.0 · DuckDB · PostgreSQL (Neon) · Spring Security · AWS Elastic Beanstalk

## How it works

```
Question ──> AnalystAgent (Spring AI ChatClient + memory)
               │  plans which columns it needs
               ├─> runQuery(sql) tool ──> SqlGuard ──> DuckDB (read-only, no file/network access)
               │        ▲                                  │
               │        └──── ERROR: fix and retry ◀───────┘   (self-correction loop, max 5 queries)
               └─> final insight + SQL + result table ──> Vaadin UI (steps streamed via @Push)
```

Safety is layered: `SqlGuard` only lets single SELECT statements through and blocks file-reading functions, and the DuckDB connection itself is read-only with `enable_external_access = false`.

## Run locally

Needs Java 21 and Maven. Node is downloaded by Vaadin automatically on first run.

```bash
# OpenAI
export OPENAI_API_KEY=sk-...

# or Groq (free tier, OpenAI-compatible)
export OPENAI_API_KEY=gsk_...
export OPENAI_BASE_URL=https://api.groq.com/openai
export OPENAI_MODEL=llama-3.3-70b-versatile

mvn spring-boot:run
```

Open http://localhost:8080, create an account, upload `samples/sales.csv`, and try:

- Which region made the most revenue in 2025?
- Why did East region sales drop in March?
- What are the top 3 products by units sold in Q4?
- Do discounts actually increase units sold?

Local dev uses an H2 file database in `./data`. No setup needed.

## Project layout

```
src/main/java/com/insightagent/
├── agent/      AnalystAgent (LLM loop + memory), DatasetTools (@Tool methods), AgentAnswer
├── data/       DuckDbService, DatasetService, SqlGuard, ExcelConverter, QueryResult
├── domain/     AppUser, Dataset, ChatMessage (JPA)
├── repo/       Spring Data repositories
├── security/   SecurityConfig, UserService, CurrentUser
└── ui/         LoginView, RegisterView, MainLayout, DatasetsView, DatasetDetailView, ChatView
```

## Deploy (no Docker)

1. Create a free Postgres on [Neon](https://neon.tech) and copy the JDBC URL.
2. Build: `mvn -Pproduction package` produces `target/insightagent.jar`.
3. AWS Elastic Beanstalk: create a **Java SE (Corretto 21)** environment and upload the JAR.
4. Set environment properties:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `5000` |
| `DATABASE_URL` | `jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from Neon |
| `OPENAI_API_KEY` (+ `OPENAI_BASE_URL`, `OPENAI_MODEL` for Groq) | your key |

CI runs on every push (`.github/workflows/build.yml`). The deploy step is there, commented out, for Week 4.

Note: uploaded datasets are stored on the instance disk, so they reset on redeploy. Moving them to S3 is a good later upgrade.

## Roadmap

- [x] Week 1: auth, upload CSV/Excel, schema + preview
- [x] Week 2: single agent with tools, SQL guard, self-correction, chat memory, saved history
- [ ] Week 3: Planner + Critic + Report agents, charts
- [ ] Week 4: dashboards, weekly email reports, 30-question eval benchmark, Beanstalk deploy

## Tests

```bash
mvn test
```

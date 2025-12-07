# Repository Guidelines

## Project Structure & Module Organization
- Root services: `backend/` (Spring Boot 3.3) and `frontend/` (Angular 17). Docker orchestration lives in `docker-compose.yml`.
- Backend code: `backend/src/main/java/com/zant/backend/**`; templates/static assets in `backend/src/main/resources/{templates,static}`; configuration in `backend/src/main/resources/application.properties`.
- Backend tests: `backend/src/test/java/**` (JUnit 5). Generate new packages under `com.zant.backend`.
- Frontend app: `frontend/src/app/**` with shared UI assets under `frontend/src/assets/`. Angular build output lands in `frontend/dist/`.

## Build, Test, and Development Commands
- Backend dev server: `cd backend && ./mvnw spring-boot:run` (uses local DB config; override with env vars).
- Backend tests: `cd backend && ./mvnw clean test`.
- Backend package: `cd backend && ./mvnw clean package` (produces JAR under `backend/target/`).
- Frontend dev server: `cd frontend && npm install && npm run start` (serves on 4200 with live reload).
- Frontend build: `cd frontend && npm run build` (outputs to `frontend/dist/frontend/`).
- Full stack via Docker: `docker-compose up --build` (requires `GEMINI_API_KEY` and exposes 8081/4200); tear down with `docker-compose down -v`.

## Coding Style & Naming Conventions
- Java: 4-space indentation, Lombok is available; annotate layers with `@Controller`, `@Service`, `@Repository`; class names `PascalCase`, fields/methods `camelCase`. Prefer constructor injection.
- TypeScript/HTML/SCSS: 2-space indentation; Angular components/directories `kebab-case` (e.g., `user-card.component.ts`); exported symbols `PascalCase` for classes/components and `camelCase` for functions/variables.
- Use `ng generate ...` for new Angular artifacts to keep scaffolding consistent; keep DTOs/mappers grouped under `backend/src/main/java/com/zant/backend/{dto,mapper}`.

## Testing Guidelines
- Backend: place unit/integration tests in `backend/src/test/java`, suffix files with `*Test.java`; favor JUnit 5 and Spring Boot test slices. Cover controllers, services, and data access for new features.
- Frontend: create specs alongside components as `*.spec.ts`; keep DOM queries resilient (test ids or role-based selectors). Run `npm test` locally before pushes.

## Commit & Pull Request Guidelines
- Commit messages in history are short and imperative (often Polish); follow that style, keep scope focused, and group related changes together. Reference issues as `#123` when applicable.
- Pull requests should include: brief problem/solution summary, affected areas, manual/automated test notes, and screenshots or GIFs for UI changes. Mention config or migration impacts explicitly.

## Security & Configuration Tips
- Never commit secrets. Provide `GEMINI_API_KEY` and DB credentials via environment variables (`application.properties` has overridable defaults). For Docker, set `GEMINI_API_KEY` in your shell or compose overrides.
- Postgres data persists via `postgres_data` volume; remove with `docker-compose down -v` when a clean slate is required. Validate PDFs/DOCX outputs locally before sharing externally.

# Phase 2+ Backlog

Forward-looking items surfaced during Phase 1 reviews. Phase 1 implementations are spec-correct; these are deliberate Phase 2/3 revisits.

## From Task 5 review (GlobalExceptionHandler — commit `b55eaf2`)

### Phase 2 — extend exception coverage
**Why:** `@ExceptionHandler(Exception.class)` currently catches all framework exceptions and maps them to 500. Once Phase 2 ships POST `/auth/login` with JSON bodies, malformed input gets `500 INTERNAL_SERVER_ERROR` instead of 400. Same for path mismatches, missing params, etc.

**How to apply:** Either (a) make `GlobalExceptionHandler` extend `ResponseEntityExceptionHandler` and override `handleExceptionInternal` to wrap responses in `ApiResponse`, or (b) add explicit `@ExceptionHandler` for `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, `HttpRequestMethodNotSupportedException`, `NoResourceFoundException`.

### Phase 2 — split `AuthenticationException`
**Why:** Currently all auth failures map to `INVALID_TOKEN`. The `EXPIRED_TOKEN` and `INVALID_CREDENTIALS` ErrorCode entries exist but are unreachable. Once JWT auth is wired in Phase 2, login failures and token expiration should produce different error codes for client UX (e.g. "재로그인 필요" vs "다시 시도").

**How to apply:** Add explicit handlers for `BadCredentialsException` → `INVALID_CREDENTIALS`, `ExpiredJwtException` → `EXPIRED_TOKEN`. Keep generic `AuthenticationException` → `INVALID_TOKEN` as fallback.

### Phase 1.5 — decide validation `error.details` shape (BEFORE frontend consumes it)
**Why:** Current handler joins field errors as `"email: must not be blank, password: ..."` string. Frontend can't programmatically attach errors to fields, can't translate, can't show inline. Once `LoginView` / `SignUpView` start consuming validation errors in Phase 2 frontend, changing this is a breaking API change.

**How to apply:** Decide before Phase 2 frontend forms. If switching: extend `ApiResponse.ErrorBody` with optional `Map<String, String> details` field. Update `handleValidation` to populate the map. Update test fixtures.

### Phase 2 — `AccessDeniedException` / `AuthenticationException` test coverage
**Why:** Phase 1 test slice uses `addFilters = false`, so security-thrown exceptions don't flow through. Two of the 5 handlers are untested.

**How to apply:** Once `SecurityConfig` is hardened in Phase 2, integration-test the security paths through real filters.

### Phase 2 polish (optional, low cost)
- `handleAccessDenied` and `handleAuthentication` discard the `ex` parameter. Add `log.warn("Authentication failed: {}", ex.getClass().getSimpleName())` for ops diagnostics.
- Add a code-review checklist item: "Custom validation messages must not interpolate `{validatedValue}` if logged" (avoids future PII leak).

## From Task 2 review (Backend Gradle skeleton — commit `201baf6`)

### Optional polish
- `build.gradle`: add `options.encoding = 'UTF-8'` and `options.release = 17` to `JavaCompile` config to harden against Windows codepage / Java 21 host issues.
- `application.yml`: `MySQL8Dialect` is deprecated in Hibernate 6 — switch to `MySQLDialect` (auto-version detection) when convenient. Currently a startup WARN only.
- README: add Quick Start prerequisites line ("Requires Java 17, Node 20+, Docker Compose v2") to prevent first-run failures.

## From Task 1 review (README — commit `db9c2ae`)

### Optional polish (after Task 14 verification)
- `cp .env.example .env` is POSIX (works in PowerShell via alias, breaks in cmd.exe). Mention `Windows: copy .env.example .env` if cmd users hit it.
- Annotate `docker compose up db` as `# starts MySQL only` for clarity.

## From Task 6 review (SecurityConfig + WebConfig — commit `adc2b31`)

### Phase 2 — document or consolidate CORS source
**Why:** `SecurityConfig.cors(cors -> {})` (empty lambda) implicitly delegates to `WebConfig.addCorsMappings`. A future maintainer reading `SecurityConfig` alone can't see where CORS rules live, and adding any other `CorsConfigurationSource` bean elsewhere will silently override `WebConfig`.

**How to apply:** Either (a) move CORS to a `CorsConfigurationSource` bean inside `SecurityConfig` and delete `WebConfig.addCorsMappings`, or (b) add a Javadoc on `SecurityConfig.cors(cors -> {})` pointing readers to `WebConfig`. Phase 2 (during JWT filter wiring) is a natural moment to revisit.

### Phase 2 — split `requestMatchers` per resource for grep-ability
**Why:** `SecurityConfig` currently lumps `GET /api/songs/**`, `GET /api/characters`, `GET /api/songs/*/comments` on one `requestMatchers` line. When Phase 4 adds `POST /api/songs/{id}/comments` (authenticated), reviewers must trace that the `GET, "/api/songs/*/comments"` line does NOT cover POST. Subtle but easy to misread.

**How to apply:** Split into one resource per `.requestMatchers(...)` line in Phase 2 when the chain gets more complex anyway.

### Phase 2 — migrate `disable()` to method references
**Why:** Spring Security 6.1+ deprecates the lambda form `csrf -> csrf.disable()` in favor of `AbstractHttpConfigurer::disable` method references. Identical behavior, idiomatic for SS6.

**How to apply:** Phase 2 SecurityConfig edit pass:
```
.csrf(AbstractHttpConfigurer::disable)
.formLogin(AbstractHttpConfigurer::disable)
.httpBasic(AbstractHttpConfigurer::disable)
```

### Phase 5 — richer `/api/health` payload (only if monitoring requires)
**Why:** `Map.of("status", "UP")` is enough for `docker compose healthcheck`, but Prometheus / external probes typically expect `version`, `timestamp`, `db` status, etc.

**How to apply:** If Phase 5 monitoring needs more, extend the `health()` response. Don't preempt — only add fields actually consumed.

## From Task 7 review (JpaConfig + Testcontainers — commit `e44f5c6`)

### Phase 2 — verify Flyway behavior with empty migration dir
**Why:** `application-test.yml` has `spring.flyway.enabled: true` but `db/migration/` is empty until Task 9. Current Spring Boot 3.2.5 / Flyway tolerates this (logs "no migrations found"), but it's not contractual. Once V1 lands in Task 9, this becomes a non-issue automatically.

**How to apply:** During Task 9 implementation, confirm Flyway-enabled context boot before AND after V1 is added — both should be green. No standalone fix needed if Task 9 verifies both states.

### Phase 5 — Testcontainers cross-run reuse
**Why:** Singleton container pattern (commit `6fb4d6e`) shares one container across test classes within a JVM, but each fresh `./gradlew test` still pays the ~25s container start. With `.withReuse(true)`, a Ryuk-managed container persists across runs locally and (with cache mounts) on CI. Becomes meaningful once `forkEvery` or `maxParallelForks` is added, OR when CI cold-start is the bottleneck.

**How to apply:** Add `.withReuse(true)` to the static container, document `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` for local dev. Evaluate when CI is set up.

### Optional polish — drop or update `MySQL8Dialect`
**Why:** `application-test.yml` (and `application.yml`) sets `hibernate.dialect: org.hibernate.dialect.MySQL8Dialect`. Hibernate 6.4 deprecates this in favor of `MySQLDialect` with auto-version detection. Currently emits a startup deprecation warning.

**How to apply:** Either drop the dialect property entirely (Hibernate detects MySQL 8 from JDBC connection) or rename to `MySQLDialect`. Trivial; defer until a quiet maintenance pass.

### Optional — `MYSQL` field visibility in `AbstractMysqlContainerTest`
**Why:** Currently package-private `static final`. No subclass reads it. `private static final` would be more conventional. JUnit `@Container` works regardless of visibility.

**How to apply:** One-character change (`static` → `private static`) when convenient.

## From Task 8 review (Backend Dockerfile — commit `ffb66ed`)

### Phase 5 — Spring Boot layered jar for image rebuild caching
**Why:** Final image is 476 MB. `jlink` could shave 100-150 MB but adds module-analysis build complexity. Spring Boot's layered jar (`bootJar { layered { enabled = true } }` + extracting layers in Dockerfile) is a single-line change with much better rebuild caching. Try this first, before reaching for `jlink`.

**How to apply:** Phase 5 ops pass — switch `backend/build.gradle` and adjust Dockerfile to extract layers separately.

### Phase 5 — JVM flags on ENTRYPOINT
**Why:** Phase 1 ENTRYPOINT is bare `["java", "-jar", "/app/app.jar"]`. OCI Free Tier has tight memory limits; `-XX:MaxRAMPercentage=75.0` is critical there.

**How to apply:** Phase 5 — add `-XX:MaxRAMPercentage=75.0`, `-XX:+ExitOnOutOfMemoryError`. Pass `SPRING_PROFILES_ACTIVE=docker` via env var (already done in `docker-compose.yml`), NOT baked into the image.

### Optional polish — make `gradlew` executable in git index
**Why:** Currently the Dockerfile runs `chmod +x gradlew` because Windows hosts don't preserve the +x bit. The canonical fix stores the bit in git's index regardless of host filesystem.

**How to apply:** `git update-index --chmod=+x gradlew && git commit -m "build: mark gradlew executable in git index"`. Then drop the `chmod +x` from the Dockerfile.

### Optional polish — pin bootJar output filename
**Why:** Dockerfile's `COPY --from=builder /workspace/build/libs/*.jar app.jar` uses a glob. Today Spring Boot 3.2 disables the `*-plain.jar` by default so only one jar exists, but if a future config enables it, the COPY fails confusingly.

**How to apply:** Add `bootJar { archiveFileName = 'app.jar' }` to `backend/build.gradle`. Then tighten Dockerfile to `COPY --from=builder /workspace/build/libs/app.jar app.jar`.

### Optional polish — Dockerfile inline comment
**Why:** `RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true` swallows real failures with `|| true`. Without context, a future maintainer might "fix" this by removing `|| true` and break cache priming.

**How to apply:** Add comment above that line: `# tolerate transient dependency-resolution warnings during cache prime; real failures resurface in bootJar`.

### Optional polish — extend `.dockerignore`
**Why:** Cheap insurance — adding `.git/` and `*.log` costs nothing and prevents footguns if someone runs `docker build .` from the repo root in the future.

**How to apply:** Append `.git/` and `*.log` to `backend/.dockerignore` when convenient.

## From Task 9 review (Flyway V1 schema — commit `49f7c39`)

### Phase 2 — decide user-deletion policy
**Why:** `fk_songs_registered_by` is now `ON DELETE RESTRICT` (explicit). Means deleting a user FAILS if any of their songs exist. Fine until v1 needs a "delete account" flow.

**How to apply:** When Phase 2 adds account deletion, decide: cascade-delete user's songs, transfer ownership to a "deleted user" placeholder account, or soft-delete the user. Update FK + add Vn migration if cascade is the answer.

### Optional polish — V1 SQL header comment
**Why:** Future contributors might be tempted to edit V1 to "fix" something, but Flyway tracks checksums; modifying applied migrations corrupts schema history.

**How to apply:** Add at top of `V1__init_schema.sql`: `-- DO NOT EDIT after first apply. For schema changes, create a new V<n>__ migration.`

### Optional polish — `ix_playlist_songs_order` naming
**Why:** Other indexes follow `ix_<table>_<col>` pattern. This one is `ix_playlist_songs_order` instead of `ix_playlist_songs_playlist_id_order_index`. Cosmetic.

**How to apply:** If renaming during a future maintenance pass, do it as Vn migration with `ALTER TABLE` rather than editing V1.

### Optional — additional schema integrity test (low cost)
**Why:** `FlywayMigrationTest` checks table existence + 2 column properties but doesn't exercise FK actions, unique constraints, or indexes. Phase 2's `ddl-auto: validate` covers column types but NOT FK/unique behavior.

**How to apply:** Add one integration test that inserts a user + song, then attempts to delete the user — assert it fails with constraint violation. Costs ~10 lines, locks in the I-1 decision.

## From Task 11 review (Frontend Vite + Vue 3 skeleton — commit `630eff7`)

### Task 12 — handle dangling `favicon.ico` reference
**Why:** `index.html` declares `<link rel="icon" href="/favicon.ico" />` but no file exists. Each dev page load logs a 404.

**How to apply:** Either drop the `<link>` line (browsers fall back) or add a 1 KB placeholder under `frontend/public/favicon.ico`. Cleanest as part of Task 12.

### Task 12 — add `frontend/.env.example`
**Why:** Once `api/index.js` reads `import.meta.env.VITE_API_BASE_URL` (Task 12), a committed `.env.example` documents expected envs.

**How to apply:** Create `frontend/.env.example` with `VITE_API_BASE_URL=http://localhost:8080/api` and add `.env` to `.gitignore` patterns if not already covered.

### Phase 6 — split `global.css` into `tokens.css` + `reset.css`
**Why:** Current `global.css` mixes CSS reset and design tokens (only `--bg`, `--text`). Phase 6 UI design will need a full token system (palette, spacing scale, typography, radii, shadows, breakpoints). The `global.css` filename will outgrow its content.

**How to apply:** Phase 6 design pass — split into `assets/styles/reset.css` and `assets/styles/tokens.css`, import both from `main.js`. Add full token system.

### Phase 2-5 — add Prettier (no ESLint/TS yet)
**Why:** Across Phase 2-5 frontend work, multiple `.vue` files will accumulate without formatter enforcement. Task 12 alone adds ~10 view stubs + router/store/api. Natural inflection point to add Prettier (zero-config, ~30 sec setup) before code volume makes later cleanup painful.

**How to apply:** `npm i -D prettier` + `.prettierrc` (default config). Optional `npm run format` script. Defer ESLint/TypeScript until Phase 6 unless a pain point appears.

### Phase 2+ — i18n via reactive `lang` attribute
**Why:** `<html lang="ko">` is hard-coded. Spec is Korean-first for v1; if i18n is added later, `document.documentElement.lang` should update reactively from a locale store.

**How to apply:** Defer until i18n is on the roadmap.

## From Task 12 review (Frontend router + Pinia + axios + view stubs — commit `10aad8e`)

### Phase 2 — bootstrap access token from refresh during app init
**Why:** `isAuthenticated` getter checks only `state.accessToken`. On app cold-start, a returning user has `refreshToken` in localStorage but no `accessToken`, so `requiresAuth` pages redirect to `/login` even though refresh would succeed.

**How to apply:** Either (a) widen the getter to `!!state.accessToken || !!state.refreshToken` and let the request interceptor refresh on demand, or (b) call `authStore.refresh()` in `main.js` before `app.mount()` so the access token is populated before the first navigation. Option (b) is cleaner if startup latency is acceptable.

### Phase 6 (or earlier if dev links go stale) — add 404 catch-all route
**Why:** `/random/path` produces an empty `<RouterView />` and a Vue Router warning. Even a 3-line `NotFoundView.vue` would close it.

**How to apply:** Add `{ path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }` at the end of the routes array. Phase 6 polish or earlier if friction surfaces.

### Phase 5 deployment — server fallback for SPA history mode
**Why:** Router uses `createWebHistory()`, which requires the server to serve `index.html` for non-API paths. Vite dev handles this; production OCI deploy needs nginx `try_files` or equivalent.

**How to apply:** When the OCI deployment plan is written, include the SPA fallback config (nginx `try_files $uri $uri/ /index.html;` or equivalent on the chosen server).

### Phase 6 polish — per-route `<title>` updates
**Why:** Browser tab always says "VocaloidArchive" regardless of route. Standard polish for SPA.

**How to apply:** Either lightweight (`document.title = ...` in route `meta.title`-aware `afterEach`) or library (`@vueuse/head`/`unhead`). Phase 6 design pass.

### Phase 6 polish — cross-tab logout sync
**Why:** `localStorage.getItem('refreshToken')` runs once at store init. If tab A signs out, tab B keeps the stale token until manual refresh.

**How to apply:** Add a `storage` event listener in `auth.js` to clear state when localStorage changes externally. Phase 6 polish unless flake surfaces earlier.

## From Task 13 review (Frontend Dockerfile — commit `1e0ee5e`)

### Phase 5 deployment — frontend multi-stage prod build
**Why:** Phase 1 ships dev-mode container (Vite serving with hot reload, `node_modules/` baked in, ~280 MB). Production should be a multi-stage build: `vite build` → `nginx:alpine` serving `dist/`, ~30 MB.

**How to apply:** Phase 5 OCI deployment — split Dockerfile into builder stage (`npm ci && npm run build`) and runtime stage (`nginx:alpine` with custom config that includes SPA fallback `try_files $uri /index.html`).

### Phase 5 deployment — pin Docker base image digests
**Why:** Both backend (`eclipse-temurin:17-jdk`/`-jre`) and frontend (`node:20-alpine`) use mutable tags. Reproducible builds require `@sha256:...` pins.

**How to apply:** Phase 5 OCI deployment — pin both Dockerfiles to specific digests captured at deployment time. Set up a routine to refresh digests quarterly.

### Phase 5 deployment — non-root USER in containers
**Why:** Both containers currently run as root. `node:20-alpine` ships with a `node` user; backend can use `useradd appuser` in the runtime stage. Cheap defense-in-depth.

**How to apply:** Add `USER node` to frontend Dockerfile (with `chown` on `/app` if needed) and a non-root user to backend runtime stage in Phase 5.

### Phase 5 deployment — HEALTHCHECK directives
**Why:** Backend has `/api/health`; frontend has Vite's `/__vite_ping`. Neither Dockerfile declares HEALTHCHECK. Compose orchestrates startup order, but plain `docker run` and reverse-proxy gates would benefit.

**How to apply:** Add `HEALTHCHECK CMD curl -f http://localhost:8080/api/health || exit 1` to backend Dockerfile, similar for frontend `/__vite_ping`. Phase 5 only if monitoring/proxy needs them.

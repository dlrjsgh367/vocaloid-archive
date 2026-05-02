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

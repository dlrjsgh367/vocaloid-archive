# Phase 2: User + JWT 설계 스펙

- 작성일: 2026-05-03
- 상태: 설계 확정 (구현 시작 전)
- 범위: User 도메인 (회원가입/로그인/refresh/logout) + JWT 발급 및 필터, 백로그 Phase 2 항목

---

## 1. 범위 결정

### 포함
- 백엔드: User/RefreshToken 엔티티, Repository, Service, AuthController
- 백엔드: JwtTokenProvider, JwtAuthenticationFilter, AuthenticationEntryPoint/AccessDeniedHandler, CustomUserDetails/Service, SecurityUtil
- 백엔드: SecurityConfig 고도화 (JWT 필터 연결, backlog 항목 처리)
- 백엔드: GlobalExceptionHandler 확장 (ResponseEntityExceptionHandler 상속, auth 에러 분리, validation details Map)
- 백엔드: 테스트 (Service BDD Mockito, Controller @WebMvcTest, Repository @DataJpaTest)

### 제외 (Phase 6)
- 프론트엔드 뷰 구현 (LoginView, SignUpView)
- 프론트엔드 auth store actions 구현
- axios 인터셉터 JWT 주입/갱신 구현

---

## 2. 신규 파일 구조

```
backend/src/main/java/com/vocaloidarchive/
├── common/
│   ├── config/
│   │   └── SecurityConfig.java              [수정]
│   ├── exception/
│   │   └── GlobalExceptionHandler.java      [수정]
│   └── security/
│       ├── JwtTokenProvider.java            [신규]
│       ├── JwtAuthenticationFilter.java     [신규]
│       ├── JwtAuthenticationEntryPoint.java [신규]
│       ├── JwtAccessDeniedHandler.java      [신규]
│       ├── CustomUserDetails.java           [신규]
│       ├── CustomUserDetailsService.java    [신규]
│       └── SecurityUtil.java               [신규]
└── user/
    ├── controller/AuthController.java       [신규]
    ├── service/
    │   ├── UserService.java                 [신규]
    │   └── RefreshTokenService.java         [신규]
    ├── repository/
    │   ├── UserRepository.java              [신규]
    │   └── RefreshTokenRepository.java      [신규]
    ├── domain/
    │   ├── User.java                        [신규]
    │   └── RefreshToken.java                [신규]
    └── dto/
        ├── request/
        │   ├── SignUpRequest.java            [신규]
        │   ├── LoginRequest.java             [신규]
        │   └── RefreshRequest.java           [신규]
        └── response/
            ├── UserResponse.java             [신규]
            └── TokenResponse.java            [신규]

backend/src/test/java/com/vocaloidarchive/
├── common/exception/
│   └── GlobalExceptionHandlerTest.java      [수정]
└── user/
    ├── controller/AuthControllerTest.java   [신규]
    ├── service/UserServiceTest.java         [신규]
    ├── service/RefreshTokenServiceTest.java [신규]
    └── repository/UserRepositoryTest.java   [신규]
```

---

## 3. 도메인 모델

### User 엔티티

```java
@Entity @Table(name = "users")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
```

### RefreshToken 엔티티

```java
@Entity @Table(name = "refresh_tokens")
public class RefreshToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;   // SHA-256(raw token), raw token은 저장하지 않음

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
```

---

## 4. JWT 토큰 정책

| 항목 | 값 |
|---|---|
| 알고리즘 | HS256 |
| Access Token 유효기간 | 30분 |
| Refresh Token 유효기간 | 14일 |
| 시크릿 키 | `${JWT_SECRET}` 환경변수 (256bit 이상 랜덤) |
| Access Token claims | `sub` = userId (Long → String) |
| Refresh Token DB 저장 | SHA-256 해시만 저장 |
| Refresh 회전 정책 | 새 refresh 발급 시 기존 token DB에서 즉시 삭제 |
| 재사용 공격 처리 | 폐기된 token 재사용 → `INVALID_TOKEN` (Phase 2 단순 거부) |

### application.yml 추가

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry-minutes: 30
    refresh-token-expiry-days: 14
```

---

## 5. API 명세

### 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/api/auth/signup` | X | `SignUpRequest` | `ApiResponse<UserResponse>` 201 |
| POST | `/api/auth/login` | X | `LoginRequest` | `ApiResponse<TokenResponse>` 200 |
| POST | `/api/auth/refresh` | X | `RefreshRequest` | `ApiResponse<TokenResponse>` 200 |
| POST | `/api/auth/logout` | O (Bearer) | `RefreshRequest` | `ApiResponse<Void>` 200 |

### DTO 상세

**SignUpRequest**
```java
@NotBlank String username;   // 3-20자, ^[a-zA-Z0-9_]+$
@NotBlank @Email String email;
@NotBlank String password;   // 8-72자, 영문+숫자 혼합 (정규식: (?=.*[A-Za-z])(?=.*\d).{8,72})
```

**LoginRequest**
```java
@NotBlank @Email String email;
@NotBlank String password;
```

**RefreshRequest**
```java
@NotBlank String refreshToken;
```

**UserResponse**
```java
Long id;
String username;
String email;
String profileImageUrl;
LocalDateTime createdAt;
```

**TokenResponse**
```java
String accessToken;
String refreshToken;
```

---

## 6. 핵심 흐름

### 회원가입

1. `SignUpRequest` Bean Validation
2. `userRepository.existsByUsername(username)` → true → `DUPLICATE_USERNAME`
3. `userRepository.existsByEmail(email)` → true → `DUPLICATE_EMAIL`
4. `passwordEncoder.encode(password)` → `User` 저장
5. `UserResponse` 반환 (HTTP 201)

### 로그인

1. `LoginRequest` Bean Validation
2. `userDetailsService.loadUserByUsername(email)` → 없으면 `INVALID_CREDENTIALS`
   (Spring의 `loadUserByUsername(String username)` 파라미터명과 다르게, 이 앱은 email로 조회)
3. `passwordEncoder.matches(raw, hash)` → false → `INVALID_CREDENTIALS`
4. `jwtTokenProvider.generateAccessToken(userId)` (30분)
5. `refreshTokenService.issue(user)` → raw UUID 생성 → SHA-256 해시 → DB 저장 → raw 반환
6. `TokenResponse{accessToken, refreshToken(raw)}` 반환

### 토큰 갱신

1. `RefreshRequest` Bean Validation
2. `SHA-256(refreshToken)` → `refreshTokenRepository.findByTokenHashAndExpiresAtAfter(hash, now)`
3. 없으면 `INVALID_TOKEN`
4. 새 AccessToken + 새 raw RefreshToken 생성
5. 기존 RefreshToken DB 삭제 → 새 RefreshToken DB 저장
6. `TokenResponse` 반환

### 로그아웃

1. `SecurityContextHolder`에서 인증 확인 (필터가 보장)
2. `SHA-256(refreshToken)` → `refreshTokenRepository.deleteByTokenHash(hash)`
3. 없어도 성공 (멱등)
4. `ApiResponse<Void>` 반환

---

## 7. JWT 에러 처리 (Option A)

### JwtAuthenticationFilter 흐름

```
Authorization: Bearer <token> 헤더 없음
    → SecurityContext 비움, 체인 통과
    → public 엔드포인트: 정상 처리
    → auth 필요 엔드포인트: Spring → JwtAuthenticationEntryPoint(401, INVALID_TOKEN)

Bearer 있음, 유효
    → userId 추출 → UsernamePasswordAuthenticationToken 생성
    → SecurityContextHolder 저장 → 체인 통과

Bearer 있음, 만료 (ExpiredJwtException)
    → request.setAttribute("jwtError", ErrorCode.EXPIRED_TOKEN)
    → SecurityContext 비움 → 체인 통과
    → Spring → JwtAuthenticationEntryPoint (attribute에서 에러코드 읽어 응답)

Bearer 있음, 무효 (JwtException 등)
    → request.setAttribute("jwtError", ErrorCode.INVALID_TOKEN)
    → 동일
```

### JwtAuthenticationEntryPoint

```java
// request.getAttribute("jwtError")가 있으면 해당 ErrorCode로,
// 없으면 INVALID_TOKEN으로 ApiResponse.error() 직렬화 후 응답
```

### JwtAccessDeniedHandler

```java
// 인증은 됐으나 권한 없음 → FORBIDDEN으로 ApiResponse.error() 응답
```

### SecurityConfig 변경 사항

```java
// 1. AbstractHttpConfigurer::disable 방식
.csrf(AbstractHttpConfigurer::disable)
.formLogin(AbstractHttpConfigurer::disable)
.httpBasic(AbstractHttpConfigurer::disable)

// 2. requestMatchers 리소스별 분리
.requestMatchers("/api/health").permitAll()
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/songs/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/characters").permitAll()
.requestMatchers(HttpMethod.GET, "/api/songs/*/comments").permitAll()
.requestMatchers(HttpMethod.GET, "/api/playlists/*").permitAll()
.anyRequest().authenticated()

// 3. 예외 처리 및 필터 등록
.exceptionHandling(eh -> eh
    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
    .accessDeniedHandler(jwtAccessDeniedHandler))
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

---

## 8. GlobalExceptionHandler 변경 사항

### ResponseEntityExceptionHandler 상속

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  // Spring 내부 예외(HttpMessageNotReadableException 등) 일괄 래핑
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ApiResponse<?> response = ApiResponse.error(
        ErrorCode.VALIDATION_FAILED.getCode(),
        ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다"
    );
    return new ResponseEntity<>(response, headers, status);
  }
}
```

### 인증 예외 분리

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
  // → INVALID_CREDENTIALS (401)
}

@ExceptionHandler(ExpiredJwtException.class)
public ResponseEntity<ApiResponse<?>> handleExpiredJwt(ExpiredJwtException ex) {
  // → EXPIRED_TOKEN (401)
}

// 기존 AuthenticationException 핸들러는 INVALID_TOKEN fallback으로 유지
```

### Validation details Map

```java
// ApiResponse.ErrorBody에 추가
@JsonInclude(JsonInclude.Include.NON_NULL)
private Map<String, String> details;

// handleValidation에서
Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
    .collect(Collectors.toMap(
        FieldError::getField,
        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값",
        (a, b) -> a  // 동일 필드 첫 번째 메시지 유지
    ));
```

---

## 9. 테스트 전략

### Service 단위 테스트 (BDD Mockito)

**UserServiceTest**
- `given` 정상 입력 → `when` signup → `then` User 저장, UserResponse 반환
- `given` 중복 username → `then` `DUPLICATE_USERNAME` BusinessException
- `given` 중복 email → `then` `DUPLICATE_EMAIL` BusinessException

**RefreshTokenServiceTest**
- `given` 유효한 refresh token → `when` rotate → `then` 기존 삭제 + 신규 저장 + TokenResponse 반환
- `given` DB에 없는 hash → `then` `INVALID_TOKEN` BusinessException
- `given` 만료된 token → `then` `INVALID_TOKEN` BusinessException

### Controller 슬라이스 테스트 (`@WebMvcTest`)

**AuthControllerTest** (실제 Security 필터 포함)
- `POST /api/auth/signup` 정상 → 201
- `POST /api/auth/signup` Bean Validation 실패 → 400 + details Map 구조 검증
- `POST /api/auth/login` 정상 → 200 + TokenResponse
- `POST /api/auth/login` 잘못된 credentials → 401 + `INVALID_CREDENTIALS`
- `POST /api/auth/refresh` 정상 → 200
- `POST /api/auth/logout` 인증 없음 → 401 (EntryPoint 경로)
- `POST /api/auth/logout` 유효한 Bearer 토큰 → 200

### Repository 통합 테스트 (`@DataJpaTest` + Testcontainers)

**UserRepositoryTest**
- `findByEmail`, `existsByUsername`, `existsByEmail` 쿼리 검증

**RefreshTokenRepositoryTest**
- `findByTokenHashAndExpiresAtAfter`: 만료 전 → 결과 있음, 만료 후 → 결과 없음
- `deleteByTokenHash`: 삭제 확인

### GlobalExceptionHandlerTest (기존 파일 수정)
- `HttpMessageNotReadableException` → 400 + ApiResponse (ResponseEntityExceptionHandler 경로)
- `BadCredentialsException` → 401 + `INVALID_CREDENTIALS`
- Validation 에러 → details 필드 `Map<String, String>` 구조 확인

---

## 10. 백로그 처리 현황

| 항목 | 출처 | 처리 |
|---|---|---|
| GlobalExceptionHandler: framework 예외 400 처리 | Task 5 review | ResponseEntityExceptionHandler 상속으로 해결 |
| AuthenticationException 분리 (BadCredentials/ExpiredJwt) | Task 5 review | 명시적 @ExceptionHandler 추가 |
| validation details shape → Map | Task 5 review | ApiResponse.ErrorBody.details 필드 추가 |
| SecurityConfig: CORS 출처 명시 | Task 6 review | WebConfig 위임을 SecurityConfig에 Javadoc으로 명시 |
| SecurityConfig: requestMatchers 분리 | Task 6 review | 리소스별 1줄씩 분리 |
| SecurityConfig: AbstractHttpConfigurer::disable | Task 6 review | 방식 전환 |
| AccessDeniedException/AuthenticationException 테스트 | Task 5 review | AuthControllerTest에서 실제 필터 통과 시나리오로 커버 |
| handleAccessDenied/handleAuthentication 로그 추가 | Task 5 review | log.warn 추가 (선택 polish) |

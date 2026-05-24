# Testcontainers 가 최신 Docker 엔진과 핸드셰이크 실패

- 날짜: 2026-05-24
- 증상: `@DataJpaTest`(Testcontainers MySQL) 실행 시 컨테이너 기동 전 `DockerClientProviderStrategy` 에서 실패.

## 에러

```
Could not find a valid Docker environment.
  UnixSocketClientProviderStrategy: failed with exception BadRequestException
  (Status 400: {"message":"client version 1.32 is too old.
   Minimum supported API version is 1.40, please upgrade your client to a newer version"})
```

## 원인

이 서버의 Docker 엔진은 29.x(최소 지원 API 1.40)인데, 프로젝트의 Testcontainers `1.19.7` 이 끌어오는 docker-java 가 API `1.32` 로 핸드셰이크를 시도 → 데몬이 거부.

## 해결

docker-java 가 사용할 API 버전을 고정한다. **env(`DOCKER_API_VERSION`)만으로는 테스트 워커 JVM에 안 먹혔고, 시스템 프로퍼티 `api.version` 을 함께 줘야 동작**했다. `backend/build.gradle` 의 test 태스크에 추가:

```groovy
tasks.named('test') {
  useJUnitPlatform()
  def dockerApi = System.getenv('DOCKER_API_VERSION') ?: '1.43'
  environment 'DOCKER_API_VERSION', dockerApi
  systemProperty 'api.version', dockerApi   // ← 이게 실제로 먹은 키
}
```

## 부수 주의

- 이 머신에서 gradle 테스트는 샌드박스에선 docker 소켓 접근이 막힐 수 있어 `--no-daemon` + 소켓 접근 가능한 환경에서 실행해야 한다. gradle 데몬이 옛 환경을 캐시하면 `./gradlew --stop` 후 재실행.
- `ddl-auto: validate` 이므로 Flyway 컬럼 타입과 JPA 매핑이 정확히 일치해야 한다. 예: `@Column(length=10)` String 은 `VARCHAR(10)` 을 기대 → 마이그레이션에서 `CHAR(10)` 쓰면 "wrong column type ... found [char], expecting [varchar(10)]" 로 컨텍스트 로드 실패.

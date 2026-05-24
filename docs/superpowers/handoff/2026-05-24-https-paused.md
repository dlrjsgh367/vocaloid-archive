# Handoff — HTTPS 적용 (보류 / 재개용)

- 날짜: 2026-05-24
- 상태: **⏸ 보류** — 인증서 발급 단계에서 막힘. 사이트는 **HTTP(80)에서 정상 운영 중**.
- 대상: `vocaloid-archive.kro.kr` → `138.2.127.124` (OCI 프로덕션, self-hosted runner)
- 관련: 플랜 `docs/superpowers/plans/2026-05-24-https-nginx-certbot.md`, 메모리 `project-https-blocked`

## 지금까지 된 것 (Stage 1, 배포 완료·무해)

main에 배포됨 (`docker compose -f docker-compose.prod.yml up -d --build`):
- `frontend/nginx.conf` 80 서버에 ACME 챌린지 location:
  ```nginx
  location ^~ /.well-known/acme-challenge/ {
      root /var/www/certbot;
      default_type "text/plain";
      try_files $uri =404;
  }
  ```
- `docker-compose.prod.yml`: `certbot` 서비스(renew 루프) + 볼륨 `certbot-etc`(/etc/letsencrypt), `certbot-www`(/var/www/certbot). `web`에 두 볼륨 ro 마운트.
- 컴포즈 프로젝트명 = `vocaloid-archive` → 볼륨 `vocaloid-archive_certbot-etc`, `vocaloid-archive_certbot-www`. (런너 워크스페이스와 로컬 `/home/ubuntu/vocaloid-archive` 둘 다 basename 동일 → `docker compose run`이 같은 볼륨 공유)
- 검증: `http://vocaloid-archive.kro.kr/` 200, health 200, `vocaloid-certbot` 컨테이너 Up, `GET /.well-known/acme-challenge/x` → 404(빈 상태, location은 동작).

## 막힌 지점 (Stage 2 = 인증서 발급)

- **Let's Encrypt**: `kro.kr`이 무료 공용 도메인 → LE가 `kro.kr` 전체를 등록도메인으로 보고 **주 50건 한도** 적용 → 타 사용자가 소진 → `too many certificates (50) already issued for "kro.kr"`. 만성적, 재시도해도 불확실.
- **Buypass** 무료 ACME: `api.buypass.com/acme/directory` 등 전부 **404**(서비스 종료/이전 추정).
- EAB 없이 바로 되는 무료 CA는 LE뿐 → 막힘.

> 원래 HTTPS를 꺼낸 트리거(공유 "링크 복사" 실패)는 **execCommand 폴백으로 이미 해결·배포됨**(`c35b82f`). HTTPS는 급한 게 아니라 품질 개선 차원.

## 재개 방법

### 옵션 A — ZeroSSL (가장 현실적, 무료)
1. zerossl.com 무료 가입 → Developer → **EAB Credentials** → Generate → `KID`, `HMAC Key` 확보.
2. 인증서 발급(포트 80, webroot):
   ```bash
   cd /home/ubuntu/vocaloid-archive
   docker compose -f docker-compose.prod.yml run --rm --entrypoint certbot certbot \
     certonly --webroot -w /var/www/certbot -d vocaloid-archive.kro.kr \
     --server https://acme.zerossl.com/v2/DV90 \
     --eab-kid <KID> --eab-hmac-key <HMAC> \
     --email dlrjsgh367@gmail.com --agree-tos --no-eff-email --non-interactive
   ```
   성공 시 `/etc/letsencrypt/live/vocaloid-archive.kro.kr/{fullchain,privkey}.pem` 생성(`certbot renew`가 CA를 renewal conf에 기억하므로 자동 갱신도 ZeroSSL로 됨).

### 옵션 B — kro.kr 탈피 (근본 해결)
- 본인 통제 도메인으로 이전 또는 Cloudflare 무료 플랜(엣지 HTTPS). LE 공용 한도 문제 자체가 사라짐. DNS 변경 필요(kro.kr 무료 DDNS는 A레코드만 가능할 수 있어 Cloudflare 프록시는 제약될 수 있음 — 확인 필요).

### 옵션 C — LE 재시도
- 한도 리셋(에러 메시지의 `retry after` 시각) 후 동일 webroot 명령(`--server` 생략 = LE). 경쟁으로 실패 가능, 여러 번 시도 각오.

## 인증서 발급 후 남은 단계 (Stage 3, 복붙용)

### Stage 3a — 443 TLS 블록 (외부 443 필요)
**전제: OCI VCN Security List 인바운드 TCP 443 오픈** (호스트 iptables엔 이미 443 ACCEPT 있음). OCI 콘솔에서만 가능.

- `frontend/Dockerfile.prod`: `EXPOSE 80` 아래 `EXPOSE 443` 추가.
- `docker-compose.prod.yml` `web.ports`에 `- "443:443"` 추가. (`web`의 `certbot-etc` 마운트는 이미 있음)
- `frontend/nginx.conf`에 443 서버 블록 추가(기존 80의 location 전부 복제 — acme/assets/p/share//api). **80은 그대로 유지(아직 리다이렉트 X)**:
  ```nginx
  server {
      listen 443 ssl;
      http2 on;
      server_name vocaloid-archive.kro.kr;
      ssl_certificate     /etc/letsencrypt/live/vocaloid-archive.kro.kr/fullchain.pem;
      ssl_certificate_key /etc/letsencrypt/live/vocaloid-archive.kro.kr/privkey.pem;
      # ... (80 서버의 location 블록들을 그대로 복사) ...
  }
  ```
  ⚠️ 인증서 파일이 존재해야만 nginx가 뜸. 발급 성공 확인 후에만 이 블록 배포(아니면 web 컨테이너 기동 실패).
- 갱신분 자동 반영: `web` command에 주기적 reload 루프 추가(예: `while :; do sleep 6h & wait $${!}; nginx -s reload; done & nginx -g 'daemon off;'`) 또는 다음 배포 때 자동 반영.
- 배포 후 외부에서 `https://vocaloid-archive.kro.kr/` 200 확인. 막히면 OCI 443 오픈.

### Stage 3b — 강제 HTTPS (443 외부 확인 후에만)
- 80 서버: acme location은 두고 나머지는 `return 301 https://$host$request_uri;`.
- `APP_CORS_ORIGINS`(배포 secret/.env)를 `https://vocaloid-archive.kro.kr`로 변경(또는 http 병기).
- OG 절대 URL은 nginx가 `X-Forwarded-Proto $scheme` 전달 → https로 채워지는지 확인(필요 시 Spring `ForwardedHeaderFilter`/`server.forward-headers-strategy=framework`).

## 검증 / 롤백
- 검증: `curl -I https://vocaloid-archive.kro.kr/` 200 + 인증서 유효, `curl -A Twitterbot https://.../p/{code}`로 OG, 카드 PNG.
- 롤백: Stage 3 커밋 revert 후 재배포. Stage 3b(리다이렉트) 전까지 80이 항상 살아있어 안전.

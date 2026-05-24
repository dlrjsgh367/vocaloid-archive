# HTTPS 적용 플랜 (nginx + certbot, 무중단 단계별)

- 작성일: 2026-05-24
- 대상: `vocaloid-archive.kro.kr` (→ 138.2.127.124, OCI 프로덕션)
- 목표: 사이트를 HTTPS로. 클립보드 API/보안/OG 미리보기 제약 해소.

## ⏸ 상태: 보류 (2026-05-24)

Stage 1(ACME 챌린지 location + certbot 서비스/볼륨)은 **배포 완료**, 사이트는 80에서 정상. **인증서 발급(Stage 2)에서 막힘:**
- **Let's Encrypt**: `kro.kr`이 무료 공용 도메인이라 LE가 `kro.kr` 전체를 등록 도메인으로 보고 주 50개 한도 적용 → 타 사용자들이 소진 → 발급 불가(만성적).
- **Buypass** 무료 ACME: 엔드포인트 404(서비스 종료/변경 추정).
- EAB 없이 바로 되는 무료 CA는 LE뿐인데 그게 막힘.

**재개 옵션:** ① ZeroSSL(무료, EAB 키 발급 후 `--server https://acme.zerossl.com/v2/DV90 --eab-kid ... --eab-hmac-key ...`) ② kro.kr이 아닌 본인 통제 도메인으로 이전(또는 Cloudflare 엣지 HTTPS) → LE 한도/공용 문제 근본 해소. ③ LE 재시도(불확실).
참고: 원 트리거였던 "링크 복사"는 execCommand 폴백으로 이미 해결됨(커밋 `c35b82f`), HTTPS 없이도 동작.

## 전제 / 정찰 결과
- DNS `vocaloid-archive.kro.kr → 138.2.127.124` 일치 ✅
- 호스트 iptables 443 ACCEPT 존재, ufw 비활성 ✅
- 현재 `vocaloid-web`(nginx 컨테이너)가 80만 listen. nginx.conf는 이미지에 COPY됨.
- **⚠️ 외부 의존: OCI VCN 보안 목록(security list) 인바운드 TCP 443** — 호스트에서 확인/변경 불가. **유저가 OCI 콘솔에서 열어야 함.**
- Let's Encrypt 등록 이메일 필요 (제안: `dlrjsgh367@gmail.com`).

## 핵심 위험과 회피
nginx는 존재하지 않는 인증서 파일을 참조하는 443 블록이 있으면 **기동 실패 → 전체 다운**. 그래서 인증서 발급 전에는 443 블록을 절대 배포하지 않는다. 또한 443이 외부에서 확인되기 전에는 80→443 리다이렉트를 켜지 않는다(켜면 외부 차단 시 전면 다운).

## 단계 (각 단계 = git 커밋 → 배포)

### Stage 1 — ACME 챌린지 준비 (무중단, 443 불필요)
- `nginx.conf` 80 서버에 `location /.well-known/acme-challenge/ { root /var/www/certbot; }` 추가 (그 외 80 동작 그대로, 리다이렉트 없음).
- `docker-compose.prod.yml`: `certbot` 서비스 + 볼륨 `certbot-etc`(/etc/letsencrypt), `certbot-www`(/var/www/certbot). `web`에 두 볼륨 마운트(letsencrypt ro, www ro) + 인증서 자동 반영용 nginx 주기적 reload.
- 배포 후 사이트는 80에서 그대로 동작.

### Stage 2 — 인증서 발급 (무중단, 80만 사용)
- 1회 부트스트랩:
  `docker compose -f docker-compose.prod.yml run --rm certbot certonly --webroot -w /var/www/certbot -d vocaloid-archive.kro.kr --email <email> --agree-tos --no-eff-email`
- 성공 시 `certbot-etc` 볼륨에 `live/vocaloid-archive.kro.kr/fullchain.pem`,`privkey.pem` 생성.

### Stage 3a — 443 TLS 블록 활성 (외부 443 필요)
- `Dockerfile.prod`에 `EXPOSE 443`, compose `web.ports`에 `"443:443"`.
- `nginx.conf`에 443 server 블록 추가 (fullchain/privkey 사용, 기존 location 전부 복제). **80은 사이트 계속 서빙(리다이렉트 아직 X)**.
- 배포 후 https 외부 접속 테스트. 막히면 → 유저가 OCI 443 오픈. 이 동안 80은 정상.

### Stage 3b — 강제 HTTPS 전환 (443 확인 후에만)
- 80 서버: acme 경로 제외 전부 `return 301 https://$host$request_uri`.
- `APP_CORS_ORIGINS`를 `https://vocaloid-archive.kro.kr`로 변경(또는 http와 병기).
- 필요 시 OG 절대 URL은 X-Forwarded-Proto 기반(이미 nginx가 헤더 전달) — https로 채워지는지 확인.

## 자동 갱신
- `certbot` 컨테이너가 12h마다 `certbot renew`(webroot) → 볼륨 갱신.
- `web` nginx가 주기적 `nginx -s reload`로 갱신분 반영(또는 다음 배포 시 자동 반영, 배포가 잦음).

## 롤백
- 문제 시 해당 stage 커밋 revert 후 재배포. Stage 3b(리다이렉트) 전까지는 80이 항상 살아있어 안전.

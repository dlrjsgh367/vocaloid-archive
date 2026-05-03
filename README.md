# VocaloidArchive

보컬로이드 곡 큐레이션 플랫폼. 사용자가 곡을 등록하고, 캐릭터/태그/분위기로 탐색하고,
플레이리스트와 좋아요/댓글로 큐레이션하는 웹 서비스.

## Stack

- Backend: Spring Boot 3, Java 17, JPA, QueryDSL, MySQL 8.0
- Frontend: Vue 3 (Vite), Pinia, Vue Router, Axios
- Infra: Docker Compose

## Quick Start (Docker)

```bash
cp .env.example .env   # fill in secrets
docker compose up --build
```

Then open http://localhost:5173 (frontend) and http://localhost:8080/api/health (backend).

## Local Dev (without Docker)

- Backend: `cd backend && ./gradlew bootRun`
- Frontend: `cd frontend && npm install && npm run dev`
- DB: `docker compose up db` (just MySQL)

## Docs

- Spec: `docs/superpowers/specs/2026-05-03-vocaloid-archive-design.md`
- Plans: `docs/superpowers/plans/`

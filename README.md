# SmartATS

Application bilingue (FR/EN) de matching CV / offre d'emploi assistée par IA :
score ATS explicable, feedback actionnable, suivi de candidatures.

## Architecture (microservices polyglotte)
- `frontend/`   — Next.js 16 (React 19, TypeScript)
- `backend/`    — Spring Boot 4.1 (Java 25) — cœur métier
- `ml-service/` — FastAPI (Python 3.13) — NLP / embeddings / ML / RAG
- `infra/`      — Docker Compose, puis Kubernetes, observabilité
- `docs/`       — PRD + diagrammes UML (diagrams-as-code)

## Prérequis
Docker + Compose v2, Java 25, Node 24, Python 3.13.

## Démarrage (à venir)
`docker compose up` lancera frontend + backend + ml-service + PostgreSQL.

## Statut
Phase 0 — Fondations (en cours).

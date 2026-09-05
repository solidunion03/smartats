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

## Démarrage local (rituel de session)

Lancer les 3 services **dans cet ordre** (chacun dépend du précédent).

### 0. Prérequis à chaque session
- Lancer **Docker Desktop** (Windows) et attendre « Engine running ».

### 1. Base de données (PostgreSQL via Docker)
```bash
cd ~/projects/smartats/infra
docker compose up -d
docker compose ps          # attendre que smartats-db soit "Up"
```

### 2. Service ML (Python / FastAPI)
```bash
cd ~/projects/smartats/ml-service
source .venv/bin/activate  # le prompt doit afficher (.venv)
uvicorn main:app           # sans --reload (modèle lourd) ; écoute sur :8000
```

### 3. Backend (Java / Spring Boot)
```bash
cd ~/projects/smartats/backend
./mvnw spring-boot:run     # écoute sur :8080 ; attendre "Started BackendApplication"
```

### Vérifier que tout tourne
```bash
curl http://localhost:8080/ping        # backend  -> pong
curl http://localhost:8000/ping        # ML       -> pong
docker compose -f infra/docker-compose.yml ps   # base -> Up
```

### Arrêter
```bash
# backend / uvicorn : Ctrl+C dans leur terminal (ou : pkill -f spring-boot:run)
cd ~/projects/smartats/infra
docker compose down        # arrête la base (garde les données)
# docker compose down -v   # ATTENTION : -v efface aussi les données
```

### Dépannage rapide
- Backend : `Connection refused ... 5432` → la base n'est pas lancée. Refaire l'étape 1.
- `docker: command not found` / `cannot connect` → Docker Desktop n'est pas démarré.
- `uvicorn: command not found` → l'environnement Python n'est pas activé (`source .venv/bin/activate`).
- Voir pourquoi un conteneur meurt : `docker compose logs db`.

### Commandes utiles (base de données)
```bash
# ouvrir un accès SQL au conteneur
docker exec -it smartats-db psql -U smartats -d smartats
# lister les tables
docker exec -it smartats-db psql -U smartats -d smartats -c "\dt"
```

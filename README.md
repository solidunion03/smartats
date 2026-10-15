# SmartATS

Application **bilingue (FR/EN)** de matching **CV ↔ offre d'emploi** assistée par IA :
score de compatibilité **explicable** (règles + sémantique), authentification par rôles,
architecture **microservices polyglotte**.

Projet à vocation d'apprentissage couvrant trois piliers : **backend métier (Java/Spring)**,
**service ML (Python/FastAPI)** et **DevOps (Docker, migrations, CI à venir)**.

---

## Architecture (microservices polyglotte)

| Service | Techno | Rôle | Port |
|---|---|---|---|
| `backend/` | Spring Boot 4.1 (Java 25) | Cœur métier : utilisateurs, CV, offres, scans, orchestration, sécurité | 8080 |
| `ml-service/` | FastAPI (Python 3.13) | Scoring : mots-clés + embeddings sémantiques multilingues | 8000 |
| `frontend/` | Next.js 16 (React, TS) | Interface utilisateur *(à venir)* | 3000 |
| `infra/` | Docker Compose | PostgreSQL 18 (puis observabilité, Kubernetes) | 5432 |
| `docs/` | Markdown + PlantUML | PRD, diagrammes UML, cours du projet | — |

Le **backend** possède les données (fichiers, base) et orchestre. Le **service ML** ne
raisonne que sur du texte. Ils communiquent par **HTTP** : au moment d'un scan, le backend
extrait le texte du CV et de l'offre et les envoie au service Python, qui renvoie un score.

```
                 ┌───────────────┐        ┌────────────────────┐
   client ─────► │ Backend Spring │ ─HTTP─►│ Service ML (Python)│
   (JWT)         │   :8080        │        │   :8000            │
                 └───────┬────────┘        └────────────────────┘
                         │ JPA
                 ┌───────▼────────┐
                 │ PostgreSQL :5432│
                 └────────────────┘
```

---

## Fonctionnalités déjà implémentées (backend + ML)

### Authentification & autorisation
- **Inscription** (`POST /register`) : email + mot de passe, stocké **haché** (BCrypt, jamais en clair).
- **Connexion** (`POST /login`) : vérifie les identifiants et renvoie un **JWT signé** (identité + rôle + expiration 1 h).
- **Filtre JWT stateless** : chaque route protégée exige un header `Authorization: Bearer <token>`.
- **Rôles différenciés** : `CLIENT` (par défaut) et `ADMIN` ; les routes `/admin/**` sont réservées aux admins.

### Gestion des CV
- **Upload** (`POST /cv`) : fichier PDF/DOCX (`multipart/form-data`).
- **Validation robuste** : fichier vide, taille (≤ 5 Mo), extension, **et signature du contenu** (magic bytes — un `.exe` renommé `.pdf` est rejeté).
- **Extraction de texte** : le PDF est parsé (Apache PDFBox) et son texte stocké en base à l'upload.
- **Récupération** (`GET /cv/{ticket}`).

### Gestion des offres
- **Soumission** (`POST /job-post`) : texte brut de l'offre (JSON).
- **Récupération** (`GET /job-post/{ticket}`).

### Scan & scoring (moteur hybride)
- **Scan** (`POST /scan`) : relie un CV et une offre par leurs tickets, appelle le service ML, stocke le score.
- **Récupération** (`GET /scan/{ticket}`).
- **Score hybride** (service Python) :
  - *Couche A — mots-clés* : % de recouvrement du vocabulaire de l'offre dans le CV (tokenization propre, ponctuation ignorée).
  - *Couche B — sémantique* : similarité de sens via embeddings **multilingues** pré-entraînés (`sentence-transformers`, modèle `paraphrase-multilingual-MiniLM-L12-v2`).
  - Combinaison **50/50**, résultat explicable (`score`, `keyword_score`, `semantic_score`).

### Persistance & migrations
- **PostgreSQL 18** dans Docker.
- Schéma versionné par **Flyway** (migrations `V1`…`V6` : `cv_document`, `job_post`, `scan`, `app_user`, colonnes `score` / `extracted_text`).
- **JPA / Hibernate** (`ddl-auto=validate` : Flyway a le contrôle exclusif du schéma).

---

## Stack technique

Java 25 · Spring Boot 4.1 (Web, Data JPA, Security) · PostgreSQL 18 · Flyway · Apache PDFBox ·
jjwt (JWT) · BCrypt · Python 3.13 · FastAPI · Uvicorn · sentence-transformers · Docker Compose.

---

## Prérequis
Docker + Compose v2, Java 25, Node 24, Python 3.13.

---

## Démarrage local (rituel de session)

Lancer les 3 services **dans cet ordre** (chacun dépend du précédent).

### 0. À chaque session
- Lancer **Docker Desktop** et attendre « Engine running ».

### 1. Base de données (PostgreSQL via Docker)
```bash
cd infra
docker compose up -d
docker compose ps          # attendre que smartats-db soit "Up"
```

### 2. Service ML (Python / FastAPI)
```bash
cd ml-service
source .venv/bin/activate   # le prompt doit afficher (.venv)
uvicorn main:app            # sans --reload (modèle lourd) ; écoute sur :8000
```
> Première exécution : le modèle sémantique (~470 Mo) se télécharge dans `~/.cache/huggingface`.

### 3. Backend (Java / Spring Boot)
```bash
cd backend
./mvnw spring-boot:run      # écoute sur :8080 ; attendre "Started BackendApplication"
```

### Vérifier que tout tourne
```bash
curl http://localhost:8080/ping                 # backend -> pong
curl http://localhost:8000/ping                 # ML      -> pong
docker compose -f infra/docker-compose.yml ps   # base    -> Up
```

### Arrêter
```bash
# backend / uvicorn : Ctrl+C dans leur terminal (ou : pkill -f spring-boot:run)
cd infra
docker compose down         # arrête la base (garde les données)
# docker compose down -v    # ATTENTION : -v efface aussi les données
```

---

## Exemple d'utilisation (bout en bout)

```bash
# 1. Créer un compte
curl -X POST http://localhost:8080/register -H "Content-Type: application/json" \
  -d '{"email":"me@test.com","password":"pass1234"}'

# 2. Se connecter et récupérer le token
TOKEN=$(curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" \
  -d '{"email":"me@test.com","password":"pass1234"}' | grep -o 'eyJ[^"]*')

# 3. Uploader un CV (PDF)
curl -F "file=@mon_cv.pdf" http://localhost:8080/cv -H "Authorization: Bearer $TOKEN"

# 4. Soumettre une offre
curl -X POST http://localhost:8080/job-post -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Dev Java","rawText":"java spring docker postgresql"}'

# 5. Lancer un scan (avec les tickets renvoyés)
curl -X POST http://localhost:8080/scan -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"cvTicket":"<TICKET_CV>","jobPostTicket":"<TICKET_OFFRE>"}'
```

---

## Endpoints

| Méthode | Route | Auth | Description |
|---|---|---|---|
| POST | `/register` | publique | Créer un compte |
| POST | `/login` | publique | Se connecter, obtenir un JWT |
| GET  | `/ping` | publique | Test de vie |
| POST | `/cv` | JWT | Uploader un CV (PDF/DOCX) |
| GET  | `/cv/{ticket}` | JWT | Récupérer un CV |
| POST | `/job-post` | JWT | Soumettre une offre |
| GET  | `/job-post/{ticket}` | JWT | Récupérer une offre |
| POST | `/scan` | JWT | Scanner (CV + offre → score) |
| GET  | `/scan/{ticket}` | JWT | Récupérer un scan |
| GET  | `/admin/users` | JWT + ADMIN | Lister les utilisateurs |

Codes de réponse : `200` OK · `400` requête invalide · `401` non authentifié ·
`403` accès interdit (rôle) · `404` introuvable · `409` email déjà utilisé · `413` fichier trop volumineux.

---

## Dépannage rapide
- Backend : `Connection refused ... 5432` → la base n'est pas lancée. Refaire l'étape 1.
- `docker: command not found` / `cannot connect` → Docker Desktop n'est pas démarré.
- `uvicorn: command not found` → environnement Python non activé (`source .venv/bin/activate`).
- `/scan` renvoie une erreur serveur → le service ML (:8000) n'est pas lancé.
- Voir pourquoi un conteneur meurt : `docker compose logs db`.

### Accès SQL rapide
```bash
docker exec -it smartats-db psql -U smartats -d smartats
docker exec -it smartats-db psql -U smartats -d smartats -c "\dt"
```

---

## Feuille de route (à venir)
- Remonter le détail du score (`keyword_score`, `semantic_score`) jusqu'à la réponse du scan.
- Détection automatique de la langue (FR/EN).
- Frontend Next.js (interface complète).
- Historique des scans par utilisateur, dashboard.
- CI (GitHub Actions), puis conteneurisation des 3 services + observabilité.

---

## Sécurité — notes
- Les secrets de développement (mot de passe DB, clé JWT) sont dans la config pour le confort local
  et **doivent être remplacés par des variables d'environnement en production**.
- Les mots de passe ne sont **jamais** stockés en clair (BCrypt, salé, à coût élevé).
- Les JWT sont signés (HS512) : lisibles mais infalsifiables sans la clé secrète.

## Taxonomie des compétences (service ML)

Le moteur reconnaît environ 14 000 compétences issues d'ESCO v1.2.1 (référentiel européen),
en français et en anglais, complétées par `ml-service/data/skills_custom.json`
(outils tech, abréviations). Le fichier custom est prioritaire sur ESCO.

Le service charge `ml-service/data/taxonomy.json` (généré et versionné). Pour le régénérer :

1. Télécharger ESCO v1.2.1, format CSV, en français et en anglais :
   https://esco.ec.europa.eu/en/use-esco/download
2. Copier `skills_fr.csv` et `skills_en.csv` dans `ml-service/data/` (non versionnés).
3. Lancer :
```bash
   cd ml-service && source .venv/bin/activate
   python3 scripts/build_taxonomy.py
```
4. Redémarrer le service ML.

Après toute modification de `skills_custom.json`, relancer l'étape 3.
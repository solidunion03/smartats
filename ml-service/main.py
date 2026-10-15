import json
import re
import unicodedata
from datetime import date
from pathlib import Path

from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer, util


# 1. Démarrage : application et modèle sémantique
app = FastAPI()

# Chargé UNE SEULE FOIS au démarrage, réutilisé à chaque requête
model = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")


# 2. Normalisation du texte

def fold(text: str) -> str:
    """Minuscules + accents retirés : 'Orientée' -> 'orientee'."""
    decomposed = unicodedata.normalize("NFKD", text.lower())
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def normalize_tokens(text: str) -> list[str]:
    """Découpe un texte en mots normalisés (minuscules, sans accents)."""
    return [t.rstrip(".") for t in re.findall(r"\w[\w+#.]*", fold(text))]


# 3. Taxonomie et index des compétences

TAXONOMY_PATH = Path(__file__).parent / "data" / "taxonomy.json"
with open(TAXONOMY_PATH, encoding="utf-8") as f:
    SKILL_TAXONOMY = json.load(f)

# Index construit UNE fois au démarrage : variante normalisée -> compétences canoniques
VARIANT_INDEX = {}
for canonical, variants in SKILL_TAXONOMY.items():
    for variant in variants:
        key = " ".join(normalize_tokens(variant))
        if key:
            VARIANT_INDEX.setdefault(key, set()).add(canonical)

# Nombre de mots de la plus longue variante
MAX_NGRAM = max(len(key.split()) for key in VARIANT_INDEX)


def extract_skills(text: str) -> set:
    """Retourne l'ensemble des compétences CANONIQUES trouvées dans le texte."""
    tokens = normalize_tokens(text)
    found = set()
    for n in range(1, MAX_NGRAM + 1):              # groupes de 1, 2, 3... mots
        for i in range(len(tokens) - n + 1):
            key = " ".join(tokens[i:i + n])
            if key in VARIANT_INDEX:
                found |= VARIANT_INDEX[key]
    return found



# 4. Sections du CV et poids par section

SECTION_HEADERS = {
    "experience": ["experience", "parcours professionnel"],
    "skills":     ["competences", "skills", "technologies", "stack technique"],
    "education":  ["formation", "education", "diplome"],
}

# Poids de confiance selon la section où la compétence apparaît
SECTION_WEIGHTS = {
    "experience": 1.0,   # prouvée
    "skills": 0.7,       # déclarée dans une liste
    "education": 0.6,    # mentionnée en formation
    "other": 0.5,        # hors section
}


def split_sections(text: str) -> dict:
    """Découpe un CV en sections {nom_section: contenu} (texte sans accents)."""
    text_folded = fold(text)

    positions = []
    for section, headers in SECTION_HEADERS.items():
        for h in headers:
            idx = text_folded.find(fold(h))
            if idx != -1:
                positions.append((idx, section))
                break

    positions.sort()

    sections = {}
    for i, (idx, section) in enumerate(positions):
        end = positions[i + 1][0] if i + 1 < len(positions) else len(text_folded)
        sections[section] = text_folded[idx:end]
    return sections


def extract_skills_weighted(text: str) -> dict:
    """Retourne {compétence_canonique: poids} selon la section où elle apparaît."""
    skill_weights = {}

    for section, content in split_sections(text).items():
        weight = SECTION_WEIGHTS.get(section, SECTION_WEIGHTS["other"])
        for skill in extract_skills(content):
            skill_weights[skill] = max(skill_weights.get(skill, 0), weight)

    # filet de sécurité : compétences hors de toute section détectée
    for skill in extract_skills(text):
        if skill not in skill_weights:
            skill_weights[skill] = SECTION_WEIGHTS["other"]

    return skill_weights


# 5. Score compétences

def skill_match(cv_text: str, job_text: str) -> dict:
    cv_weights = extract_skills_weighted(cv_text)   # {compétence: poids}
    job_skills = extract_skills(job_text)            # compétences demandées

    if not job_skills:
        return {"skill_score": 0, "matched": [], "missing": []}

    matched = []
    earned = 0.0
    for skill in job_skills:
        if skill in cv_weights:
            matched.append(skill)
            earned += cv_weights[skill]              # on ajoute le POIDS, pas 1

    missing = [s for s in job_skills if s not in cv_weights]
    score = round(earned / len(job_skills) * 100)

    return {
        "skill_score": score,
        "matched": sorted(matched),
        "missing": sorted(missing),
    }


# 6. Expérience

# --- années demandées dans l'offre ---
YEARS_PATTERN = re.compile(
    r"\b(\d{1,2})\s*\+?\s*(?:(?:a|-|to)\s*\d{1,2}\s*)?(?:ans?|annees?|years?|yrs?)\b"
)
EXPERIENCE_WINDOW = 25   # caractères autour du nombre où chercher "experience"


def extract_required_years(job_text: str):
    """Années d'expérience demandées dans l'offre. None si l'offre n'en parle pas."""
    text = fold(job_text)
    found = []
    for m in YEARS_PATTERN.finditer(text):
        before = text[max(0, m.start() - EXPERIENCE_WINDOW): m.start()]
        after = text[m.end(): m.end() + EXPERIENCE_WINDOW]
        if "experience" in before or "experience" in after:
            found.append(int(m.group(1)))
    return max(found) if found else None


# --- années estimées du candidat ---
PRESENT_WORDS = r"(?:aujourd.?hui|present|actuel(?:lement)?|now|current|en cours|ce jour)"
DATE = r"(?:(\d{1,2})/)?((?:19|20)\d{2})"     # mois optionnel (MM/), puis une année

RANGE_PATTERN = re.compile(
    DATE + r"\s*(?:-|–|—|a|au|to)\s*(?:" + DATE + "|" + PRESENT_WORDS + ")"
)
SINCE_PATTERN = re.compile(r"(?:depuis|since)\s+" + DATE)


def to_months(year: str, month: str | None) -> int:
    """'2021', '09' -> nombre de mois (janvier si pas de mois)."""
    return int(year) * 12 + (int(month) if month else 1) - 1


def merged_months(intervals: list) -> int:
    """Durée totale en mois, en fusionnant les périodes qui se chevauchent."""
    total = 0
    cur_start = cur_end = None
    for start, end in sorted(intervals):
        if cur_end is None or start > cur_end:      # nouvelle période séparée
            if cur_end is not None:
                total += cur_end - cur_start
            cur_start, cur_end = start, end
        else:                                       # chevauchement : on prolonge
            cur_end = max(cur_end, end)
    if cur_end is not None:
        total += cur_end - cur_start
    return total


def extract_candidate_years(cv_text: str):
    """Années d'expérience estimées depuis la section expérience. None si introuvable."""
    text = split_sections(cv_text).get("experience")
    if text is None:
        return None

    today = date.today()
    now = today.year * 12 + today.month - 1

    intervals = []
    for m in RANGE_PATTERN.finditer(text):
        start = to_months(m.group(2), m.group(1))
        end = to_months(m.group(4), m.group(3)) if m.group(4) else now
        if start <= end:
            intervals.append((start, min(end, now)))
    for m in SINCE_PATTERN.finditer(text):
        intervals.append((to_months(m.group(2), m.group(1)), now))

    if not intervals:
        return None
    return round(merged_months(intervals) / 12, 1)


def experience_match(cv_text: str, job_text: str) -> dict:
    required = extract_required_years(job_text)
    candidate = extract_candidate_years(cv_text)

    if required is None or candidate is None:
        exp_score = None                 # dimension non applicable
    elif required == 0:
        exp_score = 100
    else:
        exp_score = min(100, round(candidate / required * 100))

    return {
        "experience_score": exp_score,
        "required_years": required,
        "candidate_years": candidate,
    }



# 7. Score sémantique

def semantic_score(cv_text: str, job_text: str) -> int:
    cv_vec = model.encode(cv_text)
    job_vec = model.encode(job_text)
    similarity = float(util.cos_sim(cv_vec, job_vec))
    return round(similarity * 100)


# 8. Combinaison des scores

SCORE_WEIGHTS = {"skills": 0.6, "semantic": 0.25, "experience": 0.15}


def combine(scores: dict) -> int:
    """Moyenne pondérée des dimensions disponibles (les None sont ignorées)."""
    available = {k: v for k, v in scores.items() if v is not None}
    total_weight = sum(SCORE_WEIGHTS[k] for k in available)
    return round(sum(SCORE_WEIGHTS[k] * v for k, v in available.items()) / total_weight)


# 9. API
class ScoreRequest(BaseModel):
    cv_text: str
    job_text: str


class ScoreResponse(BaseModel):
    score: int
    skill_score: int
    semantic_score: int
    matched_skills: list[str]
    missing_skills: list[str]
    experience_score: int | None = None
    required_years: int | None = None
    candidate_years: float | None = None


@app.get("/ping")
def ping():
    return {"message": "pong from SmartATS ML service"}


@app.post("/score")
def score(request: ScoreRequest) -> ScoreResponse:
    skills = skill_match(request.cv_text, request.job_text)
    sem = semantic_score(request.cv_text, request.job_text)
    exp = experience_match(request.cv_text, request.job_text)

    final = combine({
        "skills": skills["skill_score"],
        "semantic": sem,
        "experience": exp["experience_score"],
    })

    return ScoreResponse(
        score=final,
        skill_score=skills["skill_score"],
        semantic_score=sem,
        matched_skills=skills["matched"],
        missing_skills=skills["missing"],
        **exp,
    )
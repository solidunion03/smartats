import re
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer, util

app = FastAPI()

# Chargé UNE SEULE FOIS au démarrage du service, réutilisé à chaque requête
model = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")


class ScoreRequest(BaseModel):
    cv_text: str
    job_text: str


class ScoreResponse(BaseModel):
    score: int
    keyword_score: int
    semantic_score: int


def tokenize(text: str) -> set:
    return set(re.findall(r"[a-z0-9]+", text.lower()))


def keyword_score(cv_text: str, job_text: str) -> int:
    cv_words = tokenize(cv_text)
    job_words = tokenize(job_text)
    if not job_words:
        return 0
    common = cv_words & job_words
    return round(len(common) / len(job_words) * 100)


def semantic_score(cv_text: str, job_text: str) -> int:
    cv_vec = model.encode(cv_text)
    job_vec = model.encode(job_text)
    similarity = float(util.cos_sim(cv_vec, job_vec))
    return round(similarity * 100)


@app.get("/ping")
def ping():
    return {"message": "pong from SmartATS ML service"}


@app.post("/score")
def score(request: ScoreRequest) -> ScoreResponse:
    kw = keyword_score(request.cv_text, request.job_text)
    sem = semantic_score(request.cv_text, request.job_text)
    final = round((kw + sem) / 2)   # moyenne 50/50
    return ScoreResponse(score=final, keyword_score=kw, semantic_score=sem)
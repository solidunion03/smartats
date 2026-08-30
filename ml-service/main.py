from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class ScoreRequest(BaseModel):
    cv_text: str
    job_text: str


class ScoreResponse(BaseModel):
    score: int


@app.get("/ping")
def ping():
    return {"message": "pong from SmartATS ML service"}


@app.post("/score")
def score(request: ScoreRequest) -> ScoreResponse:
    # score bidon pour l'instant : on valide juste le tuyau
    return ScoreResponse(score=50)
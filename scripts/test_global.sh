#!/usr/bin/env bash
# Test global SmartATS : sécurité, auth, CV, offre, scan (compétences + expérience), rôles.
# Prérequis : les 3 services tournent, et l'environnement Python du ml-service est activé (pour fpdf).

API=http://localhost:8080
PASS=0
FAIL=0

check() {   # check <nom> <attendu> <obtenu>
  if [ "$2" == "$3" ]; then
    echo "  OK   $1 ($3)"; PASS=$((PASS + 1))
  else
    echo "  FAIL $1 : attendu=$2 obtenu=$3"; FAIL=$((FAIL + 1))
  fi
}
json() {    # json <champ> : lit un champ du JSON reçu en entrée
  python3 -c "import sys, json; print(json.load(sys.stdin).get('$1'))"
}
code() {    # code <arguments curl> : renvoie seulement le code HTTP
  curl -s -o /dev/null -w "%{http_code}" "$@"
}

echo "== Préparation"
python3 -c "
from fpdf import FPDF
p = FPDF(); p.add_page(); p.set_font('helvetica', size=12)
p.multi_cell(0, 10, 'Competences: java, docker\nExperience: Developpeur spring boot et postgresql 2021 - 2024\nFormation: Ingenieur 2016 - 2021')
p.output('/tmp/test_cv.pdf')"
echo "pas un pdf" > /tmp/faux.pdf
EMAIL="test$(date +%s)@test.com"
JSON="Content-Type: application/json"

echo "== Routes publiques et sécurité"
check "/ping"              200 "$(code $API/ping)"
check "/scan sans token"   403 "$(code -X POST $API/scan -H "$JSON" -d '{}')"

echo "== Authentification"
check "register"           200 "$(code -X POST $API/register -H "$JSON" -d "{\"email\":\"$EMAIL\",\"password\":\"pass1234\"}")"
check "register doublon"   409 "$(code -X POST $API/register -H "$JSON" -d "{\"email\":\"$EMAIL\",\"password\":\"pass1234\"}")"
check "login mauvais mdp"  401 "$(code -X POST $API/login -H "$JSON" -d "{\"email\":\"$EMAIL\",\"password\":\"FAUX\"}")"
TOKEN=$(curl -s -X POST $API/login -H "$JSON" -d "{\"email\":\"$EMAIL\",\"password\":\"pass1234\"}" | json token)
AUTH="Authorization: Bearer $TOKEN"
check "login -> token"     true "$([ ${#TOKEN} -gt 20 ] && echo true || echo false)"

echo "== CV et offre"
check "cv faux contenu"    400 "$(code -F file=@/tmp/faux.pdf $API/cv -H "$AUTH")"
CV=$(curl -s -F file=@/tmp/test_cv.pdf $API/cv -H "$AUTH" | json ticket)
JOB=$(curl -s -X POST $API/job-post -H "$JSON" -H "$AUTH" \
  -d '{"title":"Dev","rawText":"Developpeur Java Spring PostgreSQL Docker, 3 ans d experience"}' | json ticket)
check "cv -> ticket"       true "$([ ${#CV} -eq 36 ] && echo true || echo false)"
check "offre -> ticket"    true "$([ ${#JOB} -eq 36 ] && echo true || echo false)"

echo "== Scan"
SCAN=$(curl -s -X POST $API/scan -H "$JSON" -H "$AUTH" -d "{\"cvTicket\":\"$CV\",\"jobPostTicket\":\"$JOB\"}")
echo "  réponse : $SCAN"
check "skillScore"         85    "$(echo "$SCAN" | json skillScore)"
check "missingSkills"      "[]"  "$(echo "$SCAN" | json missingSkills)"
check "requiredYears"      3     "$(echo "$SCAN" | json requiredYears)"
check "candidateYears"     3.0   "$(echo "$SCAN" | json candidateYears)"
check "experienceScore"    100   "$(echo "$SCAN" | json experienceScore)"
SCAN_TICKET=$(echo "$SCAN" | json ticket)
check "GET /scan relit l'expérience" 100 "$(curl -s $API/scan/$SCAN_TICKET -H "$AUTH" | json experienceScore)"

echo "== Rôles"
check "/admin en client"   403 "$(code $API/admin/users -H "$AUTH")"
docker exec smartats-db psql -U smartats -d smartats -c "UPDATE app_user SET role='ADMIN' WHERE email='$EMAIL';" > /dev/null
ADMIN_TOKEN=$(curl -s -X POST $API/login -H "$JSON" -d "{\"email\":\"$EMAIL\",\"password\":\"pass1234\"}" | json token)
check "/admin en admin"    200 "$(code $API/admin/users -H "Authorization: Bearer $ADMIN_TOKEN")"

echo "== Résultat : $PASS OK, $FAIL FAIL"
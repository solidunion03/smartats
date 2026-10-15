"""Transforme le CSV ESCO en taxonomie {forme_canonique: [variantes]}."""
import csv
import json
import re
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
SOURCES = ["skills_fr.csv", "skills_en.csv"]
OUTPUT = DATA_DIR / "taxonomy.json"
CUSTOM = DATA_DIR / "skills_custom.json"
MIN_LEN = 2                      # ignore les variantes d'une seule lettre

# Verbes d'action ESCO à retirer en début de libellé
ACTION_PREFIXES = [
    # anglais
    "knowledge of", "work with", "use", "utilise", "utilize", "apply", "operate", "implement",
    # français
    "connaissance des", "connaissance du", "connaissance de", "connaissances en",
    "travailler avec", "mettre en œuvre", "faire fonctionner", "utiliser", "appliquer",
]
# Articles à retirer juste après le verbe
ARTICLES = ["the", "a", "an", "le", "la", "les", "un", "une", "des", "du", "de"]
ELIDED = ["l'", "l’", "d'", "d’"]     # articles collés au mot suivant
MIN_WORDS_DERIVED = 2                # garde-fou : une variante générée doit faire >= 2 mots

def clean(label: str) -> str:
    """Minuscules + espaces normalisés."""
    return " ".join(label.lower().split())


def without_qualifier(label: str) -> str:
    """Retire la précision entre parenthèses : 'python (programmation)' -> 'python'."""
    return clean(re.sub(r"\(.*?\)", "", label))

def strip_action(label: str):
    """'use object-oriented programming' -> 'object-oriented programming'. Sinon None."""
    # 1. retirer le verbe d'action
    rest = None
    for prefix in ACTION_PREFIXES:
        if label.startswith(prefix + " "):
            rest = label[len(prefix) + 1:]
            break
    if rest is None:
        return None          # le libellé ne commence pas par un verbe connu

    # 2. retirer un article en tête
    for art in ELIDED:
        if rest.startswith(art):
            rest = rest[len(art):]
            break
    else:
        for art in ARTICLES:
            if rest.startswith(art + " "):
                rest = rest[len(art) + 1:]
                break

    # 3. garde-fou
    return rest if len(rest.split()) >= MIN_WORDS_DERIVED else None

def variants_of(row: dict) -> set:
    labels = [row["preferredLabel"]] + row["altLabels"].split("\n")
    out = set()
    for label in labels:
        base = {clean(label), without_qualifier(label)}
        derived = {strip_action(b) for b in base} - {None}
        for v in base | derived:
            if len(v) >= MIN_LEN:
                out.add(v)
    return out

def merge_custom(taxonomy: dict) -> dict:
    """Fusionne ESCO avec le fichier custom. Le custom est prioritaire."""
    with open(CUSTOM, encoding="utf-8") as f:
        custom = json.load(f)

    # toutes les variantes revendiquées par le fichier custom
    claimed = {v for variants in custom.values() for v in variants}

    # 1. on retire ces variantes des entrées ESCO
    merged = {}
    for label, variants in taxonomy.items():
        kept = [v for v in variants if v not in claimed]
        if kept:
            merged[label] = kept

    # 2. on ajoute les entrées custom (en gardant les variantes ESCO restantes si même nom)
    for label, variants in custom.items():
        merged[label] = sorted(set(merged.get(label, [])) | set(variants) | {label})

    return merged

def main():
    by_uri = {}     # conceptUri -> {"label": ..., "variants": set()}
    read = kept = 0

    for name in SOURCES:
        with open(DATA_DIR / name, encoding="utf-8", newline="") as f:
            for row in csv.DictReader(f):
                read += 1
                if row["status"] != "released":
                    continue
                kept += 1
                entry = by_uri.setdefault(row["conceptUri"], {
                    "label": without_qualifier(row["preferredLabel"]),
                    "variants": set(),
                })
                entry["variants"] |= variants_of(row)

    # même format que skills.json : {canonique: [variantes]}
    taxonomy = {}
    for entry in by_uri.values():
        taxonomy.setdefault(entry["label"], set()).update(entry["variants"])
    taxonomy = {k: sorted(v) for k, v in taxonomy.items() if k}
    taxonomy = merge_custom(taxonomy)

    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(taxonomy, f, ensure_ascii=False, indent=1)

    print(f"lignes lues : {read} | gardées : {kept} | compétences : {len(taxonomy)}")
    print(f"écrit dans : {OUTPUT}")


if __name__ == "__main__":
    main()
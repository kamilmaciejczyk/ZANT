import re
from pathlib import Path

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import classification_report, confusion_matrix
import joblib


# === ŚCIEŻKI ===

SCRIPT_DIR = Path(__file__).resolve().parent
BASE_OUTPUT_DIR = SCRIPT_DIR.parent / "OCR_OUTPUT"        # HackNation/OCR_OUTPUT
LABELS_CSV = BASE_OUTPUT_DIR / "gemini_wnioski_labels.csv"  # etykiety z Geminiego
MODELS_DIR = SCRIPT_DIR.parent / "MODELS"                 # tu zapiszemy model
MODELS_DIR.mkdir(exist_ok=True)


# === POMOCNICZE FUNKCJE ===

def basic_clean(text: str) -> str:
    """Proste czyszczenie tekstu z OCR (jak wcześniej)."""
    if not isinstance(text, str):
        return ""

    text = re.sub(r"=+\s*STRONA\s+\d+\s*=+", " ", text, flags=re.IGNORECASE)
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def load_labels() -> pd.DataFrame:
    """
    Wczytuje gemini_wnioski_labels.csv i filtruje:
      - label w {wniosek uznany, wniosek nieuznany}
      - raw_model_output NIE zaczyna się od 'ERROR'
    Zwraca DataFrame z kolumnami: case_id, opinion_file, label.
    """
    if not LABELS_CSV.exists():
        raise FileNotFoundError(f"Nie znaleziono pliku etykiet: {LABELS_CSV}")

    df = pd.read_csv(LABELS_CSV, encoding="utf-8")

    # odfiltruj błędne / puste
    df["label"] = df["label"].astype(str).str.strip()
    df["raw_model_output"] = df["raw_model_output"].astype(str)

    mask_ok = df["label"].isin(["wniosek uznany", "wniosek nieuznany"])
    mask_not_error = ~df["raw_model_output"].str.startswith("ERROR during Gemini call")

    df = df[mask_ok & mask_not_error].copy()

    if df.empty:
        raise ValueError("Brak poprawnych etykiet po filtracji. Sprawdź plik gemini_wnioski_labels.csv.")

    return df[["case_id", "opinion_file", "label"]]


def collect_case_text(case_id: str) -> str:
    """
    Zbiera tekst ze WSZYSTKICH plików TXT w danym wypadku,
    z WYJĄTKIEM:
      - plików typu 'opinia*.txt'
      - plików karty wypadku (np. 'karta wypadku 10.txt')
    Zwraca jeden duży string (concatenacja).
    """
    case_dir = BASE_OUTPUT_DIR / case_id
    if not case_dir.exists():
        return ""

    parts = []

    for txt_path in sorted(case_dir.glob("*.txt")):
        name_lower = txt_path.name.lower()

        # 1) pomijamy opinię – na jej podstawie powstał label
        if name_lower.startswith("opinia"):
            continue

        # 2) pomijamy kartę wypadku – zawiera informacje,
        #    które mogą zaburzać predykcję
        if "karta" in name_lower and "wypadku" in name_lower:
            continue

        try:
            content = txt_path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            content = ""

        content_clean = basic_clean(content)
        if content_clean:
            parts.append(f"\n\n===== PLIK: {txt_path.name} =====\n\n{content_clean}")

    full_text = "\n".join(parts).strip()
    return full_text



def build_dataset() -> pd.DataFrame:
    """
    Tworzy DataFrame z kolumnami:
      - case_id
      - text   (z pozostałych plików, poza opinią)
      - label  ('wniosek uznany' / 'wniosek nieuznany')
      - y      (0/1)
    """
    labels_df = load_labels()

    texts = []
    y = []
    case_ids = []

    for _, row in labels_df.iterrows():
        case_id = row["case_id"]
        label = row["label"]

        full_text = collect_case_text(case_id)
        if not full_text:
            # jeśli brak tekstu (np. nie ma innych plików) – pomijamy
            print(f"[INFO] {case_id}: brak tekstu z innych plików, pomijam w trenowaniu.")
            continue

        texts.append(full_text)
        case_ids.append(case_id)
        y.append(1 if label == "wniosek uznany" else 0)

    if not texts:
        raise ValueError("Nie zebrano żadnych danych tekstowych do trenowania.")

    data = pd.DataFrame({
        "case_id": case_ids,
        "text": texts,
        "label": y,  # 1 = uznany, 0 = nieuznany
    })

    return data


# === GŁÓWNA LOGIKA ===

def main():
    print("Buduję dataset...")
    data = build_dataset()

    print(f"Liczba przykładów: {len(data)}")
    print("Rozkład klas (1=uznany, 0=nieuznany):")
    print(data["label"].value_counts())

    X = data["text"].tolist()
    y = data["label"].values

    # train / test split ze stratykacją
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=42,
        stratify=y,
    )

    # Pipeline: TF-IDF (unigramy+bigramy) + LogisticRegression
    model = Pipeline([
        ("tfidf", TfidfVectorizer(
            max_features=50000,
            ngram_range=(1, 2),
            min_df=2
        )),
        ("clf", LogisticRegression(
            max_iter=200,
            class_weight="balanced",
            n_jobs=-1
        )),
    ])

    print("Trenuję model...")
    model.fit(X_train, y_train)

    print("Ewaluacja na zbiorze testowym:")
    y_pred = model.predict(X_test)
    print(classification_report(y_test, y_pred, digits=3))
    print("Macierz pomyłek:")
    print(confusion_matrix(y_test, y_pred))

    # zapis modelu
    model_path = MODELS_DIR / "wniosek_model.joblib"
    joblib.dump(model, model_path)

    print(f"\n=== GOTOWE ===")
    print(f"Zapisano wytrenowany model do: {model_path}")
    print("Możesz go potem wczytać i używać do predykcji nowych spraw.")


if __name__ == "__main__":
    main()

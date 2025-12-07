import argparse
import re
from pathlib import Path

import joblib


# === POMOCNICZE FUNKCJE (takie same jak przy trenowaniu) ===

def basic_clean(text: str) -> str:
    """Proste czyszczenie tekstu z OCR."""
    if not isinstance(text, str):
        return ""

    text = re.sub(r"=+\s*STRONA\s+\d+\s*=+", " ", text, flags=re.IGNORECASE)
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def collect_case_text_from_dir(case_dir: Path) -> str:
    """
    Zbiera tekst ze WSZYSTKICH plików TXT w danym wypadku,
    z WYJĄTKIEM:
      - plików typu 'opinia*.txt'
      - plików karty wypadku (np. 'karta wypadku 10.txt').

    To jest dokładnie to samo założenie co przy trenowaniu.
    """
    if not case_dir.exists() or not case_dir.is_dir():
        raise FileNotFoundError(f"Katalog sprawy nie istnieje: {case_dir}")

    parts = []

    for txt_path in sorted(case_dir.glob("*.txt")):
        name_lower = txt_path.name.lower()

        # 1) pomijamy opinię – na jej podstawie była etykieta
        if name_lower.startswith("opinia"):
            continue

        # 2) pomijamy kartę wypadku
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
    if not full_text:
        raise ValueError(f"Nie zebrano żadnego tekstu z katalogu: {case_dir}")

    return full_text


def load_model(model_path: Path):
    if not model_path.exists():
        raise FileNotFoundError(f"Plik modelu nie istnieje: {model_path}")
    model = joblib.load(model_path)
    return model


def predict_from_text(model, text: str) -> float:
    """
    Zwraca prawdopodobieństwo klasy 1 (wniosek uznany) jako float 0..1.
    """
    text_clean = basic_clean(text)
    if not text_clean:
        raise ValueError("Tekst do predykcji jest pusty po czyszczeniu.")

    proba = model.predict_proba([text_clean])[0][1]  # indeks 1 = klasa '1'
    return proba


# === CLI ===

def main():
    parser = argparse.ArgumentParser(
        description="Predykcja, czy zdarzenie było wypadkiem przy pracy na bazie wytrenowanego modelu."
    )
    parser.add_argument(
        "--model-path",
        type=str,
        required=True,
        help="Ścieżka do pliku modelu .joblib (np. MODELS/wniosek_model.joblib)",
    )
    parser.add_argument(
        "--case-dir",
        type=str,
        help="Ścieżka do katalogu z plikami TXT dla nowej sprawy (jak w OCR_OUTPUT/wypadek XX).",
    )
    parser.add_argument(
        "--text-file",
        type=str,
        help="Ścieżka do pojedynczego pliku TXT z pełnym opisem zdarzenia + resztą danych.",
    )

    args = parser.parse_args()

    model_path = Path(args.model_path)
    model = load_model(model_path)

    # Pobierz tekst wejściowy:
    if args.case_dir:
        # tryb: katalog jak przy trenowaniu
        case_dir = Path(args.case_dir)
        text = collect_case_text_from_dir(case_dir)
        source_info = f"katalog: {case_dir}"
    elif args.text_file:
        # tryb: pojedynczy plik TXT
        text_path = Path(args.text_file)
        if not text_path.exists():
            raise FileNotFoundError(f"Plik tekstowy nie istnieje: {text_path}")
        text = text_path.read_text(encoding="utf-8", errors="ignore")
        source_info = f"plik: {text_path}"
    else:
        raise ValueError("Musisz podać albo --case-dir, albo --text-file jako źródło danych wejściowych.")

    proba = predict_from_text(model, text)
    percent = proba * 100.0

    print(f"\nŹródło danych: {source_info}")
    print(f"AI jest w {percent:.1f}% pewien, że to był wypadek przy pracy!")

    # Opcjonalnie możesz też wypisać odwrotność:
    print(f"(Prawdopodobieństwo, że to NIE był wypadek przy pracy: {100.0 - percent:.1f}% )")


if __name__ == "__main__":
    main()

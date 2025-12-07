import os
import re
import json
import csv
from pathlib import Path

from google import genai  # pip install google-genai


# === ŚCIEŻKI ===

SCRIPT_DIR = Path(__file__).resolve().parent
BASE_OUTPUT_DIR = SCRIPT_DIR.parent / "OCR_OUTPUT"   # HackNation/OCR_OUTPUT
OUTPUT_CSV = BASE_OUTPUT_DIR / "gemini_wnioski_labels.csv"


# === KLIENT GEMINI ===

API_KEY = os.environ.get("GEMINI_API_KEY", "AIzaSyB9Jt4HfDRF4UNZ8hUTVeRGQqtAN_QvEKI").strip()
if not API_KEY:
    raise RuntimeError(
        "Brak klucza GEMINI_API_KEY. Ustaw zmienną środowiskową "
        "GEMINI_API_KEY lub wpisz klucz w zmiennej API_KEY."
    )

client = genai.Client(api_key=API_KEY)
GEMINI_MODEL = "gemini-2.5-flash"  # możesz zmienić np. na "gemini-1.5-flash"


# === FUNKCJE POMOCNICZE ===

def get_case_number(case_id: str) -> str:
    m = re.search(r"\d+", case_id)
    return m.group(0) if m else case_id


def basic_clean(text: str) -> str:
    text = re.sub(r"=+\s*STRONA\s+\d+\s*=+", " ", text, flags=re.IGNORECASE)
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def extract_wniosek_fragment(text: str) -> str | None:
    """
    Wyciąga fragment od 'Wniosek:' do 'Uzasadnienie:'.
    Zwraca None, jeśli nie znajdzie 'Wniosek'.
    """
    lower = text.lower()

    m_start = re.search(r"wniosek\s*:", lower)
    if not m_start:
        return None

    start_idx = m_start.end()

    m_end = re.search(r"uzasadnienie\s*:", lower)
    if m_end and m_end.start() > start_idx:
        end_idx = m_end.start()
    else:
        end_idx = len(text)

    fragment = text[start_idx:end_idx].strip()
    return fragment or None


def build_prompt(fragment: str) -> str:
    return f"""
Jesteś klasyfikatorem decyzji w sprawach powypadkowych.

Otrzymasz fragment tekstu z sekcji „Wniosek” opinii ubezpieczeniowej
(albo cały tekst opinii, jeśli nie udało się dokładnie wyciąć sekcji).

Tekst pochodzi z OCR, więc może mieć literówki i błędy.

Twoje zadanie:
- Zinterpretuj sens decyzji.
- Zwróć dokładnie JEDNO z dwóch słów (bez innych słów, bez komentarza):
  - "wniosek uznany"    -> gdy roszczenie / odpowiedzialność / świadczenie zostało uznane (choćby częściowo),
  - "wniosek nieuznany" -> gdy roszczenie / odpowiedzialność / świadczenie zostało odmówione / odrzucone.

Jeśli tekst jest niejednoznaczny, wybierz wariant, który jest bliższy znaczeniowo.

ZWRÓĆ ODPOWIEDŹ WYŁĄCZNIE W POSTACI CZYSTEGO JSON-a:

{{"label": "wniosek uznany"}}
lub
{{"label": "wniosek nieuznany"}}

Nie dodawaj żadnych innych pól, komentarzy ani wyjaśnień.

---
TEKST DO KLASYFIKACJI:

{fragment}
""".strip()


def robust_parse_label(raw_output: str) -> str | None:
    """
    1) próba json.loads
    2) wycięcie {...} regexem, podmiana ' na "
    3) fallback po słowach kluczowych
    """
    raw = (raw_output or "").strip()
    if not raw:
        return None

    # 1) pełny JSON
    try:
        data = json.loads(raw)
        if isinstance(data, dict) and "label" in data:
            return data["label"]
    except Exception:
        pass

    # 2) fragment {...}
    m = re.search(r"\{.*\}", raw, flags=re.DOTALL)
    if m:
        candidate = m.group(0)
        try:
            data = json.loads(candidate)
            if isinstance(data, dict) and "label" in data:
                return data["label"]
        except Exception:
            fixed = candidate.replace("'", '"')
            try:
                data = json.loads(fixed)
                if isinstance(data, dict) and "label" in data:
                    return data["label"]
            except Exception:
                pass

    # 3) fallback
    low = raw.lower()
    if "wniosek uznany" in low:
        return "wniosek uznany"
    if "wniosek nieuznany" in low or "wniosek nie uznany" in low:
        return "wniosek nieuznany"

    return None


def classify_fragment_with_gemini(fragment: str):
    """
    Wysyła fragment do Gemini, zwraca (label, raw_output).
    NIE rzuca wyjątków – w razie problemów zwraca label=None i raw_output='ERROR: ...'.
    """
    # przycinamy długie teksty, żeby zmniejszyć ryzyko problemów sieciowych
    fragment_short = fragment[:4000]

    prompt = build_prompt(fragment_short)

    try:
        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
        )
        raw_output = (response.text or "").strip()
    except Exception as e:
        err_msg = f"ERROR during Gemini call: {type(e).__name__}: {e}"
        print(f"  [BŁĄD] {err_msg}")
        return None, err_msg

    label = robust_parse_label(raw_output)
    return label, raw_output


def load_existing_results() -> dict[tuple[str, str], dict]:
    """
    Ładuje istniejący CSV (jeśli jest) do słownika:
      key = (case_id, opinion_file)
      value = wiersz (dict)
    Dzięki temu przy kolejnym uruchomieniu nie wołamy API dla już oznaczonych przypadków.
    """
    existing = {}
    if not OUTPUT_CSV.exists():
        return existing

    with open(OUTPUT_CSV, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            key = (row["case_id"], row["opinion_file"])
            existing[key] = row
    return existing


# === GŁÓWNA LOGIKA ===

def main():
    if not BASE_OUTPUT_DIR.exists():
        raise FileNotFoundError(f"Nie znaleziono OCR_OUTPUT: {BASE_OUTPUT_DIR}")

    existing_results = load_existing_results()
    results = []

    for case_dir in sorted(BASE_OUTPUT_DIR.iterdir()):
        if not case_dir.is_dir():
            continue

        case_id = case_dir.name
        case_number = get_case_number(case_id)

        opinion_files = list(case_dir.glob("opinia*.txt"))
        if not opinion_files:
            print(f"[{case_id}] brak plików 'opinia*.txt', pomijam")
            continue

        for opinion_path in opinion_files:
            opinion_file_str = str(opinion_path)
            key = (case_id, opinion_file_str)

            # jeśli mamy już wynik z poprzedniego uruchomienia i label NIE jest pusty
            # i NIE jest to ERROR – NIE wywołujemy ponownie Geminiego
            if key in existing_results:
                prev = existing_results[key]
                prev_label = (prev.get("label") or "").strip()
                prev_raw = prev.get("raw_model_output") or ""
                if prev_label and not prev_raw.startswith("ERROR during Gemini call"):
                    # zachowaj poprzedni wiersz
                    results.append(prev)
                    print(f"[{case_id}] {opinion_path.name} – już oznaczone, pomijam API.")
                    continue
                else:
                    print(f"[{case_id}] {opinion_path.name} – ponawiam próbę (poprzednio ERROR / brak label).")

            print(f"[{case_id}] przetwarzam opinię: {opinion_path.name}")

            text = opinion_path.read_text(encoding="utf-8", errors="ignore")
            text = basic_clean(text)

            fragment = extract_wniosek_fragment(text)
            if fragment is not None:
                source = "wniosek_fragment"
            else:
                print(f"  [INFO] Nie znaleziono sekcji 'Wniosek:' w {opinion_path.name} – używam całej opinii.")
                fragment = text
                source = "full_opinion"

            label, raw_output = classify_fragment_with_gemini(fragment)

            if label is None:
                print(f"  [UWAGA] Nie udało się jednoznacznie ustalić label dla {opinion_path.name}")

            row = {
                "case_id": case_id,
                "case_number": case_number,
                "opinion_file": opinion_file_str,
                "source": source,
                "wniosek_fragment": fragment,
                "label": label or "",
                "raw_model_output": raw_output,
            }
            results.append(row)

    # zapis CSV – zawsze nadpisujemy pełnym aktualnym stanem
    fieldnames = [
        "case_id",
        "case_number",
        "opinion_file",
        "source",
        "wniosek_fragment",
        "label",
        "raw_model_output",
    ]

    with open(OUTPUT_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(results)

    print("\n=== GOTOWE ===")
    print(f"Zapisano etykiety do: {OUTPUT_CSV}")
    print("Możesz uruchomić skrypt ponownie – ponowi tylko te przypadki, które mają ERROR / pusty label.")
    

if __name__ == "__main__":
    main()

import os
import re
import json
from pathlib import Path

from google import genai  # pip install google-genai


SCRIPT_DIR = Path(__file__).resolve().parent
OCR_OUTPUT_DIR = SCRIPT_DIR.parent / "OCR_OUTPUT"
ANALYSIS_DIR = SCRIPT_DIR.parent / "ANALYSIS"
ANALYSIS_DIR.mkdir(exist_ok=True)

API_KEY = os.environ.get("GEMINI_API_KEY", "AIzaSyB9Jt4HfDRF4UNZ8hUTVeRGQqtAN_QvEKI").strip()
GEMINI_MODEL = "gemini-2.5-flash"


# ========= POMOCNICZE: cleaning i anonimizacja =========

def basic_clean(text: str) -> str:
    if not isinstance(text, str):
        return ""
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def anonymize_text(text: str) -> str:
    """
    Bardzo prosta anonimizacja:
    - PESEL / numery 11 cyfr -> [PESEL]
    - maile -> [EMAIL]
    - tel. 9+ cyfr -> [TEL]
    - imię i nazwisko w formacie 'Imię Nazwisko' -> [OSOBA_X]
    To nie jest perfekcyjne RODO, ale już mocno ogranicza wrażliwość.
    """
    t = text

    # PESEL / inne 11-cyfrowe
    t = re.sub(r"\b\d{11}\b", "[PESEL]", t)

    # maile
    t = re.sub(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", "[EMAIL]", t)

    # telefony (9+ cyfr)
    t = re.sub(r"\b\d{9,}\b", "[TEL]", t)

    # bardzo prosty wzorzec na "Imię Nazwisko"
    # (zostawiamy tylko 1. wystąpienia na osoby)
    pattern_name = re.compile(r"\b([A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+)\s+([A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+)\b")
    seen = {}
    counter = 1

    def repl(m):
        nonlocal counter
        full = m.group(0)
        if full in seen:
            return seen[full]
        tag = f"[OSOBA_{counter}]"
        counter += 1
        seen[full] = tag
        return tag

    t = pattern_name.sub(repl, t)

    return t


def collect_case_text(case_id: str) -> str:
    """
    Zbiera tekst ze wszystkich plików TXT w OCR_OUTPUT/case_id,
    Z WYJĄTKIEM:
      - 'opinia*.txt'
      - 'karta' + 'wypadku' (karta wypadku – ma być OUTPUT, nie INPUT).
    """
    case_dir = OCR_OUTPUT_DIR / case_id
    if not case_dir.exists():
        raise FileNotFoundError(f"Brak katalogu sprawy: {case_dir}")

    parts = []

    for txt_path in sorted(case_dir.glob("*.txt")):
        name_lower = txt_path.name.lower()
        if name_lower.startswith("opinia"):
            continue
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
        raise ValueError(f"W {case_id} nie ma tekstu wejściowego (poza opinią/kartą).")
    return full_text


# ========= PROMPT DO GEMINI: spełnia punkty 2–6 =========

def build_case_prompt(case_id: str, case_text: str) -> str:
    """
    Tu kodujemy wymagania:
    2) spójność danych,
    3) elementy definicji wypadku,
    4) wątpliwości + brakujące dokumenty,
    5) wątpliwości co do urazu -> Główny Lekarz Orzecznik,
    6) projekt opinii (stanowisko + uzasadnienie + kwestie + wniosek).
    """
    return f"""
Jesteś systemem wspomagającym pracownika ZUS przy kwalifikacji zdarzenia jako wypadku przy pracy.

Pracujesz na zanonimizowanych dokumentach: zawiadomienie o wypadku, wyjaśnienia poszkodowanego, informacje o świadkach,
inne dokumenty techniczne. Twoim zadaniem jest analiza konkretnej sprawy o ID: {case_id}.

TEKST SPRAWY (z OCR, może mieć literówki; oddzielne pliki oznaczone nagłówkiem '===== PLIK: ... ====='):

\"\"\" 
{case_text}
\"\"\"

Twoje zadania:

1. Wydobądź podstawowe dane z dokumentów:
   - data_zdarzenia (jeśli kilka – wskaż wszystkie i te, które są sprzeczne),
   - godzina_zdarzenia,
   - miejsce_zdarzenia,
   - dane_poszkodowanego (tylko opisowo: np. [OSOBA_1], stanowisko, rodzaj umowy),
   - dane_swiadkow (lista opisów: [OSOBA_X], rola, co widzieli),
   - opis_zdarzenia (krótki, 3–5 zdań, streszczający przebieg),
   - rodzaj_urazu (opisowo, np. „skręcenie stawu skokowego prawego”),
   - dokumenty_medyczne (jakie są wspomniane: np. „karta informacyjna SOR”, „opis RTG” itd.).

2. Sprawdź spójność:
   - czy data, godzina, miejsce w różnych dokumentach są zgodne?
   - czy opis okoliczności w różnych dokumentach jest zasadniczo spójny?
   W razie rozbieżności wypisz je w polu `rozbieznosci` jako listę opisów.

3. Oceń, czy zdarzenie spełnia elementy definicji wypadku przy pracy:
   - nagłość (nagłe / wątpliwe / brak danych),
   - przyczyna zewnętrzna (tak / nie / wątpliwe),
   - skutek w postaci urazu (tak / nie / wątpliwe),
   - związek z pracą (tak / nie / wątpliwe) – czy zdarzenie miało miejsce podczas wykonywania zwykłych obowiązków, w związku z działalnością itd.
   Dla każdego elementu podaj `ocena` i `uzasadnienie`.

4. Wskaż wątpliwości i braki:
   - jeśli brakuje istotnych informacji (np. brak daty, brak jednoznacznego opisu przyczyny, brak dokumentacji medycznej),
     wypisz w polu `braki` listę brakujących informacji/dokumentów oraz propozycję: co należy pozyskać w postępowaniu wyjaśniającym
     (np. „dopytać poszkodowanego o przebieg zdarzenia”, „pozyskać kartę informacyjną z SOR”, „pozyskać pisemne oświadczenie świadka X” itd.).

5. Oceń, czy opis urazu i przebieg zdarzenia są na tyle jasne, że można ocenić związek urazu ze zdarzeniem,
   czy też należy wystąpić o opinię Głównego Lekarza Orzecznika ZUS:
   - pole `czy_potrzebna_opinia_lekarza` (true/false),
   - pole `uzasadnienie_potrzebnej_opinii` (jeśli true, krótko dlaczego).

6. Opracuj projekt opinii w sprawie kwalifikacji prawnej wypadku:
   - `stanowisko`:
       - "wypadek przy pracy – uznać" LUB
       - "wypadek przy pracy – nie uznać"
   - `uzasadnienie_prawne`: spójne, kilkupunktowe uzasadnienie decyzji, odnoszące się do elementów definicji (nagłość, przyczyna zewnętrzna, uraz, związek z pracą).
   - `kwestie_do_rozstrzygniecia`: lista kwestii, które pozostają niejasne, jeśli takie są.
   - `wniosek`: krótko sformułowany wniosek na końcu opinii (np. „Wnoszę o uznanie zdarzenia z dnia ... za wypadek przy pracy w rozumieniu art. 3 ustawy ...”).

Odpowiedz wyłącznie w postaci poprawnego JSON-a (bez komentarzy, bez zbędnego tekstu) o strukturze:

{{
  "case_id": "...",
  "dane_podstawowe": {{
    "data_zdarzenia": ["..."],
    "godzina_zdarzenia": ["..."],
    "miejsce_zdarzenia": ["..."],
    "dane_poszkodowanego": "...",
    "dane_swiadkow": ["..."],
    "opis_zdarzenia": "...",
    "rodzaj_urazu": "...",
    "dokumenty_medyczne": ["..."]
  }},
  "rozbieznosci": [
    "opis rozbieznosci 1",
    "opis rozbieznosci 2"
  ],
  "ocena_definicji": {{
    "naglosc": {{
      "ocena": "nagłe / wątpliwe / brak danych",
      "uzasadnienie": "..."
    }},
    "przyczyna_zewnetrzna": {{
      "ocena": "tak / nie / wątpliwe",
      "uzasadnienie": "..."
    }},
    "uraz": {{
      "ocena": "tak / nie / wątpliwe",
      "uzasadnienie": "..."
    }},
    "zwiazek_z_praca": {{
      "ocena": "tak / nie / wątpliwe",
      "uzasadnienie": "..."
    }}
  }},
  "braki": [
    "opis brakującego dokumentu / informacji i proponowane działanie"
  ],
  "czy_potrzebna_opinia_lekarza": true,
  "uzasadnienie_potrzebnej_opinii": "...",
  "projekt_opinii": {{
    "stanowisko": "wypadek przy pracy – uznać" ,
    "uzasadnienie_prawne": "tekst...",
    "kwestie_do_rozstrzygniecia": ["..."],
    "wniosek": "tekst wniosku..."
  }}
}}
""".strip()


def robust_parse_json(raw_output: str):
    raw = (raw_output or "").strip()
    if not raw:
        return None

    # najpierw spróbuj bezpośrednio
    try:
        return json.loads(raw)
    except Exception:
        pass

    # spróbuj wyciągnąć pierwszy blok {...}
    m = re.search(r"\{.*\}", raw, flags=re.DOTALL)
    if m:
        candidate = m.group(0)
        try:
            return json.loads(candidate)
        except Exception:
            fixed = candidate.replace("'", '"')
            try:
                return json.loads(fixed)
            except Exception:
                return None
    return None


def analyze_case_with_gemini(case_id: str, anonymize: bool = True) -> dict:
    if not API_KEY:
        raise RuntimeError("Brak GEMINI_API_KEY – ustaw zmienną środowiskową.")

    client = genai.Client(api_key=API_KEY)

    text = collect_case_text(case_id)
    if anonymize:
        text_for_model = anonymize_text(text)
    else:
        text_for_model = text

    prompt = build_case_prompt(case_id, text_for_model)

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
    )
    raw_output = (response.text or "").strip()

    data = robust_parse_json(raw_output)
    if data is None:
        raise ValueError(f"Nie udało się sparsować JSON z odpowiedzi dla sprawy {case_id}.")

    return data


# ========= GENEROWANIE OPINII I KARTY WYPADKU =========

def save_analysis_outputs(case_id: str, analysis: dict):
    case_dir = ANALYSIS_DIR / case_id
    case_dir.mkdir(exist_ok=True)

    # 1) surowy JSON z analizy
    json_path = case_dir / "analysis.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2)

    # 2) projekt opinii (ładny tekst)
    opinion_txt = build_opinion_text(analysis)
    opinion_path = case_dir / "projekt_opinii.txt"
    opinion_path.write_text(opinion_txt, encoding="utf-8")

    # 3) projekt karty wypadku (pola uporządkowane)
    card_txt = build_karta_wypadku_text(analysis)
    card_path = case_dir / "projekt_karty_wypadku.txt"
    card_path.write_text(card_txt, encoding="utf-8")

    print(f"[ANALIZA] Zapisano analysis.json, projekt_opinii.txt i projekt_karty_wypadku.txt w {case_dir}")


def build_opinion_text(analysis: dict) -> str:
    """
    Tekstowy szkic opinii prawnej na podstawie analysis["projekt_opinii"] i reszty pól.
    """
    case_id = analysis.get("case_id", "")
    dane = analysis.get("dane_podstawowe", {})
    ocena = analysis.get("ocena_definicji", {})
    proj = analysis.get("projekt_opinii", {})

    # krótki opis faktów
    opis = dane.get("opis_zdarzenia", "")
    data_zdarzenia = ", ".join(dane.get("data_zdarzenia", []) or [])
    miejsce = ", ".join(dane.get("miejsce_zdarzenia", []) or [])

    stanowisko = proj.get("stanowisko", "")
    uzasadnienie_prawne = proj.get("uzasadnienie_prawne", "")
    kwestie = proj.get("kwestie_do_rozstrzygniecia", []) or []
    wniosek = proj.get("wniosek", "")

    lines = []
    lines.append(f"OPINIA W SPRAWIE PRAWNEJ KWALIFIKACJI WYPADKU – {case_id}")
    lines.append("")
    lines.append("I. Stan faktyczny")
    lines.append("-----------------")
    if data_zdarzenia:
        lines.append(f"Data zdarzenia: {data_zdarzenia}")
    if miejsce:
        lines.append(f"Miejsce zdarzenia: {miejsce}")
    if opis:
        lines.append("")
        lines.append("Opis zdarzenia (skrót):")
        lines.append(opis)

    lines.append("")
    lines.append("II. Ocena elementów definicji wypadku przy pracy")
    lines.append("-----------------------------------------------")
    for key, label in [
        ("naglosc", "Nagłość zdarzenia"),
        ("przyczyna_zewnetrzna", "Przyczyna zewnętrzna"),
        ("uraz", "Skutek w postaci urazu"),
        ("zwiazek_z_praca", "Związek z pracą"),
    ]:
        el = ocena.get(key, {}) or {}
        lines.append(f"{label}: {el.get('ocena', 'brak danych')}")
        uz = el.get("uzasadnienie", "")
        if uz:
            lines.append(f"Uzasadnienie: {uz}")
        lines.append("")

    lines.append("III. Uzasadnienie prawne")
    lines.append("------------------------")
    lines.append(uzasadnienie_prawne or "(brak treści – uzupełnić ręcznie)")

    if kwestie:
        lines.append("")
        lines.append("IV. Kwestie wymagające dalszego wyjaśnienia")
        lines.append("-------------------------------------------")
        for k in kwestie:
            lines.append(f"- {k}")

    lines.append("")
    lines.append("V. Wniosek")
    lines.append("----------")
    if stanowisko:
        lines.append(f"Stanowisko: {stanowisko}")
    if wniosek:
        lines.append("")
        lines.append(wniosek)

    return "\n".join(lines)


def build_karta_wypadku_text(analysis: dict) -> str:
    """
    Prosty tekstowy szkic Karty Wypadku zgodnie z zakresem danych z rozporządzenia.
    To NIE jest idealne odwzorowanie formularza, ale pola są mapowane logicznie.
    """
    dane = analysis.get("dane_podstawowe", {})
    podstawowe = []
    podstawowe.append("KARTA WYPADKU – PROJEKT")
    podstawowe.append("----------------------------------------")
    podstawowe.append("")
    podstawowe.append("A. Dane identyfikacyjne zdarzenia")
    podstawowe.append(f"1. Data zdarzenia: {', '.join(dane.get('data_zdarzenia', []) or [])}")
    podstawowe.append(f"2. Godzina zdarzenia: {', '.join(dane.get('godzina_zdarzenia', []) or [])}")
    podstawowe.append(f"3. Miejsce zdarzenia: {', '.join(dane.get('miejsce_zdarzenia', []) or [])}")
    podstawowe.append("")
    podstawowe.append("B. Dane poszkodowanego")
    podstawowe.append(f"1. Dane poszkodowanego (zanonimizowane): {dane.get('dane_poszkodowanego', '')}")
    podstawowe.append("")
    podstawowe.append("C. Okoliczności i przyczyny wypadku")
    podstawowe.append(f"1. Opis zdarzenia: {dane.get('opis_zdarzenia', '')}")
    podstawowe.append(f"2. Dane świadków: {', '.join(dane.get('dane_swiadkow', []) or [])}")
    podstawowe.append("")
    podstawowe.append("D. Skutki wypadku")
    podstawowe.append(f"1. Rodzaj urazu: {dane.get('rodzaj_urazu', '')}")
    podstawowe.append(f"2. Dokumenty medyczne: {', '.join(dane.get('dokumenty_medyczne', []) or [])}")
    podstawowe.append("")
    podstawowe.append("E. Kwalifikacja prawna zdarzenia")
    ocena = analysis.get("ocena_definicji", {})
    for key, label in [
        ("naglosc", "Nagłość zdarzenia"),
        ("przyczyna_zewnetrzna", "Przyczyna zewnętrzna"),
        ("uraz", "Skutek w postaci urazu"),
        ("zwiazek_z_praca", "Związek z pracą"),
    ]:
        el = ocena.get(key, {}) or {}
        podstawowe.append(f"- {label}: {el.get('ocena', 'brak danych')} (uzasadnienie: {el.get('uzasadnienie', '')})")

    proj = analysis.get("projekt_opinii", {})
    podstawowe.append("")
    podstawowe.append("F. Ustalenia podmiotu sporządzającego kartę")
    podstawowe.append(f"1. Stanowisko: {proj.get('stanowisko', '')}")
    podstawowe.append(f"2. Wniosek: {proj.get('wniosek', '')}")
    podstawowe.append("")
    podstawowe.append("G. Uwagi / kwestie do wyjaśnienia")
    for k in proj.get("kwestie_do_rozstrzygniecia", []) or []:
        podstawowe.append(f"- {k}")

    return "\n".join(podstawowe)


# ========= CLI: analiza pojedynczej sprawy =========

def main():
    import argparse

    parser = argparse.ArgumentParser(
        description="Analiza sprawy ZUS wypadek przy pracy: spójność dokumentów, definicja wypadku, projekt opinii i karty wypadku."
    )
    parser.add_argument(
        "--case-id",
        type=str,
        required=True,
        help='Nazwa folderu sprawy w OCR_OUTPUT, np. "wypadek 23".'
    )
    parser.add_argument(
        "--no-anonymize",
        action="store_true",
        help="Nie anonimizuj tekstu przed wysłaniem do Gemini (domyślnie: anonimizuje)."
    )

    args = parser.parse_args()
    case_id = args.case_id
    anonymize = not args.no_anonymize

    analysis = analyze_case_with_gemini(case_id, anonymize=anonymize)
    save_analysis_outputs(case_id, analysis)


if __name__ == "__main__":
    main()

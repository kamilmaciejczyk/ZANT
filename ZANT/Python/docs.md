1. Cel narzędzia

Celem systemu jest wspieranie pracowników ZUS w obsłudze spraw wypadków przy pracy, w szczególności:

wspomaganie decyzji o uznaniu bądź nieuznaniu zdarzenia za wypadek przy pracy (projekt opinii),

przygotowanie projektu karty wypadku,

wskazanie rozbieżności i braków w dokumentacji,

wsparcie w ocenie, czy spełnione są elementy definicji wypadku przy pracy (nagłość, przyczyna zewnętrzna, uraz, związek z pracą),

sygnalizowanie potrzeby opinii Głównego Lekarza Orzecznika ZUS.

System działa na:

zanonimizowanych skanach dokumentów (PDF):
zawiadomienie o wypadku, wyjaśnienia poszkodowanego, opinia, karta wypadku,

danych po lokalnym OCR,

lokalnym modelu ML (TF-IDF + Logistic Regression),

modelu językowym Gemini (tylko dla zanonimizowanych tekstów i/lub fragmentów bez danych osobowych).

2. Architektura – przegląd

Główne komponenty:

OCR (PDF → TXT)

Skrypty: część pipeline_master.py (krok 1), wcześniejszy OCR.

Technologia: pypdfium2 + RapidOCR.

Efekt: OCR_OUTPUT/wypadek XX/*.txt.

Etykietowanie (wniosek uznany / nieuznany)

Skrypt: opinie_validation.py lub krok 2 w pipeline_master.py.

Model: Gemini (google-genai).

Input: tekst z opinii (sekcja Wniosek lub cała opinia).

Output: OCR_OUTPUT/gemini_wnioski_labels.csv.

Trenowanie lokalnego modelu ML

Skrypt: model_training.py (w kodzie nazywałeś go np. model_trainign.py) lub krok 3 w pipeline_master.py.

Dane:

X: teksty z pozostałych plików w sprawie (wyjaśnienia, zawiadomienie, inne),
bez opinia*.txt i bez karta wypadku*.txt,

y: etykiety z Geminiego (wniosek uznany / wniosek nieuznany).

Model: TfidfVectorizer + LogisticRegression.

Output: MODELS/wniosek_model.joblib.

Predykcja (scoring)

Skrypt: predictor.py.

Input:

ścieżka do modelu,

albo katalog sprawy (wypadek XX w OCR_OUTPUT),

albo pojedynczy plik .txt z pełnym opisem.

Output: tekst typu:
AI jest w XX.X% pewien, że to był wypadek przy pracy!.

Analiza sprawy i projekt opinii/karty wypadku

Skrypt: zus_case_analyzer.py.

Input: katalog OCR_OUTPUT/wypadek XX.

Output:

ANALYSIS/wypadek XX/analysis.json – szczegółowa struktura,

ANALYSIS/wypadek XX/projekt_opinii.txt,

ANALYSIS/wypadek XX/projekt_karty_wypadku.txt.

Master pipeline

Skrypt: pipeline_master.py.

Spina kroki:

OCR,

labeling,

trening,

(opcjonalnie) batch predykcję.

3. Struktura katalogów

Przyjęta struktura projektu:

HackNation/
 ├─ PDF/
 │   ├─ wypadek 10/
 │   │   ├─ karta wypadku 10.pdf
 │   │   ├─ opinia 10.pdf
 │   │   ├─ wyjaśnienia poszkodowanego 10.pdf
 │   │   └─ zawiadomienie o wypadku 10.pdf
 │   └─ wypadek XX/...
 │
 ├─ OCR_OUTPUT/
 │   ├─ wypadek 10/
 │   │   ├─ karta wypadku 10 10.txt
 │   │   ├─ opinia 10.txt
 │   │   ├─ wyjaśnienia poszkodowanego 10.txt
 │   │   └─ zawiadomienie o wypadku 10.txt
 │   ├─ gemini_wnioski_labels.csv
 │   └─ ...
 │
 ├─ MODELS/
 │   └─ wniosek_model.joblib
 │
 ├─ ANALYSIS/
 │   └─ wypadek 10/
 │       ├─ analysis.json
 │       ├─ projekt_opinii.txt
 │       └─ projekt_karty_wypadku.txt
 │
 └─ Python/
     ├─ OCR.py                  (stare / pomocnicze)
     ├─ opinie_validation.py    (etykietowanie Geminim)
     ├─ model_training.py       (trenowanie ML)
     ├─ predictor.py            (scoring pojedynczej sprawy)
     ├─ pipeline_master.py      (master pipeline)
     └─ zus_case_analyzer.py    (analiza sprawy, opinia, karta)

4. Wymagania i instalacja
4.1. Środowisko

Python 3.11+ / 3.13 (u Ciebie 3.13 na Windows/ARM),

Windows na architekturze ARM,

dostęp do internetu tylko dla kroków z Gemini.

4.2. Biblioteki Python

Przykładowy zestaw:

pip install pypdfium2 rapidocr onnxruntime google-genai scikit-learn pandas joblib


Dodatkowo, jeśli potrzeba:

pip install python-dotenv

4.3. Konfiguracja zmiennych środowiskowych

Gemini wymaga klucza API:

setx GEMINI_API_KEY "TWÓJ_KLUCZ_GEMINI"


Po ustawieniu otwórz nowy terminal, żeby zmienna była widoczna.

5. Moduł OCR – PDF → TXT
5.1. Gdzie jest logika?

Zaimplementowana w pipeline_master.py w funkcjach:

ocr_pdf(pdf_path),

ocr_page_image(image),

step1_run_ocr().

Wcześniejszy OCR (OCR.py) był oparty o Tesseract i został zastąpiony szybszym rozwiązaniem (RapidOCR + pypdfium2).

5.2. Działanie

Skrypt przechodzi po PDF/wypadek XX.

Dla każdego PDF:

renderuje strony do obrazów (pypdfium2),

czyta tekst z każdej strony (RapidOCR),

filtruje linie po score >= MIN_SCORE,

składa pełen tekst pliku,

zapisuje .txt w OCR_OUTPUT/wypadek XX/.

Jednocześnie tworzy zbiorczy CSV z wynikami OCR (opcjonalnie).

5.3. Parametry

W pipeline_master.py:

MIN_SCORE – minimalny score z RapidOCR (np. 0.70),

RENDER_SCALE – skala renderowania PDF (np. 2.0),

mapowanie nazw PDF → nazwy TXT w infer_output_filename():

zawiera opinia → opinia {nr}.txt,

zawiera wyja / poszkodowan → wyjaśnienia poszkodowanego {nr}.txt,

zawiera zawiadom → zawiadomienie o wypadku {nr}.txt,

reszta Idzie jako "{stem} {nr}.txt".

5.4. Uruchamianie (w ramach master pipeline)
& python "C:\...\Python\pipeline_master.py"


W pliku:

RUN_OCR = True

6. Etykietowanie (Gemini) – opinie_validation.py / krok 2
6.1. Cel

Odczytać z opinii tekst sekcji „Wniosek:” (lub całą opinię, jeśli OCR nie wyłapał nagłówka),

wysłać fragment do modelu Gemini,

otrzymać label:

"wniosek uznany"

"wniosek nieuznany"

zapisać wyniki w OCR_OUTPUT/gemini_wnioski_labels.csv.

6.2. Najważniejsze funkcje

extract_wniosek_fragment(text):

szuka w tekście Wniosek: → Uzasadnienie:,

jeśli nie znajdzie Wniosek, zwraca None, wtedy używana jest cała opinia.

build_prompt(fragment):

tworzy prompt dla Gemini, prosząc o zwrot czystego JSON z polem label.

classify_fragment_with_gemini(fragment):

wysyła prompt do Gemini,

próbuje sparsować odpowiedź (robust_parse_label),

w razie błędów sieciowych zwraca label=None i raw_output="ERROR ...".

load_existing_results():

wczytuje wcześniejsze wyniki,

pozwala wznawiać etykietowanie tylko dla błędnych/przerwanych przypadków.

main() / step2_run_labeling():

przechodzi po OCR_OUTPUT/wypadek XX,

bierze opinia*.txt,

klasyfikuje,

zapisuje CSV.

6.3. Output CSV: gemini_wnioski_labels.csv

Kolumny:

case_id – np. wypadek 23,

case_number – np. 23,

opinion_file – ścieżka do opinia XX.txt,

source – wniosek_fragment / full_opinion,

wniosek_fragment – fragment tekstu wysłany do Gemini,

label – wniosek uznany / wniosek nieuznany / puste przy błędach,

raw_model_output – pełna odpowiedź Gemini, lub ERROR during Gemini call: ....

7. Trenowanie modelu ML – model_training.py / krok 3
7.1. Cel

Zbudować lokalny, szybki model przewidujący:

czy wniosek zostanie uznany (1),

czy wniosek nie zostanie uznany (0),

tylko na podstawie tekstów z dokumentów:

zawiadomienie o wypadku,

wyjaśnienia poszkodowanego,

inne opisy (bez opinii i bez karty wypadku).

7.2. Budowa datasetu

Funkcja build_training_dataset():

Wczytuje gemini_wnioski_labels.csv:

filtruje tylko wiersze, gdzie:

label ∈ {wniosek uznany, wniosek nieuznany},

raw_model_output nie zaczyna się od ERROR ....

Dla każdego case_id:

collect_case_text(case_id) łączy wszystkie *.txt w OCR_OUTPUT/wypadek XX/, z wyłączeniem:

opinia*.txt,

plików zawierających w nazwie karta i wypadku,

tekst jest czyszczony (usuwane nadmiarowe entery, spacje, nagłówki stron),

label y = 1, jeśli wniosek uznany, inaczej 0.

Wynik: DataFrame z kolumnami:

case_id,

text,

label (0/1).

7.3. Model

W step3_train_model():

Train/test split: test_size=0.2, stratify=y, random_state=42.

Pipeline:

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


Wyświetla:

liczebność klas,

classification_report,

confusion_matrix.

Zapisuje model:
MODELS/wniosek_model.joblib (przez joblib.dump).

8. Predykcja – predictor.py
8.1. Cel

Dla nowej sprawy:

zebrać tekst z dokumentów (tak jak przy trenowaniu),

podać do wniosek_model.joblib,

wyświetlić wynik w postaci:

AI jest w XX.X% pewien, że to był wypadek przy pracy!

gdzie klasa 1 = „wniosek uznany”.

8.2. Wejścia

Skrypt przyjmuje argumenty CLI:

--model-path (wymagany) – ścieżka do modelu .joblib,

dokładnie jedno z:

--case-dir – katalog analogiczny do OCR_OUTPUT/wypadek XX,

--text-file – pojedynczy .txt z pełnym opisem zdarzenia.

8.3. Zbieranie tekstu

Jeśli używasz --case-dir:

collect_case_text_from_dir(case_dir):

wczytuje wszystkie *.txt w katalogu,

pomija:

opinia*.txt,

karta wypadku*.txt (rozpoznane po karta + wypadku w nazwie),

czyści tekst (tak samo jak w treningu),

skleja w jeden string.

Jeśli używasz --text-file:

skrypt po prostu czyta zawartość pliku i czyści.

8.4. Predykcja

predict_from_text(model, text):

predict_proba([text])[0][1] → prawdopodobieństwo klasy 1 (wniosek uznany),

drukuje:

AI jest w XX.X% pewien, że to był wypadek przy pracy!
(Prawdopodobieństwo, że to NIE był wypadek przy pracy: YY.Y% )

9. Master pipeline – pipeline_master.py
9.1. Cel

Jeden skrypt, który umie:

wykonać OCR (jeśli potrzeba),

etykietowanie Geminim,

trening lokalnego modelu,

(opcjonalnie) batch predykcję dla wszystkich spraw.

9.2. Flagi sterujące

Na górze pliku:

RUN_OCR = True
RUN_LABELING = True
RUN_TRAIN = True
RUN_BATCH_PREDICT = False   # ustaw na True, jeśli chcesz hurtową predykcję

9.3. Krok 1 – step1_run_ocr(force=False)

Jeśli w OCR_OUTPUT są już .txt i force=False → OCR jest pomijany.

Jeśli force=True → zawsze OCR-uje od nowa.

9.4. Krok 2 – step2_run_labeling()

Jak w opinie_validation.py:

bierze opinia*.txt,

używa Gemini,

wznowienia: ponownie woła tylko przypadki z ERROR lub pustym label.

9.5. Krok 3 – step3_train_model()

Jak w model_training.py.

9.6. Krok 4 – step4_batch_predict() (opcjonalny)

Wczytuje model z MODELS/wniosek_model.joblib,

przechodzi po wszystkich katalogach OCR_OUTPUT/wypadek XX,

dla każdego robi:

collect_case_text(case_id),

predict_proba,

zapisuje CSV: OCR_OUTPUT/predictions_batch.csv, kolumny:

case_id,

prob_wniosek_uznany,

prob_wniosek_nieuznany.

10. Analiza sprawy i projekt opinii/karty – zus_case_analyzer.py

Ten skrypt odpowiada za punkty 2–7 z opisu wymagań ZUS.

10.1. Wejście

Argumenty CLI:

--case-id (wymagany) – np. "wypadek 23" (nazwa folderu w OCR_OUTPUT),

--no-anonymize (opcjonalny) – jeśli podany, nie anonimizuje tekstu dla Gemini
(domyślnie – anonimizacja włączona).

Skrypt:

zbiera tekst z OCR_OUTPUT/{case_id}/:

wszystkie .txt poza:

opinia*.txt,

karta wypadku*.txt,

czyści tekst (jak wcześniej),

anonimizuje go (maskuje PESEL, telefony, maile, „Imię Nazwisko” → [OSOBA_X]),

buduje rozbudowany prompt (build_case_prompt),

wysyła go do Gemini i oczekuje czystego JSON o zadanej strukturze.

10.2. Struktura JSON z analizy

Przykładowy schemat (nieco uproszczony):

{
  "case_id": "wypadek 23",
  "dane_podstawowe": {
    "data_zdarzenia": ["2024-10-10"],
    "godzina_zdarzenia": ["13:45"],
    "miejsce_zdarzenia": ["hala produkcyjna, linia 3"],
    "dane_poszkodowanego": "[OSOBA_1], operator produkcji, umowa o pracę",
    "dane_swiadkow": [
      "[OSOBA_2], operator, widział upadek",
      "[OSOBA_3], kontroler, wskazała na przewód w przejściu"
    ],
    "opis_zdarzenia": "skrótowy opis przebiegu zdarzenia...",
    "rodzaj_urazu": "skręcenie stawu barkowego lewego",
    "dokumenty_medyczne": [
      "karta informacyjna SOR",
      "opis badania RTG"
    ]
  },
  "rozbieznosci": [
    "W zawiadomieniu data 10.10.2024, w wyjaśnieniach 11.10.2024.",
    "Różne opisy miejsca zdarzenia: hala A vs hala B."
  ],
  "ocena_definicji": {
    "naglosc": {
      "ocena": "nagłe",
      "uzasadnienie": "opisuje jednorazowe, nagłe potknięcie i upadek"
    },
    "przyczyna_zewnetrzna": {
      "ocena": "tak",
      "uzasadnienie": "przewód leżący w przejściu"
    },
    "uraz": {
      "ocena": "tak",
      "uzasadnienie": "stwierdzony uraz barku w dokumentacji medycznej"
    },
    "zwiazek_z_praca": {
      "ocena": "tak",
      "uzasadnienie": "zdarzenie podczas wykonywania zwykłych obowiązków"
    }
  },
  "braki": [
    "Brak kopii karty informacyjnej z SOR – pozyskać.",
    "Brak pisemnego oświadczenia świadka [OSOBA_2] – pozyskać w ramach postępowania."
  ],
  "czy_potrzebna_opinia_lekarza": false,
  "uzasadnienie_potrzebnej_opinii": "",
  "projekt_opinii": {
    "stanowisko": "wypadek przy pracy – uznać",
    "uzasadnienie_prawne": "szczegółowe, kilkupunktowe uzasadnienie...",
    "kwestie_do_rozstrzygniecia": [],
    "wniosek": "Wnoszę o uznanie zdarzenia z dnia ... za wypadek przy pracy..."
  }
}

10.3. Wyjścia

Funkcja save_analysis_outputs(case_id, analysis) tworzy:

ANALYSIS/{case_id}/analysis.json
→ pełne dane, łatwe do dalszego przetwarzania.

ANALYSIS/{case_id}/projekt_opinii.txt
→ generowany w build_opinion_text(analysis) – zawiera:

nagłówek z case_id,

Stan faktyczny – data, miejsce, opis,

Ocena elementów definicji – nagłość, przyczyna, uraz, związek z pracą,

Uzasadnienie prawne – długi tekst z uzasadnienie_prawne,

Kwestie do rozstrzygnięcia – lista,

Wniosek – stanowisko i końcowy wniosek.

ANALYSIS/{case_id}/projekt_karty_wypadku.txt
→ generowany w build_karta_wypadku_text(analysis) – sekcje A–G:

A. Dane zdarzenia – data, godzina, miejsce,

B. Dane poszkodowanego,

C. Okoliczności i przyczyny,

D. Skutki i dokumentacja medyczna,

E. Kwalifikacja prawna (ocena definicji),

F. Ustalenia (stanowisko, wniosek),

G. Uwagi / kwestie do wyjaśnienia.

10.4. Uruchomienie
cd "C:\Users\frani\OneDrive\Dokumenty\HackNation\Python"

& python zus_case_analyzer.py --case-id "wypadek 23"


lub bez anonimizacji (do testów):

& python zus_case_analyzer.py --case-id "wypadek 23" --no-anonymize

11. Bezpieczeństwo i RODO

OCR, trenowanie modelu ML, predykcje – działają w całości lokalnie, bez wysyłania danych na zewnątrz.

Gemini:

do labeling (wniosków) wysyłany jest tylko fragment opinii (sekcja Wniosek),

w zus_case_analyzer.py domyślnie działa anonimizacja:

maskuje PESEL, telefony, maile,

zamienia imiona i nazwiska na [OSOBA_X].

Dla pełnej zgodności z RODO w realnym wdrożeniu:

albo trzeba:

zagwarantować, że przekazywane teksty są zanonimizowane,

mieć odpowiednie umowy powierzenia z dostawcą LLM,

albo użyć lokalnego modelu językowego (np. PLLuM lub inny on-prem LLM) zamiast Gemini.

12. Ograniczenia i kierunki rozwoju

Obecnie model ML bazuje na kilkudziesięciu sprawach → statystycznie to mało, szczególnie dla klasy wniosek nieuznany.
→ warto zbierać więcej przykładów, zwłaszcza odmów.

Model jest:

klasyczny (TF-IDF + LogReg) – zalety: szybki, łatwo wdrożyć, małe wymagania,

można go w przyszłości zastąpić/fine-tunować modelem typu BERT/HerBERT/plBERT.

projekt_karty_wypadku.txt jest tekstową reprezentacją pól – kolejnym krokiem może być:

generowanie wypełnionego PDF/DOCX na bazie oficjalnego wzoru z rozporządzenia.

System ma charakter wspierający:

nie zastępuje decyzji pracownika ZUS,

dostarcza wstępny projekt opinii, karty, listę braków i rozbieżności.
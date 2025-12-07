# Integracja modelu ML dla scoringu zgłoszeń EWYP

## Przegląd

Zaimplementowano integrację modelu Machine Learning do automatycznego scoringu zgłoszeń wypadków przy pracy (EWYP). System wywołuje skrypt Python `predictor.py`, który używa wytrenowanego modelu do predykcji. Model jest uruchamiany automatycznie przy zapisie zgłoszenia (wywołanie `submitReport`) i zapisuje wynik scoringu w tabeli `ewyp_reports` w kolumnie `scoring_classification`.

## Architektura rozwiązania

Backend Java → wywołuje proces Python → `predictor.py` → używa modelu `.joblib` → zwraca predykcję

## Zmiany w projekcie

### 1. Utworzono serwis `OnnxScoringService`
**Lokalizacja:** `backend/src/main/java/com/zant/backend/service/OnnxScoringService.java`

**Funkcjonalność:**
- Sprawdza przy starcie aplikacji czy Python3 jest dostępny
- Weryfikuje istnienie skryptu `predictor.py` i modelu `.joblib`
- Ekstraktuje tekst z obiektu `EWYPReport` (okoliczności wypadku, dane poszkodowanego, etc.)
- Tworzy tymczasowy plik z tekstem
- Wywołuje skrypt Python z odpowiednimi parametrami
- Parsuje output ze skryptu i zwraca wynik w formacie: `"WYPADEK_PRZY_PRACY: XX.X%"`

### 2. Zmodyfikowano `EWYPReportController`
Metoda `submitReport` została rozszerzona o:
```java
// Wykonaj scoring przy użyciu modelu Python
String scoringResult = onnxScoringService.scoreReport(report);
report.setScoringClassification(scoringResult);
```

### 3. Model danych `EWYPReport`
Pole `scoringClassification` przechowuje wynik predykcji.

### 4. Konfiguracja (`application.properties`)
Dodano właściwości:
```properties
python.executable=${PYTHON_EXECUTABLE:python3}
python.predictor.script=${PYTHON_PREDICTOR_SCRIPT:Python/predictor.py}
python.model.path=${PYTHON_MODEL_PATH:MODELS/wniosek_model.joblib}
```

## Wymagania

### 1. Python 3 i zależności
Upewnij się, że Python3 jest zainstalowany i dostępny w PATH:
```bash
python3 --version
```

Zainstaluj wymagane biblioteki Python:
```bash
pip install joblib scikit-learn
```

### 2. Wytrenowany model
Model musi być dostępny w katalogu `MODELS/wniosek_model.joblib`. Model trenuje się przy użyciu skryptu:
```bash
cd Python
python3 model_training.py
```

### 3. Skrypt predykcji
Skrypt `Python/predictor.py` musi być dostępny w projekcie (już jest).

## Jak to działa

### 1. Przygotowanie danych
Serwis Java ekstraktuje z `EWYPReport` następujące dane:
- Dane poszkodowanego (imię, nazwisko, data urodzenia, adres)
- Dane zgłaszającego (jeśli inny niż poszkodowany)
- **Szczegóły wypadku** (data, godzina, miejsce, **okoliczności**, obrażenia)
- Informacje o pierwszej pomocy
- Informacje o maszynie (jeśli dotyczy)
- Świadkowie

Tekst jest formatowany i zapisywany do tymczasowego pliku.

### 2. Wywołanie Pythona
```bash
python3 Python/predictor.py \
  --model-path MODELS/wniosek_model.joblib \
  --text-file /tmp/ewyp_report_XXXXX.txt
```

### 3. Parsowanie wyniku
Skrypt Python zwraca output w formacie:
```
AI jest w 87.5% pewien, że to był wypadek przy pracy!
```

Java parsuje ten output i ekstraktuje wartość procentową, formatując wynik jako:
```
WYPADEK_PRZY_PRACY: 87.5%
```

### 4. Zapis do bazy
Wynik jest zapisywany w polu `scoring_classification` w tabeli `ewyp_reports`.

## Działanie systemu

### Gdy Python i model są dostępne:
1. Przy starcie aplikacji logowane jest: `"Python scoring service zainicjalizowany pomyślnie..."`
2. Przy każdym wywołaniu `POST /api/ewyp-reports/{id}/submit`:
   - System ekstraktuje dane z zgłoszenia
   - Tworzy tymczasowy plik z tekstem
   - Wywołuje `predictor.py`
   - Parsuje wynik
   - Zapisuje w `scoringClassification` (np. "WYPADEK_PRZY_PRACY: 87.5%")
   - Usuwa tymczasowy plik
   - Zwraca zaktualizowane zgłoszenie

### Gdy Python/model nie są dostępne:
1. Przy starcie: `"Python nie jest dostępny. Scoring będzie niedostępny."`
2. Przy submitReport: `scoringClassification` pozostaje `null`
3. System działa normalnie, tylko bez scoringu

## Testowanie

### 1. Wytrenuj model
```bash
cd Python
python3 model_training.py
# Sprawdź czy utworzył się plik MODELS/wniosek_model.joblib
```

### 2. Przetestuj skrypt Python samodzielnie
```bash
cd Python
python3 predictor.py \
  --model-path ../MODELS/wniosek_model.joblib \
  --text-file ../OCR_OUTPUT/wypadek\ 10/zawiadomienie\ o\ wypadku\ 10.txt
```

Powinno wyświetlić się:
```
Źródło danych: plik: ...
AI jest w XX.X% pewien, że to był wypadek przy pracy!
```

### 3. Kompilacja projektu
```bash
cd backend
mvn clean compile
```

### 4. Uruchomienie backendu
```bash
mvn spring-boot:run
```

Sprawdź logi - powinno być:
```
Python scoring service zainicjalizowany pomyślnie. 
Skrypt: Python/predictor.py, Model: MODELS/wniosek_model.joblib
```

### 5. Test API
```bash
# Utwórz draft zgłoszenia
curl -X POST http://localhost:8081/api/ewyp-reports/draft \
  -H "Content-Type: application/json" \
  -d '{
    "injuredPerson": {
      "firstName": "Jan",
      "lastName": "Kowalski"
    },
    "accidentInfo": {
      "circumstancesAndCauses": "Podczas pracy na budowie pracownik spadł z rusztowania z wysokości 3 metrów. Nie był przypięty linką bezpieczeństwa. W wyniku upadku doznał złamania nogi."
    }
  }'

# Zapisz ID z odpowiedzi, następnie:
curl -X POST http://localhost:8081/api/ewyp-reports/{ID}/submit

# Sprawdź wynik:
curl http://localhost:8081/api/ewyp-reports/{ID}
```

W odpowiedzi powinieneś zobaczyć pole `scoringClassification` z wartością jak np.: `"WYPADEK_PRZY_PRACY: 87.5%"`

## Konfiguracja

### Zmienne środowiskowe

Możesz dostosować konfigurację przez zmienne środowiskowe:

```bash
# Ścieżka do interpretera Python (domyślnie: python3)
export PYTHON_EXECUTABLE=/usr/bin/python3.11

# Ścieżka do skryptu predykcji (domyślnie: Python/predictor.py)
export PYTHON_PREDICTOR_SCRIPT=/ścieżka/do/Python/predictor.py

# Ścieżka do modelu (domyślnie: MODELS/wniosek_model.joblib)
export PYTHON_MODEL_PATH=/ścieżka/do/modelu.joblib
```

### W Dockerze

W `docker-compose.yml`:
```yaml
backend:
  environment:
    - PYTHON_EXECUTABLE=python3
    - PYTHON_PREDICTOR_SCRIPT=Python/predictor.py
    - PYTHON_MODEL_PATH=MODELS/wniosek_model.joblib
  volumes:
    - ./Python:/app/Python
    - ./MODELS:/app/MODELS
```

## Struktura danych ekstraktowanych do scoringu

Serwis ekstraktuje następującą strukturę:
```
=== POSZKODOWANY ===
Imię i nazwisko: Jan Kowalski
Data urodzenia: 1980-05-15
PESEL: 80051512345
Adres zamieszkania: ul. Kwiatowa 10, 00-001, Warszawa, Polska

=== SZCZEGÓŁY WYPADKU ===
Data wypadku: 2024-03-15
Godzina wypadku: 10:30
Miejsce wypadku: Budowa przy ul. Polnej 5, Warszawa
Okoliczności i przyczyny wypadku: Podczas pracy na rusztowaniu 
pracownik stracił równowagę i spadł z wysokości 3 metrów...
Opis obrażeń: Złamanie kości piszczelowej prawej nogi...
```

Tekst jest automatycznie czyszczony (usuwane nadmiarowe spacje, puste linie).

## Monitorowanie

Sprawdź logi aplikacji na poziomie DEBUG aby zobaczyć output Pythona:
```properties
# W application.properties
logging.level.com.zant.backend.service.OnnxScoringService=DEBUG
```

Logi pokażą:
- `INFO`: Python scoring service zainicjalizowany pomyślnie
- `WARN`: Python nie jest dostępny / Model nie znaleziony
- `DEBUG`: Python output: ... (każda linia ze skryptu)
- `ERROR`: Błędy podczas wywołania lub parsowania

## Przykładowy output

Po poprawnej integracji, w tabeli `ewyp_reports` zobaczysz:

| id | ... | scoring_classification | status |
|----|-----|------------------------|---------|
| uuid-1 | ... | WYPADEK_PRZY_PRACY: 92.3% | SUBMITTED |
| uuid-2 | ... | WYPADEK_PRZY_PRACY: 35.7% | SUBMITTED |
| uuid-3 | ... | null | DRAFT |

## Troubleshooting

### Problem: "Python nie jest dostępny"
**Rozwiązanie:** 
```bash
# Sprawdź czy Python3 jest zainstalowany
which python3
python3 --version

# Jeśli nie jest zainstalowany
sudo apt install python3
```

### Problem: "Skrypt predykcji nie został znaleziony"
**Rozwiązanie:** 
- Upewnij się, że uruchamiasz backend z głównego katalogu projektu (ZANT)
- Sprawdź czy plik `Python/predictor.py` istnieje
- Ustaw zmienną `PYTHON_PREDICTOR_SCRIPT` na pełną ścieżkę

### Problem: "Model nie został znaleziony"
**Rozwiązanie:** 
```bash
# Wytrenuj model
cd Python
python3 model_training.py

# Sprawdź czy model istnieje
ls -la ../MODELS/wniosek_model.joblib
```

### Problem: "Python predictor zakończył się błędem"
**Przyczyna:** Brak wymaganych bibliotek Python lub błąd w skrypcie
**Rozwiązanie:** 
```bash
# Zainstaluj zależności
pip install joblib scikit-learn

# Przetestuj skrypt bezpośrednio
cd Python
python3 predictor.py --model-path ../MODELS/wniosek_model.joblib --text-file test.txt
```

### Problem: scoringClassification zawsze null
**Przyczyny i rozwiązania:**
1. Backend nie może znaleźć Pythona - sprawdź logi przy starcie
2. Model nie istnieje - wytrenuj model
3. Błąd parsowania outputu - sprawdź logi DEBUG

## Zalety tego podejścia

✅ **Prostota:** Wykorzystuje istniejący skrypt Python bez potrzeby konwersji do ONNX  
✅ **Łatwość aktualizacji:** Wystarczy zaktualizować model `.joblib` i nie trzeba nic zmieniać w Javie  
✅ **Debugging:** Łatwo przetestować skrypt Python osobno  
✅ **Elastyczność:** Można łatwo zmienić skrypt bez rekompilacji Javy  
✅ **Brak dodatkowych zależności:** Nie potrzeba ONNX Runtime w Java

## Frontend

Frontend może wyświetlić wynik scoringu:
```typescript
// W komponencie Angular
if (report.scoringClassification) {
  const match = report.scoringClassification.match(/(\d+\.?\d*)%/);
  if (match) {
    const percentage = parseFloat(match[1]);
    // Pokaż progress bar lub badge z kolorem zależnym od procentu
  }
}
```

Możliwe wizualizacje:
- Progress bar (zielony dla >70%, żółty 40-70%, czerwony <40%)
- Badge z ikoną
- Tooltip z dodatkową interpretacją

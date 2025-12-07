# Integracja modelu ONNX dla scoringu zgłoszeń EWYP

## Przegląd

Zaimplementowano integrację modelu ONNX do automatycznego scoringu zgłoszeń wypadków przy pracy (EWYP). Model jest uruchamiany automatycznie przy zapisie zgłoszenia (wywołanie `submitReport`) i zapisuje wynik scoringu w tabeli `ewyp_reports` w kolumnie `scoring_classification`.

## Zmiany w projekcie

### 1. Dodano zależność ONNX Runtime (`backend/pom.xml`)
```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.3</version>
</dependency>
```

### 2. Utworzono serwis `OnnxScoringService`
**Lokalizacja:** `backend/src/main/java/com/zant/backend/service/OnnxScoringService.java`

**Funkcjonalność:**
- Ładuje model ONNX przy starcie aplikacji
- Ekstraktuje tekst z obiektu `EWYPReport` (okoliczności wypadku, dane poszkodowanego, etc.)
- Wykonuje czyszczenie tekstu (analogicznie do `basic_clean` z Python)
- Przeprowadza predykcję przy użyciu modelu ONNX
- Zwraca wynik w formacie: `"WYPADEK_PRZY_PRACY: XX.X%"`

### 3. Zmodyfikowano `EWYPReportController`
Metoda `submitReport` została rozszerzona o:
```java
// Wykonaj scoring przy użyciu modelu ONNX
String scoringResult = onnxScoringService.scoreReport(report);
report.setScoringClassification(scoringResult);
```

### 4. Model danych `EWYPReport`
Pole `scoringClassification` już istniało w modelu - teraz jest aktywnie wykorzystywane.

### 5. Konfiguracja (`application.properties`)
Dodano właściwość:
```properties
onnx.model.path=${ONNX_MODEL_PATH:models/wniosek_model.onnx}
```

## Jak przygotować model ONNX

### Opcja 1: Konwersja z scikit-learn do ONNX (z TF-IDF)

Aby wyeksportować model z `predictor.py` (który używa joblib) do ONNX:

```python
import joblib
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import StringTensorType

# Załaduj model
model = joblib.load('MODELS/wniosek_model.joblib')

# Konwertuj do ONNX
# Model powinien zawierać pipeline z TF-IDF vectorizer
initial_type = [('input', StringTensorType([None, 1]))]
onx = convert_sklearn(model, initial_types=initial_type, target_opset=12)

# Zapisz
with open("models/wniosek_model.onnx", "wb") as f:
    f.write(onx.SerializeToString())
```

**Wymagane biblioteki:**
```bash
pip install skl2onnx onnxruntime
```

### Opcja 2: Jeśli model wymaga innego podejścia

Aktualna implementacja w `OnnxScoringService.predict()` zakłada:
- Input: tensor typu `string[]` 
- Output: tensor typu `float[][]` z prawdopodobieństwami dla klas

**UWAGA:** Może wymagać dostosowania w zależności od rzeczywistej struktury modelu ONNX.

## Jak umieścić model w projekcie

### Lokalnie (rozwój):
1. Utwórz katalog `models/` w głównym katalogu projektu:
   ```bash
   mkdir models
   ```

2. Umieść tam plik `wniosek_model.onnx`

3. Lub ustaw zmienną środowiskową:
   ```bash
   export ONNX_MODEL_PATH=/ścieżka/do/modelu.onnx
   ```

### W Dockerze:
Dodaj volume lub skopiuj model do kontenera w `Dockerfile`:
```dockerfile
COPY models/wniosek_model.onnx /app/models/wniosek_model.onnx
```

Lub w `docker-compose.yml`:
```yaml
backend:
  volumes:
    - ./models:/app/models
  environment:
    - ONNX_MODEL_PATH=/app/models/wniosek_model.onnx
```

## Działanie systemu

### Gdy model jest dostępny:
1. Przy starcie aplikacji logowane jest: `"Model ONNX załadowany pomyślnie z: ..."`
2. Przy każdym wywołaniu `POST /api/ewyp-reports/{id}/submit`:
   - System ekstraktuje dane z zgłoszenia
   - Wykonuje predykcję
   - Zapisuje wynik w `scoringClassification` (np. "WYPADEK_PRZY_PRACY: 87.5%")
   - Zwraca zaktualizowane zgłoszenie

### Gdy model nie jest dostępny:
1. Przy starcie: `"Model ONNX nie został znaleziony w: ... Scoring będzie niedostępny..."`
2. Przy submitReport: `scoringClassification` pozostaje `null`
3. System działa normalnie, tylko bez scoringu

## Testowanie

### 1. Kompilacja projektu
```bash
cd backend
mvn clean compile
```

### 2. Uruchomienie (bez modelu - do testowania podstawowej funkcjonalności)
```bash
mvn spring-boot:run
```

### 3. Uruchomienie (z modelem)
```bash
# Umieść model w models/wniosek_model.onnx, następnie:
mvn spring-boot:run
```

### 4. Test API
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
      "circumstances": "Podczas pracy na budowie spadł z rusztowania..."
    }
  }'

# Zapisz ID z odpowiedzi, następnie:
curl -X POST http://localhost:8081/api/ewyp-reports/{ID}/submit

# Sprawdź wynik:
curl http://localhost:8081/api/ewyp-reports/{ID}
```

W odpowiedzi powinieneś zobaczyć pole `scoringClassification` z wartością jak np.: `"WYPADEK_PRZY_PRACY: 87.5%"`

## Struktura danych ekstraktowanych do scoringu

Serwis `OnnxScoringService` ekstraktuje następujące dane z `EWYPReport`:
- **Poszkodowany:** imię, nazwisko, dane urodzenia, adres
- **Zgłaszający:** (jeśli różni się od poszkodowanego)
- **Szczegóły wypadku:** 
  - Data i godzina
  - Miejsce wypadku (adres)
  - **Okoliczności** (najistotniejsze!)
  - Rodzaj i lokalizacja urazu
  - Informacje o leczeniu
  - Status pracy
  - Informacje o zgonie (jeśli dotyczy)
- **Świadkowie:** dane kontaktowe

Tekst jest formatowany i czyszczony zgodnie z logiką z pythonowego `basic_clean()`.

## Dostosowanie implementacji

Jeśli rzeczywisty model ONNX ma inną strukturę wejść/wyjść, zmodyfikuj metodę `predict()` w `OnnxScoringService.java`:

```java
private float predict(String text) throws OrtException {
    // Dostosuj do rzeczywistej struktury modelu:
    // - Nazwa inputu (np. "text_input" zamiast "input")
    // - Typ tensora
    // - Indeksy outputu
    // - Format prawdopodobieństw
}
```

## Monitorowanie

Sprawdź logi aplikacji:
- `INFO`: Model załadowany pomyślnie
- `WARN`: Model nie znaleziony (scoring wyłączony)
- `ERROR`: Błędy podczas ładowania lub predykcji

## Następne kroki

1. **Wytrenuj i skonwertuj model** z `Python/model_training.py` do formatu ONNX
2. **Umieść model** w katalogu `models/`
3. **Przetestuj** działanie scoringu
4. **Dostosuj** metodę `predict()` jeśli struktura modelu wymaga zmian
5. **Zaimplementuj UI** do wyświetlania wyniku scoringu w frontend (Angular)

## Troubleshooting

### Problem: "Cannot resolve symbol 'OnnxScoringService'"
**Rozwiązanie:** Wykonaj `mvn clean compile`

### Problem: OrtException podczas predykcji
**Przyczyna:** Niezgodność struktury danych wejściowych z modelem
**Rozwiązanie:** Sprawdź rzeczywistą strukturę modelu ONNX i dostosuj `predict()`

### Problem: Model zawsze zwraca null
**Przyczyna:** Model nie został załadowany
**Rozwiązanie:** 
- Sprawdź ścieżkę do modelu w logach
- Upewnij się, że plik istnieje
- Sprawdź format pliku (musi być .onnx)

## Przykładowy output

Po poprawnej integracji, w tabeli `ewyp_reports` zobaczysz:

| id | ... | scoring_classification | status |
|----|-----|------------------------|---------|
| uuid-1 | ... | WYPADEK_PRZY_PRACY: 92.3% | SUBMITTED |
| uuid-2 | ... | WYPADEK_PRZY_PRACY: 35.7% | SUBMITTED |

Frontend może wykorzystać tę wartość do wizualizacji (np. progress bar, kolor oznaczenia, etc.)

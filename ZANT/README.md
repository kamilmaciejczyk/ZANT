# ZANT - System Zgłaszania Wypadków przy Pracy (ZUS)

Kompletna aplikacja Angular + Java Spring Boot realizująca ETAP I systemu ZANT ZUS - Wirtualny Asystent do zgłaszania wypadków przy pracy.

## Architektura

- **Backend**: Java Spring Boot 3.x (port 8080)
- **Frontend**: Angular 17 (port 4200)
- **AI**: Google Gemini API
- **PDF Generation**: Flying Saucer + OpenPDF

## Struktura Projektu

```
ZANT/
├── backend/          # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zant/backend/
│   │   │   │   ├── ai/              # AI Client (Gemini)
│   │   │   │   ├── config/          # RequiredField configuration
│   │   │   │   ├── controller/      # REST API controllers
│   │   │   │   ├── model/           # DTOs and models
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       ├── templates/       # HTML templates for PDF
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
└── frontend/         # Angular frontend
    ├── src/
    │   ├── app/
    │   │   ├── components/
    │   │   │   └── chat/           # Chat component
    │   │   ├── models/             # TypeScript interfaces
    │   │   └── services/           # HTTP services
    │   └── styles.scss
    └── package.json
```

## Funkcjonalności

### Backend

1. **REST API**:
   - `POST /api/assistant/{conversationId}/message` - wysyłanie wiadomości do asystenta
   - `POST /api/reports/{id}/validate` - walidacja zgłoszenia
   - `POST /api/reports/{id}/pdf?type=NOTICE` - generowanie PDF zawiadomienia
   - `POST /api/reports/{id}/pdf?type=EXPLANATION` - generowanie PDF wyjaśnień

2. **Asystent AI** (Gemini):
   - Wyciąganie danych z naturalnego języka
   - Wykrywanie brakujących pól
   - Generowanie pytań uzupełniających
   - Walidacja kompletności zgłoszenia

3. **Generowanie PDF**:
   - Zawiadomienie o wypadku (wzór EWYP)
   - Wyjaśnienia poszkodowanego

### Frontend

1. **Chat UI**:
   - Interfejs czatu z botem
   - Lista brakujących pól w czasie rzeczywistym
   - Pasek postępu wypełnienia
   - Sugerowane pytania

2. **Pobieranie PDF**:
   - Przycisk pobierania zawiadomienia
   - Przycisk pobierania wyjaśnień

## Wymagania

- **Java**: JDK 17 lub nowszy
- **Maven**: 3.6 lub nowszy
- **Node.js**: v20.19 lub v22.12 lub nowszy
- **npm**: 9.x lub nowszy
- **Klucz API Google Gemini** (uzyskaj z: https://makersuite.google.com/app/apikey)

## Instalacja i Uruchomienie

### 1. Konfiguracja Backend

```bash
cd backend
```

**Ustaw klucz API Gemini w `application.properties`:**

```properties
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
```

**Kompilacja:**

```bash
mvn clean compile
```

**Uruchomienie:**

```bash
mvn spring-boot:run
```

Backend będzie dostępny pod adresem: `http://localhost:8080`

### 2. Konfiguracja Frontend

```bash
cd frontend
```

**Instalacja zależności:**

```bash
npm install
```

**Uruchomienie:**

```bash
npm start
# lub
ng serve
```

Frontend będzie dostępny pod adresem: `http://localhost:4200`

## Użycie

1. Otwórz przeglądarkę i przejdź do `http://localhost:4200`
2. Rozpocznij rozmowę z wirtualnym asystentem
3. Opisz wypadek naturalnym językiem
4. Odpowiadaj na pytania asystenta
5. Śledź postęp wypełnienia widoczny na pasku i liście brakujących pól
6. Gdy wypełnienie osiągnie 100%, pobierz dokumenty PDF

## API Gemini

Asystent używa modelu `gemini-1.5-flash` do:
- Ekstrakcji danych ze swobodnych wypowiedzi użytkownika
- Generowania pytań follow-up
- Identyfikacji brakujących informacji

Format odpowiedzi Gemini:

```json
{
  "extractedFields": {
    "firstName": "Jan",
    "lastName": "Kowalski",
    ...
  },
  "summaryForUser": "Rozumiem, że...",
  "followUpQuestions": ["Kiedy dokładnie...?", "Gdzie...?"]
}
```

## Checklist Wymaganych Pól (ZANT)

Backend waliduje następujące pola obowiązkowe:

**Dane poszkodowanego:**
- Imię i nazwisko
- PESEL
- Adres
- Dane kontaktowe

**Dane działalności:**
- NIP
- PKD
- Nazwa i adres działalności

**Dane wypadku:**
- Data i godzina wypadku
- Miejsce wypadku
- Planowane godziny pracy
- Czynności wykonywane przed wypadkiem
- Okoliczności i przyczyny wypadku
- Urazy

**Opcjonalne:**
- Świadkowie
- Dane pełnomocnika
- Informacje o maszynach
- Informacje BHP
- Pomoc medyczna

## Technologie

### Backend
- Spring Boot 3.3.1
- Lombok
- Thymeleaf (dla szablonów HTML)
- Flying Saucer + OpenPDF (dla PDF)
- Google Gemini API

### Frontend
- Angular 17
- Angular Material
- RxJS
- TypeScript
- SCSS

## Troubleshooting

### Backend nie kompiluje się

```bash
cd backend
mvn clean install -DskipTests
```

### Frontend nie może połączyć się z backendem

Sprawdź:
1. Czy backend działa na porcie 8080
2. Czy CORS jest poprawnie skonfigurowany
3. Czy adres API w `assistant.service.ts` jest poprawny

### Błąd API Gemini

1. Sprawdź czy klucz API jest ustawiony w `application.properties`
2. Sprawdź czy masz dostęp do internetu
3. Sprawdź limity na koncie Gemini

## Rozwój

### Dodawanie nowych pól wymaganych

Edytuj `backend/src/main/java/com/zant/backend/service/MissingFieldsCalculator.java`

### Modyfikacja szablonów PDF

Edytuj pliki w `backend/src/main/resources/templates/`:
- `notice-template.html` - Zawiadomienie o wypadku
- `explanation-template.html` - Wyjaśnienia poszkodowanego

### Zmiana stylu frontendu

Edytuj `frontend/src/styles.scss` dla globalnych stylów
Edytuj `frontend/src/app/components/chat/chat.component.scss` dla stylu czatu

## Licencja

Projekt utworzony dla hackathonu ZANT.

## Kontakt

Dla pytań i wsparcia, skontaktuj się z zespołem rozwojowym.

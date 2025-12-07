# ZANT REST API Documentation

## Spis Treści

- [Wprowadzenie](#wprowadzenie)
- [Autoryzacja](#autoryzacja)
- [Endpointy API](#endpointy-api)
  - [EWYP Reports](#ewyp-reports)
  - [Assistant (Chat)](#assistant-chat)
  - [Documents (PDF/DOCX)](#documents-pdfdocx)
  - [AI Configuration](#ai-configuration)
- [Modele Danych](#modele-danych)
- [Kody Błędów](#kody-błędów)
- [Przykłady Użycia](#przykłady-użycia)

## Wprowadzenie

Backend ZANT udostępnia RESTful API oparte na Spring Boot 3.3.1. Wszystkie endpointy (poza publicznymi) wymagają autoryzacji przez JWT token.

### Base URL

```
http://localhost:8081/api
```

### Content Type

Wszystkie requesty i odpowiedzi używają JSON:

```
Content-Type: application/json
```

## Autoryzacja

### OAuth2 + JWT

System wykorzystuje Keycloak jako dostawcę tożsamości. Każdy request musi zawierać nagłówek:

```http
Authorization: Bearer <jwt_token>
```

### Role Użytkowników

| Rola | Endpoint Access |
|------|-----------------|
| `ZANT_USER` | `/api/ewyp-reports/**`, `/api/assistant/**`, `/api/pdf/**` |
| `ZANT_CONTROLLER` | Wszystkie endpointy + `/api/ewyp-reports/all`, `/api/ewyp-reports/search` |

### Uzyskanie Tokena

Token jest automatycznie zarządzany przez Keycloak-js w frontend. Do testowania API:

```bash
# Pobierz token
TOKEN=$(curl -X POST "http://localhost:8080/realms/zant/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=controller" \
  -d "password=controller123" \
  -d "grant_type=password" \
  -d "client_id=zant-frontend" | jq -r '.access_token')

# Użyj tokena
curl -X GET "http://localhost:8081/api/ewyp-reports/all" \
  -H "Authorization: Bearer $TOKEN"
```

## Endpointy API

### EWYP Reports

#### 1. Utwórz Draft Zgłoszenia

Tworzy nowy draft zgłoszenia wypadku.

```http
POST /api/ewyp-reports/draft
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "injuredPerson": {
    "firstName": "Jan",
    "lastName": "Kowalski",
    "pesel": "80051512345",
    "dateOfBirth": "1980-05-15",
    "address": {
      "street": "ul. Kwiatowa",
      "houseNumber": "10",
      "apartmentNumber": "5",
      "postalCode": "00-001",
      "city": "Warszawa",
      "country": "Polska"
    }
  },
  "accidentInfo": {
    "accidentDate": "2024-12-01",
    "accidentTime": "10:30",
    "accidentPlace": "Biuro firmy XYZ",
    "circumstancesAndCauses": "Pracownik poślizgnął się na mokrej podłodze...",
    "injuryType": "Skręcenie kostki",
    "injuryLocation": "Prawa kostka"
  }
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DRAFT",
  "createdAt": "2024-12-07T10:00:00Z",
  "updatedAt": "2024-12-07T10:00:00Z",
  "injuredPerson": { ... },
  "accidentInfo": { ... },
  "scoringClassification": null
}
```

---

#### 2. Aktualizuj Draft

Aktualizuje istniejący draft.

```http
PUT /api/ewyp-reports/{id}/draft
```

**Path Parameters:**
- `id` (UUID) - ID zgłoszenia

**Request Body:** Pełny obiekt zgłoszenia

**Response:** `200 OK` - zaktualizowany obiekt

---

#### 3. Wyślij Zgłoszenie

Zmienia status draft na SUBMITTED i uruchamia scoring ML.

```http
POST /api/ewyp-reports/{id}/submit
```

**Path Parameters:**
- `id` (UUID) - ID zgłoszenia

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUBMITTED",
  "scoringClassification": "WYPADEK_PRZY_PRACY: 87.5%",
  ...
}
```

---

#### 4. Pobierz Zgłoszenie

Pobiera szczegóły zgłoszenia po ID.

```http
GET /api/ewyp-reports/{id}
```

**Path Parameters:**
- `id` (UUID) - ID zgłoszenia

**Response:** `200 OK` - obiekt zgłoszenia

---

#### 5. Pobierz Wszystkie Zgłoszenia (Kontroler)

Wymaga roli `ZANT_CONTROLLER`.

```http
GET /api/ewyp-reports/all
```

**Query Parameters:**
- `page` (int, optional) - numer strony (default: 0)
- `size` (int, optional) - rozmiar strony (default: 20)

**Response:** `200 OK`
```json
{
  "content": [
    { "id": "...", "status": "SUBMITTED", ... },
    { "id": "...", "status": "DRAFT", ... }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

#### 6. Wyszukaj Zgłoszenia (Kontroler)

Wymaga roli `ZANT_CONTROLLER`.

```http
GET /api/ewyp-reports/search
```

**Query Parameters:**
- `query` (string, optional) - wyszukiwanie pełnotekstowe
- `status` (string, optional) - filtrowanie po statusie (DRAFT/SUBMITTED)
- `fromDate` (string, optional) - data od (ISO 8601)
- `toDate` (string, optional) - data do (ISO 8601)

**Example:**
```http
GET /api/ewyp-reports/search?query=Kowalski&status=SUBMITTED&fromDate=2024-01-01
```

**Response:** `200 OK` - lista zgłoszeń

---

#### 7. Usuń Zgłoszenie

```http
DELETE /api/ewyp-reports/{id}
```

**Path Parameters:**
- `id` (UUID) - ID zgłoszenia

**Response:** `204 No Content`

---

### Assistant (Chat)

#### 1. Nowy Chat

Tworzy nową sesję chatową z asystentem AI.

```http
POST /api/assistant/new
```

**Request Body:**
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```json
{
  "sessionId": "session-123",
  "turns": [],
  "reportContext": { ... }
}
```

---

#### 2. Wyślij Wiadomość

Wysyła wiadomość do asystenta AI.

```http
POST /api/assistant/message
```

**Request Body:**
```json
{
  "sessionId": "session-123",
  "message": "Jak wypełnić pole okoliczności wypadku?",
  "reportContext": { ... }
}
```

**Response:** `200 OK`
```json
{
  "response": "Pole 'okoliczności wypadku' powinno zawierać szczegółowy opis...",
  "suggestions": [
    {
      "field": "accidentInfo.circumstancesAndCauses",
      "value": "Przykładowy opis..."
    }
  ],
  "confidence": 0.92
}
```

---

#### 3. Historia Konwersacji

Pobiera historię konwersacji dla danej sesji.

```http
GET /api/assistant/history/{sessionId}
```

**Path Parameters:**
- `sessionId` (string) - ID sesji

**Response:** `200 OK`
```json
{
  "turns": [
    {
      "userMessage": "Jak wypełnić...?",
      "assistantResponse": "Pole powinno zawierać...",
      "timestamp": "2024-12-07T10:00:00Z"
    }
  ]
}
```

---

### Documents (PDF/DOCX)

#### 1. Generuj PDF

Generuje dokument PDF dla zgłoszenia.

```http
POST /api/pdf/generate
```

**Request Body:**
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "format": "PDF",
  "template": "EWYP_STANDARD"
}
```

**Response:** `200 OK`
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="EWYP_Kowalski_2024-12-07.pdf"

<binary PDF data>
```

---

#### 2. Generuj DOCX

Generuje dokument DOCX dla zgłoszenia.

```http
POST /api/pdf/generate-docx
```

**Request Body:**
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```
Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
Content-Disposition: attachment; filename="EWYP_Kowalski_2024-12-07.docx"

<binary DOCX data>
```

---

### AI Configuration

#### 1. Pobierz Konfigurację AI

```http
GET /api/ai-config
```

**Response:** `200 OK`
```json
{
  "availableModels": ["GEMINI", "PLLUM"],
  "currentModel": "GEMINI",
  "geminiConfigured": true,
  "pllumConfigured": false
}
```

---

#### 2. Zmień Model AI

```http
POST /api/ai-config/switch
```

**Request Body:**
```json
{
  "model": "PLLUM"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "currentModel": "PLLUM"
}
```

---

## Modele Danych

### EWYPReport

```typescript
{
  id: string;                          // UUID
  status: 'DRAFT' | 'SUBMITTED';       // Status zgłoszenia
  createdAt: string;                   // ISO 8601 datetime
  updatedAt: string;                   // ISO 8601 datetime
  scoringClassification: string | null; // ML scoring result
  
  injuredPerson: {
    firstName: string;
    lastName: string;
    pesel: string;                     // 11 cyfr
    dateOfBirth: string;               // YYYY-MM-DD
    email?: string;
    phone?: string;
    address: Address;
  };
  
  accidentInfo: {
    accidentDate: string;              // YYYY-MM-DD
    accidentTime: string;              // HH:mm
    accidentPlace: string;
    circumstancesAndCauses: string;    // Szczegółowy opis
    injuryType: string;
    injuryLocation: string;
    witnesses?: WitnessInfo[];
  };
  
  reporter?: {
    // Jeśli różni się od poszkodowanego
    firstName: string;
    lastName: string;
    relationship: string;
    ...
  };
  
  attachments?: {
    hasPhotos: boolean;
    hasWitnessStatements: boolean;
    hasMedicalDocuments: boolean;
  };
}
```

### Address

```typescript
{
  street: string;
  houseNumber: string;
  apartmentNumber?: string;
  postalCode: string;                  // XX-XXX format
  city: string;
  country: string;
}
```

### WitnessInfo

```typescript
{
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  description?: string;
}
```

---

## Kody Błędów

### HTTP Status Codes

| Code | Znaczenie | Przykład |
|------|-----------|----------|
| 200 | OK | Sukces |
| 201 | Created | Utworzono nowy zasób |
| 204 | No Content | Usunięto zasób |
| 400 | Bad Request | Nieprawidłowe dane wejściowe |
| 401 | Unauthorized | Brak tokena lub token wygasł |
| 403 | Forbidden | Brak uprawnień |
| 404 | Not Found | Zasób nie istnieje |
| 500 | Internal Server Error | Błąd serwera |

### Error Response Format

```json
{
  "timestamp": "2024-12-07T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "PESEL musi składać się z dokładnie 11 cyfr",
  "path": "/api/ewyp-reports/draft"
}
```

### Validation Errors

```json
{
  "timestamp": "2024-12-07T10:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": [
    {
      "field": "injuredPerson.pesel",
      "message": "PESEL musi składać się z dokładnie 11 cyfr"
    },
    {
      "field": "accidentInfo.accidentDate",
      "message": "Data wypadku jest wymagana"
    }
  ]
}
```

---

## Przykłady Użycia

### cURL Examples

#### Utworzenie Draft Zgłoszenia

```bash
curl -X POST http://localhost:8081/api/ewyp-reports/draft \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "injuredPerson": {
      "firstName": "Jan",
      "lastName": "Kowalski",
      "pesel": "80051512345",
      "dateOfBirth": "1980-05-15",
      "address": {
        "street": "ul. Kwiatowa",
        "houseNumber": "10",
        "postalCode": "00-001",
        "city": "Warszawa",
        "country": "Polska"
      }
    },
    "accidentInfo": {
      "accidentDate": "2024-12-01",
      "accidentTime": "10:30",
      "accidentPlace": "Biuro",
      "circumstancesAndCauses": "Pracownik poślizgnął się...",
      "injuryType": "Skręcenie",
      "injuryLocation": "Kostka"
    }
  }'
```

#### Wysłanie Wiadomości do Asystenta

```bash
curl -X POST http://localhost:8081/api/assistant/message \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-123",
    "message": "Jak opisać wypadek?",
    "reportContext": {}
  }'
```

#### Pobranie Wszystkich Zgłoszeń

```bash
curl -X GET "http://localhost:8081/api/ewyp-reports/all?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

#### Generowanie PDF

```bash
curl -X POST http://localhost:8081/api/pdf/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reportId": "550e8400-e29b-41d4-a716-446655440000",
    "format": "PDF",
    "template": "EWYP_STANDARD"
  }' \
  --output report.pdf
```

### JavaScript/TypeScript Example

```typescript
// Angular Service przykład
async createDraftReport(data: EWYPReportDTO): Promise<EWYPReport> {
  const response = await this.http.post<EWYPReport>(
    `${this.apiUrl}/ewyp-reports/draft`,
    data
  ).toPromise();
  
  return response;
}

async submitReport(reportId: string): Promise<EWYPReport> {
  const response = await this.http.post<EWYPReport>(
    `${this.apiUrl}/ewyp-reports/${reportId}/submit`,
    {}
  ).toPromise();
  
  return response;
}

async getAllReports(page: number = 0, size: number = 20): Promise<PagedResponse<EWYPReport>> {
  const response = await this.http.get<PagedResponse<EWYPReport>>(
    `${this.apiUrl}/ewyp-reports/all`,
    { params: { page: page.toString(), size: size.toString() } }
  ).toPromise();
  
  return response;
}
```

### Python Example

```python
import requests

API_URL = "http://localhost:8081/api"
TOKEN = "your_jwt_token"

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

# Utworzenie draft
draft_data = {
    "injuredPerson": {
        "firstName": "Jan",
        "lastName": "Kowalski",
        # ...
    },
    "accidentInfo": {
        # ...
    }
}

response = requests.post(
    f"{API_URL}/ewyp-reports/draft",
    json=draft_data,
    headers=headers
)

report = response.json()
report_id = report["id"]

# Wysłanie zgłoszenia
response = requests.post(
    f"{API_URL}/ewyp-reports/{report_id}/submit",
    headers=headers
)

submitted_report = response.json()
print(f"Scoring: {submitted_report['scoringClassification']}")
```

---

## 🔗 Dodatkowe Zasoby

- [Główne README](./README.md)
- [OAuth2 Setup](./OAUTH2_SETUP.md)
- [Backend Source Code](./backend/src/main/java/com/zant/backend/controller/)
- [Swagger/OpenAPI](http://localhost:8081/swagger-ui.html) (jeśli włączone)

---

## Uwagi

### Rate Limiting

Obecnie brak limitów requestów. W środowisku produkcyjnym zalecane jest dodanie rate limiting.

### Versioning

API nie jest wersjonowane. W przyszłości planowane jest wprowadzenie wersjonowania (`/api/v1/...`).

### Pagination

Wszystkie endpointy zwracające listy wspierają paginację:
- Default page size: 20
- Max page size: 100

### Caching

Responses dla GET requestów mogą być cache'owane przez 5 minut.

---

**Wersja API**: 1.0.0  
**Data aktualizacji**: 7 grudnia 2025  
**Kontakt**: Zobacz [README.md](./README.md#kontakt-i-wsparcie)

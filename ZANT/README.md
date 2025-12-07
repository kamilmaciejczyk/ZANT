# ZANT - System Zgłaszania i Analiz Wypadków przy Pracy

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red)](https://angular.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Private-lightgrey)]()

## Spis Treści

- [O Projekcie](#o-projekcie)
- [Funkcjonalności](#funkcjonalności)
- [Architektura](#architektura)
- [Wymagania](#wymagania)
- [Szybki Start](#szybki-start)
- [Dokumentacja Szczegółowa](#dokumentacja-szczegółowa)
- [Struktura Projektu](#struktura-projektu)
- [Technologie](#technologie)
- [Bezpieczeństwo](#bezpieczeństwo)
- [Rozwój i Testowanie](#rozwój-i-testowanie)
- [Wdrożenie Produkcyjne](#wdrożenie-produkcyjne)
- [Troubleshooting](#troubleshooting)
- [Kontakt i Wsparcie](#kontakt-i-wsparcie)

## O Projekcie

**ZANT** (Zgłoszenia i Analizy - Nowe Technologie) to kompleksowy system wspierający proces zgłaszania, analizy i obsługi wypadków przy pracy. System został zaprojektowany dla instytucji ZUS i pracodawców, aby usprawnić procedury związane z wypadkami przy pracy zgodnie z obowiązującymi przepisami prawa.

### Cele Systemu

- **Digitalizacja procesu** zgłaszania wypadków przy pracy (formularze EWYP)
- **Asystent AI** wspierający wypełnianie formularzy i analizę spraw
- **Automatyczna analiza** dokumentacji wypadku z wykorzystaniem ML
- **Generowanie dokumentów** (PDF, DOCX) zgodnych z wzorami urzędowymi
- **Zarządzanie zgłoszeniami** z kontrolą dostępu i rolami użytkowników
- **Dostępność cyfrowa** zgodna z WCAG 2.1 Level AA

## Funkcjonalności

### Dla Użytkowników (ZANT_USER)
-  Tworzenie nowych zgłoszeń wypadków w formie elektronicznej
-  Interaktywne formularze z walidacją i podpowiedziami
-  Asystent AI wspierający wypełnianie pól
-  Automatyczne zapisywanie wersji roboczych (draft)
-  Generowanie dokumentów PDF/DOCX
-  Podgląd i edycja własnych zgłoszeń

### Dla Kontrolerów (ZANT_CONTROLLER)
-  Wszystkie funkcje użytkownika
-  Wyszukiwarka zgłoszeń z filtrowaniem
-  Dostęp do wszystkich zgłoszeń w systemie
-  Analiza ML z predykcją prawdopodobieństwa uznania wypadku
-  Raporty i statystyki

### Zaawansowane Funkcje
-  **AI Assistant**: Chatbot wykorzystujący Gemini/PLLuM do wsparcia użytkownika
-  **ML Scoring**: Automatyczna ocena prawdopodobieństwa uznania wypadku
-  **Document Generation**: Eksport do PDF i DOCX z szablonami
-  **OAuth2/Keycloak**: Bezpieczna autoryzacja i uwierzytelnianie
-  **Accessibility**: Pełne wsparcie dla czytników ekranu i nawigacji klawiaturą
-  **Multi-AI Support**: Możliwość przełączania między różnymi modelami AI

##  Architektura

System ZANT składa się z czterech głównych komponentów:

```
┌─────────────────────────────────────────────────────────────────┐
│                          ZANT System                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Frontend   │  │   Backend    │  │  Keycloak    │          │
│  │  Angular 17  │◄─┤ Spring Boot  │◄─┤   OAuth2     │          │
│  │  Port: 4200  │  │  Port: 8081  │  │  Port: 8080  │          │
│  └──────────────┘  └───────┬──────┘  └──────────────┘          │
│                            │                                      │
│                    ┌───────┴───────┐                            │
│                    │               │                             │
│            ┌───────▼──────┐ ┌─────▼────────┐                   │
│            │  PostgreSQL  │ │  Python ML   │                    │
│            │  Port: 5432  │ │  (Optional)  │                    │
│            └──────────────┘ └──────────────┘                    │
│                                                                   │
│  External Services:                                              │
│  ┌────────────────┐  ┌────────────────┐                        │
│  │  Gemini API    │  │  PLLuM API     │                        │
│  │  (Google AI)   │  │  (Local LLM)   │                        │
│  └────────────────┘  └────────────────┘                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Przepływ Danych

1. **Frontend** (Angular) → OAuth2 Login → **Keycloak**
2. **Frontend** → REST API (JWT Token) → **Backend**
3. **Backend** → Query/Save → **PostgreSQL**
4. **Backend** → AI Request → **Gemini/PLLuM**
5. **Backend** → ML Prediction → **Python Service** (opcjonalnie)

## Wymagania

### Minimalne Wymagania Systemowe

#### Development
- **OS**: Windows 10/11, macOS 12+, Linux (Ubuntu 20.04+)
- **RAM**: 8 GB (16 GB zalecane)
- **Dysk**: 10 GB wolnej przestrzeni
- **CPU**: Intel i5 lub AMD Ryzen 5 (lub nowszy)

#### Production
- **RAM**: 16 GB minimum
- **Dysk**: 50 GB (z miejscem na dane)
- **CPU**: 4+ rdzenie

### Wymagane Oprogramowanie

| Komponent | Wersja | Wymagane |
|-----------|--------|----------|
| Java JDK | 17+ |  Tak |
| Node.js | 18+ |  Tak |
| npm | 9+ |  Tak |
| Docker | 20+ |  Tak (lub instalacja lokalna) |
| Docker Compose | 2.0+ |  Tak (lub instalacja lokalna) |
| Maven | 3.8+ |  Opcjonalne (wrapper included) |
| Python | 3.11+ |  Opcjonalne (dla ML) |
| PostgreSQL | 15+ |  Opcjonalne (Docker/lokalna) |

### Klucze API

- **GEMINI_API_KEY**: Wymagany dla asystenta AI (Google AI Studio)
- **PLLUM_API_KEY**: Opcjonalny, alternatywa dla Gemini

## Szybki Start

### Opcja 1: Docker Compose (Zalecane)

```bash
# 1. Sklonuj repozytorium
git clone <repository-url>
cd ZANT

# 2. Ustaw klucz API
# Windows (CMD)
set GEMINI_API_KEY=your_api_key_here

# Windows (PowerShell)
$env:GEMINI_API_KEY="your_api_key_here"

# Linux/Mac
export GEMINI_API_KEY=your_api_key_here

# 3. Uruchom wszystko
docker-compose up --build

# 4. Otwórz przeglądarkę
# Frontend: http://localhost:4200
# Backend API: http://localhost:8081
# Keycloak: http://localhost:8080
```

### Opcja 2: Lokalne Uruchomienie

#### Backend
```bash
cd backend
./mvnw spring-boot:run
# Lub na Windows: mvnw.cmd spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm start
# Aplikacja dostępna na http://localhost:4200
```

#### Keycloak + PostgreSQL
```bash
docker-compose up db keycloak
```

### Pierwsze Logowanie

Po uruchomieniu systemu:

1. Przejdź do http://localhost:4200
2. Zostaniesz przekierowany do Keycloak
3. Użyj jednego z kont testowych:

**Konto Użytkownika:**
- Login: `user`
- Hasło: `user123`

**Konto Kontrolera:**
- Login: `controller`
- Hasło: `controller123`

## Dokumentacja Szczegółowa

### Główna Dokumentacja

| Dokument | Opis |
|----------|------|
| [AGENTS.md](./AGENTS.md) | Wytyczne dla developerów, konwencje kodowania |
| [OAUTH2_SETUP.md](./OAUTH2_SETUP.md) | Konfiguracja autoryzacji i ról użytkowników |
| [PYTHON_INTEGRATION.md](./PYTHON_INTEGRATION.md) | Integracja z Python ML (scoring) |
| [ONNX_INTEGRATION.md](./ONNX_INTEGRATION.md) | Alternatywna integracja ONNX |
| [DOSTEPNOSC_CYFROWA.md](./DOSTEPNOSC_CYFROWA.md) | Raport dostępności WCAG 2.1 |
| [Python/docs.md](./Python/docs.md) | Dokumentacja komponentów ML i OCR |

### Backend (Spring Boot)
- [Backend README](./backend/README.md) - szczegółowa dokumentacja API
- [API Endpoints](./backend/API.md) - dokumentacja REST API
- [Database Schema](./backend/DATABASE.md) - schemat bazy danych

### Frontend (Angular)
- [Frontend README](./frontend/README.md) - dokumentacja interfejsu użytkownika
- [Components Guide](./frontend/COMPONENTS.md) - przewodnik po komponentach
- [Styling Guide](./frontend/STYLES.md) - konwencje stylowania

### Python ML Components
- [OCR Pipeline](./Python/docs.md#moduł-ocr) - ekstrakcja tekstu z PDF
- [Model Training](./Python/docs.md#trenowanie-modelu-ml) - trenowanie modelu ML
- [Predictor](./Python/docs.md#predykcja) - predykcja wypadków
- [Case Analyzer](./Python/docs.md#analiza-sprawy) - analiza spraw ZUS

## Struktura Projektu

```
ZANT/
├── backend/                    # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zant/backend/
│   │   │   │   ├── ai/               # Klienty AI (Gemini, PLLuM)
│   │   │   │   ├── config/           # Konfiguracja (Security, CORS)
│   │   │   │   ├── controller/       # REST Controllers
│   │   │   │   ├── dto/              # Data Transfer Objects
│   │   │   │   ├── mapper/           # Entity-DTO Mappers
│   │   │   │   ├── model/            # JPA Entities
│   │   │   │   ├── repository/       # Spring Data Repositories
│   │   │   │   └── service/          # Business Logic
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── templates/        # Thymeleaf templates
│   │   │       └── static/           # Static assets
│   │   └── test/                     # Unit & Integration Tests
│   ├── Dockerfile
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                   # Angular Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/         # UI Components
│   │   │   │   ├── home/
│   │   │   │   ├── chat/
│   │   │   │   ├── ewyp-form/
│   │   │   │   └── ewyp-search/
│   │   │   ├── services/           # Angular Services
│   │   │   ├── models/             # TypeScript Models
│   │   │   └── interceptors/       # HTTP Interceptors
│   │   ├── assets/                 # Images, icons
│   │   └── styles.scss             # Global styles
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json                # npm dependencies
│
├── keycloak/                   # Keycloak Configuration
│   └── realm-export.json          # Realm, clients, users, roles
│
├── Python/                     # ML & OCR Components
│   ├── OCR.py                     # PDF to text extraction
│   ├── model_training.py          # Train ML model
│   ├── predictor.py               # Predict accident classification
│   ├── opinie_validation.py       # Opinion validation with AI
│   ├── zus_case_analyzer.py       # Full case analysis
│   └── docs.md                    # Python documentation
│
├── MODELS/                     # Trained ML Models
│   └── wniosek_model.joblib
│
├── OCR_OUTPUT/                 # OCR results (text files)
├── PDF/                        # Input PDF documents
├── ANALYSIS/                   # AI-generated analyses
│
├── docker-compose.yml          # Docker orchestration
├── .gitignore
└── README.md                   # This file
```

## Technologie

### Backend
- **Framework**: Spring Boot 3.3.1
- **Java**: 17
- **Security**: Spring Security + OAuth2 Resource Server
- **Database**: PostgreSQL 15 (production), H2 (development)
- **ORM**: Spring Data JPA + Hibernate
- **PDF/DOCX**: iText7, Apache POI, OpenPDF, Flying Saucer
- **HTTP Client**: OkHttp
- **JSON**: Gson
- **Build**: Maven 3.x

### Frontend
- **Framework**: Angular 17
- **UI Library**: Angular Material 17
- **Language**: TypeScript 5.4
- **Styling**: SCSS
- **Auth**: Keycloak-js 26
- **HTTP**: Angular HttpClient
- **Build**: Angular CLI

### Infrastructure
- **Authentication**: Keycloak 23
- **Database**: PostgreSQL 15
- **Containerization**: Docker + Docker Compose
- **Web Server**: Nginx (for frontend in production)

### AI/ML
- **AI Models**: Google Gemini, PLLuM (optional)
- **ML Framework**: scikit-learn
- **OCR**: RapidOCR + pypdfium2
- **Python**: 3.11+

## Bezpieczeństwo

### Autoryzacja i Uwierzytelnianie

System wykorzystuje **OAuth 2.0** z **Keycloak** jako dostawcą tożsamości.

#### Role Użytkowników

| Rola | Uprawnienia |
|------|-------------|
| **ZANT_USER** | Tworzenie zgłoszeń, asystent AI, generowanie dokumentów |
| **ZANT_CONTROLLER** | Wszystkie uprawnienia USER + wyszukiwarka + dostęp do wszystkich zgłoszeń |

#### Zabezpieczenie API

Wszystkie endpointy (poza publicznymi) wymagają ważnego JWT tokena:

```
Authorization: Bearer <jwt_token>
```

#### CORS

Backend akceptuje requesty z:
- `http://localhost:4200` (development)
- Konfigurowalny origin dla production

### Ochrona Danych

- **RODO**: System wspiera anonimizację danych (PESEL, dane osobowe)
- **Encryption**: Hasła w Keycloak są hashowane (bcrypt)
- **HTTPS**: Zalecane dla środowiska production
- **SQL Injection**: Ochrona przez JPA/Hibernate
- **XSS**: Angular automatycznie sanityzuje HTML

### Zmienne Środowiskowe

Wrażliwe dane przekazuj przez zmienne środowiskowe:

```bash
# API Keys
GEMINI_API_KEY=your_key
PLLUM_API_KEY=your_key

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zant
SPRING_DATASOURCE_USERNAME=zant
SPRING_DATASOURCE_PASSWORD=secure_password

# Keycloak
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/zant
```

## Rozwój i Testowanie

### Backend Testing

```bash
cd backend

# Uruchom wszystkie testy
./mvnw test

# Uruchom z raportami pokrycia
./mvnw clean test jacoco:report

# Uruchom tylko jeden test
./mvnw test -Dtest=EWYPReportControllerTest
```

### Frontend Testing

```bash
cd frontend

# Unit tests
npm test

# E2E tests
npm run e2e

# Lint
npm run lint
```

### Lokalne Developowanie

#### Hot Reload (Backend)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

#### Hot Reload (Frontend)
```bash
cd frontend
npm start
# Automatyczny reload przy zmianach
```

#### Debug w VS Code

Dodaj do `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Backend",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

### Code Quality

```bash
# Backend
cd backend
./mvnw checkstyle:check
./mvnw spotbugs:check

# Frontend
cd frontend
npm run lint
npm run format
```

## Wdrożenie Produkcyjne

### Przygotowanie

1. **Zmień hasła domyślne** w Keycloak
2. **Włącz HTTPS** dla wszystkich serwisów
3. **Skonfiguruj backup** bazy danych
4. **Ustaw zmienne środowiskowe** dla production
5. **Przygotuj monitoring** (logi, metryki)

### Docker Production

```bash
# Build images
docker-compose -f docker-compose.prod.yml build

# Deploy
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Środowisko Production

Przykładowy `docker-compose.prod.yml`:

```yaml
services:
  backend:
    image: zant-backend:prod
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/zant_prod
      # ... inne zmienne
    restart: always
    
  frontend:
    image: zant-frontend:prod
    restart: always
    
  # ... pozostałe serwisy
```

### Health Checks

Backend dostarcza endpoint do monitorowania:

```bash
curl http://localhost:8081/actuator/health
```

### Monitoring i Logi

```bash
# Docker logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Backend logs (lokalnie)
tail -f backend/logs/spring.log
```

## Troubleshooting

### Backend nie startuje

**Problem**: `Port 8081 already in use`

```bash
# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8081 | xargs kill -9
```

**Problem**: `Database connection failed`

- Sprawdź czy PostgreSQL działa: `docker ps | grep postgres`
- Sprawdź credentials w `application.properties`
- Zresetuj bazę: `docker-compose down -v && docker-compose up db`

### Frontend nie łączy się z Backend

**Problem**: CORS errors

- Sprawdź konfigurację CORS w `WebConfig.java`
- Upewnij się, że backend działa na porcie 8081
- Sprawdź czy Keycloak jest dostępny

**Problem**: Keycloak redirect loop

- Wyczyść cache przeglądarki i localStorage
- Sprawdź konfigurację redirect URIs w Keycloak
- Zrestartuj Keycloak: `docker-compose restart keycloak`

### Python ML nie działa

**Problem**: `Python nie jest dostępny`

```bash
# Sprawdź instalację
python3 --version

# Zainstaluj zależności
pip install joblib scikit-learn

# Wytrenuj model
cd Python
python3 model_training.py
```

**Problem**: `Model nie został znaleziony`

- Upewnij się, że plik `MODELS/wniosek_model.joblib` istnieje
- Wytrenuj model używając `model_training.py`
- Sprawdź ścieżkę w `application.properties`

### Docker Issues

**Problem**: `Cannot connect to Docker daemon`

```bash
# Start Docker Desktop (Windows/Mac)
# Lub na Linux:
sudo systemctl start docker
```

**Problem**: Out of disk space

```bash
# Wyczyść nieużywane obrazy i kontenery
docker system prune -a
docker volume prune
```

### Performance Issues

**Problem**: System działa wolno

- Zwiększ pamięć dla Docker Desktop (Settings → Resources)
- Sprawdź użycie zasobów: `docker stats`
- Zoptymalizuj zapytania do bazy danych
- Włącz caching w backend

### FAQ

**Q: Czy mogę używać systemu bez Dockera?**  
A: Tak, możesz uruchomić każdy komponent lokalnie, ale wymaga to dodatkowej konfiguracji.

**Q: Czy system działa offline?**  
A: Częściowo - formularze i zapisywanie działają, ale asystent AI wymaga połączenia z internetem.

**Q: Jak dodać nowego użytkownika?**  
A: Zobacz sekcję "Dodawanie nowego użytkownika" w [OAUTH2_SETUP.md](./OAUTH2_SETUP.md)

**Q: Czy mogę zmienić model AI?**  
A: Tak, system wspiera Gemini i PLLuM. Konfiguracja w Settings aplikacji.

## Kontakt i Wsparcie

### Zgłaszanie Błędów

Jeśli znalazłeś błąd:
1. Sprawdź czy błąd nie został już zgłoszony
2. Przygotuj szczegółowy opis problemu
3. Dołącz logi (`docker-compose logs`)
4. Opisz kroki do reprodukcji

### Dokumentacja Dodatkowa

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Angular Documentation](https://angular.io/docs)
- [Docker Documentation](https://docs.docker.com/)

### Wkład w Projekt

Projekt rozwija się aktywnie. Przed dodaniem zmian:
1. Przeczytaj [AGENTS.md](./AGENTS.md)
2. Zachowaj konwencje kodowania
3. Dodaj testy dla nowej funkcjonalności
4. Zaktualizuj dokumentację

---

## Licencja

Projekt prywatny - wszelkie prawa zastrzeżone.

## Podziękowania

Projekt wykorzystuje następujące open-source projekty:
- Spring Boot & Spring Security
- Angular & Angular Material
- Keycloak
- PostgreSQL
- Docker
- Google Gemini API
- scikit-learn

---

**Wersja dokumentacji**: 1.0.0  
**Data ostatniej aktualizacji**: 7 grudnia 2025  
**System**: ZANT - Zgłoszenia i Analizy Wypadków przy Pracy

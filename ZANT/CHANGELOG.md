# Changelog

Wszystkie istotne zmiany w projekcie ZANT będą dokumentowane w tym pliku.

Format oparty na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/),
projekt stosuje [Semantic Versioning](https://semver.org/lang/pl/).

## [Unreleased]

### Planowane
- Eksport danych do Excel
- Zaawansowane raporty i statystyki
- Integracja z systemami zewnętrznymi (API ZUS)
- Wsparcie dla wielu języków (i18n)
- Tryb offline z synchronizacją
- Powiadomienia email o statusie zgłoszeń

## [1.0.0] - 2024-12-07

###  Dodano
- **System zgłoszeń EWYP**: Pełny formularz elektroniczny zgodny z przepisami
- **Autoryzacja OAuth2**: Integracja z Keycloak dla bezpiecznego logowania
- **Role użytkowników**: ZANT_USER (podstawowe) i ZANT_CONTROLLER (rozszerzone)
- **Asystent AI**: Chatbot wspierający wypełnianie formularzy (Gemini/PLLuM)
- **ML Scoring**: Automatyczna ocena prawdopodobieństwa uznania wypadku
- **Generowanie dokumentów**: Eksport do PDF i DOCX
- **Wyszukiwarka zgłoszeń**: Dla użytkowników z rolą ZANT_CONTROLLER
- **Dostępność cyfrowa**: Zgodność z WCAG 2.1 Level AA
- **Docker Compose**: Łatwe wdrożenie całego stacku
- **Dokumentacja**: Kompletna dokumentacja techniczna i użytkownika

### Backend
- Spring Boot 3.3.1 z Java 17
- Spring Security + OAuth2 Resource Server
- PostgreSQL 15 jako baza danych
- JPA/Hibernate dla ORM
- REST API z walidacją
- Integracja z Python ML (opcjonalnie)
- Generowanie PDF (iText7, OpenPDF, Flying Saucer)
- Generowanie DOCX (Apache POI)
- AI clients (Gemini, PLLuM)

### Frontend
- Angular 17 z Material Design
- TypeScript 5.4
- Keycloak-js dla autoryzacji
- Responsywny design
- Dostępność klawiatury
- Wsparcie dla czytników ekranu
- Dark mode ready

### Infrastructure
- Docker i Docker Compose
- Keycloak 23 dla IAM
- PostgreSQL 15
- Nginx dla frontendu (produkcja)

### Python ML Components
- OCR z pypdfium2 i RapidOCR
- Model ML (TF-IDF + Logistic Regression)
- Scoring wypadków przy pracy
- Analiza spraw z AI (Gemini)
- Pipeline przetwarzania dokumentów

###  Bezpieczeństwo
- OAuth 2.0 + JWT tokens
- CORS protection
- SQL Injection protection (JPA)
- XSS protection (Angular)
- Anonimizacja danych dla AI (PESEL, dane osobowe)
- HTTPS ready

###  Dokumentacja
- README.md - główna dokumentacja projektu
- QUICKSTART.md - przewodnik szybkiego startu
- API.md - dokumentacja REST API
- OAUTH2_SETUP.md - konfiguracja autoryzacji
- PYTHON_INTEGRATION.md - integracja ML
- ONNX_INTEGRATION.md - alternatywna integracja
- DOSTEPNOSC_CYFROWA.md - raport WCAG
- AGENTS.md - wytyczne dla developerów
- Python/docs.md - dokumentacja komponentów ML

###  Testowanie
- Unit testy dla backendu (JUnit 5)
- Testy integracyjne
- Konfiguracja testów dla frontendu (Karma/Jasmine)

---

## [0.9.0] - 2024-11-20 (Beta)

###  Dodano
- Podstawowy formularz EWYP
- Zapisywanie drafts
- Prosty AI assistant (prototype)
- Podstawowa autoryzacja

###  Zmieniono
- Migracja z H2 na PostgreSQL
- Ulepszona walidacja formularzy

###  Naprawiono
- Problemy z walidacją PESEL
- Błędy w formatowaniu dat

---

## [0.8.0] - 2024-11-01 (Alpha)

###  Dodano
- Początkowa struktura projektu
- Backend Spring Boot
- Frontend Angular
- Podstawowy model danych
- Docker Compose configuration

###  Zmieniono
- Architektura backendu (refactoring)
- Struktura komponentów frontendowych

---

## [0.7.0] - 2024-10-15 (Pre-Alpha)

### Dodano
- Proof of Concept
- Podstawowe API endpoints
- Testowy UI w Angular
- Integracja z Gemini API

---

## [0.1.0] - 2024-09-01 (Initial)

### Dodano
- Inicjalizacja projektu
- Podstawowa konfiguracja
- Struktura repozytoriów

---

## Typy Zmian

- **Dodano** - nowe funkcjonalności
- **Zmieniono** - zmiany w istniejących funkcjonalnościach
- **Usunięto** - usunięte funkcjonalności
- **Naprawiono** - poprawki błędów
- **Bezpieczeństwo** - poprawki zabezpieczeń
- **Dokumentacja** - zmiany w dokumentacji
- **Wydajność** - usprawnienia wydajności
- **UI/UX** - zmiany w interfejsie użytkownika

---

## Planowane Wersje

### [1.1.0] - Q1 2025
- [ ] Eksport zbiorczy do Excel/CSV
- [ ] Zaawansowane filtry w wyszukiwarce
- [ ] Dashboard ze statystykami
- [ ] Historia zmian w zgłoszeniach (audit log)
- [ ] Powiadomienia email
- [ ] API webhooks

### [1.2.0] - Q2 2025
- [ ] Wsparcie dla załączników (zdjęcia, dokumenty)
- [ ] OCR integracja w UI
- [ ] Tryb offline z synchronizacją
- [ ] Mobile app (React Native/Flutter)
- [ ] Integracja z systemami HR

### [2.0.0] - Q3 2025
- [ ] Multi-tenancy (obsługa wielu organizacji)
- [ ] Workflow engine (zaawansowane procesy)
- [ ] Integracja z API ZUS
- [ ] Advanced analytics & BI
- [ ] Microservices architecture

---

## Migracja

### Z wersji 0.9.0 do 1.0.0

**Backend:**
1. Zaktualizuj bazę danych:
   ```sql
   ALTER TABLE ewyp_reports ADD COLUMN scoring_classification VARCHAR(255);
   ```

2. Dodaj zmienne środowiskowe:
   ```bash
   GEMINI_API_KEY=your_key
   SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/zant
   ```

3. Zaimportuj realm Keycloak:
   ```bash
   docker-compose up keycloak
   ```

**Frontend:**
1. Zaktualizuj dependencies:
   ```bash
   cd frontend
   npm install keycloak-js@26.2.1
   npm install
   ```

2. Usuń stare dane localStorage:
   ```javascript
   localStorage.clear();
   ```

**Dane:**
- Eksportuj dane przed migracją
- Uruchom skrypt migracji (dostępny w `/migrations/v1.0.0.sql`)

---

## Znane Problemy

### Wersja 1.0.0

1. **ML Scoring wymaga Python** - Optional, ale wymaga dodatkowej konfiguracji
2. **Keycloak redirect w Safari** - Czasami wymaga czyszczenia cookies
3. **Docker na Windows** - Może wymagać więcej RAM (min. 8GB)
4. **Gemini API limity** - Free tier ma ograniczenia requestów/dzień

### Workarounds

**Problem**: Python ML nie działa
- **Solution**: Wyłącz w konfiguracji lub zainstaluj Python 3.11+

**Problem**: Keycloak redirect loop
- **Solution**: Wyczyść localStorage i cookies

**Problem**: Docker Out of Memory
- **Solution**: Zwiększ memory limit w Docker Desktop (Settings → Resources)

---


## Kontakty

- **Dokumentacja**: [README.md](./README.md)
- **Quick Start**: [QUICKSTART.md](./QUICKSTART.md)
- **API Docs**: [API.md](./API.md)

---

**Legenda wersji:**
- **Major (X.0.0)**: Breaking changes, duże nowe funkcjonalności
- **Minor (0.X.0)**: Nowe funkcjonalności, backward-compatible
- **Patch (0.0.X)**: Bugfixy, małe poprawki

---

_Dokument aktualizowany przy każdym release_  
_Ostatnia aktualizacja: 7 grudnia 2025_

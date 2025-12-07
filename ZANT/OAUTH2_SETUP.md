# Autoryzacja OAuth2 z Keycloak - Instrukcja

## 📋 Przegląd

Aplikacja ZANT została zintegrowana z systemem autoryzacji OAuth2 przez Keycloak. System obsługuje dwie role użytkowników z różnymi uprawnieniami.

## 👥 Role użytkowników

### 1. ZANT_USER (Użytkownik)
- **Opis**: Użytkownik wprowadzający dane wypadku
- **Uprawnienia**:
  - Tworzenie nowych zgłoszeń wypadków
  - Wypełnianie formularzy EWYP
  - Korzystanie z asystenta AI (chat)
  - Generowanie dokumentów PDF/DOCX
  - Dostęp do własnych zgłoszeń

### 2. ZANT_CONTROLLER (Kontroler/Pracownik)
- **Opis**: Pracownik z dostępem do podglądu wniosków i wyszukiwarki
- **Uprawnienia**:
  - Wszystkie uprawnienia ZANT_USER
  - Dostęp do wyszukiwarki wszystkich zgłoszeń
  - Przeglądanie wszystkich wniosków w systemie
  - Filtrowanie i wyszukiwanie zgłoszeń

## 🔑 Konta testowe

### Konto Użytkownika
- **Username**: `user`
- **Password**: `user123`
- **Email**: user@zant.pl
- **Imię i nazwisko**: Jan Kowalski
- **Rola**: ZANT_USER

### Konto Kontrolera
- **Username**: `controller`
- **Password**: `controller123`
- **Email**: controller@zant.pl
- **Imię i nazwisko**: Anna Nowak
- **Rola**: ZANT_CONTROLLER

## 🚀 Uruchomienie aplikacji

### Wymagania
- Docker i Docker Compose
- Zmienne środowiskowe: `GEMINI_API_KEY` i/lub `PLLUM_API_KEY`

### Krok 1: Uruchomienie wszystkich serwisów

```bash
# Z katalogu głównego projektu
docker-compose up --build
```

To polecenie uruchomi:
- **PostgreSQL** (port 5432) - baza danych
- **Keycloak** (port 8180) - serwer autoryzacji
- **Backend** (port 8081) - Spring Boot API
- **Frontend** (port 4200) - Angular UI

### Krok 2: Dostęp do aplikacji

- **Aplikacja ZANT**: http://localhost:4200
- **Keycloak Admin Console**: http://localhost:8180
  - Username: `admin`
  - Password: `admin`
- **Backend API**: http://localhost:8081

### Krok 3: Logowanie

1. Otwórz http://localhost:4200 w przeglądarce
2. Zostaniesz automatycznie przekierowany do Keycloak
3. Zaloguj się używając jednego z kont testowych
4. Po zalogowaniu zostaniesz przekierowany z powrotem do aplikacji

## 🔐 Jak działa autoryzacja

### Flow logowania
1. Użytkownik otwiera aplikację
2. Angular wykrywa brak autentykacji i inicjuje Keycloak
3. Keycloak przekierowuje do strony logowania
4. Po pomyślnym logowaniu, JWT token jest wydawany
5. Angular przechowuje token i dołącza go do każdego requestu HTTP
6. Backend weryfikuje token i sprawdza uprawnienia
7. Dostęp do zasobów jest udzielany na podstawie ról

### Zabezpieczenie endpointów (Backend)

```
Publiczne:
  /api/public/** - brak wymagań

Dla ZANT_USER i ZANT_CONTROLLER:
  /api/reports/** - zarządzanie zgłoszeniami
  /api/assistant/** - chat AI
  /api/pdf/** - generowanie dokumentów
  /api/ai-config/** - konfiguracja AI

Tylko dla ZANT_CONTROLLER:
  /api/ewyp/search - wyszukiwarka zgłoszeń
  /api/ewyp/all - lista wszystkich zgłoszeń
```

### Zabezpieczenie UI (Frontend)

- Przycisk "Zapisane zgłoszenia" (wyszukiwarka) jest widoczny tylko dla roli ZANT_CONTROLLER
- Menu użytkownika pokazuje imię, nazwisko, email i przypisane role
- Przycisk wylogowania dostępny w menu użytkownika

## 🛠️ Konfiguracja Keycloak

### Realm: zant
Konfiguracja Keycloak jest automatycznie importowana przy starcie z pliku:
```
keycloak/realm-export.json
```

### Klienty
1. **zant-frontend** (Public Client)
   - Typ: Public
   - PKCE: Włączone (S256)
   - Redirect URIs: http://localhost:4200/*, http://localhost/*
   - Web Origins: http://localhost:4200, http://localhost

2. **zant-backend** (Resource Server)
   - Typ: Bearer Only
   - Tylko do weryfikacji tokenów

### Token Mapping
Role realm są mapowane do claim `roles` w JWT token:
```json
{
  "roles": ["ZANT_USER"],
  "preferred_username": "user",
  "email": "user@zant.pl",
  "given_name": "Jan",
  "family_name": "Kowalski"
}
```

## 📝 Struktura projektu

### Backend
```
backend/src/main/java/com/zant/backend/
├── config/
│   └── SecurityConfig.java          # Konfiguracja Spring Security
├── interceptors/
│   └── (JWT automatycznie dodawany przez Spring)
└── controller/
    └── (zabezpieczone przez SecurityConfig)
```

### Frontend
```
frontend/src/app/
├── services/
│   └── keycloak.service.ts          # Serwis do obsługi Keycloak
├── interceptors/
│   └── auth.interceptor.ts          # Interceptor dodający token do requestów
└── components/
    └── home/
        └── home.component.ts        # Warunkowe wyświetlanie wyszukiwarki
```

## 🧪 Testowanie

### Scenariusz 1: Logowanie jako użytkownik (ZANT_USER)
1. Zaloguj się jako: user / user123
2. Sprawdź, że widzisz przycisk "Nowe zgłoszenie"
3. Sprawdź, że NIE widzisz przycisku "Zapisane zgłoszenia"
4. Kliknij ikonę użytkownika w prawym górnym rogu
5. Sprawdź swoje dane i rolę

### Scenariusz 2: Logowanie jako kontroler (ZANT_CONTROLLER)
1. Wyloguj się z poprzedniego konta
2. Zaloguj się jako: controller / controller123
3. Sprawdź, że widzisz oba przyciski: "Nowe zgłoszenie" i "Zapisane zgłoszenia"
4. Kliknij "Zapisane zgłoszenia" - powinieneś mieć dostęp do wyszukiwarki
5. Sprawdź menu użytkownika z rolą ZANT_CONTROLLER

### Scenariusz 3: Test API bezpośrednio
```bash
# Pobierz token (wymaga jq)
TOKEN=$(curl -X POST "http://localhost:8180/realms/zant/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=controller" \
  -d "password=controller123" \
  -d "grant_type=password" \
  -d "client_id=zant-frontend" | jq -r '.access_token')

# Testuj zabezpieczony endpoint
curl -X GET "http://localhost:8081/api/ewyp/all" \
  -H "Authorization: Bearer $TOKEN"
```

## 🔧 Rozwiązywanie problemów

### Problem: Błąd CORS
**Rozwiązanie**: Upewnij się, że backend i frontend działają na odpowiednich portach (8081/4200)

### Problem: Token wygasł
**Rozwiązanie**: Token jest automatycznie odświeżany co minutę. Jeśli to nie działa, wyloguj się i zaloguj ponownie.

### Problem: Nie mogę się zalogować
**Rozwiązanie**: 
1. Sprawdź, czy Keycloak działa (http://localhost:8180)
2. Sprawdź logi Keycloak: `docker-compose logs keycloak`
3. Zresetuj bazę danych: `docker-compose down -v && docker-compose up --build`

### Problem: Aplikacja nie przekierowuje do Keycloak
**Rozwiązanie**: Wyczyść cache przeglądarki i localStorage

### Problem: 403 Forbidden na endpointach
**Rozwiązanie**: Sprawdź, czy użytkownik ma odpowiednią rolę. Dekoduj JWT na https://jwt.io

## 📚 Dodatkowe informacje

### Zmiana hasła użytkownika
1. Zaloguj się do Keycloak Admin Console (http://localhost:8180)
2. Przejdź do: Realm "zant" → Users
3. Wybierz użytkownika
4. Zakładka "Credentials" → "Reset Password"

### Dodawanie nowego użytkownika
1. W Keycloak Admin Console: Users → Add user
2. Wypełnij dane (username, email, first/last name)
3. Save
4. Zakładka "Credentials" → Set password
5. Zakładka "Role Mappings" → Assign Roles → Wybierz ZANT_USER lub ZANT_CONTROLLER

### Dodawanie nowej roli
1. W Keycloak Admin Console: Realm roles → Create role
2. Dodaj nową rolę
3. W backend/SecurityConfig.java dodaj regułę dla tej roli
4. W frontend/keycloak.service.ts dodaj metodę sprawdzającą tę rolę

## 🔒 Bezpieczeństwo w produkcji

⚠️ **WAŻNE**: Przed wdrożeniem na produkcję:

1. **Zmień hasła domyślne**:
   - Hasło admina Keycloak
   - Hasła użytkowników testowych
   - Hasło do bazy PostgreSQL

2. **Włącz HTTPS**:
   - Skonfiguruj SSL/TLS dla Keycloak
   - Użyj HTTPS dla frontend i backend

3. **Konfiguracja Keycloak**:
   - Ustaw `sslRequired: external`
   - Włącz `KC_HOSTNAME_STRICT`
   - Skonfiguruj proper redirect URIs

4. **Tokeny**:
   - Zmniejsz czas życia access token (default: 5 min)
   - Skonfiguruj refresh token rotation
   - Włącz token revocation

5. **Monitoring**:
   - Loguj próby dostępu
   - Monitoruj nieudane logowania
   - Włącz audyt w Keycloak

## 📞 Kontakt i pomoc

W razie pytań lub problemów:
- Sprawdź logi: `docker-compose logs -f [service-name]`
- Dokumentacja Keycloak: https://www.keycloak.org/documentation
- Dokumentacja Spring Security OAuth2: https://spring.io/projects/spring-security-oauth

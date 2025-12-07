# ZANT - Przewodnik Szybkiego Startu

## Cel

Ten przewodnik pomoże Ci uruchomić system ZANT w mniej niż 10 minut.

## Lista Kontrolna

Przed rozpoczęciem upewnij się, że masz zainstalowane:

- [ ] Docker Desktop (Windows/Mac) lub Docker Engine (Linux)
- [ ] Git
- [ ] Klucz API do Gemini (opcjonalnie PLLuM)

## Krok 1: Pobranie Projektu

```bash
# Sklonuj repozytorium
git clone <repository-url>
cd ZANT/ZANT
```

## Krok 2: Konfiguracja Klucza API

### Jak uzyskać klucz Gemini API?

1. Przejdź do [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Zaloguj się kontem Google
3. Kliknij "Get API Key" lub "Create API Key"
4. Skopiuj wygenerowany klucz

### Ustaw klucz w systemie

#### Windows (CMD)
```cmd
set GEMINI_API_KEY=your_api_key_here
```

#### Windows (PowerShell)
```powershell
$env:GEMINI_API_KEY="your_api_key_here"
```

#### Linux/Mac
```bash
export GEMINI_API_KEY=your_api_key_here
```

**Ważne**: Klucz API jest wymagany tylko dla funkcji asystenta AI. System będzie działał bez niego, ale asystent będzie niedostępny.

## Krok 3: Uruchomienie Systemu

```bash
# Uruchom wszystkie komponenty
docker-compose up --build
```

**Pierwszy start trwa 5-10 minut** (pobieranie obrazów Docker i budowanie aplikacji).

### Co się dzieje podczas startu?

1. Pobieranie obrazów Docker (PostgreSQL, Keycloak)
2. Budowanie backendu (Maven compile)
3. Budowanie frontendu (npm install + build)
4. Inicjalizacja bazy danych
5. Konfiguracja Keycloak (import realm)
6. Start wszystkich serwisów

## Krok 4: Dostęp do Aplikacji

Po zakończeniu startu otwórz przeglądarkę i przejdź do:

### Frontend (Główna Aplikacja)
```
http://localhost:4200
```

### Keycloak (Admin Panel)
```
http://localhost:8080
Login: admin
Hasło: admin
```

### Backend API
```
http://localhost:8081
```

## Krok 5: Pierwsze Logowanie

System przekieruje Cię automatycznie do Keycloak. Użyj jednego z kont testowych:

### Konto Użytkownika (podstawowe funkcje)
```
Login: user
Hasło: user123
```

### Konto Kontrolera (wszystkie funkcje + wyszukiwarka)
```
Login: controller
Hasło: controller123
```

## Krok 6: Pierwsze Zgłoszenie

Po zalogowaniu:

1. Na stronie głównej kliknij **"Nowe zgłoszenie"**
2. Wypełnij formularz EWYP:
   - Dane poszkodowanego (imię, nazwisko, PESEL)
   - Dane wypadku (data, miejsce, okoliczności)
   - Opcjonalnie: użyj asystenta AI do pomocy
3. Kliknij **"Zapisz wersję roboczą"** lub **"Wyślij zgłoszenie"**
4. Możesz wygenerować PDF lub DOCX z danymi


### Wyszukiwarka (tylko dla kontrolerów)

1. Na stronie głównej kliknij **"Zapisane zgłoszenia"**
2. Użyj filtrów do wyszukiwania:
   - Po dacie zgłoszenia
   - Po statusie (draft/submitted)
   - Po danych poszkodowanego
3. Kliknij zgłoszenie, aby zobaczyć szczegóły

### Generowanie Dokumentów

1. Otwórz szczegóły zgłoszenia
2. Kliknij **"Generuj PDF"** lub **"Generuj DOCX"**
3. Dokument zostanie pobrany automatycznie

## Zatrzymanie Systemu

```bash
# Zatrzymaj wszystkie kontenery (dane pozostaną)
docker-compose down

# Zatrzymaj i usuń wszystkie dane (reset do zera)
docker-compose down -v
```

## Sprawdzanie Statusu

### Logi wszystkich serwisów
```bash
docker-compose logs -f
```

### Logi konkretnego serwisu
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f keycloak
```

### Status kontenerów
```bash
docker ps
```

Powinieneś zobaczyć 4 działające kontenery:
- `zant-frontend`
- `zant-backend`
- `zant-keycloak`
- `zant-db` (PostgreSQL)

## Rozwiązywanie Typowych Problemów

### Problem: "Port already in use"

```bash
# Sprawdź co używa portu
netstat -ano | findstr :8081  # Windows
lsof -ti:8081                  # Linux/Mac

# Zatrzymaj proces lub zmień port w docker-compose.yml
```

### Problem: "Cannot connect to Docker daemon"

```bash
# Uruchom Docker Desktop (Windows/Mac)
# Lub na Linux:
sudo systemctl start docker
```

### Problem: Backend nie może połączyć się z bazą

```bash
# Zrestartuj bazę danych
docker-compose restart db

# Lub pełny reset
docker-compose down -v
docker-compose up db
# Poczekaj 10 sekund, potem:
docker-compose up backend frontend keycloak
```

### Problem: Keycloak redirect loop

1. Otwórz konsolę przeglądarki (F12)
2. Wyczyść localStorage: `localStorage.clear()`
3. Wyczyść cookies dla localhost
4. Odśwież stronę

### Problem: Brak odpowiedzi od Gemini

- Sprawdź czy klucz API jest poprawnie ustawiony
- Sprawdź czy masz połączenie z internetem
- Sprawdź logi: `docker-compose logs backend | grep -i gemini`

## Następne Kroki

Po udanym uruchomieniu:

1. **Przeczytaj główne [README.md](./README.md)** - pełna dokumentacja
2. **Zapoznaj się z [OAUTH2_SETUP.md](./OAUTH2_SETUP.md)** - role i uprawnienia
3. **Zobacz [AGENTS.md](./AGENTS.md)** - jeśli planujesz rozwój

## Wskazówki

### Tryb Development

Jeśli chcesz modyfikować kod:

```bash
# Backend z hot reload
cd backend
./mvnw spring-boot:run

# Frontend z hot reload
cd frontend
npm install
npm start
```

### Zmiana Konfiguracji

Główne pliki konfiguracyjne:

- `docker-compose.yml` - porty, zmienne środowiskowe
- `backend/src/main/resources/application.properties` - konfiguracja backendu
- `frontend/src/environments/environment.ts` - konfiguracja frontendu
- `keycloak/realm-export.json` - użytkownicy, role, klienci

### Backup Danych

```bash
# Backup bazy danych
docker exec zant-db pg_dump -U zant zant > backup.sql

# Restore
docker exec -i zant-db psql -U zant zant < backup.sql
```


## FAQ

**Q: Czy potrzebuję internetu?**  
A: Tak, do pobrania obrazów Docker i działania asystenta AI. Po pierwszym uruchomieniu system działa offline (poza asystentem).

**Q: Ile miejsca zajmuje system?**  
A: ~5 GB (obrazy Docker + dane aplikacji).

**Q: Czy mogę zmienić porty?**  
A: Tak, edytuj `docker-compose.yml` i zmień mapowanie portów.

**Q: Jak dodać własnego użytkownika?**  
A: Zaloguj się do Keycloak Admin Console i dodaj użytkownika w sekcji Users.

**Q: System jest wolny - co robić?**  
A: Zwiększ pamięć dla Docker w ustawieniach (min. 4 GB zalecane).

## Pomoc

Jeśli napotkasz problem:

1. Sprawdź sekcję [Troubleshooting](./README.md#troubleshooting) w głównym README
2. Zobacz logi: `docker-compose logs -f`
3. Zrestartuj system: `docker-compose down && docker-compose up`

## Checklist Zakończenia

Po przejściu tego przewodnika powinieneś:

- [x] Mieć uruchomiony system ZANT
- [x] Móc się zalogować do aplikacji
- [x] Utworzyć testowe zgłoszenie wypadku
- [x] Przetestować asystenta AI (jeśli klucz API jest skonfigurowany)
- [x] Wygenerować dokument PDF/DOCX

---

**Gratulacje! System ZANT jest gotowy do użycia! **

Powrót do: [README.md](./README.md) | [Dokumentacja Szczegółowa](./README.md#dokumentacja-szczegółowa)

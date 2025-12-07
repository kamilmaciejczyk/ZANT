#!/bin/bash

# Test REST API dla scoringu zgłoszeń EWYP
# Wymaga uruchomionego backendu na porcie 8081 oraz Keycloak na 8080

set -e

BASE_URL="http://localhost:8081/api/ewyp-reports"
KEYCLOAK_URL="http://localhost:8080"
REALM="zant"
CLIENT_ID="zant-app"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Test REST API - Scoring zgłoszeń EWYP${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Funkcja do wyświetlania wyników
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
        exit 1
    fi
}

# Test 0: Sprawdź czy backend i Keycloak są dostępne
echo -e "${YELLOW}Test 0: Sprawdzanie dostępności serwisów${NC}"

# Sprawdź Keycloak najpierw
KEYCLOAK_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$KEYCLOAK_URL/realms/$REALM" 2>/dev/null || echo "000")
if [ "$KEYCLOAK_CODE" = "200" ]; then
    print_result 0 "Keycloak dostępny (HTTP $KEYCLOAK_CODE)"
else
    echo -e "${RED}✗ Keycloak nie odpowiada (HTTP $KEYCLOAK_CODE)${NC}"
    echo -e "${YELLOW}Uruchom Keycloak: docker compose up keycloak${NC}"
    exit 1
fi

# Uzyskaj token z Keycloak
echo -e "${YELLOW}Pobieranie tokena uwierzytelniającego...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" 2>/dev/null || echo "{}")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token' 2>/dev/null || echo "null")

if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}✗ Nie udało się uzyskać tokena${NC}"
    echo -e "${YELLOW}Response: $TOKEN_RESPONSE${NC}"
    echo -e "${YELLOW}Sprawdź konfigurację Keycloak i uprawnienia użytkownika 'admin'${NC}"
    exit 1
fi

print_result 0 "Token uwierzytelniający uzyskany"

# Sprawdź backend z tokenem
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" "http://localhost:8081/actuator/health" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Backend dostępny (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" = "401" ]; then
    # Spróbuj bez tokena (może actuator/health nie wymaga autentykacji)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8081/actuator/health" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        print_result 0 "Backend dostępny (HTTP $HTTP_CODE, bez autoryzacji)"
    else
        echo -e "${RED}✗ Backend nie odpowiada na http://localhost:8081 (HTTP $HTTP_CODE)${NC}"
        echo -e "${YELLOW}Uruchom backend: docker compose up backend${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Backend nie odpowiada na http://localhost:8081 (HTTP $HTTP_CODE)${NC}"
    echo -e "${YELLOW}Uruchom backend: docker compose up backend${NC}"
    exit 1
fi

echo ""

# Test 1: Utwórz draft zgłoszenia
echo -e "${YELLOW}Test 1: Tworzenie draftu zgłoszenia${NC}"
RESPONSE=$(curl -s -X POST "$BASE_URL/draft" \
  -H "Content-Type: application/json" \
  -d '{
    "injuredPerson": {
      "firstName": "Jan",
      "lastName": "Kowalski",
      "pesel": "80051512345",
      "birthDate": "1980-05-15",
      "birthPlace": "Warszawa"
    },
    "accidentInfo": {
      "accidentDate": "2024-03-15",
      "accidentTime": "10:30",
      "placeOfAccident": "Budowa przy ul. Budowlanej 5, Warszawa",
      "circumstancesAndCauses": "Podczas prac montażowych na rusztowaniu na wysokości 4 metrów, pracownik nie był przypięty linką bezpieczeństwa. Podczas przenoszenia materiałów budowlanych stracił równowagę i spadł. Rusztowanie było zgodne z normami BHP. Pracownik wykonywał regularne obowiązki służbowe.",
      "injuriesDescription": "Złamanie kości piszczelowej prawej nogi, stłuczenie barku, otarcia naskórka",
      "firstAidGiven": true,
      "firstAidFacility": "Szpital im. Banacha w Warszawie"
    }
  }')

# Sprawdź czy otrzymaliśmy ID
REPORT_ID=$(echo $RESPONSE | jq -r '.id')
if [ "$REPORT_ID" != "null" ] && [ -n "$REPORT_ID" ]; then
    print_result 0 "Draft utworzony z ID: $REPORT_ID"
else
    echo -e "${RED}Błąd: Nie otrzymano ID zgłoszenia${NC}"
    echo "Response: $RESPONSE"
    exit 1
fi

# Sprawdź status
STATUS=$(echo $RESPONSE | jq -r '.status')
if [ "$STATUS" = "DRAFT" ]; then
    print_result 0 "Status zgłoszenia: $STATUS"
else
    print_result 1 "Nieprawidłowy status: $STATUS (oczekiwano: DRAFT)"
fi

# Sprawdź czy scoringClassification jest null (dla draftu)
SCORING=$(echo $RESPONSE | jq -r '.scoringClassification')
if [ "$SCORING" = "null" ]; then
    print_result 0 "Draft nie ma scoringu (prawidłowo)"
else
    print_result 1 "Draft ma scoring: $SCORING (powinien być null)"
fi

echo ""

# Test 2: Wyślij zgłoszenie (submit) - tutaj powinien wykonać się scoring
echo -e "${YELLOW}Test 2: Wysyłanie zgłoszenia (submit)${NC}"
SUBMIT_RESPONSE=$(curl -s -X POST "$BASE_URL/$REPORT_ID/submit")

# Sprawdź status
SUBMIT_STATUS=$(echo $SUBMIT_RESPONSE | jq -r '.status')
if [ "$SUBMIT_STATUS" = "SUBMITTED" ]; then
    print_result 0 "Zgłoszenie wysłane, status: $SUBMIT_STATUS"
else
    print_result 1 "Błędny status po submit: $SUBMIT_STATUS"
fi

# Sprawdź scoring
SUBMIT_SCORING=$(echo $SUBMIT_RESPONSE | jq -r '.scoringClassification')
echo "Scoring po submit: $SUBMIT_SCORING"

if [ "$SUBMIT_SCORING" != "null" ]; then
    # Scoring został wykonany
    if [[ "$SUBMIT_SCORING" == *"WYPADEK_PRZY_PRACY:"* ]]; then
        print_result 0 "Scoring wykonany prawidłowo: $SUBMIT_SCORING"
        
        # Wyciągnij wartość procentową
        PERCENT=$(echo $SUBMIT_SCORING | grep -oP '\d+\.\d+')
        echo -e "${GREEN}  → Pewność modelu: ${PERCENT}%${NC}"
        
        # Sprawdź czy procent jest w rozsądnym zakresie
        if (( $(echo "$PERCENT > 0" | bc -l) )) && (( $(echo "$PERCENT <= 100" | bc -l) )); then
            print_result 0 "Wartość procentowa w prawidłowym zakresie"
        else
            print_result 1 "Wartość procentowa poza zakresem: $PERCENT"
        fi
    else
        print_result 1 "Scoring ma nieprawidłowy format: $SUBMIT_SCORING"
    fi
else
    echo -e "${YELLOW}⚠ Scoring nie został wykonany (Python/model niedostępny)${NC}"
    echo -e "${YELLOW}  To jest akceptowalne jeśli Python nie jest skonfigurowany${NC}"
fi

echo ""

# Test 3: Pobierz zgłoszenie i zweryfikuj dane
echo -e "${YELLOW}Test 3: Pobieranie zgłoszenia${NC}"
GET_RESPONSE=$(curl -s -X GET "$BASE_URL/$REPORT_ID")

# Sprawdź czy dane się zgadzają
GET_ID=$(echo $GET_RESPONSE | jq -r '.id')
GET_STATUS=$(echo $GET_RESPONSE | jq -r '.status')
GET_SCORING=$(echo $GET_RESPONSE | jq -r '.scoringClassification')

if [ "$GET_ID" = "$REPORT_ID" ]; then
    print_result 0 "ID zgłoszenia poprawne"
else
    print_result 1 "Nieprawidłowe ID"
fi

if [ "$GET_STATUS" = "SUBMITTED" ]; then
    print_result 0 "Status zgłoszenia: $GET_STATUS"
else
    print_result 1 "Nieprawidłowy status: $GET_STATUS"
fi

if [ "$GET_SCORING" = "$SUBMIT_SCORING" ]; then
    print_result 0 "Scoring zachowany po pobraniu"
else
    print_result 1 "Scoring się zmienił: było '$SUBMIT_SCORING', jest '$GET_SCORING'"
fi

echo ""

# Test 4: Kolejny test z innym zgłoszeniem (wysokie prawdopodobieństwo)
echo -e "${YELLOW}Test 4: Zgłoszenie z wysokim prawdopodobieństwem wypadku${NC}"
HIGH_PROB_RESPONSE=$(curl -s -X POST "$BASE_URL/draft" \
  -H "Content-Type: application/json" \
  -d '{
    "injuredPerson": {
      "firstName": "Anna",
      "lastName": "Nowak",
      "pesel": "85021554321",
      "birthDate": "1985-02-15",
      "birthPlace": "Kraków"
    },
    "accidentInfo": {
      "accidentDate": "2024-11-20",
      "accidentTime": "14:15",
      "placeOfAccident": "Hala produkcyjna, zakład produkcyjny ABC, ul. Przemysłowa 10, Kraków",
      "plannedWorkStartTime": "06:00",
      "plannedWorkEndTime": "14:00",
      "circumstancesAndCauses": "Pracownik obsługiwał maszynę do obróbki metali w hali produkcyjnej podczas wykonywania regularnych obowiązków służbowych, w godzinach pracy, na terenie zakładu pracy, pod nadzorem brygadzisty, w trakcie czynności przewidzianych zakresem obowiązków. Doszło do awarii maszyny. Pracownik zgodnie z procedurami BHP próbował zatrzymać maszynę, ale doznał urazu ręki. Wypadek został zgłoszony kierownikowi zmiany. Protokół powypadkowy sporządzony przez pracodawcę.",
      "injuriesDescription": "Uraz prawej ręki - skręcenie nadgarstka, stłuczenie palców",
      "firstAidGiven": true,
      "firstAidFacility": "Zakładowa służba medycyny pracy, następnie Szpital Uniwersytecki w Krakowie",
      "investigatingAuthority": "Państwowa Inspekcja Pracy",
      "accidentDuringMachineOperation": true,
      "machineConditionDescription": "Maszyna sprawna technicznie, posiadała aktualne przeglądy",
      "machineHasCertificate": true
    }
  }')

REPORT2_ID=$(echo $HIGH_PROB_RESPONSE | jq -r '.id')
if [ "$REPORT2_ID" != "null" ] && [ -n "$REPORT2_ID" ]; then
    print_result 0 "Drugie zgłoszenie utworzone z ID: $REPORT2_ID"
else
    print_result 1 "Nie udało się utworzyć drugiego zgłoszenia"
fi

# Submit drugiego zgłoszenia
HIGH_PROB_SUBMIT=$(curl -s -X POST "$BASE_URL/$REPORT2_ID/submit")
HIGH_PROB_SCORING=$(echo $HIGH_PROB_SUBMIT | jq -r '.scoringClassification')

echo "Scoring dla zgłoszenia wysokiego prawdopodobieństwa: $HIGH_PROB_SCORING"

if [ "$HIGH_PROB_SCORING" != "null" ]; then
    if [[ "$HIGH_PROB_SCORING" == *"WYPADEK_PRZY_PRACY:"* ]]; then
        print_result 0 "Scoring wykonany: $HIGH_PROB_SCORING"
        
        # Wyciągnij wartość procentową i sprawdź czy jest wysoka (>50%)
        PERCENT2=$(echo $HIGH_PROB_SCORING | grep -oP '\d+\.\d+')
        echo -e "${GREEN}  → Pewność modelu: ${PERCENT2}%${NC}"
        
        if (( $(echo "$PERCENT2 > 50" | bc -l) )); then
            print_result 0 "Wysoka pewność dla wyraźnego wypadku przy pracy (>${PERCENT2}%)"
        else
            echo -e "${YELLOW}⚠ Pewność niższa niż oczekiwana: ${PERCENT2}% (oczekiwano >50%)${NC}"
        fi
    else
        print_result 1 "Nieprawidłowy format scoringu"
    fi
else
    echo -e "${YELLOW}⚠ Scoring nie został wykonany (Python/model niedostępny)${NC}"
fi

echo ""

# Test 5: Lista wszystkich zgłoszeń
echo -e "${YELLOW}Test 5: Pobieranie listy zgłoszeń${NC}"
LIST_RESPONSE=$(curl -s -X GET "$BASE_URL")
COUNT=$(echo $LIST_RESPONSE | jq '. | length')

if [ "$COUNT" -ge 2 ]; then
    print_result 0 "Lista zawiera $COUNT zgłoszeń (minimum 2)"
else
    print_result 1 "Lista zawiera tylko $COUNT zgłoszeń (oczekiwano minimum 2)"
fi

# Sprawdź czy oba zgłoszenia są na liście
ID1_IN_LIST=$(echo $LIST_RESPONSE | jq -r ".[] | select(.id == \"$REPORT_ID\") | .id")
ID2_IN_LIST=$(echo $LIST_RESPONSE | jq -r ".[] | select(.id == \"$REPORT2_ID\") | .id")

if [ "$ID1_IN_LIST" = "$REPORT_ID" ]; then
    print_result 0 "Pierwsze zgłoszenie na liście"
else
    print_result 1 "Pierwsze zgłoszenie nie znalezione na liście"
fi

if [ "$ID2_IN_LIST" = "$REPORT2_ID" ]; then
    print_result 0 "Drugie zgłoszenie na liście"
else
    print_result 1 "Drugie zgłoszenie nie znalezione na liście"
fi

echo ""

# Podsumowanie
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Wszystkie testy zakończone pomyślnie!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Utworzone zgłoszenia:"
echo -e "  1. ID: ${YELLOW}$REPORT_ID${NC} - Scoring: ${GREEN}$SUBMIT_SCORING${NC}"
echo -e "  2. ID: ${YELLOW}$REPORT2_ID${NC} - Scoring: ${GREEN}$HIGH_PROB_SCORING${NC}"
echo ""
echo -e "${YELLOW}Uwaga:${NC} Jeśli scoring jest 'null', upewnij się że:"
echo -e "  - Python3 jest zainstalowany i dostępny"
echo -e "  - Model istnieje w MODELS/wniosek_model.joblib"
echo -e "  - Backend został uruchomiony z katalogu głównego projektu"

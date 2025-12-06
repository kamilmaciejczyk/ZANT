Jesteś ekspertem full-stack i masz wygenerować kompletną aplikację (Angular + Java Spring Boot) realizującą ETAP I systemu ZANT ZUS, korzystając z dokumentacji użytkownika.

Twoim celem jest zbudowanie Wirtualnego Asystenta, który:

1️⃣ Przyjmuje zgłoszenie wypadku od osoby prowadzącej działalność gospodarczą

– zawiadomienie o wypadku (EWYP wzór, dane jak w pliku ZUS).
– zapis wyjaśnień poszkodowanego.

2️⃣ Analizuje zgłoszenie i wskazuje brakujące elementy

Wymagania pochodzą z dokumentu ZANT. W szczególności asystent musi zbierać i walidować:

dane poszkodowanego,

dane działalności (NIP/REGON, PKD – backend może mockować),

datę i godzinę wypadku,

miejsce wypadku,

planowane godziny pracy,

czynności wykonywane przed wypadkiem,

okoliczności i przyczyny,

urazy,

dane świadków,

informacje o maszynach,

informacje o BHP, asekuracji,

informacje o udzielonej pomocy medycznej,

dane o pełnomocniku (jeśli zgłasza pełnomocnik),

listę dokumentów wymaganych do zgłoszenia.

(Źródło: ZANT – „Jakie informacje powinieneś podać...”, „Jakich jeszcze dokumentów będziemy oczekiwać…”, „Wymagania w stosunku do ZANT – Etap I”)

3️⃣ Prowadzi rozmowę w formie czat-bota

– użytkownik pisze naturalnym językiem,
– asystent wyciąga dane (sloty),
– sprawdza kompletność wg checklisty ZANT,
– generuje pytania follow-up,
– buduje kompletne zgłoszenie.

4️⃣ Generuje dokumenty PDF

Zawiadomienie o wypadku (na podstawie zebranych danych).

Wyjaśnienia poszkodowanego (sekcje takie jak w typowym druku ZUS).
Format zbliżony do PDF-ów udostępnionych przez użytkownika.

5️⃣ Backend Java – wymagania

Framework: Spring Boot 3.x

Zaimplementuj:

MODELE (DTO + state)

AssistantState

AccidentReport

PersonData

BusinessData

AccidentData

Witness

ValidationResult

AssistantTurn

PdfRequest

REST API
POST /assistant/{conversationId}/message
POST /reports/{id}/validate
POST /reports/{id}/pdf?type=NOTICE
POST /reports/{id}/pdf?type=EXPLANATION

ASYSTENT

Komponenty:

AssistantService

MissingFieldsCalculator

AiClient (używa Gemini)

PdfService (HTML → PDF)

Flow:

pobierz stan rozmowy,

wywołaj AiClient.extractInfoFromUserMessage(...),

zaktualizuj sloty w AssistantState,

oblicz brakujące pola,

wygeneruj follow-up perguntas,

zwróć AssistantTurn.

AI – format odpowiedzi

Gemini ma zwracać JSON:

{
"extractedFields": { ... },
"summaryForUser": "…",
"followUpQuestions": ["…"]
}

6️⃣ Frontend Angular – wymagania

Framework: Angular 17, narzędzia: Angular Material.

Moduły:

app/
├─ features/
│   ├─ assistant-chat/ (chat z botem)
│   └─ summary/         (podsumowanie + pobieranie PDF)
├─ core/
└─ shared/


Komponenty:

AssistantChatComponent

ChatMessageComponent

MissingFieldsPanelComponent

SummaryComponent

Serwisy:

AssistantService (REST → Java backend)

PdfService

Funkcje UI:

chat bubble UI (bot/user),

lista brakujących pól aktualizowana w czasie rzeczywistym,

pasek postępu wypełnienia (X/Y pól),

przycisk finalizacji zgłoszenia po uzupełnieniu pól obowiązkowych,

pobieranie PDF.

7️⃣ Logika checklisty (wymagane pola)

Backend musi posiadać tablicę RequiredField[] opartą o dokument ZANT:

Każdy field ma:

code: "accident.date",
section: "ACCIDENT",
label: "Data wypadku",
mandatory: true,
description: "Podaj datę wypadku (dzień, miesiąc, rok)"


Asystent musi uzupełniać i walidować pola zgodnie z tym zestawem.

8️⃣ Prompt wbudowany w AiClient

Gemini ma otrzymywać:

aktualny stan pól,

wiadomość od użytkownika,

definicję wypadku (nagłość, przyczyna zewnętrzna, uraz, związek z pracą) – ZANT, str. 2–3,

pełną checklistę wymaganych pól.

Model ma:

wyciągać dane z wiadomości,

dopisywać je do pól,

wykrywać braki,

generować pytania uzupełniające.

9️⃣ Output końcowy

CLINE musi:

wygenerować kompletny projekt backendu (Spring Boot),

wygenerować kompletny projekt frontendu (Angular),

wygenerować przykładowe HTML-template dla PDF,

zbudować pełny asystent czatowy.

Architektura musi być gotowa do uruchomienia komendami:

mvn spring-boot:run
ng serve


Backend i frontend muszą komunikować się REST-em.
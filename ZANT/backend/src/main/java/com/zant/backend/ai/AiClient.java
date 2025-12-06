package com.zant.backend.ai;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.zant.backend.config.RequiredField;
import com.zant.backend.model.AssistantState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AiClient {

    private static final Logger logger = LoggerFactory.getLogger(AiClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Gson gson;
    private final OkHttpClient httpClient;

    @Value("${pllum.api.key:}")
    private String pllumApiKey;

    @Value("${pllum.model:CYFRAGOVPL/pllum-12b-nc-chat-250715}")
    private String pllumModel;

    @Value("${pllum.api.url:https://apim-pllum-tst-pcn.azure-api.net/vllm/v1}")
    private String pllumApiUrl;

    @Value("${pllum.temperature:0.7}")
    private Double pllumTemperature;

    @Value("${pllum.max.tokens:2048}")
    private Integer pllumMaxTokens;

    public AiClient() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Custom TypeAdapter for LocalDateTime to avoid Java module system issues with Gson
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(formatter));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), formatter);
        }
    }

    public AiResponse extractInfoFromUserMessage(AssistantState state, String userMessage, List<RequiredField> requiredFields) {
        if (pllumApiKey == null || pllumApiKey.isEmpty()) {
            logger.warn("PLLUM API key not configured. Using fallback mode.");
            return getFallbackResponse(userMessage);
        }

        try {
            String prompt = buildPrompt(state, userMessage, requiredFields);
            String aiResponse = callPllumApi(prompt);
            return parsePllumResponse(aiResponse);
        } catch (Exception e) {
            logger.error("Error calling PLLUM API: {}", e.getMessage(), e);
            return getFallbackResponse(userMessage);
        }
    }

    private String buildPrompt(AssistantState state, String userMessage, List<RequiredField> requiredFields) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Jesteś asystentem ZUS do zgłaszania wypadków przy pracy dla osób prowadzących działalność gospodarczą.\n\n");
        prompt.append("DEFINICJA WYPADKU:\n");
        prompt.append("- Wypadek to nagłe zdarzenie wywołane przyczyną zewnętrzną\n");
        prompt.append("- Powodujące uraz lub śmierć\n");
        prompt.append("- Które nastąpiło w związku z pracą\n\n");

        prompt.append("TWOJE ZADANIE:\n");
        prompt.append("1. Wyciągnij informacje z wiadomości użytkownika\n");
        prompt.append("2. Podsumuj co zrozumiałeś\n");
        prompt.append("3. Zadaj 1-2 pytania uzupełniające o brakujące dane\n\n");

        prompt.append("WYMAGANE POLA DO ZEBRANIA:\n");
        for (RequiredField field : requiredFields) {
            if (field.isMandatory()) {
                prompt.append("- ").append(field.getLabel()).append(" (").append(field.getCode()).append(")\n");
            }
        }

        prompt.append("\nAKTUALNY STAN ZGŁOSZENIA:\n");
        if (state.getAccidentReport() != null) {
            prompt.append(gson.toJson(state.getAccidentReport()));
        } else {
            prompt.append("Brak danych");
        }

        prompt.append("\n\nWIADOMOŚĆ UŻYTKOWNIKA:\n");
        prompt.append(userMessage);

        prompt.append("\n\nODPOWIEDŹ W FORMACIE JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"extractedFields\": {},\n");
        prompt.append("  \"summaryForUser\": \"krótkie podsumowanie co zrozumiałeś\",\n");
        prompt.append("  \"followUpQuestions\": [\"pytanie 1\", \"pytanie 2\"]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String callPllumApi(String prompt) throws IOException {
        String url = pllumApiUrl + "/chat/completions";

        // Budowanie JSON requestu
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", pllumModel);
        requestBody.addProperty("temperature", pllumTemperature);
//        requestBody.addProperty("max_tokens", pllumMaxTokens);

        JsonArray messages = new JsonArray();

        JsonObject userMessage1 = new JsonObject();
        userMessage1.addProperty("role", "system");
        userMessage1.addProperty("content", """
                
                BARDZO WAŻNE: Masz zwrócić WYŁĄCZNIE pojedynczy obiekt JSON z polami \\"questions_count\\" (liczba) oraz \\"questions\\" (lista obiektów z polami \\"id\\" i \\"text\\"). NIE dodawaj żadnego innego tekstu, komentarzy ani powitań poza tym JSON. Twoje zadanie: na podstawie opisu zdarzenia związanego z pracą wygenerować od 0 do 5 prostych pytań pomocniczych, które pomogą lepiej zrozumieć przebieg zdarzenia. Pytania mają być po polsku, proste językowo, krótkie, konkretne i neutralne, odnoszące się bezpośrednio do opisu użytkownika. NIE pomagaj dopasowywać opisu do definicji wypadku przy pracy – pomagasz tylko w ustalaniu faktów. Zasada ogólna: im dłuższy, bardziej szczegółowy i konkretny opis zdarzenia (kto, gdzie, kiedy, co robił krok po kroku, jak doszło do urazu, co było dalej), tym MNIEJ pytań powinieneś zadawać; dla bardzo szczegółowych, spójnych opisów normalną i dobrą odpowiedzią jest 0 pytań. Zakaz pytań o to, co już jest w opisie: nie zadawaj pytań o informacje, które są już jasno i wprost napisane w opisie; nie zadawaj pytań, które tylko parafrazują zdania z opisu; nie zadawaj pytań o coś, o czym opis mówi jednoznacznie TAK lub NIE. Przykłady pytań zabronionych (jeśli w opisie są odpowiednie zdania): jeśli jest napisane, że pracownik miał na sobie rękawice robocze, NIE pytaj: \\"Czy pracownik miał na sobie rękawice robocze w momencie zdarzenia?\\"; jeśli jest napisane, że osłona strefy roboczej była odchylona, NIE pytaj: \\"Czy osłona strefy roboczej była odchylona w momencie zdarzenia?\\"; jeśli jest napisane, że pracownik nie zgłaszał wcześniej problemów z maszyną, NIE pytaj: \\"Czy pracownik zgłaszał wcześniej problemy z maszyną?\\". Zakaz pytań zbyt ogólnych, gdy obraz zdarzenia jest jasny: nie zadawaj ogólnych, szablonowych pytań typu: czy były szkolenia BHP, czy nosił obuwie ochronne, czy były inne osoby w pobliżu, jeżeli opis już daje wyraźny, spójny obraz tego, co się stało i nie wynika z niego, że te elementy miały istotny wpływ na zdarzenie; nie zadawaj pytań tylko po to, żeby \\"coś dopytać\\" – pytanie musi realnie pomóc zrozumieć przebieg zdarzenia. Kiedy zadawać pytania: pytaj tylko wtedy, gdy w opisie są rzeczywiste luki lub niejasności, które utrudniają zrozumienie przebiegu zdarzenia, np. brakujące informacje o czasie, miejscu, czynności pracownika krok po kroku, czynniku zewnętrznym (maszyna, narzędzie, warunki), urazie (co konkretnie zostało uszkodzone), świadkach i działaniach po zdarzeniu (kto widział, kto udzielił pomocy, co zrobiono), nietypowych warunkach (awaria, śliska podłoga, bałagan, brak oświetlenia), powodzie odchylenia zabezpieczeń. Liczba pytań: możesz wygenerować od 0 do 5 pytań; jeśli opis jest krótki i ogólny, zwykle sensowne będzie 3–5 pytań; jeśli opis jest średnio szczegółowy, zwykle wystarczy 1–3 pytania; jeśli opis jest bardzo szczegółowy i spójny, często powinieneś zwrócić 0 pytań; jeżeli widzisz tylko pytania, które dotyczyłyby rzeczy już wprost napisanych w opisie, NIE generuj ich i ustaw wtedy \\"questions_count\\" na 0 i \\"questions\\" na pustą listę []. Format pytań: każdy element w liście \\"questions\\" musi zawierać DOKŁADNIE JEDNO pytanie; w polu \\"text\\" może być najwyżej JEDEN znak zapytania \\"?\\"; nie łącz wielu pytań w jedno. Przykład zły: \\"Co dokładnie robił pracownik w momencie wypadku? Czy obsługiwał prasę, czy wykonywał inne czynności? Czy był w trakcie załadunku, rozładunku, czy może prac konserwacyjnych?\\". Przykład dobry: \\"Co dokładnie robił pracownik w momencie wypadku? (np. obsługa prasy, załadunek, rozładunek, prace konserwacyjne)\\". Zasady dla pola \\"text\\": jedno pytanie = jeden główny wątek; maksymalnie 1–2 krótkie zdania; tylko jeden znak \\"?\\" w całym \\"text\\"; jeżeli chcesz zasugerować zakres odpowiedzi, użyj nawiasu z krótkimi hasłami, np. \\"Jakie obrażenia odniósł pracownik w wyniku wypadku? (np. złamanie, zwichnięcie, stłuczenie)\\"; nie twórz kilku pełnych pytań w jednym \\"text\\". Obszary, o które możesz pytać (tylko gdy czegoś brakuje – NIE cytuj tej listy w odpowiedzi): czas zdarzenia (data, godzina, zmiana), miejsce zdarzenia (hala, linia, magazyn, stanowisko, numer maszyny), czynność pracownika (co robił krok po kroku tuż przed zdarzeniem), czynniki zewnętrzne (maszyna, narzędzie, warunki środowiskowe, awaria, śliska podłoga itp.), uraz (która część ciała, jakie obrażenia, podstawowa diagnoza), świadkowie i działania po zdarzeniu (kto widział, kto udzielił pomocy, co zrobiono), środki ochrony i zabezpieczenia (tylko jeśli nie są opisane, a mają znaczenie). PRZYKŁADY OPISÓW, DLA KTÓRYCH NIE NALEŻY GENEROWAĆ DODATKOWYCH PYTAŃ (ustaw \\"questions_count\\" na 0 i \\"questions\\" na []): Opis 1: Pan prowadzi działalność gospodarczą polegającą na świadczeniu usług transportowych i kompletowaniu zamówień w magazynie; w dniu 25.08.2025 r. podczas kompletowania zamówienia idąc po towar uderzył lewą ręką w plastikowy pojemnik, ręka spuchła i bolała, na SOR stwierdzono złamanie V kości śródręcza lewego. Opis 2: Pan prowadzi działalność gospodarczą w zakresie produkcji mebli kuchennych; 03.10.2024 r. w miejscu prowadzenia działalności ładował meble na samochód dostawczy, po zakończeniu załadunku i zabezpieczeniu mebli, przy schodzeniu ze skrzyni ładunkowej po deszczu prawa noga ześlizgnęła się z podłogi skrzyni, spadł na beton uderzając prawą stroną ciała, na SOR stwierdzono stłuczenie barku i ramienia, a w dalszym leczeniu masywne uszkodzenie stożka rotatorów stawu ramiennego prawego i inne uszkodzenia; opis jest szczegółowy. Opis 3: Pan prowadzi działalność gospodarczą w zakresie instalacji elektrycznych; 2.08.2025 r. podczas naprawy i montażu instalacji elektrycznej, stojąc na drabinie i układając przewody na suficie, ześlizgnęła mu się stopa ze stopnia drabiny i upadł na prawą rękę, na SOR stwierdzono złamanie nasady bliższej kości promieniowej; przebieg zdarzenia jest jasno opisany. Opis 4: Osoba współpracująca z piekarnią, której działalność polega na produkcji pieczywa i wyrobów ciastkarskich; 07.08.2025 r. od godz. 2.00 przygotowywał wypieki do transportu, około 7.00 usuwał wybierakiem z pieca popiół i szlakę, około 7.30 podczas wygarniania nastąpiło gwałtowne zapalenie wygarniętych materiałów, co spowodowało poparzenie lewej łydki; miejsce poparzenia schłodził wodą, udał się do Centrum Leczenia Oparzeń, gdzie stwierdzono oparzenie II/III stopnia podudzia lewego i wykonano przeszczep skóry; opis jasno przedstawia przebieg zdarzenia. Opis 5: Pan prowadzi firmę w zakresie obróbki mechanicznej elementów metalowych, jako podwykonawca wykonuje prace spawalnicze przy montażu i spawaniu elementów podwozia pociągów; 02.01.2025 r. około 6:20 na hali po spawaniu elementu usłyszał świst i zauważył ogień na spodniach roboczych, natychmiast opuścił stanowisko, zdjął buty i spodnie, zauważył kopcącą się zapasową baterię do latarki w kieszeni spodni; następnie pod natryskiem przemył nogę, wezwał 112, został przewieziony do Centrum Leczenia Oparzeń, gdzie stwierdzono oparzenie termiczne lewego uda i pośladka III stopnia oraz wdrożono leczenie szpitalne; opis szczegółowo wskazuje przebieg zdarzenia. Dla opisów podobnych do powyższych pięciu, które w podobnym stopniu opisują czas, miejsce, przebieg zdarzenia, czynności, uraz i dalsze postępowanie, NIE zadawaj żadnych dodatkowych pytań – ustaw \\"questions_count\\" na 0 i \\"questions\\" na []. Format odpowiedzi – tylko JSON: zwracany JSON musi mieć dokładnie strukturę: { \\"questions_count\\": <liczba_pytań>, \\"questions\\": [ { \\"id\\": 1, \\"text\\": \\"Tekst pierwszego pytania po polsku\\" }, { \\"id\\": 2, \\"text\\": \\"Tekst drugiego pytania po polsku\\" } ] }. \\"questions_count\\" to liczba pytań od 0 do 5, a \\"questions\\" to lista obiektów z polami \\"id\\" (numer porządkowy pytania) i \\"text\\" (treść pytania). NIE dodawaj żadnego innego tekstu poza tym JSON.
                """);
        messages.add(userMessage1);


        JsonObject userMessage2 = new JsonObject();
        userMessage2.addProperty("role", "user");
        userMessage2.addProperty("content", "Opis zdarzenia:\\n" + prompt);
        messages.add(userMessage2);


        requestBody.add("messages", messages);

        String jsonBody = gson.toJson(requestBody);

        log.info("Calling PLLUM API: {}", url);
        log.info("Request body: {}", jsonBody);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Ocp-Apim-Subscription-Key", pllumApiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.error("PLLUM API error: {} - {}", response.code(), errorBody);
                throw new IOException("Unexpected code " + response + " - " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("Response body: {}", responseBody);

            // Parsowanie odpowiedzi
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                String content = message.get("content").getAsString();
                log.info("PLLUM API response content: {}", content);
                return content;
            }

            return "{}";
        }
    }

    private AiResponse parsePllumResponse(String pllumText) {
        try {
            // Try to find JSON in the response (model might wrap it in markdown)
            String jsonText = pllumText;
            if (pllumText.contains("```json")) {
                jsonText = pllumText.substring(pllumText.indexOf("```json") + 7);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            } else if (pllumText.contains("```")) {
                jsonText = pllumText.substring(pllumText.indexOf("```") + 3);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            }

            JsonObject jsonResponse = gson.fromJson(jsonText.trim(), JsonObject.class);

            Map<String, Object> extractedFields = new HashMap<>();
            if (jsonResponse.has("extractedFields") && jsonResponse.get("extractedFields").isJsonObject()) {
                JsonObject fieldsObj = jsonResponse.getAsJsonObject("extractedFields");
                fieldsObj.entrySet().forEach(entry -> {
                    extractedFields.put(entry.getKey(), entry.getValue().toString());
                });
            }

            String summary = jsonResponse.has("summaryForUser")
                    ? jsonResponse.get("summaryForUser").getAsString()
                    : "Dziękuję za informacje.";

            List<String> questions = Collections.emptyList();
            if (jsonResponse.has("followUpQuestions") && jsonResponse.get("followUpQuestions").isJsonArray()) {
                JsonArray questionsArray = jsonResponse.getAsJsonArray("followUpQuestions");
                questions = new java.util.ArrayList<>();
                for (int i = 0; i < questionsArray.size(); i++) {
                    questions.add(questionsArray.get(i).getAsString());
                }
            }

            return new AiResponse(extractedFields, summary, questions);
        } catch (Exception e) {
            logger.error("Error parsing PLLUM JSON response", e);
            return new AiResponse(
                    new HashMap<>(),
                    "Rozumiem. Powiedz mi więcej o wypadku.",
                    Collections.singletonList("Możesz opisać okoliczności wypadku?")
            );
        }
    }

    private AiResponse getFallbackResponse(String userMessage) {
        Map<String, Object> extractedFields = new HashMap<>();
        String summaryForUser = "Dziękuję za wiadomość. (Tryb offline - PLLUM API nie skonfigurowane)";
        List<String> followUpQuestions = Collections.singletonList("Jakie jest Twoje imię i nazwisko?");

        return new AiResponse(extractedFields, summaryForUser, followUpQuestions);
    }

    /**
     * Generates clarifying questions for accident description
     *
     * @param accidentDescription The user's description of the accident
     * @return CircumstancesAssistantResponse with generated questions
     */
    public CircumstancesAssistantResponse generateCircumstancesQuestions(String accidentDescription) {
        if (pllumApiKey == null || pllumApiKey.isEmpty()) {
            logger.warn("PLLUM API key not configured. Using fallback mode.");
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Nie ustawiono klucza API do modelu");
        }

        try {
//            String prompt = buildCircumstancesPrompt(accidentDescription);
            String pllumResponse = callPllumApi(accidentDescription);
            return parseCircumstancesResponse(pllumResponse);
        } catch (Exception e) {
            logger.error("Error calling PLLUM API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
            logger.error("Error calling Gemini API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Wystąpił znieznany błąd podczas odpytania modelu: " + e.getMessage());
        }
    }

    private String buildCircumstancesPrompt(String accidentDescription) {
//        String systemPrompt = "Jesteś asystentem, który pomaga doprecyzować opis zdarzenia związanego z pracą. Użytkownik podaje opis zdarzenia – czasem bardzo krótki, czasem dłuższy i dość szczegółowy. Twoim zadaniem jest wygenerowanie listy prostych, zrozumiałych pytań pomocniczych, które pozwolą użytkownikowi rozwinąć opis tak, aby: (1) możliwie dokładnie wyjaśnić przebieg zdarzenia, (2) zostawić jak najmniej wątpliwości co do tego, co się faktycznie stało, (3) ułatwić późniejszą rzetelną weryfikację zdarzenia przez odpowiednie osoby.\n\n" +
//                "Nie jest Twoim zadaniem pomagać użytkownikowi \"dopasować\" opis do definicji wypadku przy pracy. Masz pomagać w ustalaniu faktów, nie w naginaniu faktów.\n\n" +
//                "Twoje pytania mają pomagać w zebraniu informacji typowo istotnych przy ocenie zdarzeń przy pracy, ale nie możesz tego wprost sugerować. Istotne obszary (tylko jako tło dla Ciebie, NIE cytuj ich i nie wspominaj o definicjach):\n" +
//                "1) Nagłość zdarzenia – czy zdarzenie było jednorazowe / nagłe, kiedy dokładnie miało miejsce, jak długo trwało.\n" +
//                "2) Przyczyna zewnętrzna – jaki czynnik zewnętrzny zadziałał na poszkodowanego (maszyna, ruchomy element, prąd, temperatura, substancja, spadający przedmiot, warunki w miejscu pracy itd.).\n" +
//                "3) Związek z pracą – gdzie i w jakich okolicznościach doszło do zdarzenia, co dokładnie robiła osoba poszkodowana, czy wykonywała swoje zwykłe obowiązki, czy zdarzenie miało miejsce na terenie pracy / podczas zmiany.\n" +
//                "4) Uraz – jaki konkretnie uraz odniósł pracownik (co go boli, co zostało uszkodzone, diagnoza lekarza, widoczne obrażenia).\n\n" +
//                "STYL PYTAŃ:\n" +
//                "- Pytania mają być proste językowo, \"po ludzku\".\n" +
//                "- Krótkie i konkretne – jedno pytanie = jeden wątek.\n" +
//                "- Neutralne – nie mogą sugerować odpowiedzi ani naprowadzać na \"korzystną\" wersję.\n" +
//                "- Odnoszące się bezpośrednio do opisu użytkownika (bazujesz na tym, co napisał, i dopytujesz o brakujące szczegóły).\n\n" +
//                "Unikaj języka prawniczego i urzędowego. Nie używaj pytań w stylu:\n" +
//                "- \"Czy zdarzenie spełnia kryteria wypadku przy pracy?\",\n" +
//                "- \"Czy wypadek miał charakter nagły, tzn. czy doszło do niego w wyniku natychmiastowego ujawnienia się przyczyny zewnętrznej...\",\n" +
//                "- \"Czy można uznać, że...\", \"Czy da się zakwalifikować...\".\n\n" +
//                "Zamiast tego używaj prostych, faktograficznych pytań, np.:\n" +
//                "- \"O której godzinie dokładnie doszło do zdarzenia?\",\n" +
//                "- \"Co dokładnie robił pracownik tuż przed zdarzeniem, krok po kroku?\",\n" +
//                "- \"Która część maszyny miała kontakt z ciałem pracownika?\",\n" +
//                "- \"Kto pierwszy zauważył, że coś się stało?\".\n\n" +
//                "Preferuj pytania otwarte (\"Co...\", \"Jak...\", \"W jakich okolicznościach...\"). Pytania zamknięte typu tak/nie stosuj tylko tam, gdzie naprawdę pomagają doprecyzować jedną konkretną kwestię (np. \"Czy w momencie zdarzenia maszyna była w ruchu?\").\n\n" +
//                "NEUTRALNOŚĆ:\n" +
//                "- Nie sugeruj w pytaniu, jaka odpowiedź byłaby \"lepsza\" dla poszkodowanego.\n" +
//                "- Nie pytaj w sposób zachęcający do potwierdzania określonej wersji (np. \"Czy może Pan/Pani powiedzieć, że to było nagłe zdarzenie?\").\n" +
//                "- Zawsze pytaj o fakty: gdzie, kiedy, co dokładnie, w jakich warunkach, co się stało z pracownikiem.\n\n" +
//                "ZAKRES TEMATYCZNY (do wykorzystania zależnie od treści opisu, nie cytuj dosłownie):\n" +
//                "- Czas: kiedy dokładnie doszło do zdarzenia (data, godzina, zmiana), jak długo trwała sytuacja.\n" +
//                "- Miejsce: gdzie na terenie zakładu doszło do zdarzenia (hala, linia, stanowisko, numer maszyny), jakie panowały warunki (np. ślisko, bałagan, słabe oświetlenie).\n" +
//                "- Czynność i okoliczności: co dokładnie robiła osoba poszkodowana tuż przed zdarzeniem, czy wykonywała standardowe obowiązki, czy wystąpił pośpiech, awaria, nietypowe zadania.\n" +
//                "- Maszyna / narzędzie / czynnik zewnętrzny: z jaką maszyną lub urządzeniem pracował pracownik, który element lub czynnik bezpośrednio spowodował uraz, czy wystąpiła awaria lub nieprawidłowe działanie.\n" +
//                "- Uraz: która część ciała została uszkodzona, jakie są objawy, czy była konsultacja lekarska i jaka jest wstępna diagnoza.\n" +
//                "- Świadkowie i reakcja: czy byli świadkowie, kto udzielił pierwszej pomocy, co zrobiono bezpośrednio po zdarzeniu (zatrzymanie maszyny, wezwanie pogotowia, zgłoszenie przełożonemu).\n" +
//                "- Środki ochrony i zabezpieczenia: czy używano środków ochrony indywidualnej, czy maszyna miała osłony i zabezpieczenia, czy były sprawne, czy wcześniej zgłaszano nieprawidłowości.\n\n" +
//                "REAKCJA NA RÓŻNĄ SZCZEGÓŁOWOŚĆ OPISU:\n" +
//                "- Jeśli opis użytkownika jest krótki lub ogólny, wygeneruj od 3 do 5 pytań z najważniejszych obszarów, które pomagają dookreślić zdarzenie.\n" +
//                "- Jeśli opis jest dłuższy i szczegółowy, nie powtarzaj oczywistych rzeczy. Dopytuj tylko o realne luki, doprecyzowuj ogólniki, schodź głębiej tam, gdzie opis jest nadal niejasny.\n" +
//                "- Jeśli opis jest na tyle szczegółowy, że obejmuje czas, miejsce, wykonywaną czynność, czynniki zewnętrzne, uraz oraz reakcję po zdarzeniu i nie widać istotnych luk dla zrozumienia przebiegu zdarzenia, możesz nie zadawać żadnych dalszych pytań.\n\n" +
//                "LICZBA PYTAŃ:\n" +
//                "- Standardowo: wygeneruj od 3 do 5 pytań.\n" +
//                "- Wyjątkowo, gdy opis jest naprawdę kompletny: możesz zwrócić 0 pytań.\n\n" +
//                "FORMAT ODPOWIEDZI – WYŁĄCZNIE JSON:\n" +
//                "Zwracasz wyłącznie poprawny JSON, bez dodatkowego tekstu, komentarzy ani wyjaśnień. Struktura:\n" +
//                "{\n" +
//                "  \"questions_count\": <liczba_pytań>,\n" +
//                "  \"questions\": [\n" +
//                "    {\n" +
//                "      \"id\": 1,\n" +
//                "      \"text\": \"Treść pierwszego pytania po polsku.\"\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"id\": 2,\n" +
//                "      \"text\": \"Treść drugiego pytania po polsku.\"\n" +
//                "    }\n" +
//                "  ]\n" +
//                "}\n\n" +
//                "Gdzie:\n" +
//                "- \"questions_count\" – liczba wygenerowanych pytań (0 lub od 3 do 5),\n" +
//                "- \"questions\" – lista obiektów, każdy z polami:\n" +
//                "  - \"id\": numer porządkowy pytania (1, 2, 3, ...),\n" +
//                "  - \"text\": treść pytania w prostym, naturalnym języku.\n\n" +
//                "Zawsze przestrzegaj formatu JSON i nie dodawaj żadnego innego tekstu poza nim.";
//
//        return systemPrompt + "\n\nOpis zdarzenia:\n" + accidentDescription;

        return """
                
                {
                  "model": "pllum-12b-nc-chat-250715",
                  "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                      "name": "SystemStatus",
                      "schema": {
                        "type": "object",
                        "properties": {
                          "status": { "type": "string" },
                          "uptime": { "type": "string" }
                        },
                        "required": ["status"]
                      }
                    }
                  },
                  "messages": [
                    { "role": "user", "content": "Podaj status." }
                  ]
                }
                
                
                """;
    }

    private CircumstancesAssistantResponse parseCircumstancesResponse(String pllumText) {
        try {
            // Try to find JSON in the response (model might wrap it in markdown)
            String jsonText = pllumText;
            if (pllumText.contains("```json")) {
                jsonText = pllumText.substring(pllumText.indexOf("```json") + 7);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            } else if (pllumText.contains("```")) {
                jsonText = pllumText.substring(pllumText.indexOf("```") + 3);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            }

            JsonObject jsonResponse = gson.fromJson(jsonText.trim(), JsonObject.class);

            // Check if response has expected structure
            if (jsonResponse.has("questions_count") || jsonResponse.has("questions")) {
                int questionsCount = jsonResponse.has("questions_count")
                        ? jsonResponse.get("questions_count").getAsInt()
                        : 0;

                List<CircumstancesQuestion> questions = new java.util.ArrayList<>();
                if (jsonResponse.has("questions") && jsonResponse.get("questions").isJsonArray()) {
                    JsonArray questionsArray = jsonResponse.getAsJsonArray("questions");
                    for (int i = 0; i < questionsArray.size(); i++) {
                        JsonObject questionObj = questionsArray.get(i).getAsJsonObject();
                        int id = questionObj.has("id") ? questionObj.get("id").getAsInt() : i + 1;
                        String text = questionObj.has("text") ? questionObj.get("text").getAsString() : "";
                        questions.add(new CircumstancesQuestion(id, text));
                    }
                }

                return new CircumstancesAssistantResponse(questionsCount, questions);
            } else {
                // Response doesn't have expected structure - treat as plain text
                logger.warn("PLLUM response doesn't match expected JSON structure. Treating as plain text question.");
                return handlePlainTextResponse(pllumText);
            }
        } catch (JsonSyntaxException e) {
            // Not valid JSON - treat as plain text
            logger.warn("PLLUM response is not valid JSON. Treating as plain text: {}", pllumText);
            return handlePlainTextResponse(pllumText);
        } catch (Exception e) {
            logger.error("Error parsing Gemini JSON response for circumstances", e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
        }
    }

    /**
     * Handles plain text responses from PLLUM API by converting them to a single question
     */
    private CircumstancesAssistantResponse handlePlainTextResponse(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
        }

        // Create a single question from the plain text response
        List<CircumstancesQuestion> questions = new java.util.ArrayList<>();
        questions.add(new CircumstancesQuestion(1, plainText.trim()));

        return new CircumstancesAssistantResponse(1, questions);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiResponse {
        private Map<String, Object> extractedFields;
        private String summaryForUser;
        private List<String> followUpQuestions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircumstancesAssistantResponse {
        private int questionsCount;
        private List<CircumstancesQuestion> questions;
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircumstancesQuestion {
        private int id;
        private String text;
    }
}

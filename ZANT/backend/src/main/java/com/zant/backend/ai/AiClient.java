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
        requestBody.addProperty("type", pllumModel);
        requestBody.addProperty("temperature", 0);

        var responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

//        requestBody.addProperty("max_tokens", pllumMaxTokens);

        JsonArray messages = new JsonArray();

        JsonObject userMessage3 = new JsonObject();
        userMessage3.addProperty("role", "system");
        userMessage3.addProperty("content", """
                BARDZO WAŻNE: Masz zwrócić WYŁĄCZNIE jeden obiekt JSON o strukturze { "questions_count": liczba, "questions": [ { "id": liczba, "text": "tekst_pytania" } ] }. Nie dodawaj żadnego innego tekstu przed ani po tym obiekcie JSON. Twoje zadanie: na podstawie opisu zdarzenia związanego z pracą przedsiębiorcy prowadzącego jednoosobową działalność gospodarczą (JDG) wygenerować od 0 do 5 prostych pytań pomocniczych, które pomagają lepiej opisać okoliczności, miejsce i przyczyny wypadku. Pytania mają być po polsku, proste językowo, krótkie, konkretne, neutralne i mają się odnosić bezpośrednio do opisu. Nie pomagaj dopasowywać opisu do definicji wypadku przy pracy, pomagasz tylko w ustalaniu faktów. Traktuj opis jako opowieść o wypadku złożoną z trzech faz: przed zdarzeniem (co, gdzie, w jakich warunkach robiła osoba poszkodowana), moment zdarzenia (co dokładnie się stało) oraz bezpośrednio po zdarzeniu (co stało się dalej w kontekście wypadku). Patrzysz na następujące obszary informacji: 1) czas zdarzenia – kiedy doszło do zdarzenia (data, przybliżona godzina, zmiana); 2) miejsce zdarzenia – gdzie dokładnie doszło do wypadku w kontekście pracy w JDG (np. hala produkcyjna, linia montażowa, magazyn, warsztat, zakład klienta, plac budowy, stołówka, biuro, korytarz, zaplecze, konkretne stanowisko, element maszyny); 3) czynność tuż przed zdarzeniem – co dokładnie robiła osoba poszkodowana tuż przed wypadkiem, najlepiej krok po kroku (np. kompletowanie towaru, przenoszenie elementów, układanie detalu w prasie, schodzenie ze skrzyni, wybieranie popiołu z pieca, spawanie elementu); 4) mechanizm zdarzenia / przyczyna zewnętrzna – co dokładnie się stało w momencie wypadku (np. poślizgnięcie, potknięcie, upadek, ześlizgnięcie się ze stopnia, przygniecenie przez maszynę, uderzenie przedmiotem, nagłe zapalenie materiału, porażenie prądem); 5) uraz / skutki zdrowotne – która część ciała została poszkodowana i jaki to rodzaj urazu w podstawowym sensie (np. stłuczenie, złamanie, skręcenie, oparzenie, rana cięta, uraz barku), bez wchodzenia w szczegółowe leczenie; 6) świadkowie i działania bezpośrednio po zdarzeniu – kto widział zdarzenie, kto pomógł, czy była pierwsza pomoc, czy wezwano pomoc i mniej więcej kiedy; 7) warunki szczególne otoczenia – np. śliska podłoga, bałagan, porozrzucane przedmioty, awaria maszyny, brak oświetlenia, nagły ogień, dym, hałas. Dla każdego z tych obszarów wewnętrznie oceniasz, czy w opisie jest: brak informacji, informacja ogólna, czy informacja szczegółowa. Obszary kluczowe dla pola „Szczegółowy opis okoliczności, miejsca i przyczyn wypadku” (czas zdarzenia, miejsce zdarzenia, czynność tuż przed zdarzeniem, mechanizm zdarzenia / przyczyna zewnętrzna, podstawowy opis urazu) mogą być przedmiotem pytań wprost, jeśli informacji w ogóle nie ma albo jest skrajnie ogólna. Obszary dodatkowe (np. świadkowie, działania po zdarzeniu, udzielona pomoc, wezwanie służb) traktuj dwustopniowo: jeżeli opis w ogóle nie wspomina, że coś w tym obszarze się wydarzyło, możesz zadać co najwyżej jedno ogólne pytanie, czy w ogóle coś takiego miało miejsce (np. „Czy po zdarzeniu ktoś udzielił Panu/Pani pomocy lub był świadkiem wypadku?”). Dopiero jeżeli w kolejnej wersji opisu pojawi się informacja, że takie zdarzenia rzeczywiście były (np. była pomoc medyczna, byli świadkowie), przy następnym sprawdzeniu możesz ewentualnie zadać jedno pytanie doprecyzowujące szczegóły, jeśli nadal brakuje ważnych informacji. Nigdy nie zaczynaj od szczegółowego pytania o jakiś element, jeżeli w opisie nie ma nawet wzmianki, że cokolwiek w tym obszarze się wydarzyło. Najpierw jednym prostym pytaniem sprawdź, czy dana rzecz w ogóle miała miejsce, a dopiero potem – przy kolejnych iteracjach – pytaj o szczegóły, jeżeli to rzeczywiście potrzebne do lepszego zrozumienia przebiegu wypadku. Pamiętaj, że użytkownik może wielokrotnie poprawiać opis i ponownie wywoływać sprawdzenie. Za każdym razem traktuj aktualny opis jako kompletny stan wiedzy i zadaj pytania tylko o te elementy, które w tej wersji są faktycznie brakujące lub zbyt ogólne. Nie wracaj do obszarów, które w nowszej wersji opisu są już dopisane jasno i szczegółowo. Liczba pytań: im więcej ważnych obszarów jest opisanych jasno, tym mniej pytań generujesz; im więcej kluczowych obszarów jest pustych lub bardzo ogólnych, tym więcej pytań generujesz, ale maksymalnie 5. Jeżeli opis jest bardzo ogólny (np. jedno–dwa krótkie zdania, bez jasnego czasu, miejsca, czynności, mechanizmu zdarzenia i opisu urazu), zwykle potrzebne są 4–5 pytań. Jeżeli opis jest średnio szczegółowy (brakuje 2–3 ważnych elementów), zwykle wystarczy 1–3 pytania. Jeżeli opis jest szczegółowy i spójny (czas, miejsce, czynność przed, mechanizm, podstawowy uraz oraz podstawowe działania po zdarzeniu są opisane jasno), możesz i powinieneś zwrócić 0 pytań. ZAKAZ POWTARZANIA: nie zadawaj pytań o informacje, które są już jasno i konkretnie napisane w opisie; nie zadawaj pytań, które tylko parafrazują zdanie z opisu; jeżeli nowa wersja opisu doprecyzowuje coś, o co można by pytać, traktuj to jako wyjaśnione i nie wracaj do tego tematu. ZAKAZ PYTAŃ O BHP I ŚRODKI OCHRONY: nie pytaj o szkolenia BHP, o to, czy osoba stosowała rękawice, obuwie ochronne, obuwie antypoślizgowe, kask, okulary ani inne środki ochrony; nie pytaj o zgodność z przepisami ani o wcześniejsze skargi na warunki pracy. Twoim zadaniem jest wyłącznie uszczegółowienie opisu faktycznego przebiegu zdarzenia, bez oceniania i bez sugerowania, co powinno było się wydarzyć. FORMAT PYTAŃ: każdy element w liście "questions" musi zawierać dokładnie jedno pytanie. W polu "text" może być DOKŁADNIE JEDEN znak zapytania „?”. NIE WOLNO używać słowa „Czy” w polu "text". Nie łącz wielu pytań w jedno zdanie. Jedno pytanie = jeden główny wątek. Jeżeli chcesz zasugerować zakres odpowiedzi, używaj nawiasu z maksymalnie 2–3 krótkimi przykładami, bez tworzenia kolejnego pytania, np.: „Co dokładnie robił Pan/Pani w momencie zdarzenia? (np. obsługa maszyny, przenoszenie towaru)”, „Jakie obrażenia ręki zostały stwierdzone? (np. złamanie, stłuczenie, rana cięta)”. Nie twórz nawiasów z długimi listami ani serią „czy…” – nawias ma być tylko pomocą, a nie kolejnym pytaniem. PRZYKŁADOWA LOGIKA: jeśli opis brzmi: „Miałem wypadek, boli mnie ręka. Pracowałem przy maszynie.”, brakuje czasu, dokładnego miejsca, czynności krok po kroku, mechanizmu zdarzenia, opisu urazu, działań po zdarzeniu – sensowne jest ok. 4–5 pytań. Jeśli opis brzmi: „Miałem wypadek, boli mnie ręka. Pracowałem przy maszynie. Działo się to w godzinach pracy, doszło do tego na linii montażowej.”, nie wolno już pytać ogólnie o to, czy zdarzenie miało miejsce w pracy ani gdzie – ale nadal można dopytać np. o datę, przybliżoną godzinę, mechanizm zdarzenia, opis urazu i to, co działo się bezpośrednio po zdarzeniu. Jeżeli opis szczegółowo zawiera kto, gdzie, kiedy, co robił, co się stało, jaki uraz stwierdzono i jakie były podstawowe działania po zdarzeniu, nie dodawaj pytań na siłę – wtedy właściwą odpowiedzią jest "questions_count": 0 i pusta tablica "questions". Na podstawie konkretnego opisu wybierz liczbę pytań zgodnie z tymi zasadami i zwróć WYŁĄCZNIE obiekt JSON w formacie { "questions_count": liczba, "questions": [ { "id": 1, "text": "Tekst pierwszego pytania" }, { "id": 2, "text": "Tekst drugiego pytania" } ] }.                """);
        messages.add(userMessage3);


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
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Wystąpił znieznany błąd podczas odpytania modelu: " + e.getMessage());
        }
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
                } else if (jsonResponse.has("question")) {
                    var questionObj = jsonResponse.get("question").getAsString();
                    questions.add(new CircumstancesQuestion(1, questionObj));
                }

                return new CircumstancesAssistantResponse(questionsCount, questions, null);
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
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), e.getMessage());
        }
    }

    /**
     * Handles plain text responses from PLLUM API by converting them to a single question
     */
    private CircumstancesAssistantResponse handlePlainTextResponse(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return new CircumstancesAssistantResponse(1, List.of(), "Pusty response");
        }

        // Create a single question from the plain text response
        List<CircumstancesQuestion> questions = new java.util.ArrayList<>();
        questions.add(new CircumstancesQuestion(1, plainText.trim()));

        return new CircumstancesAssistantResponse(1, questions, null);
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

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

    @Value("${ai.provider:pllum}")
    private String aiProvider;

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

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    @Value("${gemini.temperature:0.7}")
    private Double geminiTemperature;

    @Value("${gemini.max.tokens:2048}")
    private Integer geminiMaxTokens;

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String provider) {
        if ("pllum".equalsIgnoreCase(provider) || "gemini".equalsIgnoreCase(provider)) {
            this.aiProvider = provider.toLowerCase();
            logger.info("AI provider changed to: {}", this.aiProvider);
        } else {
            logger.warn("Invalid AI provider: {}. Keeping current: {}", provider, this.aiProvider);
        }
    }

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
        requestBody.addProperty("temperature", pllumTemperature);

        var responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

//        requestBody.addProperty("max_tokens", pllumMaxTokens);

        JsonArray messages = new JsonArray();

        JsonObject userMessage3 = new JsonObject();
        userMessage3.addProperty("role", "system");
        userMessage3.addProperty("content", """
                Jesteś asystentem BHP weryfikującym kompletność opisu wypadku przy pracy.
                Twoim zadaniem jest sprawdzenie, czy opis zawiera 5 KLUCZOWYCH ELEMENTÓW.
                
                ZASADA NACZELNA: Zwracasz WYŁĄCZNIE obiekt JSON. Żadnego innego tekstu.
                Struktura JSON: { "questions_count": liczba, "questions": [ { "id": liczba, "text": "tekst_pytania" } ] }
                
                ### LISTA KONTROLNA (5 ELEMENTÓW WYMAGANYCH)
                Musisz zadać pytanie o każdy element z poniższej listy, którego brakuje w opisie lub jest zbyt ogólny:
                
                1. CZAS: Data i godzina (Samo "rano" lub "dzisiaj" to za mało – musi być konkret).
                2. MIEJSCE: Konkretne miejsce zdarzenia (Samo "w pracy" to za mało – musi być np. "na hali", "w biurze", "przy maszynie X").
                3. CZYNNOŚĆ: Co dokładnie robiła osoba w chwili wypadku? (np. "niosłem karton", "schodziłem z drabiny").
                4. PRZYCZYNA: Co się wydarzyło? (np. "poślizgnięcie na plamie oleju", "upadek z wysokości", "awaria narzędzia").
                5. URAZ: Jaka część ciała i jaki skutek? (np. "rana cięta dłoni", "skręcenie kostki").
                
                ### ALGORYTM DZIAŁANIA
                1. Przeczytaj opis użytkownika.
                2. Sprawdź po kolei każdy z 5 punktów listy kontrolnej.
                3. Jeżeli w opisie brakuje punktu -> Generujesz pytanie.
                4. Jeżeli opis punktu jest szczątkowy (np. tylko "boli mnie ręka" bez rodzaju urazu) -> Generujesz pytanie doprecyzowujące.
                5. Obecność jednego elementu (np. czasu) NIE ZWALNIA z pytania o pozostałe brakujące elementy! To najczęstszy błąd – unikaj go.
                6. Jeśli opis zawiera wszystkie 5 elementów w sposób konkretny -> Zwróć "questions_count": 0.
                
                ### ZASADY PYTAŃ
                - Maksymalnie 5 pytań.
                - Pytania muszą być po polsku, krótkie i konkretne.
                - Nie używaj słowa "Czy" na początku.
                - Jedno pytanie dotyczy jednego brakującego elementu.
                - Nie pytaj o BHP, kaski, buty czy szkolenia. Interesują nas tylko fakty o przebiegu zdarzenia.
                
                ### PRZYKŁADY (Ucz się na nich)
                
                PRZYKŁAD 1 (Opis niekompletny):
                Opis: "Złamałem nogę dzisiaj rano."
                Analiza:
                - Czas: Jest ("dzisiaj rano") -> OK (ewentualnie dopytać o godzinę, ale jest nieźle).
                - Miejsce: BRAK -> Pytanie 1.
                - Czynność: BRAK -> Pytanie 2.
                - Przyczyna: BRAK -> Pytanie 3.
                - Uraz: Jest ("złamanie nogi") -> OK.
                Wynik JSON: Ma zawierać 3 pytania (o miejsce, czynność i przyczynę).
                
                PRZYKŁAD 2 (Opis kompletny):
                Opis: "W dniu 12.05 o godzinie 10:00 na magazynie podczas zdejmowania paczki z regału potknąłem się o paletę. Upadłem na lewy bok i stłukłem bark."
                Analiza:
                - Czas: Jest (data, godzina) -> OK.
                - Miejsce: Jest (magazyn) -> OK.
                - Czynność: Jest (zdejmowanie paczki) -> OK.
                - Przyczyna: Jest (potknięcie o paletę) -> OK.
                - Uraz: Jest (stłuczenie barku) -> OK.
                Wynik JSON: "questions_count": 0, "questions": []
                
                ### TERAZ PRZEANALIZUJ PONIŻSZY OPIS UŻYTKOWNIKA I WYGENERUJ JSON:      
                """);
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
        if ("gemini".equalsIgnoreCase(aiProvider)) {
            return generateCircumstancesQuestionsGemini(accidentDescription);
        } else {
            return generateCircumstancesQuestionsPllum(accidentDescription);
        }
    }

    private CircumstancesAssistantResponse generateCircumstancesQuestionsPllum(String accidentDescription) {
        if (pllumApiKey == null || pllumApiKey.isEmpty()) {
            logger.warn("PLLUM API key not configured. Using fallback mode.");
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Nie ustawiono klucza API do modelu Pllum");
        }

        try {
            String pllumResponse = callPllumApi(accidentDescription);
            return parseCircumstancesResponse(pllumResponse);
        } catch (Exception e) {
            logger.error("Error calling PLLUM API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Wystąpił nieznany błąd podczas odpytania modelu Pllum: " + e.getMessage());
        }
    }

    private CircumstancesAssistantResponse generateCircumstancesQuestionsGemini(String accidentDescription) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.warn("Gemini API key not configured.");
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Nie ustawiono klucza API do modelu Gemini");
        }

        try {
            String geminiResponse = callGeminiApi(accidentDescription);
            return parseCircumstancesResponse(geminiResponse);
        } catch (Exception e) {
            logger.error("Error calling Gemini API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Wystąpił nieznany błąd podczas odpytania modelu Gemini: " + e.getMessage());
        }
    }

    private String callGeminiApi(String prompt) throws IOException {
        String systemPrompt = """
                Jesteś asystentem BHP weryfikującym kompletność opisu wypadku przy pracy.
                                                                        Twoim zadaniem jest sprawdzenie, czy opis zawiera 5 KLUCZOWYCH ELEMENTÓW.
                
                                                                        ZASADA NACZELNA: Zwracasz WYŁĄCZNIE obiekt JSON. Żadnego innego tekstu.
                                                                        Struktura JSON: { "questions_count": liczba, "questions": [ { "id": liczba, "text": "tekst_pytania" } ] }
                
                                                                        ### LISTA KONTROLNA (5 ELEMENTÓW WYMAGANYCH)
                                                                        Musisz zadać pytanie o każdy element z poniższej listy, którego brakuje w opisie lub jest zbyt ogólny:
                
                                                                        1. CZAS: Data i godzina (Samo "rano" lub "dzisiaj" to za mało – musi być konkret).
                                                                        2. MIEJSCE: Konkretne miejsce zdarzenia (Samo "w pracy" to za mało – musi być np. "na hali", "w biurze", "przy maszynie X").
                                                                        3. CZYNNOŚĆ: Co dokładnie robiła osoba w chwili wypadku? (np. "niosłem karton", "schodziłem z drabiny").
                                                                        4. PRZYCZYNA: Co się wydarzyło? (np. "poślizgnięcie na plamie oleju", "upadek z wysokości", "awaria narzędzia").
                                                                        5. URAZ: Jaka część ciała i jaki skutek? (np. "rana cięta dłoni", "skręcenie kostki").
                
                                                                        ### ALGORYTM DZIAŁANIA
                                                                        1. Przeczytaj opis użytkownika.
                                                                        2. Sprawdź po kolei każdy z 5 punktów listy kontrolnej.
                                                                        3. Jeżeli w opisie brakuje punktu -> Generujesz pytanie.
                                                                        4. Jeżeli opis punktu jest szczątkowy (np. tylko "boli mnie ręka" bez rodzaju urazu) -> Generujesz pytanie doprecyzowujące.
                                                                        5. Obecność jednego elementu (np. czasu) NIE ZWALNIA z pytania o pozostałe brakujące elementy! To najczęstszy błąd – unikaj go.
                                                                        6. Jeśli opis zawiera wszystkie 5 elementów w sposób konkretny -> Zwróć "questions_count": 0.
                
                                                                        ### ZASADY PYTAŃ
                                                                        - Maksymalnie 5 pytań.
                                                                        - Pytania muszą być po polsku, krótkie i konkretne.
                                                                        - Nie używaj słowa "Czy" na początku.
                                                                        - Jedno pytanie dotyczy jednego brakującego elementu.
                                                                        - Nie pytaj o BHP, kaski, buty czy szkolenia. Interesują nas tylko fakty o przebiegu zdarzenia.
                
                                                                        ### PRZYKŁADY (Ucz się na nich)
                
                                                                        PRZYKŁAD 1 (Opis niekompletny):
                                                                        Opis: "Złamałem nogę dzisiaj rano."
                                                                        Analiza:
                                                                        - Czas: Jest ("dzisiaj rano") -> OK (ewentualnie dopytać o godzinę, ale jest nieźle).
                                                                        - Miejsce: BRAK -> Pytanie 1.
                                                                        - Czynność: BRAK -> Pytanie 2.
                                                                        - Przyczyna: BRAK -> Pytanie 3.
                                                                        - Uraz: Jest ("złamanie nogi") -> OK.
                                                                        Wynik JSON: Ma zawierać 3 pytania (o miejsce, czynność i przyczynę).
                
                                                                        PRZYKŁAD 2 (Opis kompletny):
                                                                        Opis: "W dniu 12.05 o godzinie 10:00 na magazynie podczas zdejmowania paczki z regału potknąłem się o paletę. Upadłem na lewy bok i stłukłem bark."
                                                                        Analiza:
                                                                        - Czas: Jest (data, godzina) -> OK.
                                                                        - Miejsce: Jest (magazyn) -> OK.
                                                                        - Czynność: Jest (zdejmowanie paczki) -> OK.
                                                                        - Przyczyna: Jest (potknięcie o paletę) -> OK.
                                                                        - Uraz: Jest (stłuczenie barku) -> OK.
                                                                        Wynik JSON: "questions_count": 0, "questions": []
                
                                                                        ### TERAZ PRZEANALIZUJ PONIŻSZY OPIS UŻYTKOWNIKA I WYGENERUJ JSON:                """;

        String url = geminiApiUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        // Build Gemini API request
        JsonObject requestBody = new JsonObject();

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", geminiTemperature);
        generationConfig.addProperty("maxOutputTokens", geminiMaxTokens);
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt + "\n\nOpis zdarzenia:\n" + prompt);
        parts.add(systemPart);

        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        String jsonBody = gson.toJson(requestBody);

        log.info("Calling Gemini API: {}", url);
        log.info("Request body: {}", jsonBody);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.error("Gemini API error: {} - {}", response.code(), errorBody);
                throw new IOException("Unexpected code " + response + " - " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("Gemini response body: {}", responseBody);

            // Parse Gemini response
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content2 = candidate.getAsJsonObject("content");
                JsonArray parts2 = content2.getAsJsonArray("parts");
                if (parts2.size() > 0) {
                    JsonObject part = parts2.get(0).getAsJsonObject();
                    String text = part.get("text").getAsString();
                    log.info("Gemini API response content: {}", text);
                    return text;
                }
            }

            return "{}";
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

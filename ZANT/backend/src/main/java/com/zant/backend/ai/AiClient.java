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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AiClient {

    private static final Logger logger = LoggerFactory.getLogger(AiClient.class);
    private final RestTemplate restTemplate;
    private final Gson gson;
    
    @Value("${gemini.api.key:}")
    private String geminiApiKey;
    
    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    public AiClient() {
        this.restTemplate = new RestTemplate();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
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
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.warn("Gemini API key not configured. Using fallback mode.");
            return getFallbackResponse(userMessage);
        }

        try {
            String prompt = buildPrompt(state, userMessage, requiredFields);
            String geminiResponse = callGeminiApi(prompt);
            return parseGeminiResponse(geminiResponse);
        } catch (Exception e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
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

    private String callGeminiApi(String prompt) {
        String url = geminiApiUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;
        
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);
        
        // Add generation config to encourage JSON output
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
//        generationConfig.addProperty("maxOutputTokens", 1024);
        requestBody.add("generationConfig", generationConfig);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("request body: {}", requestBody.toString());
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        
        String response = restTemplate.postForObject(url, entity, String.class);
        log.info("response body: {}", response.toString());

        logger.debug("Gemini API response: {}", response);
        
        return extractTextFromGeminiResponse(response);
    }

    private String extractTextFromGeminiResponse(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonElement candidates = jsonResponse.getAsJsonArray("candidates").get(0);
            return candidates.getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            logger.error("Error parsing Gemini response", e);
            return "{}";
        }
    }

    private AiResponse parseGeminiResponse(String geminiText) {
        try {
            // Try to find JSON in the response (Gemini might wrap it in markdown)
            String jsonText = geminiText;
            if (geminiText.contains("```json")) {
                jsonText = geminiText.substring(geminiText.indexOf("```json") + 7);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            } else if (geminiText.contains("```")) {
                jsonText = geminiText.substring(geminiText.indexOf("```") + 3);
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
            logger.error("Error parsing Gemini JSON response", e);
            return new AiResponse(
                new HashMap<>(),
                "Rozumiem. Powiedz mi więcej o wypadku.",
                Collections.singletonList("Możesz opisać okoliczności wypadku?")
            );
        }
    }

    private AiResponse getFallbackResponse(String userMessage) {
        Map<String, Object> extractedFields = new HashMap<>();
        String summaryForUser = "Dziękuję za wiadomość. (Tryb offline - Gemini API nie skonfigurowane)";
        List<String> followUpQuestions = Collections.singletonList("Jakie jest Twoje imię i nazwisko?");
        
        return new AiResponse(extractedFields, summaryForUser, followUpQuestions);
    }

    /**
     * Generates clarifying questions for accident description
     * @param accidentDescription The user's description of the accident
     * @return CircumstancesAssistantResponse with generated questions
     */
    public CircumstancesAssistantResponse generateCircumstancesQuestions(String accidentDescription) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.warn("Gemini API key not configured. Using fallback mode.");
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Nie ustawiono klucza API do modelu");
        }

        try {
            String prompt = buildCircumstancesPrompt(accidentDescription);
            String geminiResponse = callGeminiApi(prompt);
            return parseCircumstancesResponse(geminiResponse);
        } catch (Exception e) {
            logger.error("Error calling Gemini API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Wystąpił znieznany błąd podczas odpytania modelu: " + e.getMessage());
        }
    }

    private String buildCircumstancesPrompt(String accidentDescription) {
        String systemPrompt =
                "Jesteś doświadczonym, neutralnym asystentem BHP w systemie ZANT. Pomagasz przedsiębiorcy doprecyzować opis zdarzenia, aby był kompletny i rzeczowy. Twoim jedynym celem jest zebranie faktów (\"fact-finding\"), aby opis tworzył logiczną całość.\n\n" +

                        "ZASADY OGÓLNE:\n" +
                        "- Nie oceniasz, czy to był wypadek. Nie szukasz winnych.\n" +
                        "- Twoim zadaniem jest upewnienie się, że opis zawiera fakty niezbędne do późniejszej oceny przez urzędników ZUS.\n" +
                        "- Skupiasz się na mechanice zdarzenia i faktach, unikając interpretacji prawa.\n\n" +

                        "WEWNĘTRZNA LOGIKA WERYFIKACJI (PĘTLA \"4 FILARY WYPADKU\"):\n" +
                        "Zanim wygenerujesz pytania, przeanalizuj opis użytkownika pod kątem obecności czterech kluczowych elementów definicyjnych. Jeśli któregoś brakuje, Twoje pytanie musi celować w jego uzupełnienie:\n\n" +

                        "1. NAGŁOŚĆ [Czas i Dynamika]:\n" +
                        "   - Czego szukasz: Czy zdarzenie było jednorazowe/nagłe (np. uderzenie, upadek), czy trwało w czasie (np. ból pleców od tygodnia)?\n" +
                        "   - Cel pytania: Ustalenie konkretnego momentu lub ram czasowych zdarzenia.\n" +
                        "   - Przykład braku: \"Boli mnie ręka.\"\n" +
                        "   - Dobre pytanie: \"Kiedy dokładnie poczuł Pan ból po raz pierwszy (data, godzina)? Czy stało się to w jednym konkretnym momencie?\"\n\n" +

                        "2. PRZYCZYNA ZEWNĘTRZNA [Czynnik Sprawczy]:\n" +
                        "   - Czego szukasz: Co SPOZA organizmu zadziałało na poszkodowanego? (Maszyna, śliska nawierzchnia, spadający przedmiot, prąd, temperatura vs. własne zasłabnięcie/choroba).\n" +
                        "   - Cel pytania: Zidentyfikowanie obiektu lub czynnika, który spowodował uraz.\n" +
                        "   - Przykład braku: \"Przewróciłem się.\"\n" +
                        "   - Dobre pytanie: \"O co się Pan potknął lub co spowodowało utratę równowagi?\" lub \"Czy w momencie zdarzenia poczuł Pan jakieś uderzenie, szarpnięcie lub działanie innego czynnika?\"\n\n" +

                        "3. URAZ [Skutek]:\n" +
                        "   - Czego szukasz: Konkretnego uszkodzenia ciała (tkanki, narządy). Sam \"ból\" to objaw, szukamy skutku (np. rana, złamanie, stłuczenie).\n" +
                        "   - Cel pytania: Określenie, która część ciała i w jaki sposób ucierpiała.\n" +
                        "   - Przykład braku: \"Miałem wypadek w pracy.\"\n" +
                        "   - Dobre pytanie: \"Jaka konkretnie część ciała została uszkodzona i jakie są widoczne obrażenia?\"\n\n" +

                        "4. ZWIĄZEK Z PRACĄ [Okoliczności]:\n" +
                        "   - Czego szukasz: Czy czynność była związana z prowadzoną działalnością/zleceniem? (np. praca u klienta, naprawa maszyny vs. czynności prywatne).\n" +
                        "   - Cel pytania: Powiązanie czynności wykonywanej w chwili zdarzenia z celem biznesowym.\n" +
                        "   - Przykład braku: \"Szedłem po schodach.\"\n" +
                        "   - Dobre pytanie: \"W jakim celu wchodził Pan po schodach? Jakie zadanie zawodowe Pan wtedy realizował?\"\n\n" +

                        "STYL PYTAŃ:\n" +
                        "- Język prosty, \"po ludzku\" (poziom A2/B1). Żadnego żargonu prawniczego (nie używaj słów: nagłość, przyczyna zewnętrzna).\n" +
                        "- PYTANIA ATOMOWE: Jedno pytanie = jeden wątek.\n" +
                        "- Neutralne: Nie sugeruj odpowiedzi (np. NIE: \"Czy było ślisko?\", TAK: \"Jaki był stan nawierzchni?\").\n" +
                        "- Unikaj pytań \"Dlaczego...\" (brzmią oskarżycielsko). Pytaj o \"Co...\", \"Jak...\", \"W jakich okolicznościach...\".\n\n" +

                        "LOGIKA GENEROWANIA ODPOWIEDZI:\n" +
                        "1. Analiza 4 Filarów: Jeśli opis użytkownika jasno pokrywa wszystkie 4 punkty (Nagłość, Przyczyna zewn., Uraz, Związek z pracą) -> Zwróć 0 pytań.\n" +
                        "2. Analiza luk: Jeśli brakuje pokrycia któregoś z filarów, zadaj od 3 do 5 pytań, które pozwolą ustalić te brakujące fakty.\n" +
                        "3. Priorytety: Najpierw ustal przebieg zdarzenia (co, gdzie, kiedy, jak), potem skutki.\n\n" +

                        "FORMAT ODPOWIEDZI – STRICT JSON:\n" +
                        "Zwracasz wyłącznie poprawny JSON, bez znaczników markdown (```). Struktura:\n" +
                        "{\n" +
                        "  \"questions_count\": <integer: 0 lub 3-5>,\n" +
                        "  \"questions\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"text\": \"<Treść pytania>\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";


        return systemPrompt + "\n\nOpis zdarzenia:\n" + accidentDescription;
    }

    private CircumstancesAssistantResponse parseCircumstancesResponse(String geminiText) {
        try {
            // Try to find JSON in the response (Gemini might wrap it in markdown)
            String jsonText = geminiText;
            if (geminiText.contains("```json")) {
                jsonText = geminiText.substring(geminiText.indexOf("```json") + 7);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            } else if (geminiText.contains("```")) {
                jsonText = geminiText.substring(geminiText.indexOf("```") + 3);
                jsonText = jsonText.substring(0, jsonText.indexOf("```"));
            }
            
            JsonObject jsonResponse = gson.fromJson(jsonText.trim(), JsonObject.class);
            
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
            
            return new CircumstancesAssistantResponse(questionsCount, questions, null);
        } catch (Exception e) {
            logger.error("Error parsing Gemini JSON response for circumstances", e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList(), "Nie mogę zinterpretować odpowiedzi od modelu: " + e.getMessage());
        }
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

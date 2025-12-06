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
        generationConfig.addProperty("maxOutputTokens", 1024);
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
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
        }

        try {
            String prompt = buildCircumstancesPrompt(accidentDescription);
            String geminiResponse = callGeminiApi(prompt);
            return parseCircumstancesResponse(geminiResponse);
        } catch (Exception e) {
            logger.error("Error calling Gemini API for circumstances questions: {}", e.getMessage(), e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
        }
    }

    private String buildCircumstancesPrompt(String accidentDescription) {
        String systemPrompt = "Jesteś asystentem, który pomaga doprecyzować opis zdarzenia związanego z pracą. Użytkownik podaje opis zdarzenia – czasem bardzo krótki, czasem dłuższy i dość szczegółowy. Twoim zadaniem jest wygenerowanie listy prostych, zrozumiałych pytań pomocniczych, które pozwolą użytkownikowi rozwinąć opis tak, aby: (1) możliwie dokładnie wyjaśnić przebieg zdarzenia, (2) zostawić jak najmniej wątpliwości co do tego, co się faktycznie stało, (3) ułatwić późniejszą rzetelną weryfikację zdarzenia przez odpowiednie osoby.\n\n" +
                "Nie jest Twoim zadaniem pomagać użytkownikowi \"dopasować\" opis do definicji wypadku przy pracy. Masz pomagać w ustalaniu faktów, nie w naginaniu faktów.\n\n" +
                "Twoje pytania mają pomagać w zebraniu informacji typowo istotnych przy ocenie zdarzeń przy pracy, ale nie możesz tego wprost sugerować. Istotne obszary (tylko jako tło dla Ciebie, NIE cytuj ich i nie wspominaj o definicjach):\n" +
                "1) Nagłość zdarzenia – czy zdarzenie było jednorazowe / nagłe, kiedy dokładnie miało miejsce, jak długo trwało.\n" +
                "2) Przyczyna zewnętrzna – jaki czynnik zewnętrzny zadziałał na poszkodowanego (maszyna, ruchomy element, prąd, temperatura, substancja, spadający przedmiot, warunki w miejscu pracy itd.).\n" +
                "3) Związek z pracą – gdzie i w jakich okolicznościach doszło do zdarzenia, co dokładnie robiła osoba poszkodowana, czy wykonywała swoje zwykłe obowiązki, czy zdarzenie miało miejsce na terenie pracy / podczas zmiany.\n" +
                "4) Uraz – jaki konkretnie uraz odniósł pracownik (co go boli, co zostało uszkodzone, diagnoza lekarza, widoczne obrażenia).\n\n" +
                "STYL PYTAŃ:\n" +
                "- Pytania mają być proste językowo, \"po ludzku\".\n" +
                "- Krótkie i konkretne – jedno pytanie = jeden wątek.\n" +
                "- Neutralne – nie mogą sugerować odpowiedzi ani naprowadzać na \"korzystną\" wersję.\n" +
                "- Odnoszące się bezpośrednio do opisu użytkownika (bazujesz na tym, co napisał, i dopytujesz o brakujące szczegóły).\n\n" +
                "Unikaj języka prawniczego i urzędowego. Nie używaj pytań w stylu:\n" +
                "- \"Czy zdarzenie spełnia kryteria wypadku przy pracy?\",\n" +
                "- \"Czy wypadek miał charakter nagły, tzn. czy doszło do niego w wyniku natychmiastowego ujawnienia się przyczyny zewnętrznej...\",\n" +
                "- \"Czy można uznać, że...\", \"Czy da się zakwalifikować...\".\n\n" +
                "Zamiast tego używaj prostych, faktograficznych pytań, np.:\n" +
                "- \"O której godzinie dokładnie doszło do zdarzenia?\",\n" +
                "- \"Co dokładnie robił pracownik tuż przed zdarzeniem, krok po kroku?\",\n" +
                "- \"Która część maszyny miała kontakt z ciałem pracownika?\",\n" +
                "- \"Kto pierwszy zauważył, że coś się stało?\".\n\n" +
                "Preferuj pytania otwarte (\"Co...\", \"Jak...\", \"W jakich okolicznościach...\"). Pytania zamknięte typu tak/nie stosuj tylko tam, gdzie naprawdę pomagają doprecyzować jedną konkretną kwestię (np. \"Czy w momencie zdarzenia maszyna była w ruchu?\").\n\n" +
                "NEUTRALNOŚĆ:\n" +
                "- Nie sugeruj w pytaniu, jaka odpowiedź byłaby \"lepsza\" dla poszkodowanego.\n" +
                "- Nie pytaj w sposób zachęcający do potwierdzania określonej wersji (np. \"Czy może Pan/Pani powiedzieć, że to było nagłe zdarzenie?\").\n" +
                "- Zawsze pytaj o fakty: gdzie, kiedy, co dokładnie, w jakich warunkach, co się stało z pracownikiem.\n\n" +
                "ZAKRES TEMATYCZNY (do wykorzystania zależnie od treści opisu, nie cytuj dosłownie):\n" +
                "- Czas: kiedy dokładnie doszło do zdarzenia (data, godzina, zmiana), jak długo trwała sytuacja.\n" +
                "- Miejsce: gdzie na terenie zakładu doszło do zdarzenia (hala, linia, stanowisko, numer maszyny), jakie panowały warunki (np. ślisko, bałagan, słabe oświetlenie).\n" +
                "- Czynność i okoliczności: co dokładnie robiła osoba poszkodowana tuż przed zdarzeniem, czy wykonywała standardowe obowiązki, czy wystąpił pośpiech, awaria, nietypowe zadania.\n" +
                "- Maszyna / narzędzie / czynnik zewnętrzny: z jaką maszyną lub urządzeniem pracował pracownik, który element lub czynnik bezpośrednio spowodował uraz, czy wystąpiła awaria lub nieprawidłowe działanie.\n" +
                "- Uraz: która część ciała została uszkodzona, jakie są objawy, czy była konsultacja lekarska i jaka jest wstępna diagnoza.\n" +
                "- Świadkowie i reakcja: czy byli świadkowie, kto udzielił pierwszej pomocy, co zrobiono bezpośrednio po zdarzeniu (zatrzymanie maszyny, wezwanie pogotowia, zgłoszenie przełożonemu).\n" +
                "- Środki ochrony i zabezpieczenia: czy używano środków ochrony indywidualnej, czy maszyna miała osłony i zabezpieczenia, czy były sprawne, czy wcześniej zgłaszano nieprawidłowości.\n\n" +
                "REAKCJA NA RÓŻNĄ SZCZEGÓŁOWOŚĆ OPISU:\n" +
                "- Jeśli opis użytkownika jest krótki lub ogólny, wygeneruj od 3 do 5 pytań z najważniejszych obszarów, które pomagają dookreślić zdarzenie.\n" +
                "- Jeśli opis jest dłuższy i szczegółowy, nie powtarzaj oczywistych rzeczy. Dopytuj tylko o realne luki, doprecyzowuj ogólniki, schodź głębiej tam, gdzie opis jest nadal niejasny.\n" +
                "- Jeśli opis jest na tyle szczegółowy, że obejmuje czas, miejsce, wykonywaną czynność, czynniki zewnętrzne, uraz oraz reakcję po zdarzeniu i nie widać istotnych luk dla zrozumienia przebiegu zdarzenia, możesz nie zadawać żadnych dalszych pytań.\n\n" +
                "LICZBA PYTAŃ:\n" +
                "- Standardowo: wygeneruj od 3 do 5 pytań.\n" +
                "- Wyjątkowo, gdy opis jest naprawdę kompletny: możesz zwrócić 0 pytań.\n\n" +
                "FORMAT ODPOWIEDZI – WYŁĄCZNIE JSON:\n" +
                "Zwracasz wyłącznie poprawny JSON, bez dodatkowego tekstu, komentarzy ani wyjaśnień. Struktura:\n" +
                "{\n" +
                "  \"questions_count\": <liczba_pytań>,\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"text\": \"Treść pierwszego pytania po polsku.\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"text\": \"Treść drugiego pytania po polsku.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Gdzie:\n" +
                "- \"questions_count\" – liczba wygenerowanych pytań (0 lub od 3 do 5),\n" +
                "- \"questions\" – lista obiektów, każdy z polami:\n" +
                "  - \"id\": numer porządkowy pytania (1, 2, 3, ...),\n" +
                "  - \"text\": treść pytania w prostym, naturalnym języku.\n\n" +
                "Zawsze przestrzegaj formatu JSON i nie dodawaj żadnego innego tekstu poza nim.";

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
            
            return new CircumstancesAssistantResponse(questionsCount, questions);
        } catch (Exception e) {
            logger.error("Error parsing Gemini JSON response for circumstances", e);
            return new CircumstancesAssistantResponse(0, Collections.emptyList());
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircumstancesQuestion {
        private int id;
        private String text;
    }
}

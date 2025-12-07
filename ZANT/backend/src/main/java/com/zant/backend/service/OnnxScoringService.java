package com.zant.backend.service;

import com.zant.backend.model.ewyp.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OnnxScoringService {
    
    @Value("${python.predictor.script:Python/predictor.py}")
    private String predictorScript;
    
    @Value("${python.model.path:MODELS/wniosek_model.joblib}")
    private String modelPath;
    
    @Value("${python.executable:python3}")
    private String pythonExecutable;
    
    private boolean pythonAvailable = false;
    
    @PostConstruct
    public void init() {
        try {
            // Sprawdź czy Python jest dostępny
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("Python nie jest dostępny. Scoring będzie niedostępny.");
                return;
            }
            
            // Sprawdź czy skrypt predykcji istnieje
            File scriptFile = new File(predictorScript);
            if (!scriptFile.exists()) {
                log.warn("Skrypt predykcji nie został znaleziony w: {}. Scoring będzie niedostępny.", predictorScript);
                return;
            }
            
            // Sprawdź czy model istnieje
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.warn("Model nie został znaleziony w: {}. Scoring będzie niedostępny.", modelPath);
                return;
            }
            
            pythonAvailable = true;
            log.info("Python scoring service zainicjalizowany pomyślnie. Skrypt: {}, Model: {}", predictorScript, modelPath);
        } catch (Exception e) {
            log.error("Błąd podczas inicjalizacji Python scoring service: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Wykonuje scoring dla zgłoszenia EWYP.
     * 
     * @param report Zgłoszenie EWYP do analizy
     * @return Prawdopodobieństwo, że to wypadek przy pracy (0.0-1.0) lub null jeśli Python niedostępny
     */
    public Double scoreReport(EWYPReport report) {
        if (!pythonAvailable) {
            log.warn("Python scoring nie jest dostępny. Pomiń scoring.");
            return null;
        }
        
        try {
            // Przygotuj tekst wejściowy z danych zgłoszenia
            String inputText = extractTextFromReport(report);
            
            // Zapisz tekst do tymczasowego pliku
            Path tempFile = Files.createTempFile("ewyp_report_", ".txt");
            try {
                Files.writeString(tempFile, inputText);
                
                // Wywołaj skrypt Python
                Double result = callPythonPredictor(tempFile.toString());
                
                return result;
            } finally {
                // Usuń tymczasowy plik
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            log.error("Błąd podczas scoringu zgłoszenia: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Wywołuje skrypt Python predictor.py i zwraca wynik predykcji.
     */
    private Double callPythonPredictor(String textFilePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            pythonExecutable,
            predictorScript,
            "--model-path", modelPath,
            "--text-file", textFilePath
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Python output: {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Python predictor zakończył się błędem (exit code: {}). Output: {}", exitCode, output);
            return null;
        }
        
        // Parsuj output - szukamy linii z procentem pewności
        return parseOutputForResult(output.toString());
    }
    
    /**
     * Parsuje output ze skryptu Python i wyciąga wynik predykcji.
     * Szuka linii typu: "AI jest w 87.5% pewien, że to był wypadek przy pracy!"
     * @return Prawdopodobieństwo jako liczba zmiennoprzecinkowa 0.0-1.0 (np. 0.875 dla 87.5%)
     */
    private Double parseOutputForResult(String output) {
        Pattern pattern = Pattern.compile("AI jest w (\\d+\\.\\d+)% pewien, że to był wypadek przy pracy");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String percentStr = matcher.group(1);
            double percent = Double.parseDouble(percentStr);
            // Konwertuj procenty na wartość 0-1 (np. 54.3% -> 0.543)
            double probability = percent / 100.0;
            log.info("Wynik scoringu: {} ({}%)", probability, percentStr);
            return probability;
        }
        
        log.warn("Nie udało się sparsować wyniku z Pythona. Output: {}", output);
        return null;
    }
    
    /**
     * Ekstraktuje tekst z zgłoszenia EWYP do analizy.
     */
    private String extractTextFromReport(EWYPReport report) {
        StringBuilder sb = new StringBuilder();
        
        // Informacje o poszkodowanym
        if (report.getInjuredPerson() != null) {
            InjuredPerson ip = report.getInjuredPerson();
            sb.append("=== POSZKODOWANY ===\n");
            appendIfNotNull(sb, "Imię i nazwisko", ip.getFirstName(), ip.getLastName());
            appendIfNotNull(sb, "Data urodzenia", ip.getBirthDate());
            appendIfNotNull(sb, "Miejsce urodzenia", ip.getBirthPlace());
            appendIfNotNull(sb, "PESEL", ip.getPesel());
            
            // Adres zamieszkania
            if (ip.getAddress() != null) {
                appendAddress(sb, "Adres zamieszkania", ip.getAddress());
            }
            
            // Polski adres
            if (ip.getLastPolishAddressOrStay() != null) {
                appendPolishAddress(sb, "Ostatni polski adres", ip.getLastPolishAddressOrStay());
            }
        }
        
        // Informacje o zgłaszającym (jeśli inny niż poszkodowany)
        if (report.getReporter() != null && Boolean.TRUE.equals(report.getReporter().getIsDifferentFromInjuredPerson())) {
            Reporter r = report.getReporter();
            sb.append("\n=== ZGŁASZAJĄCY ===\n");
            appendIfNotNull(sb, "Imię i nazwisko", r.getFirstName(), r.getLastName());
            appendIfNotNull(sb, "Data urodzenia", r.getBirthDate());
        }
        
        // Informacje o wypadku - najważniejsze dla scoringu!
        if (report.getAccidentInfo() != null) {
            AccidentInfo ai = report.getAccidentInfo();
            sb.append("\n=== SZCZEGÓŁY WYPADKU ===\n");
            appendIfNotNull(sb, "Data wypadku", ai.getAccidentDate());
            appendIfNotNull(sb, "Godzina wypadku", ai.getAccidentTime());
            appendIfNotNull(sb, "Planowany czas rozpoczęcia pracy", ai.getPlannedWorkStartTime());
            appendIfNotNull(sb, "Planowany czas zakończenia pracy", ai.getPlannedWorkEndTime());
            
            // Miejsce wypadku
            appendIfNotNull(sb, "Miejsce wypadku", ai.getPlaceOfAccident());
            
            // Okoliczności wypadku - kluczowy tekst!
            appendIfNotNull(sb, "Okoliczności i przyczyny wypadku", ai.getCircumstancesAndCauses());
            
            // Szczegóły odniesionego urazu
            appendIfNotNull(sb, "Opis obrażeń", ai.getInjuriesDescription());
            
            // Informacje o pierwszej pomocy
            appendIfNotNull(sb, "Czy udzielono pierwszej pomocy", ai.getFirstAidGiven());
            appendIfNotNull(sb, "Placówka pierwszej pomocy", ai.getFirstAidFacility());
            
            // Organ prowadzący dochodzenie
            appendIfNotNull(sb, "Organ prowadzący dochodzenie", ai.getInvestigatingAuthority());
            
            // Informacje o maszynie
            appendIfNotNull(sb, "Czy wypadek podczas obsługi maszyny", ai.getAccidentDuringMachineOperation());
            appendIfNotNull(sb, "Opis stanu maszyny", ai.getMachineConditionDescription());
            appendIfNotNull(sb, "Czy maszyna ma certyfikat", ai.getMachineHasCertificate());
        }
        
        // Świadkowie
        if (report.getWitnesses() != null && !report.getWitnesses().isEmpty()) {
            sb.append("\n=== ŚWIADKOWIE ===\n");
            for (int i = 0; i < report.getWitnesses().size(); i++) {
                WitnessInfo w = report.getWitnesses().get(i);
                sb.append("Świadek ").append(i + 1).append(": ");
                appendIfNotNull(sb, "Imię i nazwisko", w.getFirstName(), w.getLastName());
                appendIfNotNull(sb, "Adres", w.getStreet(), w.getHouseNumber(), w.getCity());
            }
        }
        
        return sb.toString();
    }
    
    private void appendIfNotNull(StringBuilder sb, String label, Object... values) {
        boolean hasValue = false;
        StringBuilder valueSb = new StringBuilder();
        
        for (Object value : values) {
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    if (hasValue) {
                        valueSb.append(" ");
                    }
                    valueSb.append(strValue);
                    hasValue = true;
                }
            }
        }
        
        if (hasValue) {
            sb.append(label).append(": ").append(valueSb).append("\n");
        }
    }
    
    private void appendAddress(StringBuilder sb, String label, Address address) {
        if (address == null) return;
        
        StringBuilder addrSb = new StringBuilder();
        appendToBuilder(addrSb, address.getStreet());
        appendToBuilder(addrSb, address.getHouseNumber());
        appendToBuilder(addrSb, address.getApartmentNumber());
        appendToBuilder(addrSb, address.getPostalCode());
        appendToBuilder(addrSb, address.getCity());
        appendToBuilder(addrSb, address.getCountry());
        
        if (addrSb.length() > 0) {
            sb.append(label).append(": ").append(addrSb).append("\n");
        }
    }
    
    private void appendPolishAddress(StringBuilder sb, String label, PolishAddress address) {
        if (address == null) return;
        
        StringBuilder addrSb = new StringBuilder();
        appendToBuilder(addrSb, address.getStreet());
        appendToBuilder(addrSb, address.getHouseNumber());
        appendToBuilder(addrSb, address.getApartmentNumber());
        appendToBuilder(addrSb, address.getPostalCode());
        appendToBuilder(addrSb, address.getCity());
        
        if (addrSb.length() > 0) {
            sb.append(label).append(": ").append(addrSb).append("\n");
        }
    }
    
    private void appendToBuilder(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value.trim());
        }
    }
    
    public boolean isModelLoaded() {
        return pythonAvailable;
    }
}

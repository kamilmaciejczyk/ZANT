package com.zant.backend.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assistant_states")
public class AssistantState {

    @Id
    private String conversationId;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = AccidentReportConverter.class)
    private AccidentReport accidentReport;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ConversationHistoryConverter.class)
    private List<Map<String, String>> conversationHistory;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> missingFields;
    
    private double completionProgress;

    // JPA Converters for JSON serialization
    @Converter
    public static class AccidentReportConverter implements AttributeConverter<AccidentReport, String> {
        private static final Gson gson = new Gson();

        @Override
        public String convertToDatabaseColumn(AccidentReport attribute) {
            return attribute == null ? null : gson.toJson(attribute);
        }

        @Override
        public AccidentReport convertToEntityAttribute(String dbData) {
            return dbData == null ? null : gson.fromJson(dbData, AccidentReport.class);
        }
    }

    @Converter
    public static class ConversationHistoryConverter implements AttributeConverter<List<Map<String, String>>, String> {
        private static final Gson gson = new Gson();
        private static final Type type = new TypeToken<List<Map<String, String>>>(){}.getType();

        @Override
        public String convertToDatabaseColumn(List<Map<String, String>> attribute) {
            return attribute == null ? null : gson.toJson(attribute);
        }

        @Override
        public List<Map<String, String>> convertToEntityAttribute(String dbData) {
            return dbData == null ? new ArrayList<>() : gson.fromJson(dbData, type);
        }
    }

    @Converter
    public static class StringListConverter implements AttributeConverter<List<String>, String> {
        private static final Gson gson = new Gson();
        private static final Type type = new TypeToken<List<String>>(){}.getType();

        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            return attribute == null ? null : gson.toJson(attribute);
        }

        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            return dbData == null ? new ArrayList<>() : gson.fromJson(dbData, type);
        }
    }
}

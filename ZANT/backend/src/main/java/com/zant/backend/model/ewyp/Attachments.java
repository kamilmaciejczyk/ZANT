package com.zant.backend.model.ewyp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Attachments {
    
    private Boolean hasHospitalCardCopy;
    
    private Boolean hasProsecutorDecisionCopy;
    
    private Boolean hasDeathDocsCopy;
    
    private Boolean hasOtherDocuments;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> otherDocuments;
    
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

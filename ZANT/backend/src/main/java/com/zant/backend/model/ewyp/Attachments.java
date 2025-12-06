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
    private String hospitalCardCopyFilename;
    
    private Boolean hasProsecutorDecisionCopy;
    private String prosecutorDecisionCopyFilename;
    
    private Boolean hasPowerOfAttorneyCopy;
    private String powerOfAttorneyCopyFilename;
    
    private Boolean hasDeathDocsCopy;
    private String deathDocsCopyFilename;
    
    private Boolean hasOtherDocuments;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = OtherDocumentListConverter.class)
    private List<OtherDocument> otherDocuments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherDocument {
        private String documentName;
        private String filename;
    }
    
    @Converter
    public static class OtherDocumentListConverter implements AttributeConverter<List<OtherDocument>, String> {
        private static final Gson gson = new Gson();
        private static final Type type = new TypeToken<List<OtherDocument>>(){}.getType();

        @Override
        public String convertToDatabaseColumn(List<OtherDocument> attribute) {
            return attribute == null ? null : gson.toJson(attribute);
        }

        @Override
        public List<OtherDocument> convertToEntityAttribute(String dbData) {
            return dbData == null ? new ArrayList<>() : gson.fromJson(dbData, type);
        }
    }
}

package com.zant.backend.model.ewyp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ewyp_reports")
public class EWYPReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "pesel", column = @Column(name = "injured_person_pesel")),
        @AttributeOverride(name = "idDocumentType", column = @Column(name = "injured_person_id_document_type")),
        @AttributeOverride(name = "idDocumentNumber", column = @Column(name = "injured_person_id_document_number")),
        @AttributeOverride(name = "firstName", column = @Column(name = "injured_person_first_name")),
        @AttributeOverride(name = "lastName", column = @Column(name = "injured_person_last_name")),
        @AttributeOverride(name = "birthDate", column = @Column(name = "injured_person_birth_date")),
        @AttributeOverride(name = "birthPlace", column = @Column(name = "injured_person_birth_place")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "injured_person_phone_number"))
    })
    private InjuredPerson injuredPerson;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "isDifferentFromInjuredPerson", column = @Column(name = "reporter_is_different")),
        @AttributeOverride(name = "pesel", column = @Column(name = "reporter_pesel")),
        @AttributeOverride(name = "idDocumentType", column = @Column(name = "reporter_id_document_type")),
        @AttributeOverride(name = "idDocumentNumber", column = @Column(name = "reporter_id_document_number")),
        @AttributeOverride(name = "firstName", column = @Column(name = "reporter_first_name")),
        @AttributeOverride(name = "lastName", column = @Column(name = "reporter_last_name")),
        @AttributeOverride(name = "birthDate", column = @Column(name = "reporter_birth_date")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "reporter_phone_number"))
    })
    private Reporter reporter;
    
    @Embedded
    private AccidentInfo accidentInfo;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ewyp_report_id")
    private List<WitnessInfo> witnesses = new ArrayList<>();
    
    @Embedded
    private Attachments attachments;
    
    @Embedded
    private DocumentsToDeliverLater documentsToDeliverLater;
    
    private String responseDeliveryMethod; // PICKUP_AT_ZUS | BY_MAIL_TO_ADDRESS | TO_PUE_ACCOUNT
    
    @Embedded
    private Signature signature;

    @Lob
    @Column(name = "attachment_file")
    private byte[] attachmentFile;
    
    @Column(name = "attachment_filename")
    private String attachmentFilename;
    
    @Column(name = "attachment_content_type")
    private String attachmentContentType;
    
    @Column(name = "status")
    private String status; // DRAFT | SUBMITTED
    
    @Column(name = "scoring_classification")
    private String scoringClassification; // Placeholder dla klasyfikacji scoringu
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "DRAFT";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

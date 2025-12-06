package com.zant.backend.model.ewyp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ewyp_reports")
public class EWYPReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    private InjuredPerson injuredPerson;
    
    @Embedded
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
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.zant.backend.controller;

import com.zant.backend.dto.ewyp.EWYPReportDTO;
import com.zant.backend.mapper.EWYPReportMapper;
import com.zant.backend.model.ewyp.EWYPReport;
import com.zant.backend.repository.EWYPReportRepository;
import com.zant.backend.service.EWYPDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ewyp-reports")
public class EWYPReportController {
    
    private final EWYPReportRepository repository;
    private final EWYPReportMapper mapper;
    private final EWYPDocumentService documentService;
    
    public EWYPReportController(EWYPReportRepository repository, EWYPReportMapper mapper, 
                                EWYPDocumentService documentService) {
        this.repository = repository;
        this.mapper = mapper;
        this.documentService = documentService;
    }
    
    @PostMapping
    public ResponseEntity<EWYPReportDTO> createReport(@Valid @RequestBody EWYPReportDTO reportDTO) {
        EWYPReport entity = mapper.toEntity(reportDTO);
        entity.setStatus("SUBMITTED");
        EWYPReport savedEntity = repository.save(entity);
        EWYPReportDTO responseDTO = mapper.toDTO(savedEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    @PostMapping("/draft")
    public ResponseEntity<EWYPReportDTO> saveDraft(@RequestBody EWYPReportDTO reportDTO) {
        EWYPReport entity = mapper.toEntity(reportDTO);
        entity.setStatus("DRAFT");
        EWYPReport savedEntity = repository.save(entity);
        EWYPReportDTO responseDTO = mapper.toDTO(savedEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EWYPReportDTO> updateReport(@PathVariable UUID id, @RequestBody EWYPReportDTO reportDTO) {
        return repository.findById(id)
                .map(existingReport -> {
                    EWYPReport updatedEntity = mapper.toEntity(reportDTO);
                    updatedEntity.setId(id);
                    // Keep the existing status unless explicitly changed
                    if (reportDTO.getStatus() == null) {
                        updatedEntity.setStatus(existingReport.getStatus());
                    }
                    EWYPReport savedEntity = repository.save(updatedEntity);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/submit")
    public ResponseEntity<EWYPReportDTO> submitReport(@PathVariable UUID id) {
        return repository.findById(id)
                .map(report -> {
                    report.setStatus("SUBMITTED");
                    EWYPReport savedEntity = repository.save(report);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EWYPReportDTO> getReportById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<EWYPReportDTO>> getAllReports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<EWYPReport> reports = repository.findAll();
        
        // Filter by search term (searches in injured person's name or PESEL)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            reports = reports.stream()
                    .filter(report -> {
                        if (report.getInjuredPerson() != null) {
                            String firstName = report.getInjuredPerson().getFirstName();
                            String lastName = report.getInjuredPerson().getLastName();
                            String pesel = report.getInjuredPerson().getPesel();
                            
                            return (firstName != null && firstName.toLowerCase().contains(searchLower)) ||
                                   (lastName != null && lastName.toLowerCase().contains(searchLower)) ||
                                   (pesel != null && pesel.contains(searchLower));
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        
        // Filter by status
        if (status != null && !status.trim().isEmpty()) {
            reports = reports.stream()
                    .filter(report -> status.equalsIgnoreCase(report.getStatus()))
                    .collect(Collectors.toList());
        }
        
        List<EWYPReportDTO> reportDTOs = reports.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(reportDTOs);
    }
    
    @PostMapping("/{id}/attachment")
    public ResponseEntity<EWYPReportDTO> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate file type (PDF only)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest().build();
        }
        
        return repository.findById(id)
                .<ResponseEntity<EWYPReportDTO>>map(report -> {
                    try {
                        report.setAttachmentFile(file.getBytes());
                        report.setAttachmentFilename(file.getOriginalFilename());
                        report.setAttachmentContentType(file.getContentType());
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    } catch (IOException e) {
                        return ResponseEntity.<EWYPReportDTO>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID id) {
        return repository.findById(id)
                .filter(report -> report.getAttachmentFile() != null)
                .map(report -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(report.getAttachmentContentType()));
                    headers.setContentDispositionFormData("attachment", report.getAttachmentFilename());
                    return new ResponseEntity<>(report.getAttachmentFile(), headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}/attachment")
    public ResponseEntity<EWYPReportDTO> deleteAttachment(@PathVariable UUID id) {
        return repository.findById(id)
                .map(report -> {
                    report.setAttachmentFile(null);
                    report.setAttachmentFilename(null);
                    report.setAttachmentContentType(null);
                    EWYPReport savedEntity = repository.save(report);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Hospital Card Copy attachment endpoints
    @PostMapping("/{id}/attachment/hospital-card")
    public ResponseEntity<EWYPReportDTO> uploadHospitalCardCopy(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return repository.findById(id)
                .<ResponseEntity<EWYPReportDTO>>map(report -> {
                    try {
                        report.setHospitalCardCopyFile(file.getBytes());
                        if (report.getAttachments() == null) {
                            report.setAttachments(new com.zant.backend.model.ewyp.Attachments());
                        }
                        report.getAttachments().setHospitalCardCopyFilename(file.getOriginalFilename());
                        report.getAttachments().setHasHospitalCardCopy(true);
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    } catch (IOException e) {
                        return ResponseEntity.<EWYPReportDTO>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/attachment/hospital-card")
    public ResponseEntity<byte[]> downloadHospitalCardCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .filter(report -> report.getHospitalCardCopyFile() != null)
                .map(report -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    String filename = report.getAttachments() != null && report.getAttachments().getHospitalCardCopyFilename() != null
                            ? report.getAttachments().getHospitalCardCopyFilename()
                            : "hospital-card.pdf";
                    headers.setContentDispositionFormData("attachment", filename);
                    return new ResponseEntity<>(report.getHospitalCardCopyFile(), headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}/attachment/hospital-card")
    public ResponseEntity<EWYPReportDTO> deleteHospitalCardCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .map(report -> {
                    report.setHospitalCardCopyFile(null);
                    if (report.getAttachments() != null) {
                        report.getAttachments().setHospitalCardCopyFilename(null);
                        report.getAttachments().setHasHospitalCardCopy(false);
                    }
                    EWYPReport savedEntity = repository.save(report);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Prosecutor Decision Copy attachment endpoints
    @PostMapping("/{id}/attachment/prosecutor-decision")
    public ResponseEntity<EWYPReportDTO> uploadProsecutorDecisionCopy(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return repository.findById(id)
                .<ResponseEntity<EWYPReportDTO>>map(report -> {
                    try {
                        report.setProsecutorDecisionCopyFile(file.getBytes());
                        if (report.getAttachments() == null) {
                            report.setAttachments(new com.zant.backend.model.ewyp.Attachments());
                        }
                        report.getAttachments().setProsecutorDecisionCopyFilename(file.getOriginalFilename());
                        report.getAttachments().setHasProsecutorDecisionCopy(true);
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    } catch (IOException e) {
                        return ResponseEntity.<EWYPReportDTO>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/attachment/prosecutor-decision")
    public ResponseEntity<byte[]> downloadProsecutorDecisionCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .filter(report -> report.getProsecutorDecisionCopyFile() != null)
                .map(report -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    String filename = report.getAttachments() != null && report.getAttachments().getProsecutorDecisionCopyFilename() != null
                            ? report.getAttachments().getProsecutorDecisionCopyFilename()
                            : "prosecutor-decision.pdf";
                    headers.setContentDispositionFormData("attachment", filename);
                    return new ResponseEntity<>(report.getProsecutorDecisionCopyFile(), headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}/attachment/prosecutor-decision")
    public ResponseEntity<EWYPReportDTO> deleteProsecutorDecisionCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .map(report -> {
                    report.setProsecutorDecisionCopyFile(null);
                    if (report.getAttachments() != null) {
                        report.getAttachments().setProsecutorDecisionCopyFilename(null);
                        report.getAttachments().setHasProsecutorDecisionCopy(false);
                    }
                    EWYPReport savedEntity = repository.save(report);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Death Docs Copy attachment endpoints
    @PostMapping("/{id}/attachment/death-docs")
    public ResponseEntity<EWYPReportDTO> uploadDeathDocsCopy(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return repository.findById(id)
                .<ResponseEntity<EWYPReportDTO>>map(report -> {
                    try {
                        report.setDeathDocsCopyFile(file.getBytes());
                        if (report.getAttachments() == null) {
                            report.setAttachments(new com.zant.backend.model.ewyp.Attachments());
                        }
                        report.getAttachments().setDeathDocsCopyFilename(file.getOriginalFilename());
                        report.getAttachments().setHasDeathDocsCopy(true);
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    } catch (IOException e) {
                        return ResponseEntity.<EWYPReportDTO>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/attachment/death-docs")
    public ResponseEntity<byte[]> downloadDeathDocsCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .filter(report -> report.getDeathDocsCopyFile() != null)
                .map(report -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    String filename = report.getAttachments() != null && report.getAttachments().getDeathDocsCopyFilename() != null
                            ? report.getAttachments().getDeathDocsCopyFilename()
                            : "death-docs.pdf";
                    headers.setContentDispositionFormData("attachment", filename);
                    return new ResponseEntity<>(report.getDeathDocsCopyFile(), headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}/attachment/death-docs")
    public ResponseEntity<EWYPReportDTO> deleteDeathDocsCopy(@PathVariable UUID id) {
        return repository.findById(id)
                .map(report -> {
                    report.setDeathDocsCopyFile(null);
                    if (report.getAttachments() != null) {
                        report.getAttachments().setDeathDocsCopyFilename(null);
                        report.getAttachments().setHasDeathDocsCopy(false);
                    }
                    EWYPReport savedEntity = repository.save(report);
                    return ResponseEntity.ok(mapper.toDTO(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Other Documents attachment endpoints (supports multiple documents with names)
    @PostMapping("/{id}/attachment/other-document")
    public ResponseEntity<EWYPReportDTO> uploadOtherDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentName") String documentName) {
        
        if (file.isEmpty() || documentName == null || documentName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return repository.findById(id)
                .<ResponseEntity<EWYPReportDTO>>map(report -> {
                    try {
                        // Store file in a map with a unique key (document name + timestamp)
                        String fileKey = documentName + "_" + System.currentTimeMillis();
                        
                        if (report.getOtherDocumentsFiles() == null) {
                            report.setOtherDocumentsFiles(new java.util.HashMap<>());
                        }
                        report.getOtherDocumentsFiles().put(fileKey, file.getBytes());
                        
                        if (report.getAttachments() == null) {
                            report.setAttachments(new com.zant.backend.model.ewyp.Attachments());
                        }
                        if (report.getAttachments().getOtherDocuments() == null) {
                            report.getAttachments().setOtherDocuments(new java.util.ArrayList<>());
                            report.getAttachments().setHasOtherDocuments(false);
                        }
                        
                        com.zant.backend.model.ewyp.Attachments.OtherDocument otherDoc = 
                            new com.zant.backend.model.ewyp.Attachments.OtherDocument(
                                documentName, 
                                file.getOriginalFilename()
                            );
                        report.getAttachments().getOtherDocuments().add(otherDoc);
                        
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    } catch (IOException e) {
                        return ResponseEntity.<EWYPReportDTO>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}/attachment/other-document/{index}")
    public ResponseEntity<EWYPReportDTO> deleteOtherDocument(
            @PathVariable UUID id,
            @PathVariable int index) {
        
        return repository.findById(id)
                .map(report -> {
                    if (report.getAttachments() != null && 
                        report.getAttachments().getOtherDocuments() != null &&
                        index >= 0 && 
                        index < report.getAttachments().getOtherDocuments().size()) {
                        
                        report.getAttachments().getOtherDocuments().remove(index);
                        // Note: We're not removing from otherDocumentsFiles map here
                        // You might want to add logic to clean up the map as well
                        
                        EWYPReport savedEntity = repository.save(report);
                        return ResponseEntity.ok(mapper.toDTO(savedEntity));
                    }
                    return ResponseEntity.badRequest().<EWYPReportDTO>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Document Generation Endpoints
    @GetMapping("/{id}/generate-document")
    public ResponseEntity<byte[]> generateDocument(
            @PathVariable UUID id,
            @RequestParam("format") String format) {
        
        return repository.findById(id)
                .<ResponseEntity<byte[]>>map(report -> {
                    try {
                        byte[] documentBytes;
                        String filename;
                        MediaType mediaType;
                        
                        if ("docx".equalsIgnoreCase(format)) {
                            documentBytes = documentService.generateDocx(report);
                            filename = "zawiadomienie_wypadku_" + id + ".docx";
                            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                        } else if ("pdf".equalsIgnoreCase(format)) {
                            documentBytes = documentService.generatePdf(report);
                            filename = "zawiadomienie_wypadku_" + id + ".pdf";
                            mediaType = MediaType.APPLICATION_PDF;
                        } else {
                            return ResponseEntity.<byte[]>badRequest().build();
                        }
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(mediaType);
                        headers.setContentDispositionFormData("attachment", filename);
                        headers.setContentLength(documentBytes.length);
                        
                        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.<byte[]>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

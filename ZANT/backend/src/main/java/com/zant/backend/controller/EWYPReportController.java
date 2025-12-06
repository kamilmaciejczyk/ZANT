package com.zant.backend.controller;

import com.zant.backend.dto.ewyp.EWYPReportDTO;
import com.zant.backend.mapper.EWYPReportMapper;
import com.zant.backend.model.ewyp.EWYPReport;
import com.zant.backend.repository.EWYPReportRepository;
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
    
    public EWYPReportController(EWYPReportRepository repository, EWYPReportMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
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
}

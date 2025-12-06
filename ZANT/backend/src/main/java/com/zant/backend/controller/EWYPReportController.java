package com.zant.backend.controller;

import com.zant.backend.dto.ewyp.EWYPReportDTO;
import com.zant.backend.mapper.EWYPReportMapper;
import com.zant.backend.model.ewyp.EWYPReport;
import com.zant.backend.repository.EWYPReportRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
}

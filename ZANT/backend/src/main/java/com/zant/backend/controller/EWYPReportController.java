package com.zant.backend.controller;

import com.zant.backend.dto.ewyp.EWYPReportDTO;
import com.zant.backend.mapper.EWYPReportMapper;
import com.zant.backend.model.ewyp.EWYPReport;
import com.zant.backend.repository.EWYPReportRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        EWYPReport savedEntity = repository.save(entity);
        EWYPReportDTO responseDTO = mapper.toDTO(savedEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EWYPReportDTO> getReportById(@PathVariable Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

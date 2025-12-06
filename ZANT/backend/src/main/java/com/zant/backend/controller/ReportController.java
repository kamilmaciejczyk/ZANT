package com.zant.backend.controller;

import com.zant.backend.model.AccidentReport;
import com.zant.backend.model.ValidationResult;
import com.zant.backend.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final PdfService pdfService;

    public ReportController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/{id}/validate")
    public ValidationResult validateReport(@PathVariable String id) {
        // TODO: Implement validation logic here
        return new ValidationResult(false, null);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable String id, @RequestParam String type) {
        // TODO: Implement PDF generation logic here
        // For now, use a mock AccidentReport
        AccidentReport mockReport = new AccidentReport();
        // Populate mockReport with some data for testing PDF generation
        // ...

        byte[] pdfBytes = pdfService.generatePdf(mockReport, type.toLowerCase() + "-template");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", type + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}

package com.zant.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EWYPDocumentServiceTest {

    @Test
    void loadsEmbeddedFont() {
        assertDoesNotThrow(EWYPDocumentService::new, "Should load embedded DejaVuSans font");
    }
}

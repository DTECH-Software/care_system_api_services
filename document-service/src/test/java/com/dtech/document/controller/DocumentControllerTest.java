package com.dtech.document.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentControllerTest {

    @Test
    void usesStoredPdfMediaTypeWhenValid() {
        assertEquals(
                MediaType.APPLICATION_PDF,
                DocumentController.resolveMediaType("application/pdf", "birth-certificate.pdf")
        );
    }

    @Test
    void infersPdfMediaTypeWhenLegacyValueIsInvalid() {
        assertEquals(
                MediaType.APPLICATION_PDF,
                DocumentController.resolveMediaType("pdf", "birth-certificate.pdf")
        );
    }

    @Test
    void fallsBackToBinaryMediaTypeWhenTypeCannotBeResolved() {
        assertEquals(
                MediaType.APPLICATION_OCTET_STREAM,
                DocumentController.resolveMediaType(null, "document.unknown")
        );
    }
}

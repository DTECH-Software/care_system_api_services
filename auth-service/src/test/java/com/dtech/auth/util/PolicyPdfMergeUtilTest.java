package com.dtech.auth.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyPdfMergeUtilTest {

    @TempDir
    Path tempDirectory;

    @Test
    void mergesPolicyDocumentsInProvidedOrder() throws Exception {
        File first = createPdf("first.pdf", 1);
        File second = createPdf("second.pdf", 2);

        byte[] merged = PolicyPdfMergeUtil.merge(List.of(first, second));

        try (PDDocument mergedDocument = PDDocument.load(merged)) {
            assertEquals(3, mergedDocument.getNumberOfPages());
        }
    }

    @Test
    void rejectsEmptyDocumentList() {
        assertThrows(IllegalArgumentException.class, () -> PolicyPdfMergeUtil.merge(List.of()));
    }

    private File createPdf(String fileName, int pageCount) throws Exception {
        File file = tempDirectory.resolve(fileName).toFile();
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pageCount; page++) {
                document.addPage(new PDPage());
            }
            document.save(file);
        }
        return file;
    }
}

package com.dtech.auth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyPdfMergeUtil {

    public static byte[] merge(List<File> sourceFiles) throws IOException {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one policy PDF is required");
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        for (File sourceFile : sourceFiles) {
            merger.addSource(sourceFile);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            merger.setDestinationStream(outputStream);
            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
            return outputStream.toByteArray();
        }
    }
}

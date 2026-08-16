package com.dtech.document.service;

import com.dtech.document.enums.DocType;
import com.dtech.document.enums.DocumentStorageProvider;
import com.dtech.document.model.Document;
import com.dtech.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentStorageServiceTest {
    private DocumentRepository documentRepository;
    private S3Client s3Client;
    private DocumentStorageService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        s3Client = mock(S3Client.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<S3Client> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(s3Client);
        service = new DocumentStorageService(documentRepository, provider);
        ReflectionTestUtils.setField(service, "configuredBucket", "wecare-test");
        ReflectionTestUtils.setField(service, "appPrefix", "app");
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesOnlyInDatabaseWhenAppObjectWritesAreDisabled() {
        ReflectionTestUtils.setField(service, "appWriteEnabled", false);
        Document saved = service.saveAppDocument(document(), "legacy".getBytes(StandardCharsets.UTF_8));

        assertEquals(DocumentStorageProvider.DATABASE, saved.getStorageProvider());
        assertEquals(Base64.getEncoder().encodeToString("legacy".getBytes(StandardCharsets.UTF_8)), saved.getDoc());
        assertNull(saved.getObjectKey());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadsWithAppPrefixAndRetainsCompatibilityCopy() {
        ReflectionTestUtils.setField(service, "appWriteEnabled", true);
        ReflectionTestUtils.setField(service, "retainDatabaseCopy", true);
        byte[] content = "app-document".getBytes(StandardCharsets.UTF_8);

        Document saved = service.saveAppDocument(document(), content);

        assertEquals(DocumentStorageProvider.LINODE_OBJECT_STORAGE, saved.getStorageProvider());
        assertEquals(Base64.getEncoder().encodeToString(content), saved.getDoc());
        assertEquals("wecare-test", saved.getBucketName());
        assertNotNull(saved.getObjectKey());
        assertEquals(true, saved.getObjectKey().startsWith("app/profile/"));
        assertNotNull(saved.getChecksumSha256());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadsWithoutKeepingDatabaseContentWhenRetentionIsDisabled() {
        ReflectionTestUtils.setField(service, "appWriteEnabled", true);
        ReflectionTestUtils.setField(service, "retainDatabaseCopy", false);

        Document saved = service.saveAppDocument(document(), "app-document".getBytes(StandardCharsets.UTF_8));

        assertEquals(DocumentStorageProvider.LINODE_OBJECT_STORAGE, saved.getStorageProvider());
        assertNull(saved.getDoc());
        assertNotNull(saved.getObjectKey());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void readsObjectWhenDatabaseCopyIsAbsent() {
        byte[] content = "downloaded-app-document".getBytes(StandardCharsets.UTF_8);
        Document document = document();
        document.setStorageProvider(DocumentStorageProvider.LINODE_OBJECT_STORAGE);
        document.setBucketName("wecare-test");
        document.setObjectKey("app/document/test.txt");
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));

        assertEquals(Base64.getEncoder().encodeToString(content), service.getBase64(document));
    }

    private Document document() {
        Document document = new Document();
        document.setType(DocType.PROFILE);
        document.setFileName("test.txt");
        document.setFileType("text/plain");
        return document;
    }
}

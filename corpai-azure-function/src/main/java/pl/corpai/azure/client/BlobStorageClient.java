package pl.corpai.azure.client;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;

@Slf4j
public class BlobStorageClient {

    private final BlobContainerClient containerClient;

    public BlobStorageClient(String connectionString, String containerName) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Utworzono kontener blob: {}", containerName);
        }
    }

    public String upload(byte[] pdfBytes, String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(new ByteArrayInputStream(pdfBytes), pdfBytes.length, true);
            log.info("Przesłano PDF do Blob Storage: {}", blobName);

            // Generate SAS token with 2 hour TTL
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                    OffsetDateTime.now().plusHours(2), permission);

            String sasToken = blobClient.generateSas(sasValues);
            String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;
            log.info("Wygenerowano SAS URL dla: {}", blobName);
            return sasUrl;

        } catch (Exception e) {
            log.error("Błąd przesyłania do Blob Storage: {}", e.getMessage());
            throw new RuntimeException("Błąd przesyłania PDF do Azure Blob Storage: " + e.getMessage(), e);
        }
    }
}

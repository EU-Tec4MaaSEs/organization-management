package gr.atc.t4m.organization_management.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gr.atc.t4m.organization_management.exception.MiniIOException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import io.minio.RemoveObjectArgs;


@Service
public class MinioService {

    private final MinioClient minioClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioService.class);

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }


    @Value("${minio.url}")
    private String minioUrl;
    @Value("${minio.bucket.name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) {
    String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
    LOGGER.info("Uploading file to MinIO: {}", fileName);
    LOGGER.info("Size file Uploaded to MinIO: {}", file.getSize());

    try (InputStream inputStream = file.getInputStream()) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

      return String.format("%s/%s/%s", minioUrl, bucketName, fileName);

    } catch (Exception e) {
        throw new MiniIOException("Error uploading file to MinIO"+ e.getMessage());
    }
}
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String objectName = extractFileName(fileUrl);
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from MinIO: " + fileUrl, e);
        }
    }

    /**
     * Extracts object filename from URL.
     * Example: "https://minio.api.../bucket-name/1234-image.jpg" -> "1234-image.jpg"
     */
    private String extractFileName(String fileUrl) {
        if (fileUrl.contains("/")) {
            return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        }
        return fileUrl;
    }
}


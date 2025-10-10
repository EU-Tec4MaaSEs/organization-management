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

    public String uploadLogo(MultipartFile file) {
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
}


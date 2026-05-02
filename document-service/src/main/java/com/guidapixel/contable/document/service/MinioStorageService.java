package com.guidapixel.contable.document.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.documents}")
    private String bucketName;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' creado exitosamente", bucketName);
            } else {
                log.info("Bucket '{}' ya existe", bucketName);
            }
        } catch (Exception e) {
            log.error("Error inicializando bucket '{}': {}", bucketName, e.getMessage());
            throw new RuntimeException("No se pudo inicializar el bucket de MinIO", e);
        }
    }

    public void uploadFile(String objectKey, InputStream inputStream, String contentType, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Archivo subido a MinIO: bucket={}, key={}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("Error subiendo archivo a MinIO: {}", e.getMessage());
            throw new RuntimeException("Error subiendo archivo a MinIO", e);
        }
    }

    public InputStream downloadFile(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Error descargando archivo de MinIO: {}", e.getMessage());
            throw new RuntimeException("Error descargando archivo de MinIO", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            log.info("Archivo eliminado de MinIO: bucket={}, key={}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("Error eliminando archivo de MinIO: {}", e.getMessage());
            throw new RuntimeException("Error eliminando archivo de MinIO", e);
        }
    }

    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("Error generando URL firmada de MinIO: {}", e.getMessage());
            throw new RuntimeException("Error generando URL firmada de MinIO", e);
        }
    }
}

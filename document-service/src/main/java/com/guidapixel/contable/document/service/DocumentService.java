package com.guidapixel.contable.document.service;

import com.guidapixel.contable.document.client.NotificationClient;
import com.guidapixel.contable.document.domain.model.Document;
import com.guidapixel.contable.document.domain.repository.DocumentRepository;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import com.guidapixel.contable.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final JwtService jwtService;
    private final NotificationClient notificationClient;

    @Value("${document.storage.path:/app/documents}")
    private String storagePath;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Transactional
    public Document uploadDocument(MultipartFile file, String category, String description, Long clientId, String clientEmail, String clientName, String tenantName) throws IOException {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        boolean fromTenant = isUserFromTenant();

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        String tenantDir = storagePath + "/" + tenantId;
        String categoryDir = tenantDir + "/" + category;

        Path directoryPath = Paths.get(categoryDir);
        Files.createDirectories(directoryPath);

        Path filePath = directoryPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document document = Document.builder()
                .fileName(storedFileName)
                .originalFileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .category(category)
                .uploadedBy("user")
                .fromTenant(fromTenant)
                .description(description)
                .clientId(clientId)
                .build();

        document.setTenantId(tenantId);
        Document saved = documentRepository.save(document);

        if (fromTenant && clientId != null && clientEmail != null && !clientEmail.isBlank()) {
            try {
                notificationClient.sendDocumentoCompartido(
                        clientEmail,
                        clientName != null ? clientName : "Cliente",
                        tenantName != null ? tenantName : "Estudio",
                        tenantId,
                        originalFileName,
                        category,
                        description
                );
            } catch (Exception e) {
                log.warn("Error enviando notificacion de documento compartido: {}", e.getMessage());
            }
        }

        return saved;
    }

    private boolean isUserFromTenant() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return true;
            }
            String authHeader = attrs.getRequest().getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return true;
            }
            String jwt = authHeader.substring(7);
            String role = jwtService.extractRole(jwt);
            return "ROLE_ADMIN".equals(role) || "ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role);
        } catch (Exception e) {
            log.warn("Error determining user role from JWT, defaulting to tenant: {}", e.getMessage());
            return true;
        }
    }

    public List<Document> getDocumentsFromTenant(Long tenantId) {
        return documentRepository.findByTenantIdAndFromTenantTrue(tenantId);
    }

    public List<Document> getDocumentsFromClients(Long tenantId) {
        return documentRepository.findByTenantIdAndFromTenantFalse(tenantId);
    }

    public List<Document> getAllDocuments(Long tenantId) {
        return documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public Document getDocument(Long documentId, Long tenantId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        if (!document.getTenantId().equals(tenantId)) {
            throw new RuntimeException("No tienes acceso a este documento");
        }

        return document;
    }

    public byte[] downloadDocument(Long documentId, Long tenantId) throws IOException {
        Document document = getDocument(documentId, tenantId);

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Archivo no encontrado en el sistema");
        }

        document.setDownloadedAt(LocalDateTime.now());
        documentRepository.save(document);

        return Files.readAllBytes(filePath);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long tenantId) throws IOException {
        Document document = getDocument(documentId, tenantId);

        Path filePath = Paths.get(document.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        documentRepository.delete(document);
    }
}

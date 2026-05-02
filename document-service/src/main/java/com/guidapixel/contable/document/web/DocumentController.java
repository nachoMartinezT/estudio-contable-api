package com.guidapixel.contable.document.web;

import com.guidapixel.contable.document.domain.model.Document;
import com.guidapixel.contable.document.service.DocumentService;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "clientEmail", required = false) String clientEmail,
            @RequestParam(value = "clientName", required = false) String clientName,
            @RequestParam(value = "tenantName", required = false) String tenantName
    ) {
        try {
            Document document = documentService.uploadDocument(file, category, description, clientId, clientEmail, clientName, tenantName);
            return ResponseEntity.ok(Map.of(
                    "status", "EXITO",
                    "document", Map.of(
                            "id", document.getId(),
                            "fileName", document.getOriginalFileName(),
                            "category", document.getCategory(),
                            "fileSize", document.getFileSize(),
                            "uploadedAt", document.getCreatedAt()
                    )
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocuments(
            @RequestParam(value = "from", required = false) String from
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            List<Document> documents;

            if ("client".equals(from)) {
                documents = documentService.getDocumentsFromClients(tenantId);
            } else if ("tenant".equals(from)) {
                documents = documentService.getDocumentsFromTenant(tenantId);
            } else {
                documents = documentService.getAllDocuments(tenantId);
            }

            return ResponseEntity.ok(documents.stream().map(d -> Map.of(
                    "id", d.getId(),
                    "fileName", d.getOriginalFileName(),
                    "fileType", d.getFileType(),
                    "fileSize", d.getFileSize(),
                    "category", d.getCategory(),
                    "description", d.getDescription() != null ? d.getDescription() : "",
                    "fromTenant", d.isFromTenant(),
                    "uploadedBy", d.getUploadedBy(),
                    "createdAt", d.getCreatedAt().toString(),
                    "downloadedAt", d.getDownloadedAt() != null ? d.getDownloadedAt().toString() : null
            )).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        try {
            Long tenantId = TenantContext.getTenantId();
            byte[] fileContent = documentService.downloadDocument(id, tenantId);

            Document document = documentService.getDocument(id, tenantId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(document.getFileType()))
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            Long tenantId = TenantContext.getTenantId();
            documentService.deleteDocument(id, tenantId);
            return ResponseEntity.ok(Map.of("status", "EXITO", "mensaje", "Documento eliminado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", e.getMessage()));
        }
    }
}

package com.guidapixel.contable.auth.domain.model;

import lombok.Getter;

@Getter
public enum Module {
    CLIENTS("clients", "/api/v1/clients/**", "Gestion de clientes"),
    INVOICES("invoices", "/api/v1/invoices/**", "Facturacion interna"),
    AFIP("afip", "/api/afip/**", "Facturacion AFIP oficial"),
    AUDIT("audit", "/api/v1/audit/**", "Logs de auditoria"),
    DASHBOARD("dashboard", "/api/v1/dashboard/**", "Dashboard y metricas"),
    DOCUMENTS("documents", "/api/v1/documents/**", "Gestion de documentos");

    private final String key;
    private final String pathPattern;
    private final String description;

    Module(String key, String pathPattern, String description) {
        this.key = key;
        this.pathPattern = pathPattern;
        this.description = description;
    }

    public static Module fromPath(String path) {
        for (Module module : values()) {
            if (pathMatches(path, module.pathPattern)) {
                return module;
            }
        }
        return null;
    }

    private static boolean pathMatches(String path, String pattern) {
        String regex = pattern.replace("**", ".*").replace("*", "[^/]+");
        return path.matches(regex);
    }
}

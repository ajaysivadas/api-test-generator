package com.apiautomation.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {
    private String path;
    private String method;
    private String operationId;
    private String summary;
    private String tag;
    @Builder.Default
    private List<FieldDefinition> pathParameters = new ArrayList<>();
    @Builder.Default
    private List<FieldDefinition> queryParameters = new ArrayList<>();
    private SchemaDefinition requestBody;
    @Builder.Default
    private Map<String, SchemaDefinition> responses = new HashMap<>();
    private String contentType;
    @Builder.Default
    private List<String> securitySchemes = new ArrayList<>();

    public String getTestMethodName() {
        if (operationId != null && !operationId.isBlank()) {
            return operationId;
        }
        String cleanPath = path.replaceAll("[{}/_-]", " ").trim();
        String[] parts = cleanPath.split("\\s+");
        StringBuilder sb = new StringBuilder(method.toLowerCase());
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public String getTestClassName() {
        if (tag != null && !tag.isBlank()) {
            String clean = tag.replaceAll("[^a-zA-Z0-9]", "");
            return clean.substring(0, 1).toUpperCase() + clean.substring(1) + "ApiTest";
        }
        String[] segments = path.split("/");
        for (String seg : segments) {
            if (!seg.isEmpty() && !seg.startsWith("{")) {
                return seg.substring(0, 1).toUpperCase() + seg.substring(1) + "ApiTest";
            }
        }
        return "GeneratedApiTest";
    }
}

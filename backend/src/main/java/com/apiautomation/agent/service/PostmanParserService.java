package com.apiautomation.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.apiautomation.agent.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostmanParserService implements ParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canParse(String content, String filename) {
        try {
            JsonNode root = objectMapper.readTree(content);
            return root.has("info") && root.has("item")
                    && root.path("info").has("schema")
                    && root.path("info").path("schema").asText().contains("postman");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ApiSpec parse(String content, String filename) {
        try {
            JsonNode root = objectMapper.readTree(content);
            ApiSpec spec = new ApiSpec();
            spec.setSourceFormat("Postman Collection v2.1");

            JsonNode info = root.path("info");
            spec.setTitle(info.path("name").asText("Untitled Collection"));
            spec.setVersion(info.path("version").asText("1.0.0"));
            spec.setDescription(info.path("description").asText(null));

            List<Endpoint> endpoints = new ArrayList<>();
            List<SchemaDefinition> schemas = new ArrayList<>();

            parseItems(root.path("item"), endpoints, schemas, "");

            spec.setEndpoints(endpoints);
            spec.setSchemas(schemas);
            return spec;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Postman collection: " + e.getMessage(), e);
        }
    }

    private void parseItems(JsonNode items, List<Endpoint> endpoints,
                            List<SchemaDefinition> schemas, String folderTag) {
        if (!items.isArray()) return;

        for (JsonNode item : items) {
            if (item.has("item")) {
                // This is a folder
                String folderName = item.path("name").asText(folderTag);
                parseItems(item.path("item"), endpoints, schemas, folderName);
            } else if (item.has("request")) {
                Endpoint endpoint = parseRequest(item, folderTag);
                if (endpoint != null) {
                    endpoints.add(endpoint);
                    extractSchemasFromBody(item, schemas);
                }
            }
        }
    }

    private Endpoint parseRequest(JsonNode item, String folderTag) {
        JsonNode request = item.path("request");

        Endpoint endpoint = new Endpoint();
        endpoint.setSummary(item.path("name").asText());
        endpoint.setTag(folderTag.isEmpty() ? "Default" : folderTag);
        endpoint.setContentType("application/json");

        // Method
        String method = request.path("method").asText("GET").toUpperCase();
        endpoint.setMethod(method);

        // URL
        JsonNode url = request.path("url");
        String path;
        if (url.isTextual()) {
            path = url.asText();
        } else {
            path = buildPathFromUrl(url);

            // Query parameters
            if (url.has("query")) {
                List<FieldDefinition> queryParams = new ArrayList<>();
                for (JsonNode param : url.path("query")) {
                    queryParams.add(FieldDefinition.builder()
                            .name(param.path("key").asText())
                            .type("string")
                            .javaType("String")
                            .description(param.path("description").asText(null))
                            .build());
                }
                endpoint.setQueryParameters(queryParams);
            }

            // Path variables
            if (url.has("variable")) {
                List<FieldDefinition> pathParams = new ArrayList<>();
                for (JsonNode variable : url.path("variable")) {
                    pathParams.add(FieldDefinition.builder()
                            .name(variable.path("key").asText())
                            .type("string")
                            .javaType("String")
                            .description(variable.path("description").asText(null))
                            .build());
                }
                endpoint.setPathParameters(pathParams);
            }
        }
        endpoint.setPath(path);

        // Generate operation ID from name
        String name = item.path("name").asText("unnamed");
        endpoint.setOperationId(toCamelCase(name));

        // Request body
        if (request.has("body")) {
            JsonNode body = request.path("body");
            if ("raw".equals(body.path("mode").asText())) {
                String rawBody = body.path("raw").asText();
                SchemaDefinition requestSchema = inferSchemaFromJson(rawBody, name + "Request");
                if (requestSchema != null) {
                    endpoint.setRequestBody(requestSchema);
                }
            }
        }

        return endpoint;
    }

    private String buildPathFromUrl(JsonNode url) {
        JsonNode pathNode = url.path("path");
        if (!pathNode.isArray()) return "/";

        StringBuilder sb = new StringBuilder();
        for (JsonNode segment : pathNode) {
            sb.append("/");
            String seg = segment.asText();
            if (seg.startsWith(":")) {
                sb.append("{").append(seg.substring(1)).append("}");
            } else {
                sb.append(seg);
            }
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private SchemaDefinition inferSchemaFromJson(String json, String name) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) return null;

            SchemaDefinition schema = new SchemaDefinition();
            schema.setName(toPascalCase(name));

            List<FieldDefinition> fields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fieldsIt = node.fields();
            while (fieldsIt.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldsIt.next();
                fields.add(FieldDefinition.builder()
                        .name(entry.getKey())
                        .type(inferJsonType(entry.getValue()))
                        .javaType(inferJavaType(entry.getValue()))
                        .example(entry.getValue().isValueNode() ? entry.getValue().asText() : null)
                        .build());
            }
            schema.setFields(fields);
            return schema;

        } catch (Exception e) {
            return null;
        }
    }

    private void extractSchemasFromBody(JsonNode item, List<SchemaDefinition> schemas) {
        JsonNode body = item.path("request").path("body");
        if ("raw".equals(body.path("mode").asText())) {
            String rawBody = body.path("raw").asText();
            SchemaDefinition schema = inferSchemaFromJson(rawBody,
                    item.path("name").asText("unnamed") + "Body");
            if (schema != null) {
                schemas.add(schema);
            }
        }
    }

    private String inferJsonType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isInt() || node.isLong()) return "integer";
        if (node.isFloat() || node.isDouble()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        return "string";
    }

    private String inferJavaType(JsonNode node) {
        if (node.isTextual()) return "String";
        if (node.isInt()) return "Integer";
        if (node.isLong()) return "Long";
        if (node.isFloat() || node.isDouble()) return "Double";
        if (node.isBoolean()) return "Boolean";
        if (node.isArray()) return "List<Object>";
        if (node.isObject()) return "Object";
        return "String";
    }

    private String toCamelCase(String input) {
        String[] parts = input.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].substring(0, 1).toUpperCase())
                        .append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private String toPascalCase(String input) {
        String[] parts = input.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}

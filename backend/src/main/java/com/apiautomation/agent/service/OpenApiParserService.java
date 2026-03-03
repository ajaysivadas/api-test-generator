package com.apiautomation.agent.service;

import com.apiautomation.agent.model.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenApiParserService implements ParserService {

    @Override
    public boolean canParse(String content, String filename) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json")) {
                return content.contains("openapi") || content.contains("swagger");
            }
        }
        return content.contains("openapi") || content.contains("swagger");
    }

    @Override
    public ApiSpec parse(String content, String filename) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " +
                    (result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown error"));
        }

        ApiSpec spec = new ApiSpec();
        spec.setSourceFormat(content.contains("openapi") ? "OpenAPI 3.0" : "Swagger 2.0");

        if (openAPI.getInfo() != null) {
            spec.setTitle(openAPI.getInfo().getTitle());
            spec.setVersion(openAPI.getInfo().getVersion());
            spec.setDescription(openAPI.getInfo().getDescription());
        }

        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            spec.setBasePath(openAPI.getServers().get(0).getUrl());
        }

        spec.setSchemas(extractSchemas(openAPI));
        spec.setEndpoints(extractEndpoints(openAPI));

        return spec;
    }

    private List<SchemaDefinition> extractSchemas(OpenAPI openAPI) {
        List<SchemaDefinition> schemas = new ArrayList<>();
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((name, schema) -> {
                SchemaDefinition schemaDef = convertSchema(name, schema);
                if (schemaDef != null) {
                    schemas.add(schemaDef);
                }
            });
        }
        return schemas;
    }

    private SchemaDefinition convertSchema(String name, Schema<?> schema) {
        SchemaDefinition def = new SchemaDefinition();
        def.setName(name);
        def.setDescription(schema.getDescription());

        List<FieldDefinition> fields = new ArrayList<>();
        Set<String> requiredFields = schema.getRequired() != null
                ? new HashSet<>(schema.getRequired())
                : Collections.emptySet();

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((fieldName, fieldSchema) -> {
                Schema<?> fs = (Schema<?>) fieldSchema;
                FieldDefinition field = FieldDefinition.builder()
                        .name((String) fieldName)
                        .type(resolveType(fs))
                        .javaType(mapToJavaType(fs))
                        .format(fs.getFormat())
                        .required(requiredFields.contains(fieldName))
                        .isArray(fs instanceof ArraySchema)
                        .description(fs.getDescription())
                        .example(fs.getExample() != null ? fs.getExample().toString() : null)
                        .refSchema(extractRefName(fs))
                        .build();
                fields.add(field);
            });
        }

        def.setFields(fields);
        return def;
    }

    private List<Endpoint> extractEndpoints(OpenAPI openAPI) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (openAPI.getPaths() == null) return endpoints;

        openAPI.getPaths().forEach((path, pathItem) -> {
            extractOperation(path, "GET", pathItem.getGet(), endpoints);
            extractOperation(path, "POST", pathItem.getPost(), endpoints);
            extractOperation(path, "PUT", pathItem.getPut(), endpoints);
            extractOperation(path, "DELETE", pathItem.getDelete(), endpoints);
            extractOperation(path, "PATCH", pathItem.getPatch(), endpoints);
        });

        return endpoints;
    }

    private void extractOperation(String path, String method, Operation operation, List<Endpoint> endpoints) {
        if (operation == null) return;

        Endpoint endpoint = new Endpoint();
        endpoint.setPath(path);
        endpoint.setMethod(method);
        endpoint.setOperationId(operation.getOperationId());
        endpoint.setSummary(operation.getSummary());
        endpoint.setContentType("application/json");

        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
            endpoint.setTag(operation.getTags().get(0));
        }

        // Extract parameters
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                FieldDefinition field = FieldDefinition.builder()
                        .name(param.getName())
                        .type(param.getSchema() != null ? resolveType(param.getSchema()) : "string")
                        .javaType(param.getSchema() != null ? mapToJavaType(param.getSchema()) : "String")
                        .required(param.getRequired() != null && param.getRequired())
                        .description(param.getDescription())
                        .build();

                if ("path".equals(param.getIn())) {
                    endpoint.getPathParameters().add(field);
                } else if ("query".equals(param.getIn())) {
                    endpoint.getQueryParameters().add(field);
                }
            }
        }

        // Extract request body
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            Content content = operation.getRequestBody().getContent();
            MediaType mediaType = content.get("application/json");
            if (mediaType != null && mediaType.getSchema() != null) {
                String refName = extractRefName(mediaType.getSchema());
                if (refName != null) {
                    endpoint.setRequestBody(SchemaDefinition.builder().name(refName).build());
                } else {
                    endpoint.setRequestBody(convertSchema("RequestBody", mediaType.getSchema()));
                }
            }
        }

        // Extract responses
        if (operation.getResponses() != null) {
            Map<String, SchemaDefinition> responses = new HashMap<>();
            operation.getResponses().forEach((statusCode, response) -> {
                if (response.getContent() != null) {
                    MediaType mediaType = response.getContent().get("application/json");
                    if (mediaType != null && mediaType.getSchema() != null) {
                        String refName = extractRefName(mediaType.getSchema());
                        if (refName != null) {
                            responses.put(statusCode, SchemaDefinition.builder().name(refName).build());
                        } else {
                            responses.put(statusCode, convertSchema("Response" + statusCode, mediaType.getSchema()));
                        }
                    }
                }
            });
            endpoint.setResponses(responses);
        }

        endpoints.add(endpoint);
    }

    private String resolveType(Schema<?> schema) {
        if (schema instanceof ArraySchema arraySchema) {
            Schema<?> items = arraySchema.getItems();
            return "array<" + resolveType(items) + ">";
        }
        if (schema.get$ref() != null) {
            return extractRefFromPath(schema.get$ref());
        }
        return schema.getType() != null ? schema.getType() : "object";
    }

    private String mapToJavaType(Schema<?> schema) {
        if (schema instanceof ArraySchema arraySchema) {
            Schema<?> items = arraySchema.getItems();
            return "List<" + mapToJavaType(items) + ">";
        }
        if (schema.get$ref() != null) {
            return extractRefFromPath(schema.get$ref());
        }
        String type = schema.getType();
        String format = schema.getFormat();

        if (type == null) return "Object";

        return switch (type) {
            case "string" -> {
                if ("date".equals(format)) yield "LocalDate";
                if ("date-time".equals(format)) yield "LocalDateTime";
                if ("uuid".equals(format)) yield "UUID";
                yield "String";
            }
            case "integer" -> "int64".equals(format) ? "Long" : "Integer";
            case "number" -> "float".equals(format) ? "Float" : "Double";
            case "boolean" -> "Boolean";
            case "object" -> "Object";
            default -> "Object";
        };
    }

    private String extractRefName(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return extractRefFromPath(schema.get$ref());
        }
        if (schema instanceof ArraySchema arraySchema && arraySchema.getItems() != null) {
            return extractRefName(arraySchema.getItems());
        }
        return null;
    }

    private String extractRefFromPath(String ref) {
        if (ref == null) return null;
        int lastSlash = ref.lastIndexOf('/');
        return lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
    }
}

package com.apiautomation.agent.service;

import com.apiautomation.agent.model.ApiSpec;
import com.apiautomation.agent.model.Endpoint;
import com.apiautomation.agent.model.FieldDefinition;
import com.apiautomation.agent.model.SchemaDefinition;
import com.apiautomation.agent.util.NamingUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestGeneratorService {

    @Autowired
    private Configuration freemarkerConfig;

    /**
     * Generate one test class per endpoint with Allure annotations, executor pattern,
     * and custom assertions. Each test class goes into its own subdirectory.
     */
    public int generateTests(ApiSpec spec, Path outputDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("test-class.ftl");
        int count = 0;
        Set<String> seen = new HashSet<>();
        Set<String> availablePojos = buildAvailablePojoNames(spec);

        for (Endpoint endpoint : spec.getEndpoints()) {
            String testMethodName = endpoint.getTestMethodName();
            String testGroupName = NamingUtils.toPascalCase(testMethodName);
            if (seen.contains(testGroupName)) continue;
            seen.add(testGroupName);

            String className = testGroupName + "WithCorrectData";
            Path groupDir = outputDir.resolve(testGroupName);
            Files.createDirectories(groupDir);

            boolean endpointHasBody = endpoint.getRequestBody() != null
                    && endpoint.getRequestBody().getFields() != null
                    && !endpoint.getRequestBody().getFields().isEmpty();

            boolean hasPathParam = endpoint.getPath().contains("{");

            // Resolve request POJO class name
            String requestPojoClass = null;
            String payloadBuilderClass = null;
            boolean hasRequestPojo = false;
            if (endpointHasBody) {
                requestPojoClass = resolveRequestPojoClass(endpoint, testGroupName, availablePojos);
                if (requestPojoClass != null) {
                    hasRequestPojo = true;
                    payloadBuilderClass = testGroupName + "Payload";
                }
            }

            // Resolve response POJO class name
            boolean hasResponsePojo = false;
            String responsePojoClass = null;
            if (endpoint.getResponses() != null && !endpoint.getResponses().isEmpty()) {
                responsePojoClass = resolveResponsePojoClass(endpoint, testGroupName, availablePojos);
                hasResponsePojo = responsePojoClass != null;
            }

            String executorClassName = testGroupName + "Executor";
            String executorMethodName = NamingUtils.toCamelCase(testMethodName);
            String responseVarName = NamingUtils.toCamelCase(testMethodName);

            String expectedStatus = switch (endpoint.getMethod()) {
                case "POST" -> "201";
                case "DELETE" -> "204";
                default -> "200";
            };

            String httpStatusCodeEnum = switch (expectedStatus) {
                case "201" -> "CREATED";
                case "204" -> "NO_CONTENT";
                default -> "OK";
            };

            String summary = endpoint.getSummary() != null ? endpoint.getSummary() : testMethodName;

            Map<String, Object> model = new HashMap<>();
            model.put("packageName", basePackage + ".tests." + testGroupName);
            model.put("basePackage", basePackage);
            model.put("className", className);
            model.put("testGroup", testGroupName);
            model.put("executorClassName", executorClassName);
            model.put("executorMethodName", executorMethodName);
            model.put("responseVarName", responseVarName);
            model.put("epicName", spec.getTitle() != null ? spec.getTitle() : "API Tests");
            model.put("featureName", summary);
            model.put("storyName", "Verify " + NamingUtils.toCamelCase(testMethodName) + " with correct data");
            model.put("hasRequestBody", endpointHasBody);
            model.put("hasRequestPojo", hasRequestPojo);
            model.put("hasResponsePojo", hasResponsePojo);
            model.put("hasPayloadBuilder", hasRequestPojo);
            model.put("hasPathParam", hasPathParam);
            model.put("requestPojoClass", requestPojoClass != null ? requestPojoClass : "");
            model.put("responsePojoClass", responsePojoClass != null ? responsePojoClass : "");
            model.put("payloadBuilderClass", payloadBuilderClass != null ? payloadBuilderClass : "");
            model.put("expectedStatus", expectedStatus);
            model.put("httpStatusCodeEnum", httpStatusCodeEnum);
            model.put("apiCallMethodName", "call" + testGroupName + "Api");

            try {
                StringWriter writer = new StringWriter();
                template.process(model, writer);
                Path file = groupDir.resolve(className + ".java");
                Files.writeString(file, writer.toString());
                count++;
            } catch (Exception e) {
                throw new IOException("Failed to generate test class: " + className, e);
            }
        }

        return count;
    }

    /**
     * Generate one executor class per endpoint group with static synchronized methods.
     */
    public int generateExecutors(ApiSpec spec, Path executorsDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("standalone-api-executor.ftl");
        int count = 0;
        Set<String> seen = new HashSet<>();

        // Group endpoints by test group name to build executor with multiple methods
        Map<String, List<Endpoint>> grouped = new LinkedHashMap<>();
        for (Endpoint endpoint : spec.getEndpoints()) {
            String testGroupName = NamingUtils.toPascalCase(endpoint.getTestMethodName());
            grouped.computeIfAbsent(testGroupName, k -> new ArrayList<>()).add(endpoint);
        }

        for (Map.Entry<String, List<Endpoint>> entry : grouped.entrySet()) {
            String testGroupName = entry.getKey();
            if (seen.contains(testGroupName)) continue;
            seen.add(testGroupName);

            String className = testGroupName + "Executor";
            List<Map<String, Object>> methods = new ArrayList<>();

            for (Endpoint endpoint : entry.getValue()) {
                String methodName = NamingUtils.toCamelCase(endpoint.getTestMethodName());

                boolean hasPathParam = endpoint.getPath().contains("{");
                String pathParamName = "id";
                if (hasPathParam && !endpoint.getPathParameters().isEmpty()) {
                    pathParamName = endpoint.getPathParameters().get(0).getName();
                }

                Map<String, Object> method = new HashMap<>();
                method.put("methodName", methodName);
                method.put("httpMethod", endpoint.getMethod());
                method.put("hasPathParam", hasPathParam);
                method.put("pathParamName", pathParamName);
                method.put("path", endpoint.getPath());
                methods.add(method);
            }

            Map<String, Object> model = new HashMap<>();
            model.put("packageName", basePackage + ".executors");
            model.put("basePackage", basePackage);
            model.put("className", className);
            model.put("methods", methods);

            try {
                StringWriter writer = new StringWriter();
                template.process(model, writer);
                Path file = executorsDir.resolve(className + ".java");
                Files.writeString(file, writer.toString());
                count++;
            } catch (Exception e) {
                throw new IOException("Failed to generate executor class: " + className, e);
            }
        }

        return count;
    }

    /**
     * Generate payload builders for endpoints with request bodies.
     * Uses Lombok .builder() factory and inner field enum.
     */
    public int generatePayloadBuilders(ApiSpec spec, Path payloadsDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("standalone-payload-builder.ftl");
        int count = 0;
        Set<String> seen = new HashSet<>();
        Set<String> availablePojos = buildAvailablePojoNames(spec);

        for (Endpoint endpoint : spec.getEndpoints()) {
            if (endpoint.getRequestBody() != null && endpoint.getRequestBody().getFields() != null
                    && !endpoint.getRequestBody().getFields().isEmpty()) {

                String testGroupName = NamingUtils.toPascalCase(endpoint.getTestMethodName());
                String className = testGroupName + "Payload";
                if (seen.contains(className)) continue;
                seen.add(className);

                String requestPojoClass = resolveRequestPojoClass(endpoint, testGroupName, availablePojos);
                if (requestPojoClass == null) continue;

                List<Map<String, Object>> fields = endpoint.getRequestBody().getFields().stream()
                        .map(f -> {
                            Map<String, Object> fm = new HashMap<>();
                            fm.put("name", NamingUtils.toCamelCase(f.getName()));
                            fm.put("javaType", f.getJavaType());
                            fm.put("enumName", NamingUtils.toEnumName(f.getName()));
                            fm.put("required", f.isRequired());
                            return fm;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> model = new HashMap<>();
                model.put("packageName", basePackage + ".payloads");
                model.put("basePackage", basePackage);
                model.put("className", className);
                model.put("requestPojoClass", requestPojoClass);
                model.put("fields", fields);

                try {
                    StringWriter writer = new StringWriter();
                    template.process(model, writer);
                    Path file = payloadsDir.resolve(className + ".java");
                    Files.writeString(file, writer.toString());
                    count++;
                } catch (Exception e) {
                    throw new IOException("Failed to generate payload builder: " + className, e);
                }
            }
        }

        return count;
    }

    /**
     * Resolves the request body POJO class name for an endpoint.
     * Returns null if no matching POJO exists.
     */
    private String resolveRequestPojoClass(Endpoint endpoint, String testGroupName, Set<String> availablePojos) {
        String className = endpoint.getRequestBody().getClassName();

        // If it's not a generic name, use it directly
        if (className != null && !className.isBlank() && !isGenericSchemaName(className)) {
            if (!className.endsWith("Request")) {
                className = className + "Request";
            }
            if (availablePojos.contains(className)) {
                return className;
            }
        }

        // Try derived name: testGroupName + "Request"
        String derived = testGroupName + "Request";
        if (availablePojos.contains(derived)) {
            return derived;
        }

        // Try just testGroupName (some schemas may not end with Request)
        if (availablePojos.contains(testGroupName)) {
            return testGroupName;
        }

        return null;
    }

    /**
     * Resolves the response POJO class name for an endpoint.
     * Returns null if no matching POJO exists.
     */
    private String resolveResponsePojoClass(Endpoint endpoint, String testGroupName, Set<String> availablePojos) {
        for (SchemaDefinition schema : endpoint.getResponses().values()) {
            if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) continue;

            String className = schema.getClassName();

            // If it's not a generic name, use it directly
            if (className != null && !isGenericSchemaName(className)) {
                if (availablePojos.contains(className)) {
                    return className;
                }
                // Try with Response suffix
                String withSuffix = className + "Response";
                if (availablePojos.contains(withSuffix)) {
                    return withSuffix;
                }
            }

            // Try to match by field overlap with available schemas
            // (generic response names like "Response200" won't have a matching POJO)
        }

        // Try common patterns
        String derived = testGroupName + "Response";
        if (availablePojos.contains(derived)) {
            return derived;
        }

        return null;
    }

    /**
     * Checks if a schema class name is a generic auto-generated name
     * (like "RequestBody", "Response200", "Response201", etc.)
     */
    private boolean isGenericSchemaName(String className) {
        if (className == null) return true;
        if ("RequestBody".equals(className)) return true;
        if ("Unknown".equals(className)) return true;
        if (className.matches("Response\\d+")) return true;
        return false;
    }

    /**
     * Builds a set of all available POJO class names from the spec schemas.
     */
    private Set<String> buildAvailablePojoNames(ApiSpec spec) {
        return spec.getSchemas().stream()
                .map(SchemaDefinition::getClassName)
                .filter(name -> name != null && !"Unknown".equals(name))
                .collect(Collectors.toSet());
    }
}

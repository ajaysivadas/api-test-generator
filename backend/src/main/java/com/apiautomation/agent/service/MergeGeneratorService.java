package com.apiautomation.agent.service;

import com.apiautomation.agent.model.*;
import com.apiautomation.agent.util.NamingUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MergeGeneratorService {

    @Value("${agent.output.dir}")
    private String outputBaseDir;

    @Autowired
    private Configuration freemarkerConfig;

    public MergeResult generate(ApiSpec spec, MergeConfig config) throws IOException {
        String generationId = UUID.randomUUID().toString();
        Path outputDir = Path.of(outputBaseDir, generationId);
        Files.createDirectories(outputDir);

        String serviceName = config.getServiceName();
        String baseUriKey = config.getBaseUriKey();
        String baseUriEnumName = config.getBaseUriEnumName() != null && !config.getBaseUriEnumName().isBlank()
                ? config.getBaseUriEnumName() : serviceName;

        // Create directory structure matching existing framework
        Path mainJava = outputDir.resolve("src/main/java/MarketFeed/Api_Test");
        Path requestPojoDir = mainJava.resolve("RequestPojo/" + serviceName);
        Path responsePojoDir = mainJava.resolve("ResponsePojo/" + serviceName);
        Path executorDir = mainJava.resolve("ApiExecutors/" + serviceName);
        Path payloadDir = mainJava.resolve("Payloads/RequestPayload/" + serviceName);
        Path testJava = outputDir.resolve("src/test/java/MarketFeed/Api_Test/" + serviceName);
        Path suiteDir = outputDir.resolve("test-suite/" + serviceName);

        Files.createDirectories(requestPojoDir);
        Files.createDirectories(responsePojoDir);
        Files.createDirectories(executorDir);
        Files.createDirectories(payloadDir);
        Files.createDirectories(testJava);
        Files.createDirectories(suiteDir);

        List<String> generatedFiles = new ArrayList<>();
        List<Map<String, Object>> endpointEntriesData = new ArrayList<>();

        // Generate Request POJOs
        int requestPojoCount = generateRequestPojos(spec, requestPojoDir, serviceName, generatedFiles);

        // Generate Response POJOs
        int responsePojoCount = generateResponsePojos(spec, responsePojoDir, serviceName, generatedFiles);

        // Build endpoint entries data for executor and enum generation
        buildEndpointEntries(spec, serviceName, endpointEntriesData);

        // Generate API Executor
        int executorMethodCount = generateApiExecutor(spec, executorDir, serviceName, baseUriEnumName, endpointEntriesData, generatedFiles);

        // Generate Payload Builders
        int payloadBuilderCount = generatePayloadBuilders(spec, payloadDir, serviceName, generatedFiles);

        // Generate Test Classes
        int testClassCount = generateTestClasses(spec, testJava, serviceName, endpointEntriesData, generatedFiles);

        // Generate TestNG Suite
        generateTestNgSuite(spec, suiteDir, serviceName, testJava, generatedFiles);

        // Generate merge instructions
        generateMergeInstructions(outputDir, serviceName, baseUriKey, baseUriEnumName, endpointEntriesData, generatedFiles);

        // Build endpoint entry strings for the response
        List<String> endpointEntryStrings = endpointEntriesData.stream()
                .map(e -> e.get("enumName") + "(\"" + e.get("path") + "\")")
                .collect(Collectors.toList());

        String baseUriEntry = baseUriEnumName + "(\"" + baseUriKey + "\")";

        return MergeResult.builder()
                .generationId(generationId)
                .outputDir(outputDir.toString())
                .requestPojoCount(requestPojoCount)
                .responsePojoCount(responsePojoCount)
                .testClassCount(testClassCount)
                .executorMethodCount(executorMethodCount)
                .payloadBuilderCount(payloadBuilderCount)
                .endpointEntries(endpointEntryStrings)
                .baseUriEntry(baseUriEntry)
                .generatedFiles(generatedFiles)
                .build();
    }

    private int generateRequestPojos(ApiSpec spec, Path outputDir, String serviceName, List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-request-pojo.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-request-pojo.ftl template", e);
        }

        int count = 0;
        Set<String> generated = new HashSet<>();

        for (Endpoint endpoint : spec.getEndpoints()) {
            if (endpoint.getRequestBody() != null && endpoint.getRequestBody().getFields() != null
                    && !endpoint.getRequestBody().getFields().isEmpty()) {
                String className = endpoint.getRequestBody().getClassName();
                if (className == null || className.isBlank()) {
                    className = toPascalCase(endpoint.getTestMethodName()) + "Request";
                }
                if (!className.endsWith("Request")) {
                    className = className + "Request";
                }
                if (generated.contains(className)) continue;
                generated.add(className);

                Map<String, Object> model = new HashMap<>();
                model.put("serviceName", serviceName);
                model.put("subPackage", "");
                model.put("className", className);
                model.put("fields", endpoint.getRequestBody().getFields().stream()
                        .map(this::fieldToMap).collect(Collectors.toList()));
                model.put("imports", resolveImports(endpoint.getRequestBody().getFields()));

                writeTemplate(template, model, outputDir.resolve(className + ".java"));
                generatedFiles.add("src/main/java/MarketFeed/Api_Test/RequestPojo/" + serviceName + "/" + className + ".java");
                count++;
            }
        }
        return count;
    }

    private int generateResponsePojos(ApiSpec spec, Path outputDir, String serviceName, List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-response-pojo.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-response-pojo.ftl template", e);
        }

        int count = 0;
        Set<String> generated = new HashSet<>();

        for (Endpoint endpoint : spec.getEndpoints()) {
            if (endpoint.getResponses() != null && !endpoint.getResponses().isEmpty()) {
                for (Map.Entry<String, SchemaDefinition> respEntry : endpoint.getResponses().entrySet()) {
                    SchemaDefinition schema = respEntry.getValue();
                    if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) continue;

                    String className = schema.getClassName();
                    if (className == null || className.isBlank() || "Unknown".equals(className)) {
                        className = toPascalCase(endpoint.getTestMethodName()) + "Response";
                    }
                    if (!className.endsWith("Response")) {
                        className = className + "Response";
                    }
                    if (generated.contains(className)) continue;
                    generated.add(className);

                    boolean hasJsonProperty = schema.getFields().stream()
                            .anyMatch(f -> f.getName() != null && !f.getName().equals(toJsonName(f.getName())));

                    Map<String, Object> model = new HashMap<>();
                    model.put("serviceName", serviceName);
                    model.put("subPackage", "");
                    model.put("className", className);
                    model.put("hasJsonProperty", hasJsonProperty || hasSnakeCaseFields(schema.getFields()));
                    model.put("fields", schema.getFields().stream()
                            .map(this::responseFieldToMap).collect(Collectors.toList()));
                    model.put("imports", resolveImports(schema.getFields()));

                    writeTemplate(template, model, outputDir.resolve(className + ".java"));
                    generatedFiles.add("src/main/java/MarketFeed/Api_Test/ResponsePojo/" + serviceName + "/" + className + ".java");
                    count++;
                }
            }
        }

        // Also generate POJOs from schemas that aren't directly tied to endpoints
        for (SchemaDefinition schema : spec.getSchemas()) {
            String className = schema.getClassName();
            if (className == null || "Unknown".equals(className)) continue;
            if (!className.endsWith("Response")) {
                className = className + "Response";
            }
            if (generated.contains(className)) continue;
            if (schema.getFields() == null || schema.getFields().isEmpty()) continue;
            generated.add(className);

            boolean hasJsonProperty = hasSnakeCaseFields(schema.getFields());

            Map<String, Object> model = new HashMap<>();
            model.put("serviceName", serviceName);
            model.put("subPackage", "");
            model.put("className", className);
            model.put("hasJsonProperty", hasJsonProperty);
            model.put("fields", schema.getFields().stream()
                    .map(this::responseFieldToMap).collect(Collectors.toList()));
            model.put("imports", resolveImports(schema.getFields()));

            writeTemplate(template, model, outputDir.resolve(className + ".java"));
            generatedFiles.add("src/main/java/MarketFeed/Api_Test/ResponsePojo/" + serviceName + "/" + className + ".java");
            count++;
        }

        return count;
    }

    private void buildEndpointEntries(ApiSpec spec, String serviceName,
                                       List<Map<String, Object>> endpointEntriesData) {
        Set<String> usedEnumNames = new HashSet<>();

        for (Endpoint endpoint : spec.getEndpoints()) {
            String enumName = buildEndpointEnumName(endpoint, serviceName);
            if (usedEnumNames.contains(enumName)) continue;
            usedEnumNames.add(enumName);

            String path = endpoint.getPath();
            // Convert path params from {id} to {param} format for the framework
            boolean hasPathParam = path.contains("{");
            String frameworkPath = path.replaceAll("\\{[^}]+}", "{param}");

            Map<String, Object> entry = new HashMap<>();
            entry.put("enumName", enumName);
            entry.put("path", frameworkPath);
            entry.put("originalPath", path);
            entry.put("hasPathParam", hasPathParam);
            entry.put("method", endpoint.getMethod());
            entry.put("operationId", endpoint.getOperationId());
            entry.put("testMethodName", endpoint.getTestMethodName());
            endpointEntriesData.add(entry);
        }
    }

    private int generateApiExecutor(ApiSpec spec, Path outputDir, String serviceName,
                                     String baseUriEnumName, List<Map<String, Object>> endpointEntriesData,
                                     List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-api-executor.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-api-executor.ftl template", e);
        }

        String className = serviceName + "Api";

        List<Map<String, Object>> methods = new ArrayList<>();
        Set<String> usedMethodNames = new HashSet<>();

        for (int i = 0; i < spec.getEndpoints().size(); i++) {
            Endpoint endpoint = spec.getEndpoints().get(i);
            Map<String, Object> epEntry = i < endpointEntriesData.size() ? endpointEntriesData.get(i) : null;

            String methodName = toCamelCase(endpoint.getTestMethodName());
            if (usedMethodNames.contains(methodName)) {
                methodName = methodName + endpoint.getMethod().substring(0, 1).toUpperCase()
                        + endpoint.getMethod().substring(1).toLowerCase();
            }
            usedMethodNames.add(methodName);

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
            method.put("endpointEnumName", epEntry != null ? epEntry.get("enumName") : buildEndpointEnumName(endpoint, serviceName));
            methods.add(method);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("serviceName", serviceName);
        model.put("className", className);
        model.put("baseUriEnumName", baseUriEnumName);
        model.put("methods", methods);

        writeTemplate(template, model, outputDir.resolve(className + ".java"));
        generatedFiles.add("src/main/java/MarketFeed/Api_Test/ApiExecutors/" + serviceName + "/" + className + ".java");

        return methods.size();
    }

    private int generatePayloadBuilders(ApiSpec spec, Path outputDir, String serviceName,
                                         List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-payload-builder.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-payload-builder.ftl template", e);
        }

        int count = 0;
        Set<String> generated = new HashSet<>();

        for (Endpoint endpoint : spec.getEndpoints()) {
            if (endpoint.getRequestBody() != null && endpoint.getRequestBody().getFields() != null
                    && !endpoint.getRequestBody().getFields().isEmpty()) {

                String requestPojoClass = endpoint.getRequestBody().getClassName();
                if (requestPojoClass == null || requestPojoClass.isBlank()) {
                    requestPojoClass = toPascalCase(endpoint.getTestMethodName()) + "Request";
                }
                if (!requestPojoClass.endsWith("Request")) {
                    requestPojoClass = requestPojoClass + "Request";
                }

                String className = toPascalCase(endpoint.getTestMethodName()) + "Payload";
                if (generated.contains(className)) continue;
                generated.add(className);

                List<Map<String, Object>> fields = endpoint.getRequestBody().getFields().stream()
                        .map(f -> {
                            Map<String, Object> fm = fieldToMap(f);
                            fm.put("enumName", toEnumName(f.getName()));
                            return fm;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> model = new HashMap<>();
                model.put("serviceName", serviceName);
                model.put("className", className);
                model.put("requestPojoClass", requestPojoClass);
                model.put("fields", fields);
                model.put("imports", resolveImports(endpoint.getRequestBody().getFields()));

                writeTemplate(template, model, outputDir.resolve(className + ".java"));
                generatedFiles.add("src/main/java/MarketFeed/Api_Test/Payloads/RequestPayload/" + serviceName + "/" + className + ".java");
                count++;
            }
        }
        return count;
    }

    private int generateTestClasses(ApiSpec spec, Path testDir, String serviceName,
                                     List<Map<String, Object>> endpointEntriesData,
                                     List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-test-class.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-test-class.ftl template", e);
        }

        int count = 0;

        for (int i = 0; i < spec.getEndpoints().size(); i++) {
            Endpoint endpoint = spec.getEndpoints().get(i);

            String testMethodName = endpoint.getTestMethodName();
            String testGroupName = toPascalCase(testMethodName);
            String className = testGroupName + "WithCorrectData";

            Path groupDir = testDir.resolve(testGroupName);
            Files.createDirectories(groupDir);

            boolean hasRequestBody = endpoint.getRequestBody() != null
                    && endpoint.getRequestBody().getFields() != null
                    && !endpoint.getRequestBody().getFields().isEmpty();

            boolean hasResponse = endpoint.getResponses() != null && !endpoint.getResponses().isEmpty();
            boolean hasPathParam = endpoint.getPath().contains("{");

            String requestPojoClass = null;
            String requestPojoImport = null;
            String payloadBuilderClass = null;
            String payloadBuilderImport = null;

            if (hasRequestBody) {
                requestPojoClass = endpoint.getRequestBody().getClassName();
                if (requestPojoClass == null || requestPojoClass.isBlank()) {
                    requestPojoClass = toPascalCase(testMethodName) + "Request";
                }
                if (!requestPojoClass.endsWith("Request")) {
                    requestPojoClass = requestPojoClass + "Request";
                }
                requestPojoImport = requestPojoClass;
                payloadBuilderClass = toPascalCase(testMethodName) + "Payload";
                payloadBuilderImport = payloadBuilderClass;
            }

            String responsePojoClass = null;
            String responsePojoImport = null;
            if (hasResponse) {
                SchemaDefinition firstResponse = endpoint.getResponses().values().iterator().next();
                if (firstResponse != null && firstResponse.getFields() != null && !firstResponse.getFields().isEmpty()) {
                    responsePojoClass = firstResponse.getClassName();
                    if (responsePojoClass == null || responsePojoClass.isBlank() || "Unknown".equals(responsePojoClass)) {
                        responsePojoClass = toPascalCase(testMethodName) + "Response";
                    }
                    if (!responsePojoClass.endsWith("Response")) {
                        responsePojoClass = responsePojoClass + "Response";
                    }
                    responsePojoImport = responsePojoClass;
                } else {
                    hasResponse = false;
                }
            }

            String executorClassName = serviceName + "Api";
            String executorMethodName = toCamelCase(testMethodName);
            String responseVarName = toCamelCase(testMethodName);

            // Determine expected status
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
            model.put("serviceName", serviceName);
            model.put("testGroup", testGroupName);
            model.put("className", className);
            model.put("executorClassName", executorClassName);
            model.put("executorMethodName", executorMethodName);
            model.put("responseVarName", responseVarName);
            model.put("epicName", serviceName);
            model.put("featureName", summary);
            model.put("storyName", "Verify " + toCamelCase(testMethodName) + " with correct data");
            model.put("hasRequestPojo", hasRequestBody);
            model.put("hasResponsePojo", hasResponse);
            model.put("hasPayloadBuilder", hasRequestBody);
            model.put("hasPathParam", hasPathParam);
            model.put("requestPojoClass", requestPojoClass != null ? requestPojoClass : "");
            model.put("requestPojoImport", requestPojoImport != null ? requestPojoImport : "");
            model.put("responsePojoClass", responsePojoClass != null ? responsePojoClass : "");
            model.put("responsePojoImport", responsePojoImport != null ? responsePojoImport : "");
            model.put("payloadBuilderClass", payloadBuilderClass != null ? payloadBuilderClass : "");
            model.put("payloadBuilderImport", payloadBuilderImport != null ? payloadBuilderImport : "");
            model.put("expectedStatus", expectedStatus);
            model.put("httpStatusCodeEnum", httpStatusCodeEnum);
            model.put("apiCallMethodName", "call" + toPascalCase(testMethodName) + "Api");

            writeTemplate(template, model, groupDir.resolve(className + ".java"));
            generatedFiles.add("src/test/java/MarketFeed/Api_Test/" + serviceName + "/" + testGroupName + "/" + className + ".java");
            count++;
        }

        return count;
    }

    private void generateTestNgSuite(ApiSpec spec, Path suiteDir, String serviceName,
                                      Path testJava, List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-testng-suite.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-testng-suite.ftl template", e);
        }

        // Group endpoints by tag or path segment
        Map<String, List<Endpoint>> grouped = spec.getEndpoints().stream()
                .collect(Collectors.groupingBy(ep -> toPascalCase(ep.getTestMethodName())));

        List<Map<String, Object>> testGroups = new ArrayList<>();
        for (Map.Entry<String, List<Endpoint>> entry : grouped.entrySet()) {
            String groupName = entry.getKey();
            List<String> testClasses = new ArrayList<>();
            testClasses.add(groupName + "WithCorrectData");

            Map<String, Object> group = new HashMap<>();
            group.put("name", groupName);
            group.put("groupName", groupName);
            group.put("threadCount", String.valueOf(Math.min(testClasses.size(), 3)));
            group.put("testClasses", testClasses);
            testGroups.add(group);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("serviceName", serviceName);
        model.put("testGroups", testGroups);

        String suiteFileName = "testng-" + serviceName + "-STAGE.xml";
        writeTemplate(template, model, suiteDir.resolve(suiteFileName));
        generatedFiles.add("test-suite/" + serviceName + "/" + suiteFileName);
    }

    private void generateMergeInstructions(Path outputDir, String serviceName, String baseUriKey,
                                            String baseUriEnumName, List<Map<String, Object>> endpointEntriesData,
                                            List<String> generatedFiles) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate("merge-enum-entries.ftl");
        } catch (IOException e) {
            throw new IOException("Failed to load merge-enum-entries.ftl template", e);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("serviceName", serviceName);
        model.put("baseUriKey", baseUriKey);
        model.put("baseUriEnumName", baseUriEnumName);
        model.put("endpointEntries", endpointEntriesData);

        writeTemplate(template, model, outputDir.resolve("MERGE_INSTRUCTIONS.txt"));
        generatedFiles.add("MERGE_INSTRUCTIONS.txt");
    }

    private void writeTemplate(Template template, Map<String, Object> model, Path outputFile) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            Files.writeString(outputFile, writer.toString());
        } catch (Exception e) {
            throw new IOException("Failed to write template to " + outputFile, e);
        }
    }

    private Map<String, Object> fieldToMap(FieldDefinition field) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", toCamelCase(field.getName()));
        map.put("javaType", field.getJavaType());
        map.put("required", field.isRequired());
        map.put("description", field.getDescription() != null ? field.getDescription() : "");
        return map;
    }

    private Map<String, Object> responseFieldToMap(FieldDefinition field) {
        Map<String, Object> map = new HashMap<>();
        String camelName = toCamelCase(field.getName());
        map.put("name", camelName);
        map.put("javaType", field.getJavaType());
        map.put("jsonName", field.getName());
        map.put("required", field.isRequired());
        map.put("description", field.getDescription() != null ? field.getDescription() : "");
        return map;
    }

    private Set<String> resolveImports(List<FieldDefinition> fields) {
        Set<String> imports = new TreeSet<>();
        for (FieldDefinition field : fields) {
            String javaType = field.getJavaType();
            if (javaType.startsWith("List<")) {
                imports.add("java.util.List");
            }
            if (javaType.contains("Map<")) {
                imports.add("java.util.Map");
            }
            if (javaType.contains("LocalDate") && !javaType.contains("DateTime")) {
                imports.add("java.time.LocalDate");
            }
            if (javaType.contains("LocalDateTime")) {
                imports.add("java.time.LocalDateTime");
            }
            if (javaType.contains("UUID")) {
                imports.add("java.util.UUID");
            }
        }
        return imports;
    }

    private boolean hasSnakeCaseFields(List<FieldDefinition> fields) {
        return fields.stream().anyMatch(f -> f.getName() != null && f.getName().contains("_"));
    }

    private String toJsonName(String name) {
        return name;
    }

    private String buildEndpointEnumName(Endpoint endpoint, String serviceName) {
        String testMethodName = endpoint.getTestMethodName();
        return toPascalCase(testMethodName);
    }

    private String toPascalCase(String input) {
        return NamingUtils.toPascalCase(input);
    }

    private String toCamelCase(String input) {
        return NamingUtils.toCamelCase(input);
    }

    private String toEnumName(String input) {
        return NamingUtils.toEnumName(input);
    }
}

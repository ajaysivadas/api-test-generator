package com.apiautomation.agent.service;

import com.apiautomation.agent.model.ApiSpec;
import com.apiautomation.agent.model.MergeConfig;
import com.apiautomation.agent.model.MergeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GeneratorService {

    @Value("${agent.output.dir}")
    private String outputBaseDir;

    @Autowired
    private PojoGeneratorService pojoGenerator;

    @Autowired
    private TestGeneratorService testGenerator;

    @Autowired
    private FrameworkGeneratorService frameworkGenerator;

    @Autowired
    private MergeGeneratorService mergeGenerator;

    public Map<String, Object> generate(ApiSpec spec, String basePackage) throws IOException {
        String generationId = UUID.randomUUID().toString();
        Path outputDir = Path.of(outputBaseDir, generationId);
        Files.createDirectories(outputDir);

        String packagePath = basePackage.replace('.', '/');

        // Create directory structure
        Path mainJava = outputDir.resolve("src/main/java/" + packagePath);
        Path modelsDir = mainJava.resolve("models");
        Path configDir = mainJava.resolve("config");
        Path testJava = outputDir.resolve("src/test/java/" + packagePath);
        Path baseDir = testJava.resolve("base");
        Path testsDir = testJava.resolve("tests");
        Path utilsDir = testJava.resolve("utils");
        Path assertionsDir = testJava.resolve("assertions");
        Path executorsDir = testJava.resolve("executors");
        Path payloadsDir = testJava.resolve("payloads");
        Path testResources = outputDir.resolve("src/test/resources");

        Files.createDirectories(modelsDir);
        Files.createDirectories(configDir);
        Files.createDirectories(baseDir);
        Files.createDirectories(testsDir);
        Files.createDirectories(utilsDir);
        Files.createDirectories(assertionsDir);
        Files.createDirectories(executorsDir);
        Files.createDirectories(payloadsDir);
        Files.createDirectories(testResources);

        // Generate POJOs
        int pojoCount = pojoGenerator.generatePojos(spec, modelsDir, basePackage);

        // Generate executors
        int executorCount = testGenerator.generateExecutors(spec, executorsDir, basePackage);

        // Generate payload builders
        int payloadBuilderCount = testGenerator.generatePayloadBuilders(spec, payloadsDir, basePackage);

        // Generate test classes
        int testCount = testGenerator.generateTests(spec, testsDir, basePackage);

        // Generate framework files
        frameworkGenerator.generateBaseTest(baseDir, basePackage);
        frameworkGenerator.generateApiConfig(configDir, basePackage);
        frameworkGenerator.generateTestUtils(utilsDir, basePackage);
        frameworkGenerator.generateAssertions(assertionsDir, basePackage);
        frameworkGenerator.generateHttpStatusCode(assertionsDir, basePackage);
        frameworkGenerator.generatePom(outputDir, basePackage, spec.getTitle());
        frameworkGenerator.generateTestNg(outputDir, basePackage, spec);
        frameworkGenerator.generateConfigProperties(testResources, spec);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generationId", generationId);
        result.put("outputDir", outputDir.toString());
        result.put("pojoCount", pojoCount);
        result.put("testCount", testCount);
        result.put("executorCount", executorCount);
        result.put("payloadBuilderCount", payloadBuilderCount);
        result.put("endpointCount", spec.getEndpoints().size());
        result.put("schemaCount", spec.getSchemas().size());
        return result;
    }

    public MergeResult generateMerge(ApiSpec spec, MergeConfig config) throws IOException {
        return mergeGenerator.generate(spec, config);
    }
}

package com.apiautomation.agent.service;

import com.apiautomation.agent.model.ApiSpec;
import com.apiautomation.agent.model.Endpoint;
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
public class FrameworkGeneratorService {

    @Autowired
    private Configuration freemarkerConfig;

    public void generateBaseTest(Path baseDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("base-test.ftl");
        Map<String, Object> model = Map.of("packageName", basePackage + ".base");
        writeTemplate(template, model, baseDir.resolve("BaseTest.java"));
    }

    public void generateApiConfig(Path configDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("config.ftl");
        Map<String, Object> model = Map.of("packageName", basePackage + ".config");
        writeTemplate(template, model, configDir.resolve("ApiConfig.java"));
    }

    public void generateTestUtils(Path utilsDir, String basePackage) throws IOException {
        Map<String, Object> model = Map.of("packageName", basePackage + ".utils");
        String content = """
                package %s;

                import com.fasterxml.jackson.databind.ObjectMapper;
                import io.restassured.response.Response;

                import java.io.IOException;
                import java.util.Random;

                public class TestUtils {

                    private static final ObjectMapper objectMapper = new ObjectMapper();
                    private static final Random random = new Random();

                    public static <T> T deserialize(Response response, Class<T> clazz) {
                        try {
                            return objectMapper.readValue(response.getBody().asString(), clazz);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to deserialize response", e);
                        }
                    }

                    public static String toJson(Object obj) {
                        try {
                            return objectMapper.writeValueAsString(obj);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to serialize object", e);
                        }
                    }

                    public static String randomString(int length) {
                        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                        StringBuilder sb = new StringBuilder(length);
                        for (int i = 0; i < length; i++) {
                            sb.append(chars.charAt(random.nextInt(chars.length())));
                        }
                        return sb.toString();
                    }

                    public static int randomInt(int min, int max) {
                        return random.nextInt(max - min) + min;
                    }
                }
                """.formatted(basePackage + ".utils");

        Files.writeString(utilsDir.resolve("TestUtils.java"), content);
    }

    public void generatePom(Path outputDir, String basePackage, String projectName) throws IOException {
        Template template = freemarkerConfig.getTemplate("pom.ftl");
        String groupId = basePackage.contains(".") ? basePackage.substring(0, basePackage.lastIndexOf('.')) : basePackage;
        String artifactId = projectName != null
                ? projectName.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                : "api-tests";

        Map<String, Object> model = Map.of(
                "groupId", groupId,
                "artifactId", artifactId,
                "projectName", projectName != null ? projectName : "API Tests"
        );
        writeTemplate(template, model, outputDir.resolve("pom.xml"));
    }

    public void generateTestNg(Path outputDir, String basePackage, ApiSpec spec) throws IOException {
        Template template = freemarkerConfig.getTemplate("testng.ftl");

        // Generate per-endpoint class names: {package}.tests.{TestGroup}.{TestGroup}WithCorrectData
        List<String> testClasses = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Endpoint endpoint : spec.getEndpoints()) {
            String testGroupName = NamingUtils.toPascalCase(endpoint.getTestMethodName());
            if (seen.contains(testGroupName)) continue;
            seen.add(testGroupName);
            testClasses.add(testGroupName + "." + testGroupName + "WithCorrectData");
        }

        Map<String, Object> model = Map.of(
                "suiteName", spec.getTitle() != null ? spec.getTitle() : "API Tests",
                "packageName", basePackage + ".tests",
                "testClasses", testClasses
        );
        writeTemplate(template, model, outputDir.resolve("testng.xml"));
    }

    public void generateAssertions(Path assertionsDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("standalone-assertions.ftl");
        Map<String, Object> model = Map.of("packageName", basePackage + ".assertions");
        writeTemplate(template, model, assertionsDir.resolve("Assertions.java"));
    }

    public void generateHttpStatusCode(Path assertionsDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("standalone-http-status-code.ftl");
        Map<String, Object> model = Map.of("packageName", basePackage + ".assertions");
        writeTemplate(template, model, assertionsDir.resolve("HttpStatusCode.java"));
    }

    public void generateConfigProperties(Path resourceDir, ApiSpec spec) throws IOException {
        String basePath = spec.getBasePath() != null ? spec.getBasePath() : "http://localhost:8080";
        String content = """
                # API Configuration
                base.url=%s
                api.version=%s

                # Authentication
                auth.type=none
                auth.token=
                auth.username=
                auth.password=

                # Test Configuration
                retry.count=1
                connection.timeout=30000
                read.timeout=30000
                """.formatted(basePath, spec.getVersion() != null ? spec.getVersion() : "1.0");

        Files.writeString(resourceDir.resolve("config.properties"), content);
    }

    private void writeTemplate(Template template, Map<String, Object> model, Path outputFile) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            Files.writeString(outputFile, writer.toString());
        } catch (Exception e) {
            throw new IOException("Failed to process template: " + template.getName(), e);
        }
    }
}

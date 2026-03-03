package com.apiautomation.agent.service;

import com.apiautomation.agent.model.ApiSpec;
import com.apiautomation.agent.model.FieldDefinition;
import com.apiautomation.agent.model.SchemaDefinition;
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
public class PojoGeneratorService {

    @Autowired
    private Configuration freemarkerConfig;

    public int generatePojos(ApiSpec spec, Path outputDir, String basePackage) throws IOException {
        Template template = freemarkerConfig.getTemplate("pojo.ftl");
        int count = 0;

        for (SchemaDefinition schema : spec.getSchemas()) {
            Map<String, Object> model = new HashMap<>();
            model.put("packageName", basePackage + ".models");
            model.put("className", schema.getClassName());
            model.put("description", schema.getDescription() != null ? schema.getDescription() : "");
            model.put("fields", schema.getFields().stream().map(this::fieldToMap).collect(Collectors.toList()));
            model.put("imports", resolveImports(schema));

            try {
                StringWriter writer = new StringWriter();
                template.process(model, writer);
                Path file = outputDir.resolve(schema.getClassName() + ".java");
                Files.writeString(file, writer.toString());
                count++;
            } catch (Exception e) {
                throw new IOException("Failed to generate POJO for " + schema.getName(), e);
            }
        }

        return count;
    }

    private Map<String, Object> fieldToMap(FieldDefinition field) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", field.getName());
        map.put("javaType", field.getJavaType());
        map.put("required", field.isRequired());
        map.put("description", field.getDescription() != null ? field.getDescription() : "");
        map.put("example", field.getExample() != null ? field.getExample() : "");
        map.put("isArray", field.isArray());
        map.put("jsonName", field.getName());
        return map;
    }

    private Set<String> resolveImports(SchemaDefinition schema) {
        Set<String> imports = new TreeSet<>();
        for (FieldDefinition field : schema.getFields()) {
            String javaType = field.getJavaType();
            if (javaType.startsWith("List<")) {
                imports.add("java.util.List");
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
}

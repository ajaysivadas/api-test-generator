package com.apiautomation.agent.controller;

import com.apiautomation.agent.model.*;
import com.apiautomation.agent.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ApiContractController {

    @Autowired
    private OpenApiParserService openApiParser;

    @Autowired
    private PostmanParserService postmanParser;

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private SchemaComparisonService comparisonService;

    @Autowired
    private PackagingService packagingService;

    // Simple in-memory store for generation history
    private final Map<String, Map<String, Object>> generationHistory = new ConcurrentHashMap<>();

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseSpec(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename();

            ApiSpec spec = parseContent(content, filename);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("title", spec.getTitle());
            response.put("version", spec.getVersion());
            response.put("sourceFormat", spec.getSourceFormat());
            response.put("endpointCount", spec.getEndpoints().size());
            response.put("schemaCount", spec.getSchemas().size());
            response.put("endpoints", spec.getEndpoints());
            response.put("schemas", spec.getSchemas());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateFramework(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "basePackage", defaultValue = "com.example.api") String basePackage) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename();

            ApiSpec spec = parseContent(content, filename);
            Map<String, Object> result = generatorService.generate(spec, basePackage);

            // Store in history
            Map<String, Object> historyEntry = new LinkedHashMap<>(result);
            historyEntry.put("timestamp", LocalDateTime.now().toString());
            historyEntry.put("specTitle", spec.getTitle());
            historyEntry.put("sourceFormat", spec.getSourceFormat());
            historyEntry.put("basePackage", basePackage);
            generationHistory.put((String) result.get("generationId"), historyEntry);

            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> mergeIntoFramework(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceName") String serviceName,
            @RequestParam("baseUriKey") String baseUriKey,
            @RequestParam(value = "baseUriEnumName", required = false) String baseUriEnumName) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename();

            ApiSpec spec = parseContent(content, filename);

            MergeConfig config = MergeConfig.builder()
                    .serviceName(serviceName)
                    .baseUriKey(baseUriKey)
                    .baseUriEnumName(baseUriEnumName)
                    .build();

            MergeResult mergeResult = generatorService.generateMerge(spec, config);

            // Store in history
            Map<String, Object> historyEntry = new LinkedHashMap<>();
            historyEntry.put("generationId", mergeResult.getGenerationId());
            historyEntry.put("timestamp", LocalDateTime.now().toString());
            historyEntry.put("specTitle", spec.getTitle());
            historyEntry.put("sourceFormat", spec.getSourceFormat());
            historyEntry.put("mode", "merge");
            historyEntry.put("serviceName", serviceName);
            generationHistory.put(mergeResult.getGenerationId(), historyEntry);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("generationId", mergeResult.getGenerationId());
            response.put("requestPojoCount", mergeResult.getRequestPojoCount());
            response.put("responsePojoCount", mergeResult.getResponsePojoCount());
            response.put("testClassCount", mergeResult.getTestClassCount());
            response.put("executorMethodCount", mergeResult.getExecutorMethodCount());
            response.put("payloadBuilderCount", mergeResult.getPayloadBuilderCount());
            response.put("endpointEntries", mergeResult.getEndpointEntries());
            response.put("baseUriEntry", mergeResult.getBaseUriEntry());
            response.put("generatedFiles", mergeResult.getGeneratedFiles());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareSpecs(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile) {
        try {
            String oldContent = new String(oldFile.getBytes(), StandardCharsets.UTF_8);
            String newContent = new String(newFile.getBytes(), StandardCharsets.UTF_8);

            ApiSpec oldSpec = parseContent(oldContent, oldFile.getOriginalFilename());
            ApiSpec newSpec = parseContent(newContent, newFile.getOriginalFilename());

            ComparisonResult comparison = comparisonService.compare(oldSpec, newSpec);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("oldSpecTitle", comparison.getOldSpecTitle());
            response.put("newSpecTitle", comparison.getNewSpecTitle());
            response.put("totalChanges", comparison.getTotalChanges());
            response.put("breakingChanges", comparison.getBreakingChanges());
            response.put("nonBreakingChanges", comparison.getNonBreakingChanges());
            response.put("changes", comparison.getChanges());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFramework(@PathVariable String id) {
        try {
            Map<String, Object> historyEntry = generationHistory.get(id);
            boolean isMerge = historyEntry != null && "merge".equals(historyEntry.get("mode"));

            byte[] zipBytes;
            String filename;
            if (isMerge) {
                zipBytes = packagingService.packageAsZip(id, "");
                String serviceName = historyEntry.get("serviceName") != null
                        ? (String) historyEntry.get("serviceName") : "merge";
                filename = serviceName.toLowerCase() + "-merge.zip";
            } else {
                zipBytes = packagingService.packageAsZip(id);
                filename = "generated-framework.zip";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(zipBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<Map<String, Object>> history = new ArrayList<>(generationHistory.values());
        history.sort((a, b) -> {
            String tsA = (String) a.get("timestamp");
            String tsB = (String) b.get("timestamp");
            return tsB.compareTo(tsA);
        });
        return ResponseEntity.ok(history);
    }

    private ApiSpec parseContent(String content, String filename) {
        if (postmanParser.canParse(content, filename)) {
            return postmanParser.parse(content, filename);
        }
        if (openApiParser.canParse(content, filename)) {
            return openApiParser.parse(content, filename);
        }
        throw new IllegalArgumentException(
                "Unsupported file format. Please upload an OpenAPI (YAML/JSON) or Postman Collection (JSON) file.");
    }

    private Map<String, Object> errorResponse(Exception e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        return error;
    }
}

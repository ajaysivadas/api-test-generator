package com.apiautomation.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult {
    private String generationId;
    private String outputDir;
    private int requestPojoCount;
    private int responsePojoCount;
    private int testClassCount;
    private int executorMethodCount;
    private int payloadBuilderCount;
    @Builder.Default
    private List<String> endpointEntries = new ArrayList<>();
    private String baseUriEntry;
    @Builder.Default
    private List<String> generatedFiles = new ArrayList<>();
}

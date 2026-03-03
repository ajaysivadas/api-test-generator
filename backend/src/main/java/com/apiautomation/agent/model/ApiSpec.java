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
public class ApiSpec {
    private String title;
    private String version;
    private String description;
    private String basePath;
    @Builder.Default
    private List<Endpoint> endpoints = new ArrayList<>();
    @Builder.Default
    private List<SchemaDefinition> schemas = new ArrayList<>();
    private String sourceFormat;
}

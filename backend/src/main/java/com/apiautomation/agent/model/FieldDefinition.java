package com.apiautomation.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    private String name;
    private String type;
    private String javaType;
    private String format;
    private boolean required;
    private boolean isArray;
    private String refSchema;
    private String description;
    private String example;
}

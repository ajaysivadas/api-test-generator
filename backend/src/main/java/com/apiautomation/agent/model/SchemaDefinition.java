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
public class SchemaDefinition {
    private String name;
    private String description;
    @Builder.Default
    private List<FieldDefinition> fields = new ArrayList<>();

    public String getClassName() {
        if (name == null) return "Unknown";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}

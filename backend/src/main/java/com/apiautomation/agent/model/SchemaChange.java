package com.apiautomation.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaChange {
    private ChangeType changeType;
    private String category;
    private String path;
    private String description;
    private String oldValue;
    private String newValue;
    private String severity;
}

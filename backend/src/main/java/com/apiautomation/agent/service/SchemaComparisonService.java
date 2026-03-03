package com.apiautomation.agent.service;

import com.apiautomation.agent.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaComparisonService {

    public ComparisonResult compare(ApiSpec oldSpec, ApiSpec newSpec) {
        ComparisonResult result = new ComparisonResult();
        result.setOldSpecTitle(oldSpec.getTitle());
        result.setNewSpecTitle(newSpec.getTitle());

        List<SchemaChange> changes = new ArrayList<>();

        // Compare endpoints
        changes.addAll(compareEndpoints(oldSpec.getEndpoints(), newSpec.getEndpoints()));

        // Compare schemas
        changes.addAll(compareSchemas(oldSpec.getSchemas(), newSpec.getSchemas()));

        result.setChanges(changes);
        result.computeSummary();
        return result;
    }

    private List<SchemaChange> compareEndpoints(List<Endpoint> oldEndpoints, List<Endpoint> newEndpoints) {
        List<SchemaChange> changes = new ArrayList<>();

        Map<String, Endpoint> oldMap = oldEndpoints.stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (a, b) -> a));
        Map<String, Endpoint> newMap = newEndpoints.stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (a, b) -> a));

        // Detect removed endpoints
        for (String key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.REMOVED)
                        .category("Endpoint")
                        .path(key)
                        .description("Endpoint removed: " + key)
                        .oldValue(key)
                        .severity("BREAKING")
                        .build());
            }
        }

        // Detect added endpoints
        for (String key : newMap.keySet()) {
            if (!oldMap.containsKey(key)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.ADDED)
                        .category("Endpoint")
                        .path(key)
                        .description("New endpoint added: " + key)
                        .newValue(key)
                        .severity("NON-BREAKING")
                        .build());
            }
        }

        // Detect modified endpoints
        for (Map.Entry<String, Endpoint> entry : oldMap.entrySet()) {
            if (newMap.containsKey(entry.getKey())) {
                Endpoint oldEp = entry.getValue();
                Endpoint newEp = newMap.get(entry.getKey());
                changes.addAll(compareEndpointDetails(oldEp, newEp));
            }
        }

        return changes;
    }

    private List<SchemaChange> compareEndpointDetails(Endpoint oldEp, Endpoint newEp) {
        List<SchemaChange> changes = new ArrayList<>();
        String path = oldEp.getMethod() + " " + oldEp.getPath();

        // Compare path parameters
        changes.addAll(compareParams(path + " > Path Parameters",
                oldEp.getPathParameters(), newEp.getPathParameters()));

        // Compare query parameters
        changes.addAll(compareParams(path + " > Query Parameters",
                oldEp.getQueryParameters(), newEp.getQueryParameters()));

        // Compare request body existence
        if (oldEp.getRequestBody() == null && newEp.getRequestBody() != null) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.ADDED)
                    .category("Request Body")
                    .path(path)
                    .description("Request body added")
                    .newValue(newEp.getRequestBody().getName())
                    .severity("BREAKING")
                    .build());
        } else if (oldEp.getRequestBody() != null && newEp.getRequestBody() == null) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.REMOVED)
                    .category("Request Body")
                    .path(path)
                    .description("Request body removed")
                    .oldValue(oldEp.getRequestBody().getName())
                    .severity("BREAKING")
                    .build());
        }

        return changes;
    }

    private List<SchemaChange> compareParams(String context,
                                              List<FieldDefinition> oldParams,
                                              List<FieldDefinition> newParams) {
        List<SchemaChange> changes = new ArrayList<>();

        Map<String, FieldDefinition> oldMap = oldParams.stream()
                .collect(Collectors.toMap(FieldDefinition::getName, f -> f, (a, b) -> a));
        Map<String, FieldDefinition> newMap = newParams.stream()
                .collect(Collectors.toMap(FieldDefinition::getName, f -> f, (a, b) -> a));

        for (String name : oldMap.keySet()) {
            if (!newMap.containsKey(name)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.REMOVED)
                        .category("Parameter")
                        .path(context + " > " + name)
                        .description("Parameter removed: " + name)
                        .oldValue(name + " (" + oldMap.get(name).getType() + ")")
                        .severity(oldMap.get(name).isRequired() ? "BREAKING" : "NON-BREAKING")
                        .build());
            }
        }

        for (String name : newMap.keySet()) {
            if (!oldMap.containsKey(name)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.ADDED)
                        .category("Parameter")
                        .path(context + " > " + name)
                        .description("New parameter added: " + name)
                        .newValue(name + " (" + newMap.get(name).getType() + ")")
                        .severity(newMap.get(name).isRequired() ? "BREAKING" : "NON-BREAKING")
                        .build());
            }
        }

        // Check type changes
        for (Map.Entry<String, FieldDefinition> entry : oldMap.entrySet()) {
            if (newMap.containsKey(entry.getKey())) {
                FieldDefinition oldF = entry.getValue();
                FieldDefinition newF = newMap.get(entry.getKey());
                if (!Objects.equals(oldF.getType(), newF.getType())) {
                    changes.add(SchemaChange.builder()
                            .changeType(ChangeType.TYPE_CHANGED)
                            .category("Parameter")
                            .path(context + " > " + entry.getKey())
                            .description("Type changed for parameter: " + entry.getKey())
                            .oldValue(oldF.getType())
                            .newValue(newF.getType())
                            .severity("BREAKING")
                            .build());
                }
            }
        }

        return changes;
    }

    private List<SchemaChange> compareSchemas(List<SchemaDefinition> oldSchemas,
                                               List<SchemaDefinition> newSchemas) {
        List<SchemaChange> changes = new ArrayList<>();

        Map<String, SchemaDefinition> oldMap = oldSchemas.stream()
                .collect(Collectors.toMap(SchemaDefinition::getName, s -> s, (a, b) -> a));
        Map<String, SchemaDefinition> newMap = newSchemas.stream()
                .collect(Collectors.toMap(SchemaDefinition::getName, s -> s, (a, b) -> a));

        // Removed schemas
        for (String name : oldMap.keySet()) {
            if (!newMap.containsKey(name)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.REMOVED)
                        .category("Schema")
                        .path(name)
                        .description("Schema removed: " + name)
                        .oldValue(name)
                        .severity("BREAKING")
                        .build());
            }
        }

        // Added schemas
        for (String name : newMap.keySet()) {
            if (!oldMap.containsKey(name)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.ADDED)
                        .category("Schema")
                        .path(name)
                        .description("New schema added: " + name)
                        .newValue(name)
                        .severity("NON-BREAKING")
                        .build());
            }
        }

        // Compare fields of existing schemas
        for (Map.Entry<String, SchemaDefinition> entry : oldMap.entrySet()) {
            if (newMap.containsKey(entry.getKey())) {
                SchemaDefinition oldSchema = entry.getValue();
                SchemaDefinition newSchema = newMap.get(entry.getKey());
                changes.addAll(compareSchemaFields(entry.getKey(), oldSchema, newSchema));
            }
        }

        return changes;
    }

    private List<SchemaChange> compareSchemaFields(String schemaName,
                                                    SchemaDefinition oldSchema,
                                                    SchemaDefinition newSchema) {
        List<SchemaChange> changes = new ArrayList<>();

        Map<String, FieldDefinition> oldFields = oldSchema.getFields().stream()
                .collect(Collectors.toMap(FieldDefinition::getName, f -> f, (a, b) -> a));
        Map<String, FieldDefinition> newFields = newSchema.getFields().stream()
                .collect(Collectors.toMap(FieldDefinition::getName, f -> f, (a, b) -> a));

        // Removed fields
        for (String name : oldFields.keySet()) {
            if (!newFields.containsKey(name)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.REMOVED)
                        .category("Schema Field")
                        .path(schemaName + "." + name)
                        .description("Field removed from " + schemaName + ": " + name)
                        .oldValue(name + " (" + oldFields.get(name).getType() + ")")
                        .severity("BREAKING")
                        .build());
            }
        }

        // Added fields
        for (String name : newFields.keySet()) {
            if (!oldFields.containsKey(name)) {
                FieldDefinition field = newFields.get(name);
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.ADDED)
                        .category("Schema Field")
                        .path(schemaName + "." + name)
                        .description("New field added to " + schemaName + ": " + name)
                        .newValue(name + " (" + field.getType() + ")")
                        .severity(field.isRequired() ? "BREAKING" : "NON-BREAKING")
                        .build());
            }
        }

        // Type changes
        for (Map.Entry<String, FieldDefinition> entry : oldFields.entrySet()) {
            if (newFields.containsKey(entry.getKey())) {
                FieldDefinition oldF = entry.getValue();
                FieldDefinition newF = newFields.get(entry.getKey());

                if (!Objects.equals(oldF.getType(), newF.getType())) {
                    changes.add(SchemaChange.builder()
                            .changeType(ChangeType.TYPE_CHANGED)
                            .category("Schema Field")
                            .path(schemaName + "." + entry.getKey())
                            .description("Type changed in " + schemaName + "." + entry.getKey())
                            .oldValue(oldF.getType())
                            .newValue(newF.getType())
                            .severity("BREAKING")
                            .build());
                }

                if (oldF.isRequired() != newF.isRequired()) {
                    changes.add(SchemaChange.builder()
                            .changeType(ChangeType.MODIFIED)
                            .category("Schema Field")
                            .path(schemaName + "." + entry.getKey())
                            .description("Required status changed in " + schemaName + "." + entry.getKey())
                            .oldValue("required=" + oldF.isRequired())
                            .newValue("required=" + newF.isRequired())
                            .severity(newF.isRequired() ? "BREAKING" : "NON-BREAKING")
                            .build());
                }
            }
        }

        return changes;
    }
}

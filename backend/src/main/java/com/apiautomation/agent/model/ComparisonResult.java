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
public class ComparisonResult {
    private String oldSpecTitle;
    private String newSpecTitle;
    @Builder.Default
    private List<SchemaChange> changes = new ArrayList<>();
    private int totalChanges;
    private int breakingChanges;
    private int nonBreakingChanges;

    public void computeSummary() {
        this.totalChanges = changes.size();
        this.breakingChanges = (int) changes.stream()
                .filter(c -> "BREAKING".equals(c.getSeverity()))
                .count();
        this.nonBreakingChanges = totalChanges - breakingChanges;
    }
}

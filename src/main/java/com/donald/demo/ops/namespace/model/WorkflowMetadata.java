package com.donald.demo.ops.namespace.model;

import lombok.Data;

@Data
public class WorkflowMetadata {
    // For the operations the only part of this entity we care about is the apiKey.
    private String apiKey;
}

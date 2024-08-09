package com.donald.demo.ops.namespace.model;

import lombok.Data;

@Data
public class CloudOperations {
    private CloudOperationsNamespace cloudOpsNamespace;
    private WorkflowMetadata wfMetadata;
}

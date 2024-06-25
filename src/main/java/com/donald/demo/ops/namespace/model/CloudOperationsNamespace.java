package com.donald.demo.ops.namespace.model;

import lombok.Data;

@Data
public class CloudOperationsNamespace {

    private String name;
    private String activeRegion;
    private String state;
}

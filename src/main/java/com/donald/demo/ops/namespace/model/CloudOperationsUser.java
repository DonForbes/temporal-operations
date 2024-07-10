package com.donald.demo.ops.namespace.model;

import java.util.Collection;

import lombok.Data;

@Data
public class CloudOperationsUser {
    private String eMail;
    private String id;
    private Collection<CloudOperationsNamespaceAccess> cloudOpsNamespaceAccess;
    private String role;
}

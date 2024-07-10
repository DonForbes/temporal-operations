package com.donald.demo.ops.namespace.model;

import java.util.Collection;

import lombok.Data;

@Data
public class CloudOperationsNamespace {

    private String name;
    private String activeRegion;
    //private String secondaryRegion;
    private String state;
    private int retentionPeriod;
    private String certAuthorityPublicCert;
    private Collection<CloudOperationsUser> cloudOpsUsers;
}

package com.donald.demo.ops.namespace.model;

import lombok.Data;
import java.util.Date;
import java.util.Collection;

@Data
public class CloudOperationsCertAuthority {
    private String caCert;
    private Date expiryDate;
    private Date notBefore;
    private String subjectPrincipal;
    private Collection<String> alternativeNames;
}

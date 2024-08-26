package com.donald.demo.ops.namespace.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigInteger;

import java.util.Collection;

@Data
public class CloudOperationsCertAuthority {
    private String caCert;
    private LocalDateTime expiryDate;
    private LocalDateTime notBefore;
    private String subjectPrincipal;
    private String issuerPrincipal;
    private Collection<String> alternativeNames;
    private BigInteger serialNumber;
}

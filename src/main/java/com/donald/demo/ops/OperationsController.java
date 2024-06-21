package com.donald.demo.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.client.VaultEndpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.donald.demo.ops.certificates.CertificateUtil;
import com.donald.demo.ops.certificates.VaultPkiProperties;

@RestController
public class OperationsController {

    @Autowired
    VaultPkiProperties pkiProperties;
    @Autowired
    VaultOperations operations;
	private final static Logger logger = LoggerFactory.getLogger(OperationsController.class);

    @GetMapping("create-certificate")
    public  ResponseEntity<String> createCertificate()
    {
        logger.debug("methodEntry: createCertificate");

         VaultCertificateResponse vaultCertificate = CertificateUtil.requestCertificate(operations, pkiProperties);

         logger.debug("Certificate is [" +  vaultCertificate.getData().getCertificate() + "]");
         logger.debug("Private Key is [" + vaultCertificate.getData().getPrivateKey() + "]");
         logger.debug("Issuing CA certificate is[" + vaultCertificate.getData().getIssuingCaCertificate() + "]");


        return new ResponseEntity<>("hello", HttpStatus.OK);
    }  // End createCertificate

    @GetMapping("get-current-ca-certificate")
    public String getCurrentCA() {
        String caCert = CertificateUtil.getCACertificate(operations, null);
        logger.debug("The CA cert is [" + caCert + "]");
        return caCert;
    } // End get current CA
}


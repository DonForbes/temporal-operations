package com.donald.demo.ops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.donald.demo.ops.certificates.CertificateUtil;
import com.donald.demo.ops.certificates.VaultPkiProperties;
import com.donald.demo.ops.namespace.NamespaceMgmt;
import com.donald.demo.ops.namespace.model.CloudOperationDetails;
import com.donald.demo.ops.namespace.model.CloudOperationsNamespace;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;

@RestController
public class OperationsController {

    @Autowired
    VaultPkiProperties pkiProperties;
    @Autowired
    VaultOperations operations;
    NamespaceMgmt namespaceMgmt;
    @Autowired
    private CloudOperationDetails cloudOpsDetails;
	private final static Logger logger = LoggerFactory.getLogger(OperationsController.class);
  //  private final TemporalCloudApiClient client = new TemporalCloudApiClient("saas-api.tmprl.cloud", 443);


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

    @GetMapping("get-namespaces")
    public ResponseEntity<Collection<CloudOperationsNamespace>> getNamespaces(@RequestParam(required=true) String apiKey) {
        cloudOpsDetails.setTmprlApiKey(apiKey);
        logger.debug("The API key that should be used is [" + apiKey + "]");
        logger.debug("The API key being used is [" + cloudOpsDetails.getTmprlApiKey() + "]"); 

        this.namespaceMgmt = new NamespaceMgmt(cloudOpsDetails);
        Collection<CloudOperationsNamespace> cloudOpsNamespaces = new ArrayList<CloudOperationsNamespace>();
        try {
            cloudOpsNamespaces = this.namespaceMgmt.getNamespaces();
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failed to retrieve namespaces [" + e.getMessage() + "]");
            CloudOperationsNamespace aCloudOpsNamespace = new CloudOperationsNamespace();
            aCloudOpsNamespace.setName("-");
            aCloudOpsNamespace.setActiveRegion("-");
            aCloudOpsNamespace.setState("Failed to retrieve namespaces [" + e.getMessage() +"]");
            cloudOpsNamespaces.add(aCloudOpsNamespace);
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(Optional.of(cloudOpsNamespaces));
    } // End getNamespaces
}


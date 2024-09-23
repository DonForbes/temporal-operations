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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.donald.demo.ops.certificates.CertificateUtil;
import com.donald.demo.ops.certificates.VaultPkiProperties;
import com.donald.demo.ops.namespace.OperationsMgmt;
import com.donald.demo.ops.namespace.model.CloudOperationDetails;
import com.donald.demo.ops.namespace.model.CloudOperations;
import com.donald.demo.ops.namespace.model.CloudOperationsApiKey;
import com.donald.demo.ops.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.ops.namespace.model.CloudOperationsNamespace;
import com.donald.demo.ops.namespace.model.CloudOperationsUser;
import com.donald.demo.ops.namespace.model.WorkflowMetadata;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;

@RestController
public class OperationsController {

    @Autowired
    VaultPkiProperties pkiProperties;
    @Autowired
    VaultOperations operations;
    @Autowired
    private CloudOperationDetails cloudOpsDetails;
    private final static Logger logger = LoggerFactory.getLogger(OperationsController.class);
    // private final TemporalCloudApiClient client = new
    // TemporalCloudApiClient("saas-api.tmprl.cloud", 443);

    @GetMapping("create-certificate")
    public ResponseEntity<String> createCertificate() {
        logger.debug("methodEntry: createCertificate");

        VaultCertificateResponse vaultCertificate = CertificateUtil.requestCertificate(operations, pkiProperties);

        logger.debug("Certificate is [" + vaultCertificate.getData().getCertificate() + "]");
        logger.debug("Private Key is [" + vaultCertificate.getData().getPrivateKey() + "]");
        logger.debug("Issuing CA certificate is[" + vaultCertificate.getData().getIssuingCaCertificate() + "]");

        return new ResponseEntity<>("hello", HttpStatus.OK);
    } // End createCertificate

    @GetMapping("get-current-ca-certificate")
    public CloudOperationsCertAuthority getCurrentCA() {
        CloudOperationsCertAuthority caCert = CertificateUtil.getCACertificate(operations, null);
        logger.debug("The CA cert is [{}]", caCert.toString());
        return caCert;
    } // End get current CA

    @GetMapping("/namespaces")
    public ResponseEntity<Collection<CloudOperationsNamespace>> getNamespaces(
            @RequestParam(required = true) String apiKey) {
        cloudOpsDetails.setTmprlApiKey(apiKey);
        logger.debug("The API key that should be used is [" + apiKey + "]");
        logger.debug("The API key being used is [" + cloudOpsDetails.getTmprlApiKey() + "]");

        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);
        Collection<CloudOperationsNamespace> cloudOpsNamespaces = new ArrayList<CloudOperationsNamespace>();
        try {
            cloudOpsNamespaces = opsMgmt.getNamespaces();
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failed to retrieve namespaces [" + e.getMessage() + "]");
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        }
        return ResponseEntity.of(Optional.of(cloudOpsNamespaces));
    } // End namespaces

    @PostMapping("/namespace")
    @ResponseBody
    public ResponseEntity createNamespace(@RequestBody CloudOperations cloudOps){
        cloudOpsDetails.setTmprlApiKey(cloudOps.getWfMetadata().getApiKey());
        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);
        logger.debug("The cloud operations object we are looking to create from [{}]", cloudOps.toString());
        try {
            logger.info("Created namespace [{}] - result [{}]", 
                                        cloudOps.getCloudOpsNamespace().getName(), 
                                        opsMgmt.createNamespace(cloudOps.getCloudOpsNamespace()));  // CREATE!
        }
        catch (InvalidProtocolBufferException e){
            logger.error("ERROR - Failed to create namespace - [" + e.getMessage() + "]");
            return ResponseEntity.badRequest()
                                 .header("OpsResponse", e.getMessage())
                                 .body(e.getMessage());
        }

        catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failed to create namespace [" + e.getMessage() + "]");
            return ResponseEntity.badRequest()
                                 .header("OpsResponse", e.getMessage())
                                 .body(e.getMessage());
        }

        return ResponseEntity.ok(HttpStatus.OK);

    } // End createNamespace

    @GetMapping("/namespace/{name}")
    public ResponseEntity<CloudOperationsNamespace> getNamespace(@RequestParam(required = true) String apiKey,
            @PathVariable String name) {
        cloudOpsDetails.setTmprlApiKey(apiKey);
        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);
        CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
        cloudOpsNamespace.setName(name);
        try {
            cloudOpsNamespace = opsMgmt.getNamespace(cloudOpsNamespace);
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failure to retrieve namespace [{}]", e.getMessage());
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        } catch (InvalidProtocolBufferException e) {
            logger.error("ERROR - Failed to retrieve namespace [{}]", e.getMessage());
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        }
        return ResponseEntity.of(Optional.of(cloudOpsNamespace));
    } // End getNamespace/name

    @PostMapping("/namespace/{name}")
    @ResponseBody
    public ResponseEntity<String> updateNamespace(@PathVariable String name,
                                                  @RequestBody CloudOperationsNamespace cloudOpsNamespace,
                                                  @RequestHeader("Authorization") String apiKeyBearer)
    {
        logger.debug("methodEntry - updateNamespace(controller)");
         
        String apiKey = apiKeyBearer.replace("Bearer ","");
        logger.debug("Delete namespace [{}] with apiKey [{}]", name, apiKey);

        cloudOpsDetails.setTmprlApiKey(apiKey);
        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);
        String updateResult = "";
        try {
            updateResult = opsMgmt.updateNamespace(cloudOpsNamespace);
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failure to delete namespace [{}]", e.getMessage());
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        } 

        return ResponseEntity.of(Optional.of("Update namespace [" + name + "] using apiKey [" 
                                              + apiKey + "] the result of the operation was [" 
                                              + updateResult + "]"));
    }   // End updateeNamespace

    @DeleteMapping("/namespace/{name}")
    public ResponseEntity<String> deleteNamespace(@PathVariable String name,
                                  @RequestHeader("Authorization") String apiKeyBearer)
    {
        logger.debug("methodEntry - deleteNamespace");
         
        String apiKey = apiKeyBearer.replace("Bearer ","");
        logger.debug("Delete namespace [{}] with apiKey [{}]", name, apiKey);

        cloudOpsDetails.setTmprlApiKey(apiKey);
        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);
        CloudOperationsNamespace cloudOpsNamespace = new CloudOperationsNamespace();
        cloudOpsNamespace.setName(name);
        try {
            opsMgmt.deleteNamespace(cloudOpsNamespace);
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("ERROR - Failure to delete namespace [{}]", e.getMessage());
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        } catch (InvalidProtocolBufferException e) {
            logger.error("ERROR - Failed to delete namespace [{}]", e.getMessage());
            return ResponseEntity.badRequest().header("OpsResponse", e.getMessage()).build();
        }

        return ResponseEntity.of(Optional.of("Deleted namespace [" + name + "] using apiKey [" + apiKey));
    }   // End deleteNamespace

    @GetMapping("/users/{role}")
    public ResponseEntity<Collection<CloudOperationsUser>> getUsersByRole(@RequestParam(required = true) String apiKey,
            @PathVariable String role) {

        Collection<CloudOperationsUser> users = new ArrayList<>();

        logger.info("TODO - Implemenbt get users by role");

        return ResponseEntity.of(Optional.of(users));
    } // End getUsersByRole

    @PostMapping("/apikey/{apiKeyName}")
    @ResponseBody
    public ResponseEntity<CloudOperationsApiKey> createShortLivedApiKey(@PathVariable String apiKeyName,
                                                                        @RequestHeader("Authorization") String apiKeyBearer)
    {
        logger.debug("Method Entry - createShortlivedApiKey");
        String apiKey = apiKeyBearer.replace("Bearer ","");
        logger.debug("create short lived apiKey [{}], using token [{}]", apiKeyName, apiKeyBearer);

        cloudOpsDetails.setTmprlApiKey(apiKey);

        OperationsMgmt opsMgmt = new OperationsMgmt(cloudOpsDetails);

        CloudOperationsApiKey cloudOpsApiKey = opsMgmt.createShortLivedApiKey(apiKeyName);
        return ResponseEntity.of(Optional.of(cloudOpsApiKey));
    }   // End createShortLivedApiKey
}
 
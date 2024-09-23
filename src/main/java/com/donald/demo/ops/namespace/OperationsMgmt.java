package com.donald.demo.ops.namespace;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donald.demo.ops.certificates.CertificateUtil;
import com.donald.demo.ops.namespace.interceptors.HeaderClientInterceptor;
import com.donald.demo.ops.namespace.interceptors.LoggingClientInterceptor;
import com.donald.demo.ops.namespace.model.CloudOperationDetails;
import com.donald.demo.ops.namespace.model.CloudOperationsCertAuthority;
import com.donald.demo.ops.namespace.model.CloudOperationsNamespace;
import com.donald.demo.ops.namespace.model.CloudOperationsNamespaceAccess;
import com.donald.demo.ops.namespace.model.CloudOperationsUser;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;
import io.temporal.api.cloud.cloudservice.v1.CloudServiceGrpc;
import io.temporal.api.cloud.cloudservice.v1.CloudServiceGrpc.CloudServiceBlockingStub;
import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.DeleteNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesResponse;
import io.temporal.api.cloud.cloudservice.v1.GetUsersRequest;
import io.temporal.api.cloud.cloudservice.v1.GetUsersResponse;
import io.temporal.api.cloud.cloudservice.v1.UpdateApiKeyRequest;
import io.temporal.api.cloud.cloudservice.v1.UpdateNamespaceRequest;
import io.temporal.api.cloud.identity.v1.User;
import io.temporal.api.cloud.namespace.v1.CodecServerSpec;
import io.temporal.api.cloud.namespace.v1.MtlsAuthSpec;
import io.temporal.api.cloud.namespace.v1.Namespace;
import io.temporal.api.cloud.namespace.v1.NamespaceSpec;

public class OperationsMgmt {

    private static CloudServiceBlockingStub cloudOpsClient;
    private final static Logger logger = LoggerFactory.getLogger(OperationsMgmt.class);

    public OperationsMgmt(CloudOperationDetails cloudOpsDetails) {
        logger.info("Creating connection to [" + cloudOpsDetails.getHost() + "] on port [" + cloudOpsDetails.getPort()
                + "]");
        logger.debug("Key used is [" + cloudOpsDetails.getTmprlApiKey() + "]");
        Channel channel = ManagedChannelBuilder
                .forAddress(cloudOpsDetails.getHost(), Integer.parseInt(cloudOpsDetails.getPort()))
                .useTransportSecurity().build();

        HeaderClientInterceptor headerInterceptor = new HeaderClientInterceptor(cloudOpsDetails.getTmprlApiKey());

        cloudOpsClient = CloudServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, headerInterceptor, new LoggingClientInterceptor()));

    } // End constructor

    public Collection<CloudOperationsNamespace> getNamespaces() {
        Collection<CloudOperationsNamespace> cloudOpsNamespaces = new ArrayList<CloudOperationsNamespace>();

        GetNamespacesRequest nsRequest = GetNamespacesRequest.newBuilder().build();

        try {
            GetNamespacesResponse nsResp = cloudOpsClient.getNamespaces(nsRequest);
        
            for (Namespace namespace : nsResp.getNamespacesList()) {

                CloudOperationsNamespace cloudOpsNS = new CloudOperationsNamespace();
                cloudOpsNS.setName(namespace.getNamespace());
                cloudOpsNS.setActiveRegion(namespace.getActiveRegion());
                cloudOpsNS.setState(namespace.getState());
                cloudOpsNamespaces.add(cloudOpsNS);
            } // End namespace loop
            
        } catch (Exception e) {
            System.out.println(e);
            throw e;
        }

        return cloudOpsNamespaces;
    }

    public CloudOperationsNamespace getNamespace(CloudOperationsNamespace cloudOpsNamespace)
            throws InvalidProtocolBufferException {
        logger.debug("Method Entry - operationsMgmt: getNamespace");
        GetNamespaceRequest nsRequest = GetNamespaceRequest.newBuilder().setNamespace(cloudOpsNamespace.getName())
                .build();
        logger.debug("Namespace Request [{}]", nsRequest.toString());
        GetNamespaceResponse nsResponse = cloudOpsClient.getNamespace(nsRequest);
        Namespace namespace = nsResponse.getNamespace();
        cloudOpsNamespace.setRetentionPeriod(namespace.getSpec().getRetentionDays());
        cloudOpsNamespace.setCertAuthorityPublicCertificates(namespace.getSpec().getMtlsAuth().getAcceptedClientCa());
        cloudOpsNamespace.setCertAuthorityPublicCerts(CertificateUtil.getCertsFromString(cloudOpsNamespace.getCertAuthorityPublicCertificates()));
        cloudOpsNamespace.setActiveRegion(namespace.getActiveRegion());
        cloudOpsNamespace.setState(namespace.getState());
        cloudOpsNamespace.setCloudOpsUsers(this.getUsersByNamespace(cloudOpsNamespace.getName()));
        cloudOpsNamespace.setCodecEndPoint(namespace.getSpec().getCodecServer().getEndpoint());

        return cloudOpsNamespace;
    } // End getNamepace



    public String createNamespace(CloudOperationsNamespace cloudOpsNamespace) throws InvalidProtocolBufferException {
        logger.debug("Method Entry - createNamespace");
        logger.debug("The namespace we are looking to create has these values [{}]", cloudOpsNamespace.toString());


        StringBuffer certs = new StringBuffer();
        for (CloudOperationsCertAuthority certAuth :  cloudOpsNamespace.getCertAuthorityPublicCerts()) 
        {
            certs.append(certAuth.getCaCert());
            certs.append(System.lineSeparator());
        }
        
        MtlsAuthSpec mtlsSpec = MtlsAuthSpec.newBuilder()
                                            .setAcceptedClientCa(Base64.getEncoder().encodeToString(certs.toString().getBytes()))
                                            .setEnabled(true)
                                            .build();
        CodecServerSpec codecServerSpec = CodecServerSpec.newBuilder().setEndpoint(cloudOpsNamespace.getCodecEndPoint()).build();

        NamespaceSpec nsSpec = NamespaceSpec.newBuilder()
                                            .setName(cloudOpsNamespace.getName())
                                            .setMtlsAuth(mtlsSpec)
                                            .setRetentionDays(cloudOpsNamespace.getRetentionPeriod())
                                            .addRegions(cloudOpsNamespace.getActiveRegion())
                                            .setCodecServer(codecServerSpec)
                                            .build();
        CreateNamespaceRequest nsRequest = CreateNamespaceRequest.newBuilder().setSpec(nsSpec).build();
        cloudOpsClient.createNamespace(nsRequest);

        return "Success";
    }  // End createNamespace

    //
    // Update the namespace, spcifically for the CA certs, codec endpoint, retention period.
    //
    public String updateNamespace(CloudOperationsNamespace cloudOpsNamespace)
    {
        logger.debug("Method Entry - updateNamespace");
        String updateResult = "Success";

        GetNamespaceRequest nsRequest = GetNamespaceRequest.newBuilder().setNamespace(cloudOpsNamespace.getName())
                .build();
        logger.debug("Namespace Request [{}]", nsRequest.toString());
        GetNamespaceResponse nsResponse = cloudOpsClient.getNamespace(nsRequest);
        

        CodecServerSpec codecSpec = CodecServerSpec.newBuilder().setEndpoint(cloudOpsNamespace.getCodecEndPoint()).build();
        StringBuffer certs = new StringBuffer();
        for (CloudOperationsCertAuthority certAuth :  cloudOpsNamespace.getCertAuthorityPublicCerts()) 
        {
            certs.append(certAuth.getCaCert());
            certs.append(System.lineSeparator());
        }
        MtlsAuthSpec mtlsSpec = MtlsAuthSpec.newBuilder()
                                            .setAcceptedClientCa(Base64.getEncoder().encodeToString(certs.toString().getBytes()))
                                            .setEnabled(true)
                                            .build();

        NamespaceSpec nsSpec = NamespaceSpec.newBuilder() 
                                            .setCodecServer(codecSpec)
                                            .setMtlsAuth(mtlsSpec)
                                            .setRetentionDays(cloudOpsNamespace.getRetentionPeriod())
                                            .setName(cloudOpsNamespace.getName())
                                            .build();
        
                                            

        UpdateNamespaceRequest nsUpdateReq = UpdateNamespaceRequest.newBuilder()
                                                                   .setNamespace(cloudOpsNamespace.getName())
                                                                   .setResourceVersion(nsResponse.getNamespace().getResourceVersion())
                                                                   .setSpec(nsSpec)
                                                                   .build();
        
        try {
            cloudOpsClient.updateNamespace(nsUpdateReq);
        }
        catch (Exception ex)
        {
            updateResult = "Failed";
            logger.error("Something failed attempting to update the namespace", ex);
            logger.error("Message from Exception [{}]", ex.getMessage());
        }
               
        return updateResult;
    }   // End updateNamespac
    

    public Collection<CloudOperationsUser> getUsers() {
        GetUsersRequest usersRequest = GetUsersRequest.newBuilder().build();

        GetUsersResponse usersResponse = cloudOpsClient.getUsers(usersRequest);

        Collection<CloudOperationsUser> cloudOpsUsers = new ArrayList<CloudOperationsUser>();
        for (User user : usersResponse.getUsersList()) {
            CloudOperationsUser aUser = new CloudOperationsUser();
            aUser.setId(user.getId());
            aUser.setEMail(user.getSpec().getEmail());

            Collection<CloudOperationsNamespaceAccess> cloudOpsNSAccess = new ArrayList<CloudOperationsNamespaceAccess>();
            for (String nsAccessKey : user.getSpec().getAccess().getNamespaceAccessesMap().keySet()) {
                CloudOperationsNamespaceAccess cloudOpsNamespaceAccess = new CloudOperationsNamespaceAccess();
                cloudOpsNamespaceAccess.setNamespace(nsAccessKey);
                cloudOpsNamespaceAccess.setPermission(
                        user.getSpec().getAccess().getNamespaceAccessesMap().get(nsAccessKey).getPermission());
                cloudOpsNSAccess.add(cloudOpsNamespaceAccess);
            }
            aUser.setCloudOpsNamespaceAccess(cloudOpsNSAccess);
            aUser.setRole(user.getSpec().getAccess().getAccountAccess().getRole());

            cloudOpsUsers.add(aUser);
        }
        return cloudOpsUsers;
    } // End getUsers


    public Collection<CloudOperationsUser> getUsersByNamespace(String namespace) {
        Collection<CloudOperationsUser> cloudOpsUsers = this.getUsers();
        logger.debug("Total users for account: {}", cloudOpsUsers.size());

        Iterator<CloudOperationsUser> cloudOpsUserIterator = cloudOpsUsers.iterator();
        while (cloudOpsUserIterator.hasNext()) {
            boolean userCanAccessNS = false;
            CloudOperationsUser cloudOpsUser = cloudOpsUserIterator.next();
            
            if (cloudOpsUser.getRole().equalsIgnoreCase("admin"))
                // If the user is an admin then they can access the namespace.
                // Otherwise check each NS they can access
                logger.debug("User [{}] has role [{}]", cloudOpsUser.getEMail(), cloudOpsUser.getRole());
            else {
                for (CloudOperationsNamespaceAccess cloudOpsNSAccess : cloudOpsUser.getCloudOpsNamespaceAccess()) {
                    if (cloudOpsNSAccess.getNamespace() == namespace) {
                        userCanAccessNS = true;
                        break;
                    }
                }
                if (!userCanAccessNS) {
                    logger.debug("Removing user [{}] as no access to namespace [{}]", cloudOpsUser.getEMail(),
                            namespace);
                    cloudOpsUserIterator.remove();
                }
            }
        }

        return cloudOpsUsers;

    }// End getUsersByNamespace

    public Collection<CloudOperationsUser> getUsersByRole(String role) {
        Collection<CloudOperationsUser> usersByRole = new ArrayList<>();
        // TODO - Implement if needed.

        return usersByRole;
    }// End getUsersByRole

    public void deleteNamespace(CloudOperationsNamespace cloudOpsNamespace) throws InvalidProtocolBufferException
    {
        logger.debug("Deleting namespace called [{}]", cloudOpsNamespace.getName());


        //  Using the name we find the namespace and get the latest resource version to enable the delete to work.
        GetNamespaceRequest nsRequest = GetNamespaceRequest.newBuilder()
                                                            .setNamespace(cloudOpsNamespace.getName())
                                                            .build();
        GetNamespaceResponse nsResponse = cloudOpsClient.getNamespace(nsRequest);


        DeleteNamespaceRequest deleteNS = DeleteNamespaceRequest.newBuilder()
                                                                .setNamespace(cloudOpsNamespace.getName()) 
                                                                .setResourceVersion(nsResponse.getNamespace().getResourceVersion())
                                                                .build();
        cloudOpsClient.deleteNamespace(deleteNS);

    }
}

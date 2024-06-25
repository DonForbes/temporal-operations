package com.donald.demo.ops.namespace;



import com.donald.demo.ops.namespace.interceptors.HeaderClientInterceptor;
import com.donald.demo.ops.namespace.interceptors.LoggingClientInterceptor;
import com.donald.demo.ops.namespace.model.CloudOperationDetails;
import com.donald.demo.ops.namespace.model.CloudOperationsNamespace;


import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.temporal.api.cloud.namespace.v1.Namespace;
import io.temporal.api.cloud.cloudservice.v1.CloudServiceGrpc;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesResponse;
import io.temporal.api.cloud.cloudservice.v1.CloudServiceGrpc.CloudServiceBlockingStub;

public class NamespaceMgmt {
  
    private static CloudServiceBlockingStub cloudOpsClient;
	private final static Logger logger = LoggerFactory.getLogger(NamespaceMgmt.class);

    public  NamespaceMgmt(CloudOperationDetails cloudOpsDetails) {
        logger.info("Creating connection to [" + cloudOpsDetails.getHost() + "] on port [" + cloudOpsDetails.getPort() + "]");
        logger.debug("Key used is [" + cloudOpsDetails.getTmprlApiKey() + "]");
        Channel channel = ManagedChannelBuilder.forAddress(cloudOpsDetails.getHost(), Integer.parseInt(cloudOpsDetails.getPort()))
                            .useTransportSecurity().build();

        HeaderClientInterceptor headerInterceptor = new HeaderClientInterceptor(cloudOpsDetails.getTmprlApiKey());
        
        cloudOpsClient = CloudServiceGrpc.newBlockingStub(ClientInterceptors.intercept(channel, headerInterceptor, new LoggingClientInterceptor() ));
    

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
        }
        catch (Exception e)
        {  
            System.out.println(e);
            throw e;
        }
        
        return cloudOpsNamespaces;
    }
}

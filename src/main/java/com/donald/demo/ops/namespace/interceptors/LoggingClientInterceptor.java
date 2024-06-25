package com.donald.demo.ops.namespace.interceptors;

import java.util.logging.Logger;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;





public class LoggingClientInterceptor  implements ClientInterceptor{
    private static final Logger logger = Logger.getLogger(LoggingClientInterceptor.class.getName());


    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        // Log the call details here if required
        // Example: logger.info("Intercepted call to " + method.getFullMethodName());
        logger.info("Intercepted call - " +  method.getFullMethodName());

        return next.newCall(method, callOptions);
    }
}

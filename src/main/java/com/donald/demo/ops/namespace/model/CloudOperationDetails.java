package com.donald.demo.ops.namespace.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("cloud-operations")
public class CloudOperationDetails {
    private String host;
    private String port;
    private String tmprlApiKey;

    // Set using the environment variable first and if that is not set then use the value from the config file.
    public void setTmprlApiKey(String apiKey) {
        String apiKeyEnv = System.getenv("TEMPORAL_CLOUD_API_KEY");
        if (apiKeyEnv == null) {
            tmprlApiKey = apiKey;
        }
        else
            tmprlApiKey = apiKeyEnv;
    } // End setter 
}

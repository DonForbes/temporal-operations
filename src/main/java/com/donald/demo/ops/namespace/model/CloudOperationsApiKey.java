package com.donald.demo.ops.namespace.model;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class CloudOperationsApiKey {
    LocalDateTime expiryTime;
    String displayName;
    String description;
    String state;
    String apikeyToken;
    String apiKeyId;

}

# Temporal Cloud Operations (Java) demo - Server side


This service includes the creation of certificates that are used to enable mTLS between the workers and the Temporal Service.  These certificates are generated using BouncyCastle (java libraries) and stored in vault to make it accessible to different services.

## Vault setup
For development purposes we are looking to run vault in docker that can be started up locally on a laptop or on a shared dev server.

To set this up there is a vault directory with a docker-compose file to hold the configuration necessary.

### Startup
```
$ cd vault
$ docker-compose up -d
```

Then using a browser connect to localhost:8200 and pick one root token and one unseal key.  On the next screen download or copy these to a safe location.  Enter the unseal key and then use the root token to authenticate and you will have access to the webUI for vault.  The docker instance can then be stopped and restarted and will have maintained the state.



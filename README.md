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




# For demo
Log into the vault UI and navigate to the pki secrets engine.
There may be several issuers (rootCAs) setup but only one will be the default.  Check also that the role (temporal-operations-role) has been setup to use the default issurer.

If all is good then navigate to the default issuer and click on "rotate this root" which will provide an option to copy the values from the previous root in, select this and provide a new issuer name.  For example temporal-operation-issuer-vX.

Once complete it is necessary to change the default issuer, from what I can tell this is only possible using teh command line:-

```
$ vault write pki/root/replace default=temporal-operations-issue-vX
```

At this point we can run the workflow to "add new rootCA" to the namespace.  This will accumulate the CAs in the namespace but not remove the old one so that existing clients can continue to operate successfully.  In the background we can have another workflow running to check the namespaces and delete any CA that has expired or if we want override to force remove a givenn CA.


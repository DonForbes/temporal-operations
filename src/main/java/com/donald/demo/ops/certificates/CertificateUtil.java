package com.donald.demo.ops.certificates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.core.RestOperationsCallback;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;
import org.springframework.vault.support.VaultIssuerCertificateRequestResponse;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CertificateUtil {
	private final static Logger logger = LoggerFactory.getLogger(CertificateUtil.class);

	public static VaultCertificateResponse requestCertificate(
			VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {

		logger.info("Requesting SSL certificate from Vault for: {}",
				pkiProperties.getCommonName());

		VaultCertificateRequest certificateRequest = VaultCertificateRequest
				.builder()
				.commonName(pkiProperties.getCommonName())
				.altNames(
						pkiProperties.getAltNames() != null ? pkiProperties.getAltNames() : Collections.<String> emptyList())
				.build();

		logger.info("Created a certuficate request.");
		
		
		VaultCertificateResponse certificateResponse = vaultOperations.opsForPki(
				pkiProperties.getBackend()).issueCertificate(pkiProperties.getRole(),
				certificateRequest);
		logger.info(("created a certificate response"));

		return certificateResponse;
	}

	public static Collection<String> listissuers(VaultOperations vaultOperations, VaultPkiProperties pkiProperties)
	{
		Collection<String> issuers = new ArrayList<String>();

		//RestOperationsCallback callback = RestOperationsCallback.

        //vaultOperations.doWithSession(null)

		return issuers;
	} // End listIssuers

	public static String getCACertificate(VaultOperations vaultOperations, String issuer) {
//		If the issuer is not set then we can set it to the String "default" which will refer to the currently configured issuer.  (Assuming this is the default on in vault.)
		if ((issuer == null)  || (issuer.isEmpty()))
			issuer = "default";
		return vaultOperations.opsForPki().getIssuerCertificate(issuer).getData().getCertificate();

	} // End getCACertificate


	public static VaultCertificateResponse getCertificate(VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {
//		vaultOperations.list(null)
		return null;
	}// End getCertificate
}

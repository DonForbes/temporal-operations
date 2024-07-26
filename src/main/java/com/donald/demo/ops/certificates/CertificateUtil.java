package com.donald.demo.ops.certificates;

import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.util.Arrays.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.core.RestOperationsCallback;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Certificate;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;
import org.springframework.vault.support.VaultIssuerCertificateRequestResponse;

import com.donald.demo.ops.namespace.model.CloudOperationsCertAuthority;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CertificateUtil {
	private final static Logger logger = LoggerFactory.getLogger(CertificateUtil.class);

	public static VaultCertificateResponse requestCertificate(VaultOperations vaultOperations,
			VaultPkiProperties pkiProperties) {

		logger.info("Requesting SSL certificate from Vault for: {}", pkiProperties.getCommonName());

		VaultCertificateRequest certificateRequest = VaultCertificateRequest.builder()
				.commonName(pkiProperties.getCommonName())
				.altNames(pkiProperties.getAltNames() != null ? pkiProperties.getAltNames()
						: Collections.<String>emptyList())
				.build();

		logger.info("Created a certuficate request.");

		VaultCertificateResponse certificateResponse = vaultOperations.opsForPki(pkiProperties.getBackend())
				.issueCertificate(pkiProperties.getRole(), certificateRequest);
		logger.info(("created a certificate response"));

		return certificateResponse;
	}

	public static Collection<String> listissuers(VaultOperations vaultOperations, VaultPkiProperties pkiProperties) {
		Collection<String> issuers = new ArrayList<String>();

		// RestOperationsCallback callback = RestOperationsCallback.

		// vaultOperations.doWithSession(null)

		return issuers;
	} // End listIssuers

	public static CloudOperationsCertAuthority getCACertificate(VaultOperations vaultOperations, String issuer) {
		// If the issuer is not set then we can set it to the String "default" which
		// will refer to the currently configured issuer. (Assuming this is the default
		// on in vault.)
		if ((issuer == null) || (issuer.isEmpty()))
			issuer = "default";

		Certificate theCACert = vaultOperations.opsForPki().getIssuerCertificate(issuer).getData();

		CloudOperationsCertAuthority cloudOpsCertAuthority = new CloudOperationsCertAuthority();
		cloudOpsCertAuthority.setCaCert(theCACert.getCertificate());
		cloudOpsCertAuthority.setNotBefore(theCACert.getX509Certificate().getNotBefore());
		cloudOpsCertAuthority.setExpiryDate(theCACert.getX509Certificate().getNotAfter());
		cloudOpsCertAuthority.setSubjectPrincipal(theCACert.getX509Certificate().getSubjectX500Principal().toString());
		try {
			cloudOpsCertAuthority.setAlternativeNames(
					CertificateUtil.getAltNames(theCACert.getX509Certificate().getSubjectAlternativeNames()));
		} catch (CertificateParsingException e) {
			// If we fail to parse the alt names just carry on without worrying about them
			// just now....
			e.printStackTrace();
		}

		return cloudOpsCertAuthority;

	} // End getCACertificate

	private static Collection<String> getAltNames(Collection<List<?>> altNames) {
		final String method = "[getOtherNameValue] - ";
		Collection<String> alternativeNames = new ArrayList<>();
		if (altNames == null)
			logger.debug("No alternative names held in certificate.");
		else {
			for (List item : altNames) {
				Integer type = (Integer) item.get(0);
				alternativeNames.add((String) item.get(1).toString());
			}
		}

		return alternativeNames;
	}

	public static VaultCertificateResponse getCertificate(VaultOperations vaultOperations,
			VaultPkiProperties pkiProperties) {
		// vaultOperations.list(null)
		return null;
	}// End getCertificate
}

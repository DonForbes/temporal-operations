package com.donald.demo.ops.certificates;

import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Base64;
import java.time.ZoneId;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.io.IOException;
import java.io.ByteArrayInputStream;

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
	private static final String endCertString = "-----END CERTIFICATE-----";
	private static final String startCertString = "-----BEGIN CERTIFICATE-----";

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
	}  // End requestCertificate


	public static CloudOperationsCertAuthority getCertAuthority(String pemEncodedCert) throws CertificateException, IOException {
		CloudOperationsCertAuthority cloudOpsCertAuth = new CloudOperationsCertAuthority();
		if (pemEncodedCert == null)
			{
				logger.debug("No certificate passed in, returning an empty CertAuth object");
				return cloudOpsCertAuth;
			}
		 X509Certificate cert;
		 byte[] certificateData = Base64.getDecoder().decode(pemEncodedCert.replaceAll("\\s", ""));
		 CertificateFactory certFactory = CertificateFactory.getInstance("X509");
		 cert = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(certificateData));

		 // Set all the values from the cert.
		 cloudOpsCertAuth.setExpiryDate(cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		 cloudOpsCertAuth.setNotBefore(cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		 cloudOpsCertAuth.setAlternativeNames(CertificateUtil.getAltNames(cert.getIssuerAlternativeNames()));
		 cloudOpsCertAuth.setCaCert(startCertString + System.lineSeparator() + pemEncodedCert + System.lineSeparator() + endCertString);
		 cloudOpsCertAuth.setSubjectPrincipal(cert.getSubjectX500Principal().getName());
		 cloudOpsCertAuth.setIssuerPrincipal(cert.getIssuerX500Principal().getName());
		 cloudOpsCertAuth.setSerialNumber(cert.getSerialNumber());


		return cloudOpsCertAuth;
	}
	public static Collection<CloudOperationsCertAuthority> getCertsFromString(String CACertList) {
        Collection<CloudOperationsCertAuthority> cloudOpsCertAuths = new ArrayList<>();
        // Split the string up into an array that starts with "-----BEGIN CERTIFICATE-----" and ends "-----END CERTIFICATE-----"

		String decodedList = new String(Base64.getDecoder().decode(CACertList));
        String[] certs = decodedList.split(endCertString);
        for (String aCert : certs) {
            // For x509 cert manipulation this is done without the begin and end strings.
            String pemCert = aCert.replace(startCertString, "");
			logger.debug("Checking certificate with content of [{}]", pemCert);
            try {
                cloudOpsCertAuths.add(CertificateUtil.getCertAuthority(pemCert));
            }
            catch (IOException ioEx) {
                logger.debug("Failed to parse the certificate successfully (IOException) [{}]", ioEx.getMessage());

            }
            catch (CertificateException certEx) {
                logger.debug("Failed to parse the certificate succussfully (CertificateException) [{}]", certEx.getMessage());
            }
        }  // End loop round the certs
        return cloudOpsCertAuths;

    } // End getCertsFromString


   // private String decodeCACert(String base64EncodedCACert)
   // {
   //     String CACertList = new String(Base64.getDecoder().decode(base64EncodedCACert));
   //
   //     return CACertList.replace("-----END CERTIFICATE-----", "-----END CERTIFICATE-----" + System.lineSeparator());
   //
   //  }  //end decodeCACert


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
		logger.debug("methodEntry: getCACertificate - getting current CA for issuer []" + issuer);

		Certificate theCACert = vaultOperations.opsForPki().getIssuerCertificate(issuer).getData();

		CloudOperationsCertAuthority cloudOpsCertAuthority = new CloudOperationsCertAuthority();
		cloudOpsCertAuthority.setCaCert(theCACert.getCertificate());
		cloudOpsCertAuthority.setNotBefore(theCACert.getX509Certificate().getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		cloudOpsCertAuthority.setExpiryDate(theCACert.getX509Certificate().getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		cloudOpsCertAuthority.setSerialNumber(theCACert.getX509Certificate().getSerialNumber());
		cloudOpsCertAuthority.setSubjectPrincipal(theCACert.getX509Certificate().getSubjectX500Principal().getName());
		cloudOpsCertAuthority.setIssuerPrincipal(theCACert.getX509Certificate().getSubjectX500Principal().getName()); // This is the CA cert by definition
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

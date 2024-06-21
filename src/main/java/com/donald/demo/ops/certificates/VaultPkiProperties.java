package com.donald.demo.ops.certificates;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@ConfigurationProperties("pki")
@Data
@Validated
@Configuration
public class VaultPkiProperties implements VaultSecretBackendDescriptor{

	/**
	 * Enable pki backend usage.
	 */
	private boolean enabled = true;

	/**
	 * Role name for credentials.
	 */
	private String role;

	/**
	 * pki backend path.
	 */
	private String backend = "pki";

	/**
	 * The CN of the certificate. Should match the host name.
	 */
	private String commonName;

	/**
	 * Alternate CN names for additional host names.
	 */
	private List<String> altNames;

	/**
	 * Prevent certificate re-creation by storing the Valid certificate inside Vault.
	 */
	private boolean reuseValidCertificate = true;

	/**
	 * Startup/Locking timeout. Used to synchronize startup and to prevent multiple SSL
	 * certificate requests.
	 */
	private int startupLockTimeout = 10000;

}

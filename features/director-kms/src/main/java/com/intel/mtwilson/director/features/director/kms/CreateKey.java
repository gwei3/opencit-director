package com.intel.mtwilson.director.features.director.kms;

import static com.intel.mtwilson.configuration.ConfigurationFactory.getConfiguration;

import java.io.File;
import java.security.PublicKey;
import java.util.Properties;

import javax.crypto.SecretKey;

import com.intel.dcsg.cpg.crypto.RsaCredentialX509;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.crypto.file.RsaPublicKeyProtectedPemKeyEnvelopeOpener;
import com.intel.dcsg.cpg.crypto.key.password.Password;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.io.pem.Pem;
import com.intel.director.common.exception.ConfigurationNotFoundException;
import com.intel.kms.api.CreateKeyRequest;
import com.intel.kms.client.jaxrs2.Keys;
import com.intel.kms.ws.v2.api.Key;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.PasswordVaultFactory;
import com.intel.mtwilson.util.crypto.keystore.PasswordKeyStore;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * 
 * @author Boski
 */
public class CreateKey {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(CreateKey.class);
	private static final String DIRECTOR_ENVELOPE_ALIAS = "director.envelope.alias";
	private static final String DIRECTOR_KEYSTORE = "director.keystore";
	private static final String DIRECTOR_KEYSTORE_PASSWORD = "director.keystore.password";
	private static final String KMS_ENDPOINT_URL = "kms.endpoint.url";
	private static final String KMS_TLS_POLICY_CERTIFICATE_SHA1 = "kms.tls.policy.certificate.sha1";
	private static final String KMS_LOGIN_BASIC_USERNAME = "kms.login.basic.username";
	private static final String KMS_LOGIN_BASIC_PASSWORD = "kms.login.basic.password";

	private String mhKeyName;

	public CreateKey() throws Exception {
	}

	public KeyContainer getKey() throws Exception {
		Password keystorePassword = null;
		PublicKey directorEnvelopePublicKey;
		String kmsEndpointUrl;
		String kmsTlsPolicyCertificateSha1;
		String kmsLoginBasicUsername;
		String kmsLoginBasicPassword;
		Keys keys;

		// Get director envelope key
		String directorEnvelopeAlias = getConfiguration().get(
				DIRECTOR_ENVELOPE_ALIAS, "director-envelope");
		if (directorEnvelopeAlias == null || directorEnvelopeAlias.isEmpty()) {
			throw new ConfigurationNotFoundException(
					"Trust Director Envelope alias not configured");
		}

		String keystorePath = getConfiguration().get(DIRECTOR_KEYSTORE,
				Folders.configuration() + File.separator + "keystore.jks");
		File keystoreFile = new File(keystorePath);
		if (!keystoreFile.exists()) {
			throw new ConfigurationNotFoundException(
					"Director Keystore file does not exist");
		}

		try (PasswordKeyStore passwordVault = PasswordVaultFactory
				.getPasswordKeyStore(getConfiguration())) {
			if (passwordVault.contains(DIRECTOR_KEYSTORE_PASSWORD)) {
				keystorePassword = passwordVault
						.get(DIRECTOR_KEYSTORE_PASSWORD);
			}
		}
		if (keystorePassword == null
				|| keystorePassword.toCharArray().length == 0) {
			throw new ConfigurationNotFoundException(
					"Director Keystore password is not configured");
		}

		SimpleKeystore keystore = new SimpleKeystore(new FileResource(
				keystoreFile), keystorePassword);
		RsaCredentialX509 wrappingKeyCertificate;
		wrappingKeyCertificate = keystore.getRsaCredentialX509(
				directorEnvelopeAlias, keystorePassword);
		log.debug("Found key {}", wrappingKeyCertificate.getCertificate()
				.getSubjectX500Principal().getName());

		directorEnvelopePublicKey = wrappingKeyCertificate.getPublicKey();
		if (directorEnvelopePublicKey == null) {
			log.error("Trust Director envelope public key is not configured");
		}

		// Collect KMS configurations
		kmsEndpointUrl = getConfiguration().get(KMS_ENDPOINT_URL, null);
		if (kmsEndpointUrl == null || kmsEndpointUrl.isEmpty()) {
			throw new ConfigurationNotFoundException(
					"KMS endpoint URL not configured");
		}

		kmsTlsPolicyCertificateSha1 = getConfiguration().get(
				KMS_TLS_POLICY_CERTIFICATE_SHA1, null);
		if (kmsTlsPolicyCertificateSha1 == null
				|| kmsTlsPolicyCertificateSha1.isEmpty()) {
			throw new ConfigurationNotFoundException(
					"KMS TLS policy certificate digest not configured");
		}

		kmsLoginBasicUsername = getConfiguration().get(
				KMS_LOGIN_BASIC_USERNAME, null);
		if (kmsLoginBasicUsername == null || kmsLoginBasicUsername.isEmpty()) {
			throw new ConfigurationNotFoundException(
					"KMS API username not configured");
		}

		try (PasswordKeyStore passwordVault = PasswordVaultFactory
				.getPasswordKeyStore(getConfiguration())) {
			if (passwordVault.contains(KMS_LOGIN_BASIC_PASSWORD)) {
				kmsLoginBasicPassword = new String(passwordVault.get(
						KMS_LOGIN_BASIC_PASSWORD).toCharArray());
			} else
				kmsLoginBasicPassword = null;
		}
		// kmsLoginBasicPassword =
		// getConfiguration().get(KMS_LOGIN_BASIC_PASSWORD,
		// kmsLoginBasicPassword);
		if (kmsLoginBasicPassword == null || kmsLoginBasicPassword.isEmpty()) {
			throw new ConfigurationNotFoundException(
					"KMS API password not configured");
		}
		// create KMS Keys API client
		Properties properties = new Properties();
		properties.setProperty("endpoint.url", kmsEndpointUrl);
		properties.setProperty("tls.policy.certificate.sha1",
				kmsTlsPolicyCertificateSha1);
		properties.setProperty("login.basic.username", kmsLoginBasicUsername);
		properties.setProperty("login.basic.password", kmsLoginBasicPassword);
		keys = new Keys(properties);

		// Request server to create a new key
		CreateKeyRequest createKeyRequest = new CreateKeyRequest();
		createKeyRequest.setAlgorithm("AES");
		createKeyRequest.setKeyLength(128);
		createKeyRequest.setMode("OFB");
		Key createKeyResponse = keys.createKey(createKeyRequest);
		// Request server to transfer the new key to us (encrypted)
		String transferKeyPemResponse = keys.transferKey(createKeyResponse
				.getId().toString());
		// decrypt the requested key
		RsaPublicKeyProtectedPemKeyEnvelopeOpener opener = new RsaPublicKeyProtectedPemKeyEnvelopeOpener(
				wrappingKeyCertificate.getPrivateKey(), kmsLoginBasicUsername);
		SecretKey secretKey = (SecretKey) opener.unseal(Pem
				.valueOf(transferKeyPemResponse));
		// package all these into a single container
		KeyContainer keyContainer = new KeyContainer();
		keyContainer.secretKey = secretKey;
		keyContainer.url = createKeyResponse.getTransferLink();
		keyContainer.attributes = createKeyResponse;
		return keyContainer;
	}
}
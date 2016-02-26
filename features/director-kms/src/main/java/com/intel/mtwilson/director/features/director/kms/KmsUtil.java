package com.intel.mtwilson.director.features.director.kms;

import static com.intel.mtwilson.configuration.ConfigurationFactory.getConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.dcsg.cpg.crypto.RsaCredentialX509;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.crypto.file.RsaPublicKeyProtectedPemKeyEnvelopeOpener;
import com.intel.dcsg.cpg.crypto.key.password.Password;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.io.pem.Pem;
import com.intel.kms.api.CreateKeyRequest;
import com.intel.kms.client.jaxrs2.Keys;
import com.intel.kms.ws.v2.api.Key;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.configuration.ConfigurationProvider;
import com.intel.mtwilson.core.PasswordVaultFactory;
import com.intel.mtwilson.util.crypto.keystore.PasswordKeyStore;

public class KmsUtil {
	RsaCredentialX509 wrappingKeyCertificate;
	String kmsLoginBasicUsername;
	Keys keys;
	Map<String, String> kmsprops = null;
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(KmsUtil.class);
	private static final String DIRECTOR_ENVELOPE_ALIAS = "director.envelope.alias";
	private static final String DIRECTOR_KEYSTORE = "director.keystore";
	private static final String DIRECTOR_KEYSTORE_PASSWORD = "director.keystore.password";
	private static final String KMS_ENDPOINT_URL = "kms.endpoint.url";
	private static final String KMS_TLS_POLICY_CERTIFICATE_SHA1 = "kms.tls.policy.certificate.sha1";
	private static final String KMS_LOGIN_BASIC_USERNAME = "kms.login.basic.username";
	private static final String KMS_LOGIN_BASIC_PASSWORD = "kms.login.basic.password";
	private static final String KMS_PROP_FILE = "kms.properties";

	public KmsUtil() throws IOException, JAXBException, XMLStreamException,
			Exception {
		Password keystorePassword = null;
		PublicKey directorEnvelopePublicKey;
		String kmsEndpointUrl;
		String kmsTlsPolicyCertificateSha1;
		String kmsLoginBasicPassword;
		kmsprops = new Gson().fromJson(getProperties(KMS_PROP_FILE), new TypeToken<HashMap<String, Object>>() {}.getType());
		// Get director envelope key
		String directorEnvelopeAlias = getConfiguration().get(
				DIRECTOR_ENVELOPE_ALIAS, "director-envelope");
		if (directorEnvelopeAlias == null || directorEnvelopeAlias.isEmpty()) {
			throw new Exception(
					"Trust Director Envelope alias not configured");
		}

		log.debug("**** KMSUTIL: Folders.configuration() : " + Folders.configuration());
		String keystorePath = getConfiguration().get(DIRECTOR_KEYSTORE,
				Folders.configuration() + File.separator + "keystore.jks");
		File keystoreFile = new File(keystorePath);
		if (!keystoreFile.exists()) {
			throw new Exception(
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
			throw new Exception(
					"Director Keystore password is not configured");
		}

		SimpleKeystore keystore = new SimpleKeystore(new FileResource(
				keystoreFile), keystorePassword);
		wrappingKeyCertificate = keystore.getRsaCredentialX509(
				directorEnvelopeAlias, keystorePassword);
		log.debug("Found key {}", wrappingKeyCertificate.getCertificate()
				.getSubjectX500Principal().getName());

		directorEnvelopePublicKey = wrappingKeyCertificate.getPublicKey();
		if (directorEnvelopePublicKey == null) {
			log.error("Trust Director envelope public key is not configured");
		}

		// Collect KMS configurations
		kmsEndpointUrl = kmsprops.get(KMS_ENDPOINT_URL.replace('.','_'));
		if (kmsEndpointUrl == null || kmsEndpointUrl.isEmpty()) {
			throw new Exception(
					"KMS endpoint URL not configured");
		}

		kmsTlsPolicyCertificateSha1 = kmsprops.get(KMS_TLS_POLICY_CERTIFICATE_SHA1.replace('.', '_'));
		if (kmsTlsPolicyCertificateSha1 == null
				|| kmsTlsPolicyCertificateSha1.isEmpty()) {
			throw new Exception(
					"KMS TLS policy certificate digest not configured");
		}

		kmsLoginBasicUsername = kmsprops.get(KMS_LOGIN_BASIC_USERNAME.replace('.', '_'));
		if (kmsLoginBasicUsername == null || kmsLoginBasicUsername.isEmpty()) {
			throw new Exception(
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
			throw new Exception(
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

	}

	public KeyContainer createKey() throws CryptographyException {
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

	private byte[] transferKey(String keyId) throws CryptographyException {
		String transferKeyPemResponse = keys.transferKey(keyId);
		// decrypt the requested key
		RsaPublicKeyProtectedPemKeyEnvelopeOpener opener = new RsaPublicKeyProtectedPemKeyEnvelopeOpener(
				wrappingKeyCertificate.getPrivateKey(), kmsLoginBasicUsername);
		SecretKey secretKey = (SecretKey) opener.unseal(Pem
				.valueOf(transferKeyPemResponse));
		byte[] key = secretKey.getEncoded();
		return key;
	}

	public String getKeyFromKMS(String id) {
		try {
			//new String(TransferKey.getKey(id), "UTF-8");
			byte[] key = transferKey(id);
			return new sun.misc.BASE64Encoder().encode(key);
		}  catch (Exception e) {
			// TODO Handle Error
			log.error("Error in getKeyFromKMS() method" + e);
		}
		return null;
	}

	public static String getProperties(String path) throws IOException {		
		File customFile = new File(Folders.configuration()+path);
		ConfigurationProvider provider = ConfigurationFactory.createConfigurationProvider(customFile);
		Configuration loadedConfiguration = provider.load();
		Map<String, String> map = new HashMap<String, String>();
		for (String key : loadedConfiguration.keys()) {
			String value = loadedConfiguration.get(key);
			map.put(key.replace(".", "_"), value);
		}
		JSONObject json = new JSONObject(map);
		return json.toString();
	}

}

/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.config.client.tls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractTlsSetup {

	protected static final String TEST_KEY_STORE_PASSWORD = "test-key-store-password";

	protected static final String TEST_KEY_PASSWORD = "test-key-password";

	protected static final String TEST_WRONG_PASSWORD = "test-wrong-password";

	protected static File caCert;

	protected static File wrongCaCert;

	protected static File serverCert;

	protected static File clientCert;

	protected static File wrongClientCert;

	@BeforeAll
	public static void createCertificates() throws Exception {
		KeyAndCert ca = createAndSaveCerts(); 
		configureSystemTrustStore(ca);
		}

		private static KeyAndCert createAndSaveCerts() throws Exception {
			KeyTool tool = new KeyTool();
			KeyAndCert ca = tool.createCA("MyCA");
			serverCert = saveKeyAndCert(ca.sign("server"));
			clientCert = saveKeyAndCert(ca.sign("client"));
		
			KeyAndCert wrongCa = tool.createCA("WrongCA");  
			KeyAndCert wrongClient = wrongCa.sign("client");
			wrongCaCert = saveCert(wrongCa);  
			wrongClientCert = saveKeyAndCert(wrongClient);
		
			return ca;
		}

		private static void configureSystemTrustStore(KeyAndCert ca){

		System.setProperty("javax.net.ssl.trustStore", caCert.getAbsolutePath());
		System.setProperty("javax.net.ssl.trustStorePassword", TEST_KEY_STORE_PASSWORD);
	}

	private static File saveKeyAndCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(), () -> keyCert.storeKeyAndCert(TEST_KEY_PASSWORD));
	}

	private static File saveCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(), keyCert::storeCert);
	}

	private static File saveKeyStore(String prefix, KeyStoreSupplier func) throws Exception {
		File result = File.createTempFile(prefix, ".p12");
		result.deleteOnExit();

		try (OutputStream output = new FileOutputStream(result)) {
			KeyStore store = func.createKeyStore();
			store.store(output, TEST_KEY_STORE_PASSWORD.toCharArray());
		}
		return result;
	}

	private interface KeyStoreSupplier {

		KeyStore createKeyStore() throws Exception;

	}

	private static class CertificateFactory {
		private final KeyTool tool = new KeyTool();
	
		public KeyAndCert createCA(String name) throws Exception {
			return tool.createCA(name);
		}
	
		public KeyAndCert signCertificate(KeyAndCert ca, String name) throws Exception {
			return ca.sign(name);
		}
}
}

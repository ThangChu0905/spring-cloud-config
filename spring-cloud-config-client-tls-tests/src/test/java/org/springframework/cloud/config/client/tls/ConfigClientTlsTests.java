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

import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigClientRequestTemplateFactory;
import org.springframework.cloud.config.server.EnableConfigServer;

class ConfigClientTlsTests extends AbstractTlsSetup {

	protected static TlsConfigServerRunner server;

	@AfterEach
	public void tearDownAll() {
		stopConfigServer();
	}

	private static void startConfigServer(boolean withKeyStore) {
		server = new TlsConfigServerRunner(TestConfigServer.class);
		if (withKeyStore) {
			server.enableTls();
			server.setKeyStore(serverCert, KEY_STORE_PASSWORD, "server", KEY_PASSWORD);
			server.setTrustStore(caCert, KEY_STORE_PASSWORD);
		}
		server.property("logging.level.org.springframework.cloud.config.server", "TRACE");

		server.start();
	}

	private static void stopConfigServer() {
		server.stop();
	}

	@Test
	void clientCertCanWork() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client);
			client.property("logging.level.org.springframework.boot.context.config", "TRACE");
			client.property("logging.level.org.springframework.cloud.config.client", "DEBUG");
			client.start();
			assertThat(client.getProperty("dumb.key")).isEqualTo("dumb-value");
		}
	}

	@Test
	void clientCanStartWithoutKeyStoreSet() {
		startConfigServer(false);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client, false);
			client.property("logging.level.org.springframework.boot.context.config", "TRACE");
			client.property("logging.level.org.springframework.cloud.config.client", "DEBUG");
			client.start();
			assertThat(client.getProperty("dumb.key")).isEqualTo("dumb-value");
		}
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	void tlsSetsRequestReadTimeout() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client);
			client.property("logging.level.org.springframework.boot.context.config", "TRACE");
			client.property("logging.level.org.springframework.cloud.config.client", "DEBUG");
			int timeoutMillis = (60 * 1000 * 3) + 5001;
			client.property("spring.cloud.config.request-read-timeout", String.valueOf(timeoutMillis));
			client.start();
			ConfigClientProperties configClientProperties = client.app().getBean(ConfigClientProperties.class);
			TestFactory factory = new TestFactory(configClientProperties);
			SocketConfig.Builder socketBuilder = factory.getSocketBuilderForTls();
			assertThat(socketBuilder.build().getSoTimeout().toMilliseconds()).isEqualTo(timeoutMillis);
		}
	}

	@Test
	void tlsClientCanBeDisabled() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client);
			client.property("spring.cloud.config.tls.enabled", "false");
			client.start();
			assertThat(client.getProperty("dumb.key")).isNull();
		}
	}

	@Test
	void noCertCannotWork() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			client.disableTls();
			client.start();
			assertThat(client.getProperty("dumb.key")).isNull();
		}
	}

	@Test
	void wrongCertCannotWork() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client);
			client.setKeyStore(wrongClientCert);
			client.start();
			assertThat(client.getProperty("dumb.key")).isNull();
		}
	}

	@Test
	void wrongPasswordCauseFailure() {
		startConfigServer(true);
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			TlsConfigClientRunner client = createConfigClient(false);
			enableTlsClient(client);
			client.setKeyStore(clientCert, WRONG_PASSWORD, WRONG_PASSWORD);
			client.start();
		});
	}

	@Test
	void nonExistKeyStoreCauseFailure() {
		startConfigServer(true);
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			TlsConfigClientRunner client = createConfigClient(false);
			enableTlsClient(client);
			client.setKeyStore(new File("nonExistFile"));
			client.start();
		});
	}

	@Test
	void wrongTrustStoreCannotWork() {
		startConfigServer(true);
		try (TlsConfigClientRunner client = createConfigClient()) {
			enableTlsClient(client);
			client.setTrustStore(wrongCaCert);
			client.start();
			assertThat(client.getProperty("dumb.key")).isNull();
		}
	}

	protected TlsConfigClientRunner createConfigClient(boolean optional) {
		TlsConfigClientRunner runner = createConfigClient();
		if (!optional) {
			runner.property("spring.cloud.config.fail-fast", "true");
		}
		return runner;
	}

	protected TlsConfigClientRunner createConfigClient() {
		return new TlsConfigClientRunner(TestApp.class, server);
	}

	private void enableTlsClient(TlsConfigClientRunner runner) {
		enableTlsClient(runner, true);
	}

	private void enableTlsClient(TlsConfigClientRunner runner, boolean withKeyStore) {
		runner.enableTls();
		if (withKeyStore) {
			runner.setKeyStore(clientCert, KEY_STORE_PASSWORD, KEY_PASSWORD);
		}
		runner.setTrustStore(caCert, KEY_STORE_PASSWORD);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableConfigServer
	public static class TestConfigServer {

	}

	static class TestFactory extends ConfigClientRequestTemplateFactory {

		TestFactory(ConfigClientProperties properties) {
			super(LogFactory.getLog(TestFactory.class), properties);
		}

		public SocketConfig.Builder getSocketBuilderForTls() {
			return createSocketBuilderForTls(getProperties());
		}

	}

}

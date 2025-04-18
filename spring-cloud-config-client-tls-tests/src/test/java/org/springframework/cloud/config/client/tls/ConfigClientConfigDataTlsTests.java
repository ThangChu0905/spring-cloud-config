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

public class ConfigClientConfigDataTlsTests extends ConfigClientTlsTests {

	@Override
	protected TlsConfigClientRunner createConfigClient(boolean optional) {
		String importValue = buildImportValue(optional);
		return new TlsConfigClientRunner(TestApp.class, server, "spring.config.import", importValue);
	}

	@Override
	protected TlsConfigClientRunner createConfigClient() {
		return createConfigClient(true); // remove code duplication in createConfigClient()
	}

	private String buildImportValue(boolean optional){
		return optional ? "optional:configserver:" : "configserver:";
	}

}

/*
 * Copyright 2012-2024 the original author or authors.
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

package am.ik.spring.opentelemetry.logs.otlp;

import java.util.Locale;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurations imported by {@link OtlpLogsAutoConfiguration}.
 *
 * @author Toshiaki Maki
 */
public class OtlpLogsConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.otlp.logging", name = "endpoint")
		public OtlpLogsConnectionDetails otlpLogsConnectionDetails(OtlpProperties properties) {
			return new PropertiesOtlpLogsConnectionDetails(properties);
		}

		/**
		 * Adapts {@link OtlpProperties} to {@link OtlpLogsConnectionDetails}.
		 */
		static class PropertiesOtlpLogsConnectionDetails implements OtlpLogsConnectionDetails {

			private final OtlpProperties properties;

			PropertiesOtlpLogsConnectionDetails(OtlpProperties properties) {
				this.properties = properties;
			}

			@Override
			public String getUrl() {
				return this.properties.getEndpoint();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Exporters {

		@ConditionalOnMissingBean(value = OtlpHttpLogRecordExporter.class,
				type = "io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter")
		@ConditionalOnBean(OtlpLogsConnectionDetails.class)
		@Bean
		public OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OtlpProperties properties,
				OtlpLogsConnectionDetails connectionDetails) {
			OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US))
				.setTimeout(properties.getTimeout());
			properties.getHeaders().forEach(builder::addHeader);
			return builder.build();
		}

	}

}

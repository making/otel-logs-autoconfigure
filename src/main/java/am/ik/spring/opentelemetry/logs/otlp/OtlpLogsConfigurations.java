package am.ik.spring.opentelemetry.logs.otlp;

import java.util.Locale;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class OtlpLogsConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.otlp.logs", name = "endpoint")
		public OtlpLogsConnectionDetails otlpLogsConnectionDetails(OtlpProperties properties) {
			return new PropertiesOtlpLogsConnectionDetails(properties);
		}

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

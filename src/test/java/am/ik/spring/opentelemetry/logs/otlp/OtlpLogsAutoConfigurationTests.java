package am.ik.spring.opentelemetry.logs.otlp;

import am.ik.spring.opentelemetry.logs.otlp.OtlpLogsConfigurations.ConnectionDetails.PropertiesOtlpLogsConnectionDetails;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OtlpLogsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpLogsAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfPropertyIsNotSet() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(OtlpLogsConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withPropertyValues("management.otlp.logs.endpoint=http://localhost:4318/v1/logs")
			.run(context -> {
				assertThat(context).hasSingleBean(OtlpLogsConnectionDetails.class);
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
					.hasSingleBean(LogRecordExporter.class);
			});
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.opentelemetry.sdk.logs", "io.opentelemetry.api",
			"io.opentelemetry.exporter.otlp.http.logs" })
	void shouldNotSupplyBeansIfDependencyIsMissing(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(OtlpLogsConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void shouldBackOffWhenCustomHttpExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomHttpExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpHttpLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomGrpcExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomGrpcExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpGrpcLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomOtlpLogsConnectionDetailsIsDefined() {
		this.contextRunner.withUserConfiguration(CustomOtlpLogsConnectionDetails.class).run((context) -> {
			assertThat(context).hasSingleBean(OtlpLogsConnectionDetails.class)
				.doesNotHaveBean(PropertiesOtlpLogsConnectionDetails.class);
			OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = context.getBean(OtlpHttpLogRecordExporter.class);
			assertThat(otlpHttpLogRecordExporter).extracting("delegate.httpSender.url")
				.isEqualTo(HttpUrl.get("https://otel.example.com/v1/logs"));
		});

	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomHttpExporterConfiguration {

		@Bean
		public OtlpHttpLogRecordExporter customOtlpHttpLogRecordExporter() {
			return OtlpHttpLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomGrpcExporterConfiguration {

		@Bean
		public OtlpGrpcLogRecordExporter customOtlpGrpcLogRecordExporter() {
			return OtlpGrpcLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomOtlpLogsConnectionDetails {

		@Bean
		public OtlpLogsConnectionDetails customOtlpLogsConnectionDetails() {
			return () -> "https://otel.example.com/v1/logs";
		}

	}

}
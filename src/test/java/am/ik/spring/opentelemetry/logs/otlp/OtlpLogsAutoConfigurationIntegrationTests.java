package am.ik.spring.opentelemetry.logs.otlp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import am.ik.spring.opentelemetry.logs.OpenTelemetryAutoConfiguration;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.GzipSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class OtlpLogsAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.application.name=otlp-logs-test",
				"management.otlp.logs.headers.Authorization=Bearer my-token")
		.withConfiguration(AutoConfigurations.of(
				org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration.class,
				OpenTelemetryAutoConfiguration.class, OtlpLogsAutoConfiguration.class));

	private final MockWebServer mockWebServer = new MockWebServer();

	@BeforeEach
	void setUp() throws IOException {
		this.mockWebServer.start();
	}

	@AfterEach
	void tearDown() throws IOException {
		this.mockWebServer.close();
	}

	@Test
	void httpLogRecordExporterShouldUseProtobufAndNoCompressionByDefault() {
		this.mockWebServer.enqueue(new MockResponse());
		this.contextRunner
			.withPropertyValues(
					"management.otlp.logs.endpoint=http://localhost:%d/v1/logs".formatted(this.mockWebServer.getPort()))
			.run((context) -> {
				SdkLoggerProvider loggerProvider = context.getBean(SdkLoggerProvider.class);
				loggerProvider.get("test")
					.logRecordBuilder()
					.setSeverity(Severity.INFO)
					.setSeverityText("INFO")
					.setBody("Hello")
					.setTimestamp(Instant.now())
					.emit();
				RecordedRequest request = this.mockWebServer.takeRequest(10, TimeUnit.SECONDS);
				assertThat(request).isNotNull();
				assertThat(request.getRequestLine()).contains("/v1/logs");
				assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
				assertThat(request.getHeader("Content-Encoding")).isNull();
				assertThat(request.getBodySize()).isPositive();
				try (Buffer body = request.getBody()) {
					String bodyString = body.readString(StandardCharsets.UTF_8);
					assertThat(bodyString).contains("otlp-logs-test");
					assertThat(bodyString).contains("test");
					assertThat(bodyString).contains("INFO");

					assertThat(bodyString).contains("Hello");
				}
			});
	}

	@Test
	void httpLogRecordExporterCanBeConfiguredToUseGzipCompression() {
		this.mockWebServer.enqueue(new MockResponse());
		this.contextRunner
			.withPropertyValues(
					"management.otlp.logs.endpoint=http://localhost:%d/v1/logs".formatted(this.mockWebServer.getPort()),
					"management.otlp.logs.compression=gzip")
			.run((context) -> {
				SdkLoggerProvider loggerProvider = context.getBean(SdkLoggerProvider.class);
				loggerProvider.get("test")
					.logRecordBuilder()
					.setBody("Hello")
					.setSeverity(Severity.INFO)
					.setSeverityText("INFO")
					.setTimestamp(Instant.now())
					.emit();
				RecordedRequest request = this.mockWebServer.takeRequest(10, TimeUnit.SECONDS);
				assertThat(request).isNotNull();
				assertThat(request.getRequestLine()).contains("/v1/logs");
				assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
				assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
				assertThat(request.getBodySize()).isPositive();
				try (Buffer uncompressed = new Buffer(); Buffer body = request.getBody()) {
					uncompressed.writeAll(new GzipSource(body));
					String bodyString = uncompressed.readString(StandardCharsets.UTF_8);
					assertThat(bodyString).contains("otlp-logs-test");
					assertThat(bodyString).contains("test");
					assertThat(bodyString).contains("INFO");
					assertThat(bodyString).contains("Hello");
				}
			});
	}

}

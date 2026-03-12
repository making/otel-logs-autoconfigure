/*
 * Copyright 2024-2026 the original author or authors.
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

package am.ik.spring.opentelemetry.logs.log4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Log4jAppenderInstallListenerIntegrationTest {

	@BeforeEach
	void setUp() {
		removeOpenTelemetryAppenders();
	}

	@AfterEach
	void tearDown() {
		removeOpenTelemetryAppenders();
	}

	private void removeOpenTelemetryAppenders() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Configuration config = loggerContext.getConfiguration();
		Map<String, Appender> appenders = config.getAppenders();
		appenders.values().stream().filter(a -> a instanceof OpenTelemetryAppender).forEach(a -> {
			config.getRootLogger().removeAppender(a.getName());
			a.stop();
			config.getAppenders().remove(a.getName());
		});
		loggerContext.updateLoggers();
	}

	private OpenTelemetryAppender findOpenTelemetryAppender() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Configuration config = loggerContext.getConfiguration();
		return config.getAppenders()
			.values()
			.stream()
			.filter(a -> a instanceof OpenTelemetryAppender)
			.map(a -> (OpenTelemetryAppender) a)
			.findFirst()
			.orElse(null);
	}

	@Test
	void appenderIsInstalledOnApplicationReady() {
		SpringApplication application = new SpringApplication(TestConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run()) {
			OpenTelemetryAppender appender = findOpenTelemetryAppender();
			assertThat(appender).isNotNull();
			assertThat(appender.isStarted()).isTrue();
		}
	}

	@Test
	void appenderIsInstalledOnApplicationFailed() {
		SpringApplication application = new SpringApplication(FailingConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		assertThatThrownBy(application::run).hasMessageContaining("Intentional failure");

		OpenTelemetryAppender appender = findOpenTelemetryAppender();
		assertThat(appender).isNotNull();
		assertThat(appender.isStarted()).isTrue();
	}

	@Test
	void appenderIsNotInstalledWhenDisabled() {
		SpringApplication application = new SpringApplication(TestConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application
			.run("--management.opentelemetry.instrumentation.log4j-appender.enabled=false")) {
			OpenTelemetryAppender appender = findOpenTelemetryAppender();
			assertThat(appender).isNull();
		}
	}

	@Test
	void appenderConfigurationIsApplied() {
		SpringApplication application = new SpringApplication(TestConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		try (ConfigurableApplicationContext context = application.run(
				"--management.opentelemetry.instrumentation.log4j-appender.capture-code-attributes=true",
				"--management.opentelemetry.instrumentation.log4j-appender.capture-marker-attribute=true",
				"--management.opentelemetry.instrumentation.log4j-appender.num-logs-captured-before-otel-install=500")) {
			OpenTelemetryAppender appender = findOpenTelemetryAppender();
			assertThat(appender).isNotNull();
			assertThat(appender.isStarted()).isTrue();
		}
	}

	@Test
	void logsAreSentToOtlpEndpointOnApplicationReady() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger requestCount = new AtomicInteger(0);
		AtomicReference<byte[]> receivedBody = new AtomicReference<>();

		HttpServer server = createMockOtlpServer(latch, requestCount, receivedBody);
		server.start();

		try {
			int port = server.getAddress().getPort();
			OtlpTestConfiguration.endpoint = "http://localhost:" + port + "/v1/logs";

			SpringApplication application = new SpringApplication(OtlpTestConfiguration.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			try (ConfigurableApplicationContext context = application.run()) {
				org.apache.logging.log4j.Logger logger = LogManager.getLogger("test.logger");
				logger.info("Test log message for OTLP export");

				// Force flush
				OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
				if (openTelemetry instanceof OpenTelemetrySdk sdk) {
					sdk.getSdkLoggerProvider().forceFlush().join(5, TimeUnit.SECONDS);
				}

				boolean received = latch.await(10, TimeUnit.SECONDS);
				assertThat(received).as("OTLP request should be received").isTrue();
				assertThat(requestCount.get()).isGreaterThanOrEqualTo(1);
				assertThat(receivedBody.get()).isNotNull();
				assertThat(receivedBody.get().length).isGreaterThan(0);

				// Verify the log message is included in the OTLP payload
				String bodyAsString = new String(receivedBody.get());
				assertThat(bodyAsString).contains("Test log message for OTLP export");
			}
		}
		finally {
			server.stop(0);
			OtlpTestConfiguration.endpoint = null;
			if (OtlpTestConfiguration.lastCreatedSdk != null) {
				OtlpTestConfiguration.lastCreatedSdk.close();
				OtlpTestConfiguration.lastCreatedSdk = null;
			}
		}
	}

	@Test
	void logsAreSentToOtlpEndpointOnApplicationFailed() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger requestCount = new AtomicInteger(0);
		AtomicReference<byte[]> receivedBody = new AtomicReference<>();

		HttpServer server = createMockOtlpServer(latch, requestCount, receivedBody);
		server.start();

		try {
			int port = server.getAddress().getPort();
			OtlpFailingConfiguration.endpoint = "http://localhost:" + port + "/v1/logs";

			SpringApplication application = new SpringApplication(OtlpFailingConfiguration.class);
			application.setWebApplicationType(WebApplicationType.NONE);

			assertThatThrownBy(application::run).hasMessageContaining("Intentional failure");

			// Force flush after application failed
			OpenTelemetrySdk sdk = OtlpFailingConfiguration.lastCreatedSdk;
			assertThat(sdk).as("OpenTelemetry SDK should be created").isNotNull();
			sdk.getSdkLoggerProvider().forceFlush().join(5, TimeUnit.SECONDS);

			boolean received = latch.await(10, TimeUnit.SECONDS);
			assertThat(received).as("OTLP request should be received even on application failure").isTrue();
			assertThat(requestCount.get()).isGreaterThanOrEqualTo(1);
			assertThat(receivedBody.get()).isNotNull();
			assertThat(receivedBody.get().length).isGreaterThan(0);

			// Verify the log message is included in the OTLP payload
			String bodyAsString = new String(receivedBody.get());
			assertThat(bodyAsString).contains("Log message before application failure");
		}
		finally {
			server.stop(0);
			OtlpFailingConfiguration.endpoint = null;
			if (OtlpFailingConfiguration.lastCreatedSdk != null) {
				OtlpFailingConfiguration.lastCreatedSdk.close();
				OtlpFailingConfiguration.lastCreatedSdk = null;
			}
		}
	}

	private HttpServer createMockOtlpServer(CountDownLatch latch, AtomicInteger requestCount,
			AtomicReference<byte[]> receivedBody) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/v1/logs", exchange -> {
			byte[] body = exchange.getRequestBody().readAllBytes();
			receivedBody.set(body);
			requestCount.incrementAndGet();

			exchange.sendResponseHeaders(200, 0);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(new byte[0]);
			}
			latch.countDown();
		});
		return server;
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@org.springframework.context.annotation.Bean
		OpenTelemetry openTelemetry() {
			return OpenTelemetry.noop();
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class FailingConfiguration {

		@org.springframework.context.annotation.Bean
		OpenTelemetry openTelemetry() {
			return OpenTelemetry.noop();
		}

		@org.springframework.context.annotation.Bean
		String failingBean() {
			throw new RuntimeException("Intentional failure for testing ApplicationFailedEvent");
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class OtlpTestConfiguration {

		static String endpoint;

		static volatile OpenTelemetrySdk lastCreatedSdk;

		@org.springframework.context.annotation.Bean
		OpenTelemetry openTelemetry() {
			OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder().setEndpoint(endpoint).build();

			SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
				.setResource(Resource.getDefault())
				.addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
				.build();

			OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
			lastCreatedSdk = sdk;
			return sdk;
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class OtlpFailingConfiguration {

		static String endpoint;

		static volatile OpenTelemetrySdk lastCreatedSdk;

		@org.springframework.context.annotation.Bean
		OpenTelemetry openTelemetry() {
			OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder().setEndpoint(endpoint).build();

			SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
				.setResource(Resource.getDefault())
				.addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
				.build();

			OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
			lastCreatedSdk = sdk;
			return sdk;
		}

		@org.springframework.context.annotation.Bean
		ApplicationRunner failingRunner() {
			return args -> {
				org.apache.logging.log4j.Logger logger = LogManager.getLogger("test.failing");
				logger.info("Log message before application failure");
				throw new RuntimeException("Intentional failure for testing ApplicationFailedEvent");
			};
		}

	}

}

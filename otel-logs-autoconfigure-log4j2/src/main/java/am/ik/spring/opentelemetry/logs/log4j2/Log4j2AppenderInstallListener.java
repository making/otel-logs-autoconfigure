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

package am.ik.spring.opentelemetry.logs.log4j2;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * An {@link GenericApplicationListener} that installs the OpenTelemetry Log4j2 appender.
 */
public class Log4j2AppenderInstallListener implements GenericApplicationListener {

	private static final String APPENDER_NAME = "OpenTelemetryLog4j2Appender";

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		if (eventType.getRawClass() == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType.getRawClass())
				|| ApplicationReadyEvent.class.isAssignableFrom(eventType.getRawClass())
				|| ApplicationFailedEvent.class.isAssignableFrom(eventType.getRawClass());
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return SpringApplication.class.isAssignableFrom(sourceType)
				|| ApplicationContext.class.isAssignableFrom(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (!ClassUtils.isPresent("org.apache.logging.log4j.core.LoggerContext", null)
				|| !ClassUtils.isPresent("io.opentelemetry.api.OpenTelemetry", null) || !ClassUtils
					.isPresent("io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender", null)) {
			return;
		}
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
			Binder binder = Binder.get(environment);
			boolean enabled = binder
				.bind("management.opentelemetry.instrumentation.log4j2-appender.enabled", Boolean.class)
				.orElse(true);
			if (!enabled) {
				return;
			}
			installAppender(binder);
		}
		else if (event instanceof ApplicationReadyEvent applicationReadyEvent) {
			ConfigurableApplicationContext applicationContext = applicationReadyEvent.getApplicationContext();
			ObjectProvider<OpenTelemetry> openTelemetry = applicationContext.getBeanProvider(OpenTelemetry.class);
			openTelemetry.ifAvailable(OpenTelemetryAppender::install);
		}
		else if (event instanceof ApplicationFailedEvent applicationFailedEvent) {
			ConfigurableApplicationContext applicationContext = applicationFailedEvent.getApplicationContext();
			ObjectProvider<OpenTelemetry> openTelemetry = applicationContext.getBeanProvider(OpenTelemetry.class);
			openTelemetry.ifAvailable(OpenTelemetryAppender::install);
		}
	}

	void installAppender(Binder binder) {
		org.apache.logging.log4j.spi.LoggerContext loggerContextSpi = LogManager.getContext(false);
		if (!(loggerContextSpi instanceof LoggerContext loggerContext)) {
			return;
		}
		Configuration config = loggerContext.getConfiguration();

		OpenTelemetryAppender.Builder<?> builder = OpenTelemetryAppender.builder();
		builder.setName(APPENDER_NAME);
		configureAppender(builder, binder);
		OpenTelemetryAppender appender = builder.build();
		appender.start();

		config.addAppender(appender);
		LoggerConfig rootLoggerConfig = config.getRootLogger();
		rootLoggerConfig.addAppender(appender, null, null);
		loggerContext.updateLoggers();
	}

	void configureAppender(OpenTelemetryAppender.Builder<?> builder, Binder binder) {
		String prefix = "management.opentelemetry.instrumentation.log4j2-appender";
		boolean captureExperimentalAttributes = binder.bind(prefix + ".capture-experimental-attributes", Boolean.class)
			.orElse(false);
		boolean captureCodeAttributes = binder.bind(prefix + ".capture-code-attributes", Boolean.class).orElse(false);
		boolean captureMapMessageAttributes = binder.bind(prefix + ".capture-map-message-attributes", Boolean.class)
			.orElse(false);
		boolean captureMarkerAttribute = binder.bind(prefix + ".capture-marker-attribute", Boolean.class).orElse(false);
		String captureContextDataAttributes = binder.bind(prefix + ".capture-context-data-attributes", String.class)
			.orElse(null);
		int numLogsCapturedBeforeOtelInstall = binder
			.bind(prefix + ".num-logs-captured-before-otel-install", Integer.class)
			.orElse(1000);

		builder.setCaptureExperimentalAttributes(captureExperimentalAttributes);
		builder.setCaptureCodeAttributes(captureCodeAttributes);
		builder.setCaptureMapMessageAttributes(captureMapMessageAttributes);
		builder.setCaptureMarkerAttribute(captureMarkerAttribute);
		if (captureContextDataAttributes != null) {
			builder.setCaptureContextDataAttributes(captureContextDataAttributes);
		}
		builder.setNumLogsCapturedBeforeOtelInstall(numLogsCapturedBeforeOtelInstall);
	}

	@Override
	public int getOrder() {
		// After org.springframework.boot.context.logging.LoggingApplicationListener
		return LoggingApplicationListener.DEFAULT_ORDER + 1;
	}

}

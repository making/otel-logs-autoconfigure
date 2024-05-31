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

package am.ik.spring.opentelemetry.logs.logback;

import ch.qos.logback.classic.Logger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
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

public class LogbackAppenderInstallListener implements GenericApplicationListener {

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		if (eventType.getRawClass() == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType.getRawClass())
				|| ApplicationReadyEvent.class.isAssignableFrom(eventType.getRawClass());
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return SpringApplication.class.isAssignableFrom(sourceType)
				|| ApplicationContext.class.isAssignableFrom(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (!ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext", null)
				|| !ClassUtils.isPresent("io.opentelemetry.api.OpenTelemetry", null) || !ClassUtils
					.isPresent("io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender", null)) {
			return;
		}
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
			ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
			Binder binder = Binder.get(environment);
			boolean enabled = binder
				.bind("management.opentelemetry.instrumentation.logback-appender.enabled", Boolean.class)
				.orElse(true);
			if (!enabled) {
				return;
			}
			this.configureOpenTelemetryAppender(openTelemetryAppender, binder);
			openTelemetryAppender.start();
			rootLogger.addAppender(openTelemetryAppender);
		}
		else if (event instanceof ApplicationReadyEvent) {
			ConfigurableApplicationContext applicationContext = ((ApplicationReadyEvent) event).getApplicationContext();
			ObjectProvider<OpenTelemetry> openTelemetry = applicationContext.getBeanProvider(OpenTelemetry.class);
			openTelemetry.ifAvailable(OpenTelemetryAppender::install);
		}
	}

	void configureOpenTelemetryAppender(OpenTelemetryAppender openTelemetryAppender, Binder binder) {
		boolean captureExperimentalAttributes = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-experimental-attributes",
					Boolean.class)
			.orElse(false);
		boolean captureCodeAttributes = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-code-attributes", Boolean.class)
			.orElse(false);
		boolean captureMarkerAttribute = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-marker-attribute", Boolean.class)
			.orElse(false);
		boolean captureKeyValuePairAttributes = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-key-value-pair-attributes",
					Boolean.class)
			.orElse(false);
		boolean captureLoggerContext = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-logger-context", Boolean.class)
			.orElse(false);
		String captureMdcAttributes = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.capture-mdc-attributes", String.class)
			.orElse(null);
		int numLogsCapturedBeforeOtelInstall = binder
			.bind("management.opentelemetry.instrumentation.logback-appender.num-logs-captured-before-otel-install",
					Integer.class)
			.orElse(1000);
		openTelemetryAppender.setCaptureExperimentalAttributes(captureExperimentalAttributes);
		openTelemetryAppender.setCaptureCodeAttributes(captureCodeAttributes);
		openTelemetryAppender.setCaptureMarkerAttribute(captureMarkerAttribute);
		openTelemetryAppender.setCaptureKeyValuePairAttributes(captureKeyValuePairAttributes);
		openTelemetryAppender.setCaptureLoggerContext(captureLoggerContext);
		openTelemetryAppender.setCaptureMdcAttributes(captureMdcAttributes);
		openTelemetryAppender.setNumLogsCapturedBeforeOtelInstall(numLogsCapturedBeforeOtelInstall);
	}

	@Override
	public int getOrder() {
		// After org.springframework.boot.context.logging.LoggingApplicationListener
		return LoggingApplicationListener.DEFAULT_ORDER + 1;
	}

}

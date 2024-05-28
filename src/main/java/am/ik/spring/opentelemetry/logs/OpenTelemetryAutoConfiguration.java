package am.ik.spring.opentelemetry.logs;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ SdkLoggerProvider.class, OpenTelemetry.class })
@ConditionalOnBean(Resource.class)
public class OpenTelemetryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public BatchLogRecordProcessor batchLogRecordProcessor(LogRecordExporter logRecordExporter) {
		return BatchLogRecordProcessor.builder(logRecordExporter).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SdkLoggerProvider otelSdkLoggerProvider(Resource resource,
			ObjectProvider<LogRecordProcessor> logRecordProcessors,
			ObjectProvider<SdkLoggerProviderBuilderCustomizer> customizers) {
		SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder().setResource(resource);
		logRecordProcessors.orderedStream().forEach(builder::addLogRecordProcessor);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

}

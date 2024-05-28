package am.ik.spring.opentelemetry.logs.otlp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass({ SdkLoggerProvider.class, OpenTelemetry.class, OtlpHttpLogRecordExporter.class })
@EnableConfigurationProperties(OtlpProperties.class)
@Import({ OtlpLogsConfigurations.ConnectionDetails.class, OtlpLogsConfigurations.Exporters.class })
public class OtlpLogsAutoConfiguration {

}

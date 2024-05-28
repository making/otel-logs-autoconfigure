# AutoConfiguration for OpenTelemetry Logs

This is an experimental project to address https://github.com/spring-projects/spring-boot/issues/37355.

* Autoconfigures `SdkLoggerProvider` and `OtlpHttpLogRecordExporter`.
* Autoconfigures the [Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library).

```xml
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure</artifactId>
	<version>0.1.1</version>
</dependency>
```
> [!NOTE]
> Please ensure that am.ik.spring.opentelemetry:otel-logs-autoconfigure is defined before io.micrometer:micrometer-tracing-bridge-otel so that the intended version of io.opentelemetry.semconv:opentelemetry-semconv is used.

## Configuration Properties for OtlpHttpLogRecordExporter

```properties
management.otlp.logs.endpoint=http://localhost:4318/v1/logs
management.otlp.logs.headers.authorization=Bearer changeme
management.otlp.logs.compression=gzip
management.otlp.logs.timeout=10s
```

## Configuration Properties for the Logback Appender

See https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library#settings-for-the-logback-appender for the details.

```properties
management.opentelemetry.instrumentation.logback-appender.enabled=true
management.opentelemetry.instrumentation.logback-appender.capture-code-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-experimental-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-key-value-pair-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-logger-context=false
management.opentelemetry.instrumentation.logback-appender.capture-marker-attribute=false
management.opentelemetry.instrumentation.logback-appender.capture-mdc-attributes=
management.opentelemetry.instrumentation.logback-appender.num-logs-captured-before-otel-install=1000
```
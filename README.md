# AutoConfiguration for OpenTelemetry Logs

> [!WARNING]
> **For versions prior to Spring 3.4 use otel-logs-autoconfigure 0.2.x**

* Autoconfigures the [Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library).

```xml
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure</artifactId>
	<version>0.3.1</version>
</dependency>
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
management.opentelemetry.instrumentation.logback-appender.capture-mdc-attributes= # comma-separated names or `*`
management.opentelemetry.instrumentation.logback-appender.num-logs-captured-before-otel-install=1000
```
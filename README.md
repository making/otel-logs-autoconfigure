# AutoConfiguration for OpenTelemetry Logs

Autoconfigures the OpenTelemetry log appender for Spring Boot. Supports both Logback and Log4j2.

Supports Spring Boot 3.5 and 4.0+.

> [!NOTE]
> Spring Boot 3.5 manages an older version of the OpenTelemetry Java SDK that is not compatible with this library.
> If you are using Spring Boot 3.5, override the OpenTelemetry version in your `pom.xml`:
> ```xml
> <properties>
> 	<opentelemetry.version>1.63.0</opentelemetry.version>
> </properties>
> ```

## Modules

| Module | Description |
|--------|------------|
| `otel-logs-autoconfigure-logback` | [Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library) autoconfiguration |
| `otel-logs-autoconfigure-log4j2` | [Log4j2 Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/library) autoconfiguration |
| `otel-logs-autoconfigure` | Deprecated. Delegates to `otel-logs-autoconfigure-logback` for backward compatibility. |

## Logback

```xml
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure-logback</artifactId>
	<version>0.6.0</version>
</dependency>
```

### Configuration Properties for the Logback Appender

See https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library#settings-for-the-logback-appender for the details.

```properties
management.opentelemetry.instrumentation.logback-appender.enabled=true
management.opentelemetry.instrumentation.logback-appender.capture-code-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-experimental-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-key-value-pair-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-logger-context=false
management.opentelemetry.instrumentation.logback-appender.capture-marker-attribute=false
management.opentelemetry.instrumentation.logback-appender.capture-mdc-attributes= # comma-separated names or `*`
management.opentelemetry.instrumentation.logback-appender.capture-template=false
management.opentelemetry.instrumentation.logback-appender.capture-arguments=false
management.opentelemetry.instrumentation.logback-appender.capture-logstash-marker-attributes=false
management.opentelemetry.instrumentation.logback-appender.capture-logstash-structured-arguments=false
management.opentelemetry.instrumentation.logback-appender.num-logs-captured-before-otel-install=1000
```

## Log4j2

```xml
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure-log4j2</artifactId>
	<version>0.6.0</version>
</dependency>
```

When using Spring Boot with Log4j2, make sure to exclude the default logging starter and include the Log4j2 starter:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
	<exclusions>
		<exclusion>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-logging</artifactId>
		</exclusion>
	</exclusions>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

### Configuration Properties for the Log4j2 Appender

See https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/library for the details.

```properties
management.opentelemetry.instrumentation.log4j2-appender.enabled=true
management.opentelemetry.instrumentation.log4j2-appender.capture-code-attributes=false
management.opentelemetry.instrumentation.log4j2-appender.capture-context-data-attributes= # comma-separated names or `*`
management.opentelemetry.instrumentation.log4j2-appender.capture-experimental-attributes=false
management.opentelemetry.instrumentation.log4j2-appender.capture-map-message-attributes=false
management.opentelemetry.instrumentation.log4j2-appender.capture-marker-attribute=false
management.opentelemetry.instrumentation.log4j2-appender.num-logs-captured-before-otel-install=1000
```

## Migration from `otel-logs-autoconfigure`

If you are using version 0.5 or earlier with the `otel-logs-autoconfigure` artifact, replace it with `otel-logs-autoconfigure-logback`:

```xml
<!-- Before -->
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure</artifactId>
</dependency>

<!-- After -->
<dependency>
	<groupId>am.ik.spring.opentelemetry</groupId>
	<artifactId>otel-logs-autoconfigure-logback</artifactId>
</dependency>
```

The old `otel-logs-autoconfigure` artifact still works (it transitively depends on `otel-logs-autoconfigure-logback`) but is deprecated and will be removed in a future release.

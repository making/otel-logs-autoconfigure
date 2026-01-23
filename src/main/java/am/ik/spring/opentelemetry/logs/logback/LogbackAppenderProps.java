package am.ik.spring.opentelemetry.logs.logback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.opentelemetry.instrumentation.logback-appender")
public class LogbackAppenderProps {

}

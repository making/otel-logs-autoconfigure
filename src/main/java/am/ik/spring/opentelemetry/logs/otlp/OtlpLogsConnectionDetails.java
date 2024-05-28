package am.ik.spring.opentelemetry.logs.otlp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface OtlpLogsConnectionDetails extends ConnectionDetails {

	String getUrl();

}

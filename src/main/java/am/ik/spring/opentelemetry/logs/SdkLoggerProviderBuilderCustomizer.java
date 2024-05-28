package am.ik.spring.opentelemetry.logs;

import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

@FunctionalInterface
public interface SdkLoggerProviderBuilderCustomizer {

	void customize(SdkLoggerProviderBuilder builder);

}

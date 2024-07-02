package am.ik.spring.opentelemetry.logs.otlp;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMigratorTest {

	private final SpringApplication application = new SpringApplication();

	@Test
	void postProcessEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("management.otlp.logs.endpoint", "a");
		environment.setProperty("management.otlp.logs.timeout", "b");
		environment.setProperty("management.otlp.logs.compression", "c");
		environment.setProperty("management.otlp.logs.headers.foo", "d");
		environment.setProperty("management.otlp.logs.headers.bar", "e");
		PropertyMigrator processor = new PropertyMigrator();
		processor.postProcessEnvironment(environment, application);
		processor.onApplicationEvent(null);
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> propertySource = propertySources.get(PropertyMigrator.SOURCE_NAME);
		assertThat(propertySource).isNotNull();
		assertThat(environment.getProperty("management.otlp.logging.endpoint")).isEqualTo("a");
		assertThat(environment.getProperty("management.otlp.logging.timeout")).isEqualTo("b");
		assertThat(environment.getProperty("management.otlp.logging.compression")).isEqualTo("c");
		assertThat(environment.getProperty("management.otlp.logging.headers.foo")).isEqualTo("d");
		assertThat(environment.getProperty("management.otlp.logging.headers.bar")).isEqualTo("e");
	}

}
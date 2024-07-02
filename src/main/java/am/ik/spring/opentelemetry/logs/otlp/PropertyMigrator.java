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

package am.ik.spring.opentelemetry.logs.otlp;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

public class PropertyMigrator implements EnvironmentPostProcessor, Ordered {

	private final Logger log = Logger.getLogger(PropertyMigrator.class.getName());

	public static final String SOURCE_NAME = "otlpLoggingPropertyMigrator";

	private static final String OLD_PREFIX = "management.otlp.logs";

	private static final String NEW_PREFIX = "management.otlp.logging";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> properties = new TreeMap<>();
		environment.getPropertySources().forEach(propertySource -> {
			replace(propertySource, "endpoint", properties);
			replace(propertySource, "timeout", properties);
			replace(propertySource, "compression", properties);
			replace(propertySource, "headers", properties);
		});
		if (!properties.isEmpty()) {
			MapPropertySource propertySource = new MapPropertySource(SOURCE_NAME, properties);
			MutablePropertySources propertySources = environment.getPropertySources();
			if (propertySources.contains(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
				propertySources.addBefore(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
			}
			else {
				propertySources.addFirst(propertySource);
			}
		}
	}

	void replace(PropertySource<?> propertySource, String key, Map<String, Object> properties) {
		String oldKey = OLD_PREFIX + "." + key;
		String keyKey = NEW_PREFIX + "." + key;
		if (propertySource.containsProperty(oldKey) && !propertySource.containsProperty(keyKey)) {
			log.warning(() -> "The old key '%s' is found. Move the property to '%s'.".formatted(oldKey, keyKey));
			properties.put(keyKey, propertySource.getProperty(oldKey));
		}
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER - 5;
	}

}

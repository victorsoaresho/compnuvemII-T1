package br.com.fatec.messaging.pubsub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.google.api.gax.core.CredentialsProvider;

@Configuration
@ConditionalOnProperty(prefix = "app.pubsub", name = "enabled", havingValue = "true")
public class PubSubCredentialsConfiguration {

	@Bean
	public PubSubCredentialsLoadResult pubSubCredentialsLoadResult(
			PubSubProperties properties, ResourceLoader resourceLoader) {
		return PubSubCredentialsLoadResult.load(properties, resourceLoader);
	}

	@Bean
	public CredentialsProvider pubSubCredentialsProvider(PubSubCredentialsLoadResult loadResult) {
		return loadResult.getCredentialsProvider();
	}
}

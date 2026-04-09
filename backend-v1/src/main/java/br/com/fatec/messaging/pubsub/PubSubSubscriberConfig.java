package br.com.fatec.messaging.pubsub;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

/**
 * Assina apenas a <strong>subscription</strong> configurada. O {@code ack()} confirma o processamento
 * nesta assinatura (mensagem não será reentregue aqui); outras assinaturas do mesmo tópico não são afetadas.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.pubsub", name = "enabled", havingValue = "true")
public class PubSubSubscriberConfig {

	private static final Logger log = LoggerFactory.getLogger(PubSubSubscriberConfig.class);

	@Bean
	@DependsOn("pubSubCredentialsLoadResult")
	public Subscriber pubSubSubscriber(
			PubSubCredentialsLoadResult loadResult,
			PubSubProperties properties,
			PubSubMessageHandler messageHandler,
			@Qualifier("pubSubCredentialsProvider") CredentialsProvider credentialsProvider) {

		ProjectSubscriptionName subscriptionName = loadResult.getSubscriptionName();
		log.info(
				"pubsub_config projectId={} subscription={} topic={}",
				properties.getProjectId(),
				subscriptionName,
				properties.getTopicName());

		MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
					try {
						messageHandler.handle(message, subscriptionName.toString(), properties.getTopicName());
						consumer.ack();
					}
					catch (Exception ex) {
						log.error(
								"pubsub_message_failed subscription={} messageId={}",
								subscriptionName,
								message.getMessageId(),
								ex);
						consumer.nack();
					}
				};

		return Subscriber.newBuilder(subscriptionName, receiver)
				.setCredentialsProvider(credentialsProvider)
				.build();
	}

	@Bean
	public PubSubSubscriberLifecycle pubSubSubscriberLifecycle(Subscriber subscriber) {
		return new PubSubSubscriberLifecycle(subscriber);
	}

	static final class PubSubSubscriberLifecycle {

		private static final Logger log = LoggerFactory.getLogger(PubSubSubscriberLifecycle.class);

		private final Subscriber subscriber;

		PubSubSubscriberLifecycle(Subscriber subscriber) {
			this.subscriber = subscriber;
		}

		@jakarta.annotation.PostConstruct
		public void start() {
			subscriber.startAsync().awaitRunning();
			log.info("pubsub_subscriber_started");
		}

		@jakarta.annotation.PreDestroy
		public void stop() {
			try {
				subscriber.stopAsync().awaitTerminated(30, TimeUnit.SECONDS);
			}
			catch (TimeoutException e) {
				log.warn("pubsub_subscriber_stop_timeout", e);
			}
			log.info("pubsub_subscriber_stopped");
		}
	}

}

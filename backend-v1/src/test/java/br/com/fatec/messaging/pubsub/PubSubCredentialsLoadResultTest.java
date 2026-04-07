package br.com.fatec.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.pubsub.v1.ProjectSubscriptionName;

class PubSubCredentialsLoadResultTest {

	@Test
	void resolveSubscription_shortId() {
		ProjectSubscriptionName n =
				PubSubCredentialsLoadResult.resolveSubscription("my-project", "my-sub");
		assertThat(n.getProject()).isEqualTo("my-project");
		assertThat(n.getSubscription()).isEqualTo("my-sub");
	}

	@Test
	void resolveSubscription_fullName() {
		ProjectSubscriptionName n =
				PubSubCredentialsLoadResult.resolveSubscription(
						"ignored",
						"projects/serjava-demo/subscriptions/meu-topico-sub-backend-v1");
		assertThat(n.getProject()).isEqualTo("serjava-demo");
		assertThat(n.getSubscription()).isEqualTo("meu-topico-sub-backend-v1");
	}
}

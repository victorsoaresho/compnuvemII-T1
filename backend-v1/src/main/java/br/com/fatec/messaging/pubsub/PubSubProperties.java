package br.com.fatec.messaging.pubsub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.pubsub")
@Validated
public class PubSubProperties {

	/**
	 * When false, the streaming subscriber is not started (e.g. tests or local without GCP).
	 */
	private boolean enabled = true;

	/**
	 * Project id GCP — preenchido a partir de {@code project_id} no JSON de credenciais quando
	 * {@link #credentialsLocation} é carregado.
	 */
	private String projectId;

	/**
	 * Assinatura — preenchida a partir de {@code pubsub.subscription} no JSON ou id curto / nome completo
	 * {@code projects/.../subscriptions/...}.
	 */
	private String subscriptionName;

	/**
	 * Tópico — opcional, vindo de {@code pubsub.topic} no JSON (logs / documentação). O consumo é sempre pela
	 * assinatura; o ack remove a mensagem apenas do backlog desta assinatura, não afeta o tópico nem outras
	 * assinaturas.
	 */
	private String topicName;

	/**
	 * JSON da service account + bloco {@code pubsub} (Spring {@code Resource}). Padrão:
	 * {@code file:./credentials/credentials.json} com diretório de trabalho em {@code backend-v1}.
	 */
	private String credentialsLocation = "file:./credentials/credentials.json";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getSubscriptionName() {
		return subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public String getCredentialsLocation() {
		return credentialsLocation;
	}

	public void setCredentialsLocation(String credentialsLocation) {
		this.credentialsLocation = credentialsLocation;
	}
}

package br.com.fatec.messaging.pubsub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.pubsub.v1.ProjectSubscriptionName;

/**
 * Lê {@link PubSubProperties#getCredentialsLocation()}: JSON de service account + bloco opcional
 * {@code pubsub} (subscription/topic). O bloco {@code pubsub} é removido antes de construir
 * {@link GoogleCredentials}, pois não faz parte do formato oficial do Google.
 */
public final class PubSubCredentialsLoadResult {

	private final ProjectSubscriptionName subscriptionName;
	private final CredentialsProvider credentialsProvider;

	private PubSubCredentialsLoadResult(
			ProjectSubscriptionName subscriptionName, CredentialsProvider credentialsProvider) {
		this.subscriptionName = subscriptionName;
		this.credentialsProvider = credentialsProvider;
	}

	public ProjectSubscriptionName getSubscriptionName() {
		return subscriptionName;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public static PubSubCredentialsLoadResult load(PubSubProperties properties, ResourceLoader resourceLoader) {
		String loc = properties.getCredentialsLocation();
		if (!StringUtils.hasText(loc)) {
			throw new IllegalStateException("app.pubsub.credentials-location não configurado");
		}
		Resource resource = resourceLoader.getResource(loc.trim());
		if (!resource.exists()) {
			throw new IllegalStateException("Arquivo de credenciais não encontrado: " + loc);
		}
		try (var in = resource.getInputStream()) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(in);
			if (!root.isObject()) {
				throw new IllegalStateException("credentials JSON inválido: raiz deve ser um objeto");
			}
			ObjectNode copy = (ObjectNode) root.deepCopy();
			JsonNode pubsub = copy.remove("pubsub");
			String projectId = textOrNull(copy, "project_id");
			if (!StringUtils.hasText(projectId)) {
				throw new IllegalStateException("credentials: project_id ausente");
			}
			String subscriptionRaw = pubsub != null ? textOrNull(pubsub, "subscription") : null;
			String topicFromFile = pubsub != null ? textOrNull(pubsub, "topic") : null;
			if (!StringUtils.hasText(subscriptionRaw)) {
				throw new IllegalStateException(
						"credentials: defina pubsub.subscription (id curto ou nome completo projects/.../subscriptions/...)");
			}

			properties.setProjectId(projectId.trim());
			properties.setSubscriptionName(subscriptionRaw.trim());
			if (StringUtils.hasText(topicFromFile)) {
				properties.setTopicName(topicFromFile.trim());
			}

			ProjectSubscriptionName subscriptionName = resolveSubscription(projectId.trim(), subscriptionRaw.trim());
			byte[] filtered = mapper.writeValueAsBytes(copy);
			CredentialsProvider cp =
					FixedCredentialsProvider.create(
							GoogleCredentials.fromStream(new ByteArrayInputStream(filtered)));

			return new PubSubCredentialsLoadResult(subscriptionName, cp);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	static ProjectSubscriptionName resolveSubscription(String projectId, String subscription) {
		String s = subscription.trim();
		if (s.contains("/subscriptions/")) {
			return ProjectSubscriptionName.parse(s);
		}
		return ProjectSubscriptionName.of(projectId, s);
	}
}

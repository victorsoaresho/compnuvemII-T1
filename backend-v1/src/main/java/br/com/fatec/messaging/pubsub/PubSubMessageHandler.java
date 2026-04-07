package br.com.fatec.messaging.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.pubsub.v1.PubsubMessage;

/**
 * Lógica de negócio das mensagens. O ack na assinatura (após sucesso) remove a mensagem do backlog
 * <em>desta</em> subscription; o tópico continua entregando a outras assinaturas normalmente.
 */
@Service
public class PubSubMessageHandler {

	private static final Logger log = LoggerFactory.getLogger(PubSubMessageHandler.class);

	public void handle(PubsubMessage message, String subscriptionName, String topicName) {
		byte[] data = message.getData().toByteArray();
		int payloadBytes = data.length;
		String messageId = message.getMessageId();

		if (payloadBytes == 0) {
			throw new IllegalArgumentException("empty payload");
		}

		// Intentionally do not log raw body (may contain sensitive data).
		log.info(
				"pubsub_message_received subscription={} topic={} messageId={} payloadBytes={}",
				subscriptionName,
				topicName != null ? topicName : "",
				messageId,
				payloadBytes);
	}

}

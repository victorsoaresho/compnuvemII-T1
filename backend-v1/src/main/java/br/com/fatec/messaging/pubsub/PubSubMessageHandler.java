package br.com.fatec.messaging.pubsub;

import br.com.fatec.messaging.model.dto.OrderPayloadDto;
import br.com.fatec.messaging.service.OrderPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.pubsub.v1.PubsubMessage;

import java.io.IOException;

/**
 * Lógica de negócio das mensagens. O ack na assinatura (após sucesso) remove a mensagem do backlog
 * <em>desta</em> subscription; o tópico continua entregando a outras assinaturas normalmente.
 */
@Service
public class PubSubMessageHandler {

	private static final Logger log = LoggerFactory.getLogger(PubSubMessageHandler.class);

	private final ObjectMapper objectMapper;
	private final OrderPersistenceService orderPersistenceService;

	public PubSubMessageHandler(ObjectMapper objectMapper, OrderPersistenceService orderPersistenceService) {
		this.objectMapper = objectMapper;
		this.orderPersistenceService = orderPersistenceService;
	}

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

		try {
			OrderPayloadDto dto = objectMapper.readValue(data, OrderPayloadDto.class);
			orderPersistenceService.persist(dto);
		} catch (IOException e) {
			throw new RuntimeException("Failed to deserialize order payload", e);
		}
	}

}

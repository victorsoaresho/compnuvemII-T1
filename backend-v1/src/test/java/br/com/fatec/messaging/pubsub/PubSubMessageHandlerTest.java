package br.com.fatec.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

class PubSubMessageHandlerTest {

	private final PubSubMessageHandler handler = new PubSubMessageHandler();

	@Test
	void handle_acceptsNonEmptyPayload() {
		PubsubMessage message =
				PubsubMessage.newBuilder()
						.setMessageId("mid-1")
						.setData(ByteString.copyFromUtf8("hello"))
						.build();

		handler.handle(message, "projects/p/subscriptions/sub", "my-topic");
	}

	@Test
	void handle_rejectsEmptyPayload() {
		PubsubMessage message =
				PubsubMessage.newBuilder().setMessageId("mid-2").setData(ByteString.EMPTY).build();

		assertThatThrownBy(() -> handler.handle(message, "projects/p/subscriptions/sub", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("empty");
	}

}

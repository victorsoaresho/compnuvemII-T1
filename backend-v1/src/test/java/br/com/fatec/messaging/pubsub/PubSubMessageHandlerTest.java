package br.com.fatec.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.fatec.messaging.service.OrderPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

class PubSubMessageHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OrderPersistenceService orderPersistenceService = mock(OrderPersistenceService.class);
	private final PubSubMessageHandler handler = new PubSubMessageHandler(objectMapper, orderPersistenceService);

	@Test
	void handle_acceptsNonEmptyPayload() {
		String json = "{\"uuid\":\"ORD-1\",\"created_at\":\"2025-10-01T10:15:00Z\",\"channel\":\"web\",\"total\":100,\"status\":\"created\",\"customer\":{\"id\":1,\"name\":\"Test\",\"email\":\"t@t.com\",\"document\":\"123\"},\"seller\":{\"id\":1,\"name\":\"S\",\"city\":\"C\",\"state\":\"SP\"},\"items\":[{\"id\":1,\"product_id\":1,\"product_name\":\"P\",\"unit_price\":100,\"quantity\":1,\"category\":{\"id\":\"C1\",\"name\":\"Cat\",\"sub_category\":{\"id\":\"SC1\",\"name\":\"Sub\"}},\"total\":100}],\"shipment\":{\"carrier\":\"C\",\"service\":\"S\",\"status\":\"shipped\",\"tracking_code\":\"T1\"},\"payment\":{\"method\":\"pix\",\"status\":\"approved\",\"transaction_id\":\"pay1\"},\"metadata\":{\"source\":\"app\",\"user_agent\":\"UA\",\"ip_address\":\"1.1.1.1\"}}";
		PubsubMessage message =
				PubsubMessage.newBuilder()
						.setMessageId("mid-1")
						.setData(ByteString.copyFromUtf8(json))
						.build();

		handler.handle(message, "projects/p/subscriptions/sub", "my-topic");
		verify(orderPersistenceService).persist(any());
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

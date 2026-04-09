package br.com.fatec.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;

import br.com.fatec.messaging.pubsub.PubSubProperties;

@SpringBootApplication(exclude = { GcpContextAutoConfiguration.class, GcpPubSubAutoConfiguration.class })
@EnableConfigurationProperties(PubSubProperties.class)
public class MessagingConsumerApplication {

	public static void main(String[] args) throws InterruptedException {
		ConfigurableApplicationContext ctx = SpringApplication.run(MessagingConsumerApplication.class, args);
		// Sem servidor web, o JVM pode encerrar se só restarem threads daemon (ex.: gRPC do Pub/Sub).
		while (ctx.isActive()) {
			Thread.sleep(500);
		}
	}

}

package com.example.gcp;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@Configuration
@RestController
public class PubSubDemo {
	private final PubSubSubscriberTemplate subscriberTemplate;
	private final PubSubPublisherTemplate publisherTemplate;

	public PubSubDemo(PubSubSubscriberTemplate subscriberTemplate,
			PubSubPublisherTemplate publisherTemplate) {
		this.subscriberTemplate = subscriberTemplate;
		this.publisherTemplate = publisherTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void subscriber() throws Exception {
		subscriberTemplate.subscribe("reservations-subscription", (msg) -> {
			log.info("RECEIVED: " + msg.getPubsubMessage().getData().toStringUtf8());
			msg.ack();
		});
	}


	@GetMapping("/publish")
	String publish(@RequestParam String name) {
		publisherTemplate.publish("reservations", "reservation for " + name);
		return "reserved!";
	}
}

package com.example.bootifulgcp;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class BootifulGcpApplication {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplateBuilder()
			.build();
	}

	@Bean
	ImageAnnotatorClient imageAnnotatorClient(CredentialsProvider credentialsProvider) throws IOException {
		return ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder()
			.setCredentialsProvider(credentialsProvider)
			.build());
	}

	public static void main(String[] args) {
		SpringApplication.run(BootifulGcpApplication.class, args);
	}
}

// Spanner

@Log4j2
@Component
class SpannerDemo {

	private final ReservationRepository reservationRepository;

	SpannerDemo(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void demo() throws Exception {

		this.reservationRepository.deleteAll();

		Stream.of("Ray", "Josh", "Jisha", "Cornelia", "Madhura", "Nabil")
			.map(name -> new Reservation(UUID.randomUUID().toString(), name))
			.map(this.reservationRepository::save)
			.forEach(log::info);
	}
}

@Component
@Log4j2
class PubSubDemo {

	private final PubSubPublisherTemplate pubSubPublisherTemplate;
	private final PubSubSubscriberTemplate pubSubSubscriberTemplate;

	PubSubDemo(PubSubPublisherTemplate pubSubPublisherTemplate, PubSubSubscriberTemplate pubSubSubscriberTemplate) {
		this.pubSubPublisherTemplate = pubSubPublisherTemplate;
		this.pubSubSubscriberTemplate = pubSubSubscriberTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void demo() throws Exception {

		this.pubSubSubscriberTemplate
			.subscribe("reservations-subscription", msg -> {
				PubsubMessage pubsubMessage = msg.getPubsubMessage();
				String strMsg = pubsubMessage.getData().toStringUtf8();
				log.info("received message " + strMsg);
				msg.ack();
			});

		this.pubSubPublisherTemplate.publish("reservations", "bonjour @ "
			+ Instant.now().toString());

	}
}

@Component
@Log4j2
class VisionDemo {
	private final ImageAnnotatorClient imageAnnotatorClient;
	private final Resource cat;

	VisionDemo(
		@Value("gs://pgtm-jlong-bucket/cat.jpg") Resource cat,
		ImageAnnotatorClient imageAnnotatorClient) {
		this.cat = cat;
		this.imageAnnotatorClient = imageAnnotatorClient;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void demo() throws Exception {

		byte[] bytes = FileCopyUtils.copyToByteArray(cat.getInputStream());

		BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(Collections.singletonList(
			AnnotateImageRequest.newBuilder()
				.addFeatures(Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION))
				.setImage(Image.newBuilder().setContent(ByteString.copyFrom(bytes)))
				.build()
		));

		log.info(response);
	}
}


interface ReservationRepository extends SpannerRepository<Reservation, String> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reservations")
class Reservation {

	@Id
	@PrimaryKey
	private String id;

	@Column
	private String name;
}

// MySQL
@Component
@Log4j2
class MySqlDemo {


	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class Reservation {
		private Long id;
		private String name;
	}

	private final JdbcTemplate jdbcTemplate;

	MySqlDemo(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void demo() {
		List<Reservation> reservationList = this.jdbcTemplate.query("select * from reservations",
			(rs, rowNum) -> new Reservation(rs.getLong("id"), rs.getString("name")));
		reservationList.forEach(log::info);
	}
}

//
@RestController
@Log4j2
class GreetingsRestController {

	private final RestTemplate restTemplate;

	GreetingsRestController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/greeter")
	Collection<String> client() {
		return Stream.of("Ray", "Badhr", "Hanae")
			.map(name -> this.restTemplate.getForEntity("http://localhost:8080/salut/{name}", String.class, name))
			.map(HttpEntity::getBody)
			.collect(Collectors.toList());
	}

	@GetMapping("/salut/{name}")
	String service(@PathVariable String name) {
		log.info("greeting " + name + ".");
		return "bonjour " + name + "!";
	}
}
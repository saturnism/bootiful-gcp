package com.example.gcp;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Stream;

@RestController
@Log4j2
public class TraceDemo {
	private final RestTemplate restTemplate;

	public TraceDemo(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/hello")
	public String hello(@RequestParam String name) {
		log.info("Hello " + name);

		return "Hello " + name;
	}

	@GetMapping("/all")
	public void all() {
		log.info("Hello to all!");
		Stream.of("josh", "ray", "jisha", "madhura")
				.forEach(name -> restTemplate.getForObject("http://localhost:8080/hello?name=" + name, String.class));
	}



}

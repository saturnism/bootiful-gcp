package com.example.gcp.vision;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@SpringBootApplication
public class VisionApplication {

		// <1>
		@Bean
		ImageAnnotatorClient imageAnnotatorClient(
			CredentialsProvider credentialsProvider) throws IOException {
				ImageAnnotatorSettings settings = ImageAnnotatorSettings
					.newBuilder()
					.setCredentialsProvider(credentialsProvider)
					.build();
				return ImageAnnotatorClient.create(settings);
		}

		@Slf4j
		@RestController
		public static class ImageAnalyzerRestController {

				private final ImageAnnotatorClient client;

				private final Feature feature = Feature
					.newBuilder()
					.setType(Feature.Type.LABEL_DETECTION) // <2>
					.build();

				ImageAnalyzerRestController(ImageAnnotatorClient client) {
						this.client = client;
				}

				@PostMapping("/analyze")
				String analyze(@RequestParam MultipartFile image) throws IOException {
						// <3>
						byte[] data = image.getBytes();
						ByteString imgBytes = ByteString.copyFrom(data);
						Image img = Image.newBuilder().setContent(imgBytes).build();
						AnnotateImageRequest request = AnnotateImageRequest
							.newBuilder()
							.addFeatures(this.feature)
							.setImage(img)
							.build();
						BatchAnnotateImagesResponse responses = this.client
							.batchAnnotateImages(Collections.singletonList(request));
						AnnotateImageResponse reply = responses.getResponses(0);
						return reply.toString();
				}
		}

		public static void main(String args[]) {
				SpringApplication.run(VisionApplication.class, args);
		}
}
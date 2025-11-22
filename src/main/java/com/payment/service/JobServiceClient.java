package com.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
public class JobServiceClient {

    private final WebClient webClient;
    private final String jobServiceUrl;

    public JobServiceClient(WebClient webClient, @Value("${payment.job-service.url:mock}") String jobServiceUrl) {
        this.webClient = webClient;
        this.jobServiceUrl = jobServiceUrl;
    }

    public Mono<Boolean> validateJob(UUID jobId) {
        log.info("Validating job existence: {}", jobId);
        
        if ("mock".equalsIgnoreCase(jobServiceUrl)) {
            return Mono.just(true);
        }

        return webClient.get()
                .uri(jobServiceUrl + "/jobs/" + jobId + "/validate")
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(e -> {
                    log.error("Error validating job {}: {}", jobId, e.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> isJobComplete(UUID jobId) {
        log.info("Checking if job is complete: {}", jobId);
        
        if ("mock".equalsIgnoreCase(jobServiceUrl)) {
            return Mono.just(true);
        }

        return webClient.get()
                .uri(jobServiceUrl + "/jobs/" + jobId + "/complete")
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(e -> {
                    log.error("Error checking job completion {}: {}", jobId, e.getMessage());
                    return Mono.just(false);
                });
    }
}

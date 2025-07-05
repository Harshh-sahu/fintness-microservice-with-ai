package com.fitness.gateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(String userId) {
        log.info("Calling user validation API for userId: {}", userId);

        return userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error validating user: {}", e.getMessage(), e);
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(false); // Optional: or Mono.error(...)
                    } else {
                        return Mono.error(new RuntimeException("User validation error: " + e.getMessage()));
                    }
                });
    }

    public Mono<UserResponse> registerUser(RegisterRequest request) {
        log.info("Calling user Registration API for email: {}", request.getEmail());

        return userServiceWebClient.post()
                .uri("/api/users/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class)  // FIXED: Expect correct response type
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error registering user: {}", e.getMessage(), e);
                    if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new RuntimeException("Bad request: " + e.getResponseBodyAsString()));
                    } else if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        return Mono.error(new RuntimeException("Server error: " + e.getResponseBodyAsString()));
                    } else {
                        return Mono.error(new RuntimeException("Unexpected error: " + e.getResponseBodyAsString()));
                    }
                });
    }
}

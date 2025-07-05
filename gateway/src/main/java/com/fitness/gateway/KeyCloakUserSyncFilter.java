package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeyCloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        RegisterRequest registerRequest = getUserDetails(token);
if(userId == null){
    userId = registerRequest.getKeyCloakId();
}
        if (userId != null && token != null) {
            String finalUserId = userId;
            return userService.validateUser(userId).flatMap(exist -> {
                if (!exist) {
                    log.info("User does not exist. Sync logic can be placed here.");
                    // Register user logic can be added here

                    if(registerRequest != null){
                        return userService.registerUser(registerRequest)
                                .then(Mono.empty());
                    }else{
                        return Mono.empty();
                    }
                } else {
                    log.info("User already exists, skipping sync.");
                    return Mono.empty();
                }
            }).then(Mono.defer(() -> {
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().header("X-User-ID", finalUserId).build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }));
        }

        // If no userId or token present, proceed without any change
        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) return null;

            String tokenWithoutBearer = token.replace("Bearer ", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String keyCloakId = claims.getStringClaim("sub");
            if (keyCloakId == null) return null; // Critical: avoid null insert

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeyCloakId(keyCloakId);
            registerRequest.setPassword("dummy@123123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));
            return registerRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

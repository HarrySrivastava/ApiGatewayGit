package com.appsdeveloper.photoapp.api.gateway;



import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class JwtAuthorizationHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory
        <JwtAuthorizationHeaderGatewayFilterFactory.Config> {
    @Autowired
    Environment env;

    public JwtAuthorizationHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request= exchange.getRequest();
            if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
            {
                return onError(exchange,"No Authorization header found", HttpStatus.UNAUTHORIZED);

            }
            String  authorizationHeader=request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt=authorizationHeader.replace("Bearer","");

            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };

    }


    private boolean isJwtValid(String jwt)
    {
        boolean isValid=true;
        env.getProperty("token.secret");
        boolean returnValue = true;
        String subject = null;
        String tokenSecret = env.getProperty("token.secret");
        byte[] secretKeyBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
        SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
        try {
            Jws<Claims> parsedToken = jwtParser.parseClaimsJws(jwt);
            subject = parsedToken.getBody().getSubject();
        } catch(Exception e) {
            returnValue =  false;
        }

        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }
        return isValid;
    }



    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus)
    {
        ServerHttpResponse response= exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();

    }

    public static class Config
    {

    }

}


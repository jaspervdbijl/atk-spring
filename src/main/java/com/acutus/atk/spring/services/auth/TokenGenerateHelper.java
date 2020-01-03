package com.acutus.atk.spring.services.auth;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Component
public class TokenGenerateHelper {

    @Autowired
    AbstractServerKeys keys;

    @Value("${token.expiry_time:31536000}")
    private int expiryTime;

    public LocalDateTime getExpiryDate() {
        return LocalDateTime.now().plusSeconds(expiryTime);
    }

    public String generate(Map<String, Object> claims) {
        DefaultJwtBuilder builder = new DefaultJwtBuilder();
        builder.setHeaderParam("alg","RS256");

        builder.setIssuedAt(new Date());
        builder.setExpiration(Timestamp.valueOf(getExpiryDate()));
        builder.signWith(SignatureAlgorithm.RS256,keys.getPrivateKey());
        builder.addClaims(claims);
        return builder.compact();
    }


}

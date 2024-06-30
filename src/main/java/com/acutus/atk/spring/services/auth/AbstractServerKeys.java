package com.acutus.atk.spring.services.auth;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public abstract class AbstractServerKeys {
    @Getter
    private KeyPair keyPair;
    @Getter
    private PublicKey publicKey;
    @Getter
    private PrivateKey privateKey;

    @PostConstruct
    @SneakyThrows
    public void init() {
        privateKey = Security.generateRSAPrivateKey(getPrivateKeyStream());

        publicKey = Security.generateRSAPublicKey(getPublicKeyStream());

        keyPair = new KeyPair(publicKey,privateKey);
    }

    protected abstract InputStream getPublicKeyStream();
    protected abstract InputStream getPrivateKeyStream();
}

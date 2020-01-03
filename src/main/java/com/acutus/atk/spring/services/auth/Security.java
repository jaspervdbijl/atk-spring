package com.acutus.atk.spring.services.auth;


import com.acutus.atk.io.IOUtil;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by jaspervdb on 2016/07/28.
 */
public class Security {

    public static PublicKey generateRSAPublicKey(InputStream is) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(IOUtil.readAvailable(is)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PrivateKey generateRSAPrivateKey(InputStream is) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(IOUtil.readAvailable(is)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}

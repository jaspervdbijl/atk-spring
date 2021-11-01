package com.acutus.atk.spring.services.auth;


import com.acutus.atk.io.IOUtil;
import com.acutus.atk.util.StringUtils;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.*;
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

    @SneakyThrows
    private static Signature getSignature(KeyPair keyPair) {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(keyPair.getPrivate());
        return sig;
    }

    @SneakyThrows
    public static byte[] signData(KeyPair keyPair, byte data[]) {
        Signature sig = getSignature(keyPair);
        sig.update(data);
        return sig.sign();
    }

    @SneakyThrows
    public static void verifySignedData(KeyPair keyPair, byte data[], byte signatureBytes[]) {
        Signature sig = getSignature(keyPair);
        sig.initVerify(keyPair.getPublic());
        sig.update(data);

        sig.verify(signatureBytes);
    }

    @SneakyThrows
    public static String md5Hash(byte data[]) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        byte[] digest = md.digest();
        return StringUtils.bytesToHex(digest).toUpperCase();
    }

}

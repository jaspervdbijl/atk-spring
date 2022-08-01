package com.acutus.atk.spring.services.auth;


import com.acutus.atk.io.IOUtil;
import com.acutus.atk.util.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jaspervdb on 2016/07/28.
 */
@Component
public class Security {

    @Value("${password.strength.regex:^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])(?=\\S+$).{8,40}$")
    private String passportStrengthRegEx = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])(?=\\S+$).{8,40}$";

    @Getter
    @Value("${password.strength.message:password is at least 8 characters long. Has at least one special, one uppercase and one lowercase character}")
    private String passportStrengthMsg;

    private Pattern passwordPattern = Pattern.compile(passportStrengthRegEx);

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

    public boolean validatePasswordStrength(String password) {
        return passwordPattern.matcher(password).matches();
    }

}

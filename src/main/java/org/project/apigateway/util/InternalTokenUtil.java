package org.project.apigateway.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class InternalTokenUtil {

    private static final String HMAC_ALGO = "HmacSHA256";

    public static String generateToken(String secret) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String payload = "ts=" + timestamp;

            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String token = payload + "&sig=" + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate internal token", e);
        }
    }

    public static boolean validateToken(String token, String secret, long allowedWindowSeconds) {
        try {
            String[] parts = token.split("&sig=");
            if (parts.length != 2) return false;

            String payload = parts[0];
            String signature = parts[1];

            // Verify HMAC
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] expectedSig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSigBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSig);
            if (!expectedSigBase64.equals(signature)) return false;

            // Verify timestamp
            long ts = Long.parseLong(payload.replace("ts=", ""));
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - ts) <= allowedWindowSeconds;

        } catch (Exception e) {
            return false;
        }
    }
}

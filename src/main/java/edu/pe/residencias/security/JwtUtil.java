package edu.pe.residencias.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Base64;
import java.security.SecureRandom;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.aes.key}")
    private String aesKey;

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24h

    public String generateToken(UUID userId, String username, String roleName) throws Exception {
        // Simpler token: include plain claims for uid, user and role (JWT is signed).
        Date now = Date.from(Instant.now());
        Date exp = Date.from(Instant.now().plusMillis(EXPIRATION_MS));

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject("auth")
                .claim("uid", userId.toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256);

        if (username != null)
            builder.claim("user", username);
        if (roleName != null)
            builder.claim("role", roleName);

        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract UUID from JWT token
     */
    public UUID extractUUID(String token) {
        Claims claims = parseToken(token);
        String uid = claims.get("uid", String.class);
        return UUID.fromString(uid);
    }

    /**
     * Generic AES encrypt method (IV + ciphertext, Base64 encoded)
     */
    public String encrypt(String text) throws Exception {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Generic AES decrypt method
     */
    public String decrypt(String encrypted) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encrypted);
        byte[] iv = new byte[16];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        byte[] cipherText = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        byte[] dec = cipher.doFinal(cipherText);
        return new String(dec, StandardCharsets.UTF_8);
    }

    // Backwards-compatible names
    public String encryptUuid(String uuid) throws Exception {
        return encrypt(uuid);
    }

    public String decryptUuid(String encrypted) throws Exception {
        return decrypt(encrypted);
    }
}

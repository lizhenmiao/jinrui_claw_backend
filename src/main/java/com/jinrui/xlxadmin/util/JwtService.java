package com.jinrui.xlxadmin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final byte[] secret;
  private final long ttlSeconds;
  private final ObjectMapper objectMapper;

  public JwtService(@Value("${xlx.jwt-secret}") String secret,
                    @Value("${xlx.jwt-ttl-seconds}") long ttlSeconds,
                    ObjectMapper objectMapper) {
    this.secret = sha256(secret == null ? "" : secret);
    this.ttlSeconds = ttlSeconds <= 0 ? 86400 : ttlSeconds;
    this.objectMapper = objectMapper;
  }

  public long ttlSeconds() {
    return ttlSeconds;
  }

  public String createAdminToken(String username) {
    try {
      long now = Instant.now().getEpochSecond();
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("alg", "HS256");
      header.put("typ", "JWT");

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("sub", username);
      payload.put("role", "admin");
      payload.put("iat", now);
      payload.put("exp", now + ttlSeconds);

      String headerPart = base64Url(objectMapper.writeValueAsBytes(header));
      String payloadPart = base64Url(objectMapper.writeValueAsBytes(payload));
      String signingInput = headerPart + "." + payloadPart;
      return signingInput + "." + base64Url(hmac(signingInput));
    } catch (Exception e) {
      throw new IllegalStateException("JWT签发失败", e);
    }
  }

  public boolean verifyAdminToken(String token) {
    try {
      if (token == null || token.isBlank()) return false;
      String[] parts = token.split("\\.");
      if (parts.length != 3) return false;
      String signingInput = parts[0] + "." + parts[1];
      String expected = base64Url(hmac(signingInput));
      if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
        return false;
      }
      Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);
      if (!"admin".equals(String.valueOf(payload.get("role")))) return false;
      long exp = longValue(payload.get("exp"));
      return exp > Instant.now().getEpochSecond();
    } catch (Exception e) {
      return false;
    }
  }

  public String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null) return "";
    String value = authorizationHeader.trim();
    if (!value.regionMatches(true, 0, "Bearer ", 0, 7)) return "";
    return value.substring(7).trim();
  }

  private byte[] hmac(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("sha256 failed", e);
    }
  }

  private static long longValue(Object value) {
    if (value instanceof Number number) return number.longValue();
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
      return 0;
    }
  }
}

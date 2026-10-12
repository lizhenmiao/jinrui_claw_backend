package com.jinrui.xlxadmin.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptoService {
  private final byte[] key;
  private final SecureRandom random = new SecureRandom();

  public CryptoService(@Value("${xlx.crypto-secret}") String secret) {
    this.key = sha256(secret == null ? "" : secret);
  }

  public String encrypt(String plain) {
    if (plain == null || plain.isBlank()) return "";
    try {
      byte[] iv = new byte[12];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new IllegalStateException("encrypt failed", e);
    }
  }

  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.isBlank()) return "";
    try {
      byte[] combined = Base64.getDecoder().decode(encrypted);
      byte[] iv = Arrays.copyOfRange(combined, 0, 12);
      byte[] body = Arrays.copyOfRange(combined, 12, combined.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "";
    }
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("sha256 failed", e);
    }
  }
}

package io.github.andyjjt123.cda.fhe;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MockFhe {
  // 把明文包成 fake 密文（base64 + 前綴）
  public static byte[] encrypt(String plain) {
    String tagged = "[enc]" + plain;
    return Base64.getEncoder().encode(tagged.getBytes(StandardCharsets.UTF_8));
  }

  // 模擬在密文上做運算（這裡只做個反轉＋再次包裝）
  public static byte[] eval(byte[] cipher) {
    String s = new String(Base64.getDecoder().decode(cipher), StandardCharsets.UTF_8);
    String body = s.replaceFirst("^\\[enc\\]", "");
    String reversed = new StringBuilder(body).reverse().toString();
    String tagged = "[res]" + reversed;
    return Base64.getEncoder().encode(tagged.getBytes(StandardCharsets.UTF_8));
  }

  // 解密（去前綴並還原）
  public static String decrypt(byte[] cipher) {
    String s = new String(Base64.getDecoder().decode(cipher), StandardCharsets.UTF_8);
    return s.replaceFirst("^\\[(enc|res)\\]", "");
  }
}

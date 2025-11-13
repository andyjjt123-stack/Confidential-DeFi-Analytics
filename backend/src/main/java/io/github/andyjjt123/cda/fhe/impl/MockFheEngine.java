package io.github.andyjjt123.cda.fhe.impl;

import io.github.andyjjt123.cda.fhe.FheEngine;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MockFheEngine implements FheEngine {
  @Override
  public byte[] encrypt(String plain) {
    String tagged = "[enc]" + plain;
    return Base64.getEncoder().encode(tagged.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public byte[] evaluate(byte[] cipher) {
    String s = new String(Base64.getDecoder().decode(cipher), StandardCharsets.UTF_8);
    String body = s.replaceFirst("^\\[enc\\]", "");
    String reversed = new StringBuilder(body).reverse().toString();
    String tagged = "[res]" + reversed;
    return Base64.getEncoder().encode(tagged.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String decrypt(byte[] cipher) {
    String s = new String(Base64.getDecoder().decode(cipher), StandardCharsets.UTF_8);
    return s.replaceFirst("^\\[(enc|res)\\]", "");
  }
}

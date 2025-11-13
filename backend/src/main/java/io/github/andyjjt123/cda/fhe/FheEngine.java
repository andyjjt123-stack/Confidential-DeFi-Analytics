package io.github.andyjjt123.cda.fhe;

public interface FheEngine {
  /** 明文 -> 密文(bytes) */
  byte[] encrypt(String plain);

  /** 在密文上進行保護運算 -> 結果密文(bytes) */
  byte[] evaluate(byte[] cipher);

  /** 解密密文 -> 明文 */
  String decrypt(byte[] cipher);
}

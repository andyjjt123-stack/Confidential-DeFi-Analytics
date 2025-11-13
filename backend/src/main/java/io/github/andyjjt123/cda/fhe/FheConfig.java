package io.github.andyjjt123.cda.fhe;

import io.github.andyjjt123.cda.fhe.impl.MockFheEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FheConfig {
  @Bean
  public FheEngine fheEngine() {
    // 之後要換 Zama/TFHE 實作，只要改這裡即可
    return new MockFheEngine();
  }
}

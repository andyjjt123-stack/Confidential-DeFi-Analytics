package io.github.andyjjt123.cda.controller;

import io.github.andyjjt123.cda.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.github.andyjjt123.cda.fhe.FheEngine;

import java.util.Map;

@RestController
@RequestMapping("/fhe")
@RequiredArgsConstructor
public class FheFlowController {

  private final ContractService svc;
  private final FheEngine engine;

  /** 明文 -> 假密文(hex) */
  @PostMapping("/encrypt")
  public Map<String,Object> encrypt(@RequestParam String plain){
    byte[] c = engine.encrypt(plain);
    String hex = "0x" + bytesToHex(c);
    return Map.of("cipherHex", hex);
  }

  /** 讀鏈上 metric -> 假運算 -> 回寫 result -> 回傳 txHash */
  @PostMapping("/eval-and-post")
  public Map<String,Object> evalAndPost() throws Exception {
    byte[] metric = svc.getMyMetric();                // 取後端地址的 metric
    byte[] resultCipher = engine.evaluate(metric);       // 假運算
    String tx = svc.postEncryptedResult(resultCipher);// 回寫到鏈上
    return Map.of("txHash", tx);
  }

  /** 讀鏈上的 result -> 假解密（返回明文） */
  @GetMapping("/decrypt-result")
  public Map<String,Object> decryptResult() throws Exception {
    String hex = svc.getMyEncryptedResult();          // 0x....
    byte[] bytes = hexToBytes(hex);
    String plain = engine.decrypt(bytes);
    return Map.of("plain", plain, "cipherHex", hex);
  }

  private static String bytesToHex(byte[] b){
    StringBuilder sb = new StringBuilder();
    for(byte x: b) sb.append(String.format("%02x", x));
    return sb.toString();
  }
  private static byte[] hexToBytes(String hex){
    hex = hex.startsWith("0x")? hex.substring(2):hex;
    int len = hex.length(); byte[] data = new byte[len/2];
    for(int i=0;i<len;i+=2) data[i/2]=(byte)((Character.digit(hex.charAt(i),16)<<4)+Character.digit(hex.charAt(i+1),16));
    return data;
  }
}

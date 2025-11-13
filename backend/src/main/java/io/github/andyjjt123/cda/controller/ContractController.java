package io.github.andyjjt123.cda.controller;

import io.github.andyjjt123.cda.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/vault")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService svc;

    /** 寫入 submitMetric(bytes) */
    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestParam String cipherHex) throws Exception {
        String tx = svc.submitEncryptedMetric(cipherHex);
        return Map.of("ok", true, "txHash", tx);
    }

    /** 讀取 getMyEncryptedResult() */
    @GetMapping("/result")
    public Map<String, Object> result() throws Exception {
        String hex = svc.getMyEncryptedResult();
        return Map.of("encryptedResult", hex);
    }
}

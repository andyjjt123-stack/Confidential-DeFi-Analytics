package io.github.andyjjt123.cda.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;

    /**
     * 啟動時只做「嘗試性」查詢，不可以讓它丟 Exception，
     * 不然 Spring 會把整個 bean 當成初始化失敗。
     */
    @PostConstruct
    public void logSenderInfo() {
        try {
            BigInteger balance = web3j.ethGetBalance(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
            ).send().getBalance();

            log.info(
                "CDA backend sender address = {}, chainId = {}, on-chain balance = {} wei",
                credentials.getAddress(), chainId, balance
            );
            log.info("Target contract address = {}", contractAddress);
        } catch (Exception e) {
            // 只寫 warning，不要讓整個 application 起不來
            log.warn(
                "Failed to query on-chain balance during startup. " +
                "address = {}, chainId = {}, reason = {}",
                credentials.getAddress(), chainId, e.getMessage()
            );
        }
    }

    /**
     * 共用：送出一個寫鏈上的合約 Function，回傳 txHash
     */
    private String sendFunctionTx(Function fn) throws Exception {
        String data = FunctionEncoder.encode(fn);

        BigInteger balance = web3j.ethGetBalance(
            credentials.getAddress(),
            DefaultBlockParameterName.LATEST
        ).send().getBalance();

        log.info(
            "Sending tx from {} , current on-chain balance = {} wei",
            credentials.getAddress(), balance
        );

        RawTransactionManager txMgr =
            new RawTransactionManager(web3j, credentials, chainId);

        EthSendTransaction tx = txMgr.sendTransaction(
            DefaultGasProvider.GAS_PRICE,
            DefaultGasProvider.GAS_LIMIT,
            contractAddress,
            data,
            BigInteger.ZERO
        );

        if (tx.hasError()) {
            String msg = tx.getError() != null ? tx.getError().getMessage() : "unknown error";
            String lower = msg.toLowerCase();

            if (lower.contains("insufficient funds")) {
                throw new IllegalStateException(String.format(
                    "Insufficient funds for sender %s. " +
                        "on-chain balance = %s wei, gasPrice = %s, gasLimit = %s, rawMsg = %s",
                    credentials.getAddress(),
                    balance.toString(),
                    DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT,
                    msg
                ));
            }

            if (lower.contains("invalid nonce") || lower.contains("tx already in mempool")) {
                throw new IllegalStateException("On-chain nonce error: " + msg);
            }

            throw new RuntimeException("On-chain tx failed: " + msg);
        }

        return tx.getTransactionHash();
    }

    /** submitMetric(bytes) 寫入鏈上 */
    public String submitEncryptedMetric(String cipherHex) throws Exception {
        byte[] bytes = Numeric.hexStringToByteArray(
            Numeric.cleanHexPrefix(cipherHex)
        );

        Function fn = new Function(
            "submitMetric",
            Collections.singletonList(new DynamicBytes(bytes)),
            Collections.emptyList()
        );

        return sendFunctionTx(fn);
    }

    /** getMyEncryptedResult() 讀鏈上回傳的 bytes（以 0x..hex 回傳） */
    public String getMyEncryptedResult() throws Exception {
        Function fn = new Function(
            "getMyEncryptedResult",
            Collections.emptyList(),
            Collections.singletonList(new TypeReference<DynamicBytes>() {})
        );

        String data = FunctionEncoder.encode(fn);

        Transaction callTx = Transaction.createEthCallTransaction(
            credentials.getAddress(),
            contractAddress,
            data
        );

        EthCall resp = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
        if (resp.isReverted()) {
            throw new RuntimeException("Call reverted: " + resp.getRevertReason());
        }

        List<Type> decoded = FunctionReturnDecoder.decode(
            resp.getValue(),
            fn.getOutputParameters()
        );

        if (decoded.isEmpty()) {
            return "0x";
        }

        byte[] out = ((DynamicBytes) decoded.get(0)).getValue();
        return Numeric.toHexString(out);
    }

    /** 從合約讀取自己（後端帳號）最近一次提交的 metric（bytes） */
    public byte[] getMyMetric() throws Exception {
        Function fn = new Function(
            "getMyMetric",
            Collections.emptyList(),
            Arrays.asList(
                new TypeReference<DynamicBytes>() {},
                new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}
            )
        );

        String data = FunctionEncoder.encode(fn);

        Transaction callTx = Transaction.createEthCallTransaction(
            credentials.getAddress(),
            contractAddress,
            data
        );

        EthCall resp = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
        if (resp.isReverted()) {
            throw new RuntimeException("Call reverted: " + resp.getRevertReason());
        }

        List<Type> out = FunctionReturnDecoder.decode(
            resp.getValue(),
            fn.getOutputParameters()
        );

        if (out.isEmpty()) {
            return new byte[0];
        }

        return ((DynamicBytes) out.get(0)).getValue();
    }

    /** 把加密「結果」寫回鏈上，供 getMyEncryptedResult() 讀取 */
    public String postEncryptedResult(byte[] resultCipher) throws Exception {
        Function fn = new Function(
            "postEncryptedResult",
            Arrays.asList(
                new Address(credentials.getAddress()),
                new DynamicBytes(resultCipher)
            ),
            Collections.emptyList()
        );

        return sendFunctionTx(fn);
    }
}

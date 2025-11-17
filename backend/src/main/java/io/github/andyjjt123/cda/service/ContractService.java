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
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
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

    // 統一使用的 gas 設定（你也可以之後改成自訂數值）
    private static final BigInteger GAS_PRICE = DefaultGasProvider.GAS_PRICE;
    private static final BigInteger GAS_LIMIT = DefaultGasProvider.GAS_LIMIT;

    /**
     * 由後端自行管理的「最後一次使用的 nonce」。
     * - 第一次送交易時會從鏈上讀取 pending nonce
     * - 之後每次在 synchronized 區塊中遞增，避免自己重複用同一個 nonce
     */
    private BigInteger lastUsedNonce = null;

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
     *
     * 這裡改成：
     *  1) 先查 eth_getTransactionCount(..., PENDING) 取得鏈上 pending nonce
     *  2) 在 synchronized 區塊裡用 lastUsedNonce 做本地遞增，避免重複 nonce
     *  3) 自己組 RawTransaction + 簽名 + eth_sendRawTransaction
     */
    private String sendFunctionTx(Function fn) throws Exception {
        String data = FunctionEncoder.encode(fn);

        // 先查餘額 (for log & insufficient-funds 訊息)
        BigInteger balance = web3j.ethGetBalance(
            credentials.getAddress(),
            DefaultBlockParameterName.LATEST
        ).send().getBalance();

        log.info(
            "Sending tx from {} , current on-chain balance = {} wei",
            credentials.getAddress(), balance
        );

        // 1. 讀鏈上的 pending nonce
        EthGetTransactionCount txCountResp = web3j.ethGetTransactionCount(
            credentials.getAddress(),
            DefaultBlockParameterName.PENDING
        ).send();
        BigInteger onChainPendingNonce = txCountResp.getTransactionCount();

        BigInteger nonceToUse;
        synchronized (this) {
            if (lastUsedNonce == null) {
                // 第一次或被 reset 過：直接跟鏈同步
                nonceToUse = onChainPendingNonce;
            } else {
                // 如果鏈上的 pending nonce 比我們記錄的大，代表有外部/其他 tx，
                // 以鏈上的為準，避免落後
                if (onChainPendingNonce.compareTo(lastUsedNonce) > 0) {
                    nonceToUse = onChainPendingNonce;
                } else {
                    // 否則在本機上遞增，避免重複 nonce
                    nonceToUse = lastUsedNonce.add(BigInteger.ONE);
                }
            }
            lastUsedNonce = nonceToUse;
        }

        log.info("Using nonce = {} for new tx (on-chain pending nonce = {})",
                 nonceToUse, onChainPendingNonce);

        // 2. 組 raw transaction
        RawTransaction rawTx = RawTransaction.createTransaction(
            nonceToUse,
            GAS_PRICE,
            GAS_LIMIT,
            contractAddress,
            BigInteger.ZERO,
            data
        );

        // 3. 簽名
        byte[] signed = TransactionEncoder.signMessage(rawTx, chainId, credentials);
        String hexTx = Numeric.toHexString(signed);

        // 4. 發送 raw tx
        EthSendTransaction tx = web3j.ethSendRawTransaction(hexTx).send();

        if (tx.hasError()) {
            String msg = tx.getError() != null ? tx.getError().getMessage() : "unknown error";
            String lower = msg.toLowerCase();

            // 如果是 nonce 類錯誤，先 reset lastUsedNonce，讓下一次重新跟鏈同步
            if (lower.contains("tx already in mempool") || lower.contains("invalid nonce")) {
                log.warn(
                    "Nonce-related error for address {}. msg = {}. " +
                    "Resetting lastUsedNonce (was = {}).",
                    credentials.getAddress(),
                    msg,
                    lastUsedNonce
                );
                synchronized (this) {
                    lastUsedNonce = null;
                }
                throw new IllegalStateException(
                    "On-chain nonce error (another tx still pending or external tx changed nonce). " +
                    "Please wait for confirmation and retry."
                );
            }

            if (lower.contains("insufficient funds")) {
                throw new IllegalStateException(String.format(
                    "Insufficient funds for sender %s. " +
                        "on-chain balance = %s wei, gasPrice = %s, gasLimit = %s, rawMsg = %s",
                    credentials.getAddress(),
                    balance.toString(),
                    GAS_PRICE,
                    GAS_LIMIT,
                    msg
                ));
            }

            // 其他錯誤
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

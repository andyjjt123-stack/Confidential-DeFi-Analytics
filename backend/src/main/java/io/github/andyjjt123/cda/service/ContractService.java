package io.github.andyjjt123.cda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Address;
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

@Service
@RequiredArgsConstructor
public class ContractService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;

    // ==========================
    // helpers
    // ==========================

    /** 共用的 sendTransaction 錯誤處理 */
    private void handleSendError(EthSendTransaction tx, String actionLabel) {
        if (!tx.hasError()) {
            return;
        }
        String rawMsg = tx.getError() != null ? tx.getError().getMessage() : "unknown error";

        // 特別處理「tx already in mempool」
        String lower = rawMsg.toLowerCase();
        if (lower.contains("tx already in mempool")) {
            // 給前端一個比較友善、可辨識的訊息
            throw new RuntimeException(
                actionLabel + " failed: TX already in mempool, please wait for confirmation before retry."
            );
        }

        // 其它錯誤原樣包起來
        throw new RuntimeException(actionLabel + " failed: " + rawMsg);
    }

    // ==========================
    // 1) submitMetric(bytes) 寫入鏈上
    // ==========================
    public String submitEncryptedMetric(String cipherHex) throws Exception {
        if (cipherHex == null || cipherHex.isEmpty()) {
            throw new IllegalArgumentException("cipherHex must not be empty");
        }

        // 去除 0x，轉成 bytes
        byte[] bytes = Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(cipherHex));

        Function fn = new Function(
            "submitMetric",
            Arrays.asList(new DynamicBytes(bytes)),
            Collections.emptyList()
        );
        String data = FunctionEncoder.encode(fn);

        RawTransactionManager txMgr = new RawTransactionManager(web3j, credentials, chainId);
        EthSendTransaction tx = txMgr.sendTransaction(
            DefaultGasProvider.GAS_PRICE,
            DefaultGasProvider.GAS_LIMIT,
            contractAddress,
            data,
            BigInteger.ZERO
        );

        // 統一在這裡做錯誤處理（含 tx already in mempool）
        handleSendError(tx, "submitMetric");

        return tx.getTransactionHash();
    }

    // ==========================
    // 2) getMyEncryptedResult() 讀取鏈上回傳的 bytes（0x..hex）
    // ==========================
    public String getMyEncryptedResult() throws Exception {
        Function fn = new Function(
            "getMyEncryptedResult",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<DynamicBytes>() {})
        );
        String data = FunctionEncoder.encode(fn);

        Transaction callTx = Transaction.createEthCallTransaction(
            credentials.getAddress(), contractAddress, data
        );

        EthCall resp = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();

        if (resp.hasError()) {
            String msg = resp.getError() != null ? resp.getError().getMessage() : "unknown error";
            throw new RuntimeException("getMyEncryptedResult call error: " + msg);
        }

        if (resp.isReverted()) {
            String reason = resp.getRevertReason() != null ? resp.getRevertReason() : "no revert reason";
            throw new RuntimeException("getMyEncryptedResult reverted: " + reason);
        }

        List<Type> decoded = FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
        if (decoded.isEmpty()) return "0x";
        byte[] out = ((DynamicBytes) decoded.get(0)).getValue();
        return Numeric.toHexString(out);
    }

    // ==========================
    // 3) 從合約讀取自己（後端帳號）最近一次提交的 metric（bytes）
    // ==========================
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
            credentials.getAddress(), contractAddress, data
        );

        EthCall resp = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();

        if (resp.hasError()) {
            String msg = resp.getError() != null ? resp.getError().getMessage() : "unknown error";
            throw new RuntimeException("getMyMetric call error: " + msg);
        }

        if (resp.isReverted()) {
            String reason = resp.getRevertReason() != null ? resp.getRevertReason() : "no revert reason";
            throw new RuntimeException("getMyMetric reverted: " + reason);
        }

        List<Type> out = FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
        if (out.isEmpty()) return new byte[0];

        return ((DynamicBytes) out.get(0)).getValue();
    }

    // ==========================
    // 4) 把加密「結果」寫回鏈上，供 getMyEncryptedResult() 讀取
    // ==========================
    public String postEncryptedResult(byte[] resultCipher) throws Exception {
        if (resultCipher == null) {
            throw new IllegalArgumentException("resultCipher must not be null");
        }

        Function fn = new Function(
            "postEncryptedResult",
            Arrays.asList(new Address(credentials.getAddress()), new DynamicBytes(resultCipher)),
            Collections.emptyList()
        );

        String data = FunctionEncoder.encode(fn);

        RawTransactionManager txMgr = new RawTransactionManager(web3j, credentials, chainId);
        EthSendTransaction tx = txMgr.sendTransaction(
            DefaultGasProvider.GAS_PRICE,
            DefaultGasProvider.GAS_LIMIT,
            contractAddress,
            data,
            BigInteger.ZERO
        );

        // 一樣透過共用邏輯處理錯誤
        handleSendError(tx, "postEncryptedResult");

        return tx.getTransactionHash();
    }
}

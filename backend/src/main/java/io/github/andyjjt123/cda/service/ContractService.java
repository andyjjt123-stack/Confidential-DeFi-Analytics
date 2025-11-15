package io.github.andyjjt123.cda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
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

    /**
     * 共用：送出一個寫鏈上的合約 Function，回傳 txHash
     */
    private String sendFunctionTx(Function fn) throws Exception {
        String data = FunctionEncoder.encode(fn);

        // 每次呼叫建立一個 RawTransactionManager（內部會自己去抓 nonce）
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

            // 把常見的 nonce 類錯誤，用比較明確的訊息丟出去
            String lower = msg.toLowerCase();
            if (lower.contains("invalid nonce") || lower.contains("tx already in mempool")) {
                // 這邊用 IllegalStateException，之後你在 Controller 可以依需要包成 400/409 等回前端
                throw new IllegalStateException("On-chain nonce error: " + msg);
            }

            throw new RuntimeException("On-chain tx failed: " + msg);
        }

        return tx.getTransactionHash();
    }

    /** submitMetric(bytes) 寫入鏈上 */
    public String submitEncryptedMetric(String cipherHex) throws Exception {
        // 去除 0x，轉成 bytes
        byte[] bytes = Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(cipherHex));

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

package io.github.andyjjt123.cda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.DynamicBytes;
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

import org.web3j.abi.datatypes.Address;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;

    /** submitMetric(bytes) 寫入鏈上 */
    public String submitEncryptedMetric(String cipherHex) throws Exception {
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
        if (tx.hasError()) throw new RuntimeException("Submit failed: " + tx.getError().getMessage());
        return tx.getTransactionHash();
    }

    /** getMyEncryptedResult() 讀鏈上回傳的 bytes（以 0x..hex 回傳） */
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
        if (resp.isReverted()) throw new RuntimeException("Call reverted: " + resp.getRevertReason());

        List<Type> decoded = FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
        if (decoded.isEmpty()) return "0x";
        byte[] out = ((DynamicBytes) decoded.get(0)).getValue();
        return Numeric.toHexString(out);
    }
	
	/** 從合約讀取自己（後端帳號）最近一次提交的 metric（bytes） */
    public byte[] getMyMetric() throws Exception {
        Function fn = new Function("getMyMetric",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<DynamicBytes>() {}, new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}));
        String data = FunctionEncoder.encode(fn);
        Transaction callTx = Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, data);
        EthCall resp = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
        List<Type> out = FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
        if (out.isEmpty()) return new byte[0];
        return ((DynamicBytes) out.get(0)).getValue();
    }

    /** 把加密「結果」寫回鏈上，供 getMyEncryptedResult() 讀取 */
    public String postEncryptedResult(byte[] resultCipher) throws Exception {
        Function fn = new Function("postEncryptedResult",
                Arrays.asList(new Address(credentials.getAddress()), new DynamicBytes(resultCipher)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);
        RawTransactionManager txMgr = new RawTransactionManager(web3j, credentials, chainId);
        EthSendTransaction tx = txMgr.sendTransaction(
                DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT,
                contractAddress, data, BigInteger.ZERO);
        if (tx.hasError()) throw new RuntimeException(tx.getError().getMessage());
        return tx.getTransactionHash();
    }
}

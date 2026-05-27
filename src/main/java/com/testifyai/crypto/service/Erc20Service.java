package com.testifyai.crypto.service;

import com.testifyai.crypto.config.CryptoProperties;
import com.testifyai.crypto.util.TokenAmountConverter;
import org.web3j.abi.FunctionReturnDecoder;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Service
public class Erc20Service {

    private final Web3j web3j;
    private final CryptoProperties cryptoProperties;
    private final TransactionManager transactionManager;
    private final ContractGasProvider gasProvider;

    public Erc20Service(
            Web3j web3j,
            CryptoProperties cryptoProperties,
            TransactionManager transactionManager,
            ContractGasProvider gasProvider
    ) {
        this.web3j = web3j;
        this.cryptoProperties = cryptoProperties;
        this.transactionManager = transactionManager;
        this.gasProvider = gasProvider;
    }

    public TransactionReceipt approveTaiEngine(BigInteger amount) {
        try {
            Function approveFunction = new Function(
                    "approve",
                    List.of(
                            new Address(cryptoProperties.getTaiEngineAddress()),
                            new Uint256(amount)
                    ),
                    List.of(new TypeReference<Bool>() {})
            );

            String encodedFunction = FunctionEncoder.encode(approveFunction);

            EthSendTransaction ethSendTransaction = transactionManager.sendTransaction(
                    gasProvider.getGasPrice(),
                    gasProvider.getGasLimit(),
                    cryptoProperties.getEurcAddress(),
                    encodedFunction,
                    BigInteger.ZERO
            );

            if (ethSendTransaction.hasError()) {
                throw new RuntimeException(ethSendTransaction.getError().getMessage());
            }

            String transactionHash = ethSendTransaction.getTransactionHash();

            PollingTransactionReceiptProcessor receiptProcessor =
                    new PollingTransactionReceiptProcessor(
                            web3j,
                            1000,
                            40
                    );

            return receiptProcessor.waitForTransactionReceipt(transactionHash);
        } catch (Exception exception) {
            throw new RuntimeException("Could not approve EURC for TAIEngine", exception);
        }
    }

    public BigDecimal getPlatformEurcBalance() {
        return getEurcBalance(getPlatformWalletAddress());
    }

    public BigDecimal getEurcBalance(String walletAddress) {
        try {
            Function balanceOfFunction = new Function(
                    "balanceOf",
                    List.of(new Address(walletAddress)),
                    List.of(new TypeReference<Uint256>() {})
            );

            String encodedFunction = FunctionEncoder.encode(balanceOfFunction);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            walletAddress,
                            cryptoProperties.getEurcAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new RuntimeException(response.getError().getMessage());
            }

            List<Type> values = FunctionReturnDecoder.decode(
                    response.getValue(),
                    balanceOfFunction.getOutputParameters()
            );
            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }
            BigInteger balance = (BigInteger) values.get(0).getValue();
            return TokenAmountConverter.fromSmallestUnit(balance);
        } catch (Exception exception) {
            throw new RuntimeException("Could not read EURC balance", exception);
        }
    }

    public String getPlatformWalletAddress() {
        return Credentials.create(cryptoProperties.getPrivateKey()).getAddress();
    }
}

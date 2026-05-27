package com.testifyai.crypto.config;

import com.testifyai.crypto.contracts.TAIEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    @Bean
    public Web3j web3j(CryptoProperties cryptoProperties) {
        return Web3j.build(new HttpService(cryptoProperties.getRpcUrl()));
    }

    @Bean
    public Credentials platformCredentials(CryptoProperties cryptoProperties) {
        return Credentials.create(cryptoProperties.getPrivateKey());
    }

    @Bean("web3jTransactionManager")
    public TransactionManager web3jTransactionManager(
            Web3j web3j,
            Credentials platformCredentials,
            CryptoProperties cryptoProperties
    ) {
        return new RawTransactionManager(
                web3j,
                platformCredentials,
                cryptoProperties.getChainId()
        );
    }

    @Bean
    public ContractGasProvider contractGasProvider(CryptoProperties cryptoProperties) {
        return new StaticGasProvider(
                cryptoProperties.getGasPriceWei(),
                cryptoProperties.getGasLimit()
        );
    }

    @Bean
    public TAIEngine taiEngine(
            Web3j web3j,
            @Qualifier("web3jTransactionManager") TransactionManager transactionManager,
            ContractGasProvider contractGasProvider,
            CryptoProperties cryptoProperties
    ) {
        return TAIEngine.load(
                cryptoProperties.getTaiEngineAddress(),
                web3j,
                transactionManager,
                contractGasProvider
        );
    }
}

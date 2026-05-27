package com.testifyai.crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;

@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {

    private String rpcUrl;
    private String privateKey;
    private long chainId;

    private String taiCoinAddress;
    private String taiEngineAddress;
    private String eurcAddress;

    private BigInteger gasPriceWei;
    private BigInteger gasLimit;

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getTaiCoinAddress() {
        return taiCoinAddress;
    }

    public void setTaiCoinAddress(String taiCoinAddress) {
        this.taiCoinAddress = taiCoinAddress;
    }

    public String getTaiEngineAddress() {
        return taiEngineAddress;
    }

    public void setTaiEngineAddress(String taiEngineAddress) {
        this.taiEngineAddress = taiEngineAddress;
    }

    public String getEurcAddress() {
        return eurcAddress;
    }

    public void setEurcAddress(String eurcAddress) {
        this.eurcAddress = eurcAddress;
    }

    public BigInteger getGasPriceWei() {
        return gasPriceWei;
    }

    public void setGasPriceWei(BigInteger gasPriceWei) {
        this.gasPriceWei = gasPriceWei;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }
}
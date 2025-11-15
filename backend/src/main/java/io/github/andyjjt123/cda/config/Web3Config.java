package io.github.andyjjt123.cda.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure().ignoreIfMissing().load();
    }

    @Bean
    public Web3j web3j(Dotenv env) {
        return Web3j.build(new HttpService(env.get("STABLE_RPC_URL")));
    }

    @Bean
    public Credentials credentials(Dotenv env) {
        return Credentials.create(env.get("PRIVATE_KEY"));
    }

    @Bean
    public String contractAddress(Dotenv env) {
        return env.get("CONTRACT_ADDRESS");
    }

    @Bean
    public long chainId(Dotenv env) {
        String v = env.get("CHAIN_ID");
        return (v == null || v.isBlank()) ? 2201L : Long.parseLong(v);
    }
}
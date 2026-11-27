package com.dapcode.backend_core.judge0;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Judge0Config {

    @Value("${judge0.base-url:http://localhost:2358}")
    private String baseUrl;

    @Value("${judge0.connection-timeout-ms:5000}")
    private int connectionTimeoutMs;

    @Value("${judge0.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Bean
    public RestClient judge0RestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectionTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((req, body, execution) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Judge0 {} {} ({} bytes)", req.getMethod(), req.getURI(), body.length);
                    }
                    return execution.execute(req, body);
                })
                .build();
    }
}

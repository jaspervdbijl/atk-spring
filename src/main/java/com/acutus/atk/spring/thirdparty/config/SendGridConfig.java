package com.acutus.atk.spring.thirdparty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SendGridConfig {

    @Value("${sendgrid.key.post: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}")
    String key;

    @Bean("sendGridRestTemplate")
    public RestTemplate getRestTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }

    @Bean("sendGridRestTemplateHeaders")
    public HttpHeaders getRestTemplateHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(key);
        return httpHeaders;
    }
}
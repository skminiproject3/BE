package com.rookies4.MiniProject3.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.server.base-url:http://localhost:9999}")
    private String aiServerBaseUrl; // application.properties 에서 URL 주입

    @Bean
    public WebClient aiWebClient() {
        // 기본 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5초
                .responseTimeout(Duration.ofSeconds(60)) // 60초
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS)) // 60초
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)) // 60초
                );

        return WebClient.builder()
                .baseUrl(aiServerBaseUrl) // AI 서버 기본 URL 설정
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // 타임아웃 설정 적용
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 기본 헤더 (JSON 통신)
                .build();
    }
}

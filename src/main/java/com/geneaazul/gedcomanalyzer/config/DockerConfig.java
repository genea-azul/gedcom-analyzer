package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.DockerService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.time.Duration;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnProperty(name = "docker.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DockerConfig {

    @Value("${docker.host-uri:tcp://host.docker.internal:2375}")
    private String dockerHostUri;

    @Bean
    public DockerClient dockerClient() {

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHostUri)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        dockerClient.pingCmd().exec();

        return dockerClient;
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "docker.enabled", havingValue = "true")
    @RequiredArgsConstructor
    public static class SubConfig {

        private final DockerClient dockerClient;
        private final DockerService dockerService;

        @PreDestroy
        public void closeDockerClient() throws IOException {
            dockerClient.close();
        }

        @Scheduled(
                fixedRateString = "${docker.db-container.stop-when-idle.fixed-rate:120000}",
                initialDelayString = "${docker.db-container.stop-when-idle.initial-delay:360000}")
        public void scheduleDbContainerStopWhenIdle() {
            dockerService.stopDbContainerWhenIdle();
        }
    }

}

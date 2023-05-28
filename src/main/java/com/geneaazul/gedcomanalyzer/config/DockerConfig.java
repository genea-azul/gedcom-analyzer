package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.DockerService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableScheduling
@Profile({"prod", "docker"})
@RequiredArgsConstructor
public class DockerConfig {

    private static final long DB_CONTAINER_STOP_WHEN_IDLE_FIXED_RATE = 2  * 60 * 1000;
    private static final long DB_CONTAINER_STOP_WHEN_IDLE_INITIAL_DELAY = 5 * 60 * 1000;

    private final ApplicationContext applicationContext;

    @Value("${docker.hostUri:tcp://host.docker.internal:2375}")
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

    @PreDestroy
    public void closeDockerClient() throws IOException {
        applicationContext
                .getBean(DockerClient.class)
                .close();
    }

    @Scheduled(fixedRate = DB_CONTAINER_STOP_WHEN_IDLE_FIXED_RATE, initialDelay = DB_CONTAINER_STOP_WHEN_IDLE_INITIAL_DELAY)
    public void scheduleDbContainerStopWhenIdle() {
        applicationContext
                .getBean(DockerService.class)
                .stopDbContainerWhenIdle();
    }

}

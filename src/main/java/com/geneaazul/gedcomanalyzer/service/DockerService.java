package com.geneaazul.gedcomanalyzer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.SyncDockerCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<DockerClient> dockerClient;

    @Value("${docker.db-container.name:gedcom-analyzer-db}")
    private String dbContainerName;

    @Value("${docker.db-container.max-idle-time:600000}")
    private long dbContainerMaxIdleTime;

    private Instant lastStartDbContainerRequest = null;

    public void startDbContainer() {
        if (dockerClient.isEmpty()) {
            return;
        }

        lastStartDbContainerRequest = Instant.now();

        InspectContainerResponse.ContainerState containerState = executeSyncDockerCmd(
                DockerClient::inspectContainerCmd,
                dbContainerName,
                InspectContainerResponse::getState);

        if (!isContainerStarted(containerState)) {
            log.info("Starting DB container {}", dbContainerName);
            executeSyncDockerCmd(DockerClient::startContainerCmd, dbContainerName);
        }
    }

    public void stopDbContainer() {
        if (dockerClient.isEmpty()) {
            return;
        }

        InspectContainerResponse.ContainerState containerState = executeSyncDockerCmd(
                DockerClient::inspectContainerCmd,
                dbContainerName,
                InspectContainerResponse::getState);

        if (isContainerStarted(containerState)) {
            log.info("Stopping DB container {}", dbContainerName);
            executeSyncDockerCmd(DockerClient::stopContainerCmd, dbContainerName);
        }
    }

    private <T, V extends SyncDockerCmd<T>> void executeSyncDockerCmd(BiFunction<DockerClient, String, V> containerCmd, String containerName) {
        executeSyncDockerCmd(containerCmd, containerName, Function.identity());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private <T, V extends SyncDockerCmd<T>, U> U executeSyncDockerCmd(BiFunction<DockerClient, String, V> containerCmd, String containerName, Function<T, U> map) {
        try (var dockerCmd = containerCmd.apply(dockerClient.get(), containerName)) {
            return map.apply(dockerCmd.exec());
        }
    }

    public void stopDbContainerWhenIdle() {
        if (isDbContainerIdleTimeExceeded()) {
            stopDbContainer();
        }
    }

    private boolean isDbContainerIdleTimeExceeded() {
        return lastStartDbContainerRequest == null
                || lastStartDbContainerRequest.plusMillis(dbContainerMaxIdleTime).isBefore(Instant.now());
    }

    private static boolean isContainerStarted(InspectContainerResponse.ContainerState containerState) {
        return Boolean.TRUE.equals(containerState.getRunning())
                || Boolean.TRUE.equals(containerState.getRestarting());
    }

}

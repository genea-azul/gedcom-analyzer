package com.geneaazul.gedcomanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.ResponseItem;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<DockerClient> dockerClient;
    private final ExecutorService singleThreadExecutorService;

    @Value("${docker.app-container.name:gedcom-analyzer-app}")
    private String appContainerName;

    @Value("${docker.app-container.image.name:geneaazul/gedcom-analyzer:latest}")
    private String appContainerImageName;

    @Value("${docker.db-container.name:gedcom-analyzer-db}")
    private String dbContainerName;

    @Value("${docker.db-container.max-idle-time:600000}")
    private Duration dbContainerMaxIdleTime;

    @Value("${docker.image.pull-timeout:120000}")
    private Duration imagePullTimeout;

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
            log.info("Starting DB container [ name={} ]", dbContainerName);
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
            log.info("Stopping DB container [ name={} ]", dbContainerName);
            executeSyncDockerCmd(DockerClient::stopContainerCmd, dbContainerName);
        }
    }

    public void deployDockerCompose() throws InterruptedException {
        if (dockerClient.isEmpty()) {
            return;
        }

        try {
            log.info("Update Docker image [ name={} ]", appContainerImageName);
            executeAsyncDockerCmd(
                    DockerClient::pullImageCmd,
                    new PullImageResultCallback(),
                    appContainerImageName,
                    imagePullTimeout);
        } catch (NotModifiedException e) {
            log.info("Docker image not modified [ name={} ]", appContainerImageName);
        }

        log.info("Start Docker container [ name={} ]", dbContainerName);
        executeSyncDockerCmd(
                DockerClient::startContainerCmd,
                dbContainerName);

        log.info("Restart Docker container [ name={} ]", appContainerName);
        singleThreadExecutorService.submit(
                () -> executeSyncDockerCmd(
                        DockerClient::restartContainerCmd,
                        appContainerName));
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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private <C extends AsyncDockerCmd<C, R>, R extends ResponseItem, A extends ResultCallback.Adapter<R>> void executeAsyncDockerCmd(
            BiFunction<DockerClient, String, C> containerCmd,
            A callback,
            String containerName,
            Duration timeout) throws InterruptedException {
        try (var dockerCmd = containerCmd.apply(dockerClient.get(), containerName)) {
            dockerCmd
                    .exec(callback)
                    .awaitCompletion(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public void stopDbContainerWhenIdle() {
        if (isDbContainerIdleTimeExceeded()) {
            stopDbContainer();
        }
    }

    private boolean isDbContainerIdleTimeExceeded() {
        return lastStartDbContainerRequest == null
                || lastStartDbContainerRequest.plusMillis(dbContainerMaxIdleTime.toMillis()).isBefore(Instant.now());
    }

    private static boolean isContainerStarted(InspectContainerResponse.ContainerState containerState) {
        return Boolean.TRUE.equals(containerState.getRunning())
                || Boolean.TRUE.equals(containerState.getRestarting());
    }

}

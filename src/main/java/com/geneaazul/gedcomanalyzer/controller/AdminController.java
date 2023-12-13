package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.service.DockerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/docker-admin")
@RequiredArgsConstructor
public class AdminController {

    private final DockerService dockerService;

    @GetMapping("/docker-compose-deployment")
    public void deployDockerCompose() throws InterruptedException {
        log.info("Deploy Docker Compose with latest images");
        dockerService.deployDockerCompose();
    }


}

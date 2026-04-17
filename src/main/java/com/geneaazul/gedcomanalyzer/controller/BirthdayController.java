package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SimplePersonDto;
import com.geneaazul.gedcomanalyzer.service.BirthdayService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/birthday")
@RequiredArgsConstructor
public class BirthdayController {

    private final GedcomHolder gedcomHolder;
    private final BirthdayService birthdayService;
    private final GedcomAnalyzerProperties properties;

    @GetMapping("/azul-today")
    @CrossOrigin(originPatterns = {
            "http://geneaazul.com.ar:[*]",
            "https://geneaazul.com.ar:[*]",
            "http://*.geneaazul.com.ar:[*]",
            "https://*.geneaazul.com.ar:[*]",
    })
    public List<SimplePersonDto> getBirthdaysInAzulToday(HttpServletRequest request) {
        LocalDate today = LocalDate.now(properties.getZoneId());
        log.debug("Fetching Azul birthdays for today [ date={}, httpRequestId={} ]", today, request.getRequestId());
        return birthdayService.getBirthdaysInAzulToday(gedcomHolder.getGedcom(), today);
    }

}

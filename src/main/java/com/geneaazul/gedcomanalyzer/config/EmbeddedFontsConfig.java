package com.geneaazul.gedcomanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Configuration
public class EmbeddedFontsConfig {

    private static final String SNEAK_CASE_REGEX = "([^_A-Z])([A-Z])";
    private static final String SNEAK_CASE_REPLACEMENT = "$1-$2";

    @Bean
    public Map<Font, String> embeddedFonts() {
        return Arrays.stream(Font.values())
                .map(font -> Map.entry(
                        font,
                        "fonts/" + toSneakCase(font.label) + "/" + font.label + (font.variant == null ? "" : "-" + font.variant) + "." + font.type))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Font {
        ROBOTO("ttf", "Roboto", "Regular"),
        ROBOTO_BOLD("ttf", "Roboto", "Bold"),
        ROBOTO_LIGHT("ttf", "Roboto", "Light"),
        ROBOTO_LIGHT_ITALIC("ttf", "Roboto", "LightItalic"),
        EVERSON_MONO("ttf", "EversonMono");

        private final String type;
        private final String label;
        private final String variant;

        Font(String type, String label) {
            this(type, label, null);
        }

    }

    private static String toSneakCase(String value) {
        return value.replaceAll(SNEAK_CASE_REGEX, SNEAK_CASE_REPLACEMENT).toLowerCase();
    }

}

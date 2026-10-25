package com.geneaazul.gedcomanalyzer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonUuidSecretTest {

    @TempDir
    Path tempDir;

    private GedcomAnalyzerProperties properties(String configuredSecret) {
        GedcomAnalyzerProperties properties = mock(GedcomAnalyzerProperties.class);
        when(properties.getTempDir()).thenReturn(tempDir);
        when(properties.getPersonUuidSecret()).thenReturn(configuredSecret);
        return properties;
    }

    @Test
    void usesTheConfiguredSecret() {
        String configured = Base64.getEncoder().encodeToString("configured".getBytes(StandardCharsets.UTF_8));

        assertThat(new PersonUuidSecret(properties(configured)).get()).isEqualTo("configured".getBytes(StandardCharsets.UTF_8));
        assertThat(tempDir.resolve(PersonUuidSecret.FILE_NAME)).doesNotExist();
    }

    @Test
    void generatesOneSecretAndReusesItAfterARestart() throws Exception {
        byte[] first = new PersonUuidSecret(properties(null)).get();
        byte[] afterRestart = new PersonUuidSecret(properties("  ")).get();

        assertThat(first).hasSize(32).isEqualTo(afterRestart);
        assertThat(Files.readString(tempDir.resolve(PersonUuidSecret.FILE_NAME))).isEqualTo(Base64.getEncoder().encodeToString(first));
    }

}

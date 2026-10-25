package com.geneaazul.gedcomanalyzer.config;

import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Secret for {@link com.geneaazul.gedcomanalyzer.utils.PersonUuids}: the {@code person-uuid-secret}
 * property, or else a random one generated once and kept in {@code temp-dir} (the persistent
 * volume in production) so UUIDs stay the same across restarts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonUuidSecret {

    static final String FILE_NAME = ".person-uuid-secret";
    private static final int SECRET_BYTES = 32;

    private final GedcomAnalyzerProperties properties;

    private volatile byte[] secret;

    public byte[] get() {
        byte[] current = secret;
        if (current == null) {
            synchronized (this) {
                current = secret;
                if (current == null) {
                    current = resolve();
                    secret = current;
                }
            }
        }
        return current;
    }

    private byte[] resolve() {
        if (StringUtils.isNotBlank(properties.getPersonUuidSecret())) {
            return Base64.getDecoder().decode(properties.getPersonUuidSecret().strip());
        }
        Path file = properties.getTempDir().resolve(FILE_NAME);
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.toAbsolutePath().getParent());
                byte[] generated = new byte[SECRET_BYTES];
                new SecureRandom().nextBytes(generated);
                try {
                    Files.writeString(file, Base64.getEncoder().encodeToString(generated), StandardCharsets.US_ASCII,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    log.info("Generated a new person UUID secret in {}", file);
                } catch (FileAlreadyExistsException e) {
                    // another process created it meanwhile: use theirs
                }
            }
            return Base64.getDecoder().decode(Files.readString(file, StandardCharsets.US_ASCII).strip());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read or create the person UUID secret " + file, e);
        }
    }

}

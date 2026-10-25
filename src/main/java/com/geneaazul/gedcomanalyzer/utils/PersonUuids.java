package com.geneaazul.gedcomanalyzer.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Nullable;

/**
 * Person UUIDs for one loaded GEDCOM: HMAC-SHA256 of (GEDCOM version, person id) with a server secret.
 *
 * <p>They used to be {@code id * nanos} plus the epoch second of the GEDCOM file, both public via
 * {@code /metadata}, so anyone could compute every person's UUID. Stable for the same file and
 * secret (cached exports and shared links survive restarts); a new GEDCOM gets new UUIDs.
 */
public final class PersonUuids {

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Keyed and fed "<version>:" once; each UUID clones it (looking up and keying a Mac per person cost ~5% of a GEDCOM load)
    private final Mac versionedMac;

    private PersonUuids(byte[] secret, String gedcomVersion) {
        try {
            versionedMac = Mac.getInstance(ALGORITHM);
            versionedMac.init(new SecretKeySpec(secret, ALGORITHM));
            versionedMac.update(gedcomVersion.getBytes(StandardCharsets.UTF_8));
            versionedMac.update((byte) ':');
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param modifiedDateTime the GEDCOM file's modification time; {@code null} (e.g. an uploaded file)
     *                         gives UUIDs that are only valid for this parse
     */
    public static PersonUuids of(byte[] secret, @Nullable ZonedDateTime modifiedDateTime) {
        String version;
        if (modifiedDateTime != null) {
            version = modifiedDateTime.toInstant().toString();
        } else {
            byte[] random = new byte[16];
            SECURE_RANDOM.nextBytes(random);
            version = HexFormat.of().formatHex(random);
        }
        return new PersonUuids(secret, version);
    }

    public UUID uuid(int personId) {
        byte[] hash;
        try {
            hash = ((Mac) versionedMac.clone()).doFinal(ByteBuffer.allocate(Integer.BYTES).putInt(personId).array());
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        // RFC 9562 version 8 (custom) UUID from the first 16 bytes
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x80);
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
        ByteBuffer bytes = ByteBuffer.wrap(hash, 0, 16);
        return new UUID(bytes.getLong(), bytes.getLong());
    }

}

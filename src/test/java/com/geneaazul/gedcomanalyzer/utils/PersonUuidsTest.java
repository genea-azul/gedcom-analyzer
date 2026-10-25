package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonUuidsTest {

    private static final byte[] SECRET = "test-secret-test-secret-test-sec".getBytes(StandardCharsets.UTF_8);
    private static final ZonedDateTime MODIFIED = ZonedDateTime.of(2026, 9, 26, 1, 25, 22, 330140105, ZoneId.of("America/Argentina/Buenos_Aires"));

    @Test
    void sameGedcomAndSecretGiveTheSameUuids() {
        assertThat(PersonUuids.of(SECRET, MODIFIED).uuid(39)).isEqualTo(PersonUuids.of(SECRET, MODIFIED).uuid(39));
    }

    @Test
    void uuidsChangeWithTheGedcomVersionAndTheSecret() {
        UUID base = PersonUuids.of(SECRET, MODIFIED).uuid(39);

        assertThat(PersonUuids.of(SECRET, MODIFIED.plusSeconds(1)).uuid(39)).isNotEqualTo(base);
        assertThat(PersonUuids.of("another-secret".getBytes(StandardCharsets.UTF_8), MODIFIED).uuid(39)).isNotEqualTo(base);
    }

    @Test
    void uuidsNoLongerEncodeTheIdOrTheFileTimestamp() {
        UUID uuid = PersonUuids.of(SECRET, MODIFIED).uuid(39);

        // Old scheme: high bits = id * (nanos / 1000), low bits = epoch second
        assertThat(uuid.getMostSignificantBits()).isNotEqualTo(39L * (330140105 / 1000));
        assertThat(uuid.getLeastSignificantBits()).isNotEqualTo(MODIFIED.toEpochSecond());
        assertThat(uuid.version()).isEqualTo(8);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void uuidsAreDistinctAcrossManyPeople() {
        PersonUuids uuids = PersonUuids.of(SECRET, MODIFIED);
        Set<UUID> seen = new HashSet<>();
        for (int id = 1; id <= 100_000; id++) {
            assertThat(seen.add(uuids.uuid(id))).isTrue();
        }
    }

    @Test
    void withoutAModifiedTimeEachParseGetsItsOwnUuids() {
        assertThat(PersonUuids.of(SECRET, null).uuid(39)).isNotEqualTo(PersonUuids.of(SECRET, null).uuid(39));
    }

}

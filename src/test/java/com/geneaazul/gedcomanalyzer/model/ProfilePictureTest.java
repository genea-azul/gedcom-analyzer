package com.geneaazul.gedcomanalyzer.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePictureTest {

    private static final String PHOTO_PATH = "/023/419/1551/000011_00192568009i5b3aee3738_J.jpg";

    static String signedUrl(String host, String signature, long expiresAtEpochSecond) {
        String token = "k=sites_v1&s=" + signature + "&e=" + expiresAtEpochSecond;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "https://" + host + "/e/1/" + encoded + PHOTO_PATH;
    }

    @Test
    void theCacheKeyIgnoresTheSignatureThatChangesOnEveryExport() {
        ProfilePicture august = new ProfilePicture("jpg", signedUrl("sites-cf.mhcache.com", "aaaa", 1786399200L));
        ProfilePicture september = new ProfilePicture("jpg", signedUrl("sites-cf.mhcache.com", "bbbb", 1790996400L));
        ProfilePicture otherPhoto = new ProfilePicture("jpg", august.file().replace("000011_", "000012_"));

        assertThat(august.cacheKey()).isEqualTo(september.cacheKey()).hasSize(24).matches("[0-9a-f]{24}");
        assertThat(otherPhoto.cacheKey()).isNotEqualTo(august.cacheKey());
    }

    @Test
    void theExpiryIsReadFromTheSignedSegment() {
        ProfilePicture picture = new ProfilePicture("jpg", signedUrl("sites-cf.mhcache.com", "sig", 1790996400L));

        assertThat(picture.expiresAt()).contains(Instant.ofEpochSecond(1790996400L));
        assertThat(picture.isExpired(Instant.ofEpochSecond(1790996399L))).isFalse();
        assertThat(picture.isExpired(Instant.ofEpochSecond(1790996400L))).isTrue();
    }

    @Test
    void decodesARealMyHeritageSegment() {
        // From an actual export: "k=sites_v1&s=d6453ca3…&e=1786399200"
        String url = "https://sites-cf.mhcache.com/e/1/az1zaXRlc192MSZzPWQ2NDUzY2EzOTI3ZWI1NzNjYTUzOTg1NDE3Y2MwY2M0NGNkZDc3YjVkZDIwMWY3NWJiYTQwNGMyNThjMzlmOTEmZT0xNzg2Mzk5MjAw"
                + PHOTO_PATH;

        assertThat(new ProfilePicture("jpg", url).expiresAt()).contains(Instant.ofEpochSecond(1786399200L));
    }

    @Test
    void otherUrlsUseTheWholeUrlAsKeyAndNeverExpire() {
        ProfilePicture picture = new ProfilePicture("jpg", "https://example.org/photos/ana.jpg");

        assertThat(picture.cacheKey()).isNotEqualTo(new ProfilePicture("jpg", "https://example.org/photos/juan.jpg").cacheKey());
        assertThat(picture.expiresAt()).isEmpty();
        assertThat(picture.isExpired(Instant.now())).isFalse();
    }

    @Test
    void malformedUrlsDoNotThrow() {
        ProfilePicture picture = new ProfilePicture("jpg", "not a url with spaces %%");

        assertThat(picture.cacheKey()).hasSize(24);
        assertThat(picture.expiresAt()).isEmpty();
    }

}

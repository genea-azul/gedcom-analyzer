package com.geneaazul.gedcomanalyzer.service.photo;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.ProfilePicture;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilePictureCacheTest {

    @TempDir
    Path tempDir;

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private GedcomAnalyzerProperties properties;
    private ProfilePictureCache cache;
    private byte[] jpeg;

    @BeforeEach
    void setUp() throws IOException {
        BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", bytes);
        jpeg = bytes.toByteArray();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            boolean missing = exchange.getRequestURI().getPath().contains("missing");
            byte[] body = missing ? "gone".getBytes(StandardCharsets.UTF_8) : jpeg;
            exchange.sendResponseHeaders(missing ? 403 : 200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        properties = mock(GedcomAnalyzerProperties.class);
        when(properties.getTempDir()).thenReturn(tempDir);
        when(properties.getPublicApiBaseUrl()).thenReturn("https://api.example.org/");
        when(properties.getProfilePictureAllowedHostSuffixes()).thenReturn(List.of("localhost"));
        when(properties.getProfilePictureDownloadConcurrency()).thenReturn(2);
        cache = new ProfilePictureCache(properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private ProfilePicture signed(String photoFile, Instant expiresAt) {
        String token = "k=sites_v1&s=" + Long.toHexString(System.nanoTime()) + "&e=" + expiresAt.getEpochSecond();
        String segment = Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return new ProfilePicture("jpg", "http://localhost:" + server.getAddress().getPort() + "/e/1/" + segment + "/023/419/" + photoFile);
    }

    private static EnrichedGedcom gedcomWith(ProfilePicture... pictures) {
        List<EnrichedPerson> people = new ArrayList<>();
        for (ProfilePicture picture : pictures) {
            EnrichedPerson person = mock(EnrichedPerson.class);
            when(person.getProfilePicture()).thenReturn(Optional.of(picture));
            people.add(person);
        }
        EnrichedPerson withoutPhoto = mock(EnrichedPerson.class);
        when(withoutPhoto.getProfilePicture()).thenReturn(Optional.empty());
        people.add(withoutPhoto);
        EnrichedGedcom gedcom = mock(EnrichedGedcom.class);
        when(gedcom.getPeople()).thenReturn(people);
        return gedcom;
    }

    private static Instant inAWeek() {
        return Instant.now().plusSeconds(7 * 24 * 3600);
    }

    @Test
    void downloadsMissingPhotosAsThumbnailsAndServesThemFromTheApi() throws Exception {
        ProfilePicture ana = signed("ana_J.jpg", inAWeek());

        ProfilePictureCache.SyncStatus status = cache.sync(gedcomWith(ana)).orElseThrow();

        assertThat(status.downloaded()).isEqualTo(1);
        assertThat(status.cached()).isEqualTo(1);
        Path file = cache.cachedFile(ana.cacheKey()).orElseThrow();
        BufferedImage thumbnail = ImageIO.read(file.toFile());
        assertThat(Math.max(thumbnail.getWidth(), thumbnail.getHeight())).isEqualTo(ProfilePictureCache.MAX_SIDE_PX);
        assertThat(cache.publicUrl(ana)).contains("https://api.example.org/api/photos/" + ana.cacheKey() + ".jpg");
    }

    @Test
    void aNewExportOfTheSamePhotoReusesTheCachedCopy() {
        cache.sync(gedcomWith(signed("ana_J.jpg", inAWeek())));
        int afterFirstSync = requests.get();

        ProfilePicture reSigned = signed("ana_J.jpg", inAWeek());
        ProfilePictureCache.SyncStatus status = cache.sync(gedcomWith(reSigned)).orElseThrow();

        assertThat(requests.get()).isEqualTo(afterFirstSync);
        assertThat(status.downloaded()).isZero();
        assertThat(cache.publicUrl(reSigned)).hasValueSatisfying(url -> assertThat(url).startsWith("https://api.example.org/api/photos/"));
    }

    @Test
    void expiredUrlsAreNotRequestedAndNeverReturned() {
        ProfilePicture expired = signed("old_J.jpg", Instant.now().minusSeconds(60));

        ProfilePictureCache.SyncStatus status = cache.sync(gedcomWith(expired)).orElseThrow();

        assertThat(status.expired()).isEqualTo(1);
        assertThat(requests).hasValue(0);
        assertThat(cache.publicUrl(expired)).isEmpty();
    }

    @Test
    void validButUncachedUrlsArePassedThroughUntilCached() {
        ProfilePicture fresh = signed("new_J.jpg", inAWeek());

        assertThat(cache.publicUrl(fresh)).contains(fresh.file());
    }

    @Test
    void failedDownloadsAreCountedAndDisallowedHostsAreNotContacted() {
        ProfilePicture gone = signed("missing_J.jpg", inAWeek());
        ProfilePicture elsewhere = new ProfilePicture("jpg", "http://127.0.0.1:" + server.getAddress().getPort() + "/elsewhere.jpg");

        ProfilePictureCache.SyncStatus status = cache.sync(gedcomWith(gone, elsewhere)).orElseThrow();

        assertThat(status.failed()).isEqualTo(2);
        assertThat(status.cached()).isZero();
        assertThat(requests).hasValue(1);
    }

    @Test
    void photosNoLongerInTheGedcomAreDeleted() throws Exception {
        ProfilePicture ana = signed("ana_J.jpg", inAWeek());
        cache.sync(gedcomWith(ana));
        Path anaFile = cache.cachedFile(ana.cacheKey()).orElseThrow();

        ProfilePictureCache.SyncStatus status = cache.sync(gedcomWith(signed("juan_J.jpg", inAWeek()))).orElseThrow();

        assertThat(status.deleted()).isEqualTo(1);
        assertThat(anaFile).doesNotExist();
        assertThat(cache.cachedFile(ana.cacheKey())).isEmpty();
    }

    @Test
    void cachedPhotosSurviveARestart() {
        ProfilePicture ana = signed("ana_J.jpg", inAWeek());
        cache.sync(gedcomWith(ana));

        ProfilePictureCache afterRestart = new ProfilePictureCache(properties);

        assertThat(afterRestart.cachedFile(ana.cacheKey())).isPresent();
        assertThat(afterRestart.publicUrl(signed("ana_J.jpg", Instant.now().minusSeconds(1)))).isPresent();
    }

    @Test
    void onlyWellFormedKeysAreLookedUp() throws Exception {
        Files.createDirectories(tempDir.resolve(ProfilePictureCache.DIR));
        Files.writeString(tempDir.resolve("secret.jpg"), "x");

        assertThat(cache.cachedFile("../secret")).isEmpty();
        assertThat(cache.cachedFile("ABCDEF")).isEmpty();
    }

}

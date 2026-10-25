package com.geneaazul.gedcomanalyzer.service.photo;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.ProfilePicture;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomLoadedEvent;
import com.geneaazul.gedcomanalyzer.utils.ThumbnailUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps small copies of the GEDCOM's profile photos, so the site keeps showing them after
 * MyHeritage's signed URLs expire (about a week after each export).
 *
 * <p>After every GEDCOM load, photos not cached yet are downloaded while their URLs still work
 * and stored as JPEG thumbnails in {@code temp-dir/profile-pictures}, keyed by
 * {@link ProfilePicture#cacheKey()} (stable across exports). Photos no longer in the GEDCOM are
 * deleted. The API then serves {@code /api/photos/<key>.jpg} instead of the MyHeritage URL.
 */
@Slf4j
@Service
public class ProfilePictureCache {

    static final String DIR = "profile-pictures";
    static final String EXTENSION = ".jpg";
    static final int MAX_SIDE_PX = 256;
    private static final Pattern KEY = Pattern.compile("[0-9a-f]{24}");
    private static final int MAX_DOWNLOAD_BYTES = 20 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final GedcomAnalyzerProperties properties;
    private final HttpClient httpClient;
    private final Set<String> cachedKeys = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean syncing = new AtomicBoolean();
    private volatile boolean cachedKeysLoaded;
    @Nullable
    private volatile SyncStatus lastSync;

    @Autowired
    public ProfilePictureCache(GedcomAnalyzerProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // Never follow redirects: the host allowlist is only checked for the URL in the GEDCOM
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    ProfilePictureCache(GedcomAnalyzerProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public record SyncStatus(
            Instant finishedAt,
            int referenced,
            int cached,
            int downloaded,
            int expired,
            int failed,
            int deleted) {
    }

    /**
     * URL the website should show for the person's photo: the cached copy, else the original URL while
     * it still works, else nothing (never a link that is known to fail).
     */
    public Optional<String> publicUrl(EnrichedPerson person) {
        return person.getProfilePicture().flatMap(this::publicUrl);
    }

    public Optional<String> publicUrl(ProfilePicture picture) {
        String key = picture.cacheKey();
        if (isCached(key)) {
            return Optional.of(Strings.CS.removeEnd(properties.getPublicApiBaseUrl(), "/") + "/api/photos/" + key + EXTENSION);
        }
        return picture.isExpired(Instant.now()) ? Optional.empty() : Optional.of(picture.file());
    }

    public Optional<Path> cachedFile(String key) {
        if (!KEY.matcher(key).matches() || !isCached(key)) {
            return Optional.empty();
        }
        return Optional.of(pathOf(key));
    }

    public Optional<SyncStatus> lastSync() {
        return Optional.ofNullable(lastSync);
    }

    @EventListener
    public void onGedcomLoaded(GedcomLoadedEvent event) {
        Thread.ofVirtual().name("profile-picture-sync").start(() -> {
            try {
                sync(event.gedcom());
            } catch (RuntimeException e) {
                log.error("Profile picture sync failed", e);
            }
        });
    }

    /**
     * Downloads missing photos and deletes the ones no longer referenced. A sync already running is not repeated.
     */
    public Optional<SyncStatus> sync(EnrichedGedcom gedcom) {
        if (!syncing.compareAndSet(false, true)) {
            log.info("Profile picture sync already running, skipping");
            return Optional.empty();
        }
        try {
            ensureCachedKeysLoaded();
            Map<String, ProfilePicture> referenced = gedcom.getPeople()
                    .stream()
                    .flatMap(person -> person.getProfilePicture().stream())
                    .collect(Collectors.toMap(ProfilePicture::cacheKey, Function.identity(), (first, _) -> first, LinkedHashMap::new));

            Instant now = Instant.now();
            AtomicInteger downloaded = new AtomicInteger();
            AtomicInteger expired = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();
            Semaphore permits = new Semaphore(Math.max(1, properties.getProfilePictureDownloadConcurrency()));

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                referenced.forEach((key, picture) -> {
                    if (cachedKeys.contains(key)) {
                        return;
                    }
                    if (picture.isExpired(now)) {
                        expired.incrementAndGet();
                        return;
                    }
                    executor.submit(() -> {
                        permits.acquireUninterruptibly();
                        try {
                            store(key, download(picture));
                            downloaded.incrementAndGet();
                        } catch (IOException | RuntimeException e) {
                            failed.incrementAndGet();
                            log.warn("Could not cache profile picture [ key={}, reason={} ]", key, e.getMessage());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failed.incrementAndGet();
                        } finally {
                            permits.release();
                        }
                    });
                });
            }

            int deleted = deleteUnreferenced(referenced.keySet());
            int cached = (int) referenced.keySet().stream().filter(cachedKeys::contains).count();
            SyncStatus status = new SyncStatus(Instant.now(), referenced.size(), cached, downloaded.get(), expired.get(), failed.get(), deleted);
            lastSync = status;
            if (expired.get() > 0) {
                log.warn("{} profile pictures have expired URLs and no cached copy: reload right after a new MyHeritage export to cache them", expired.get());
            }
            log.info("Profile picture sync: {}", status);
            return Optional.of(status);
        } finally {
            syncing.set(false);
        }
    }

    byte[] download(ProfilePicture picture) throws IOException, InterruptedException {
        URI uri = URI.create(picture.file());
        String host = Optional.ofNullable(uri.getHost()).map(h -> h.toLowerCase(Locale.ROOT)).orElse("");
        boolean allowedHost = properties.getProfilePictureAllowedHostSuffixes()
                .stream()
                .anyMatch(suffix -> host.equals(suffix) || host.endsWith("." + suffix));
        if (!allowedHost || !Set.of("http", "https").contains(uri.getScheme())) {
            throw new IOException("Host not allowed: " + host);
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "gedcom-analyzer (geneaazul.com.ar)")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            byte[] bytes = body.readNBytes(MAX_DOWNLOAD_BYTES + 1);
            if (bytes.length > MAX_DOWNLOAD_BYTES) {
                throw new IOException("Photo larger than " + MAX_DOWNLOAD_BYTES + " bytes");
            }
            return ThumbnailUtils.toJpegThumbnail(bytes, MAX_SIDE_PX);
        }
    }

    private void store(String key, byte[] jpeg) throws IOException {
        Path target = pathOf(key);
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".part-", EXTENSION);
        try {
            Files.write(temp, jpeg);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            cachedKeys.add(key);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private int deleteUnreferenced(Set<String> referencedKeys) {
        int deleted = 0;
        for (String key : Set.copyOf(cachedKeys)) {
            if (!referencedKeys.contains(key)) {
                try {
                    Files.deleteIfExists(pathOf(key));
                    cachedKeys.remove(key);
                    deleted++;
                } catch (IOException e) {
                    log.warn("Could not delete unreferenced profile picture {}", key, e);
                }
            }
        }
        return deleted;
    }

    private boolean isCached(String key) {
        ensureCachedKeysLoaded();
        return cachedKeys.contains(key);
    }

    private void ensureCachedKeysLoaded() {
        if (cachedKeysLoaded) {
            return;
        }
        synchronized (this) {
            if (cachedKeysLoaded) {
                return;
            }
            Path dir = directory();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    files.map(path -> path.getFileName().toString())
                            .filter(name -> name.endsWith(EXTENSION))
                            .map(name -> Strings.CS.removeEnd(name, EXTENSION))
                            .filter(key -> KEY.matcher(key).matches())
                            .forEach(cachedKeys::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            cachedKeysLoaded = true;
        }
    }

    private Path directory() {
        return properties.getTempDir().resolve(DIR);
    }

    private Path pathOf(String key) {
        return directory().resolve(key + EXTENSION);
    }

}

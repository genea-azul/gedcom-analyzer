package com.geneaazul.gedcomanalyzer.model;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A person's photo as exported in the GEDCOM: on MyHeritage, a signed CDN URL such as
 * {@code https://sites-cf.mhcache.com/e/1/<base64 "k=…&s=<signature>&e=<epoch>">/023/419/1551/000011_…_J.jpg}.
 * The signature segment changes on every export and stops working at {@code e} (about a week
 * later); the rest of the path identifies the photo and stays the same.
 */
public record ProfilePicture(String type, String file) {

    private static final Pattern SIGNED_PATH = Pattern.compile("^/e/\\d+/([A-Za-z0-9_-]+)/(.+)$");
    private static final Pattern EXPIRY = Pattern.compile("(?:^|&)e=(\\d{9,12})(?:&|$)");
    private static final int KEY_HEX_LENGTH = 24;

    /**
     * Identifies the photo independently of the signature, so a cached copy is reused across exports.
     */
    public String cacheKey() {
        String stablePart = signedPath()
                .map(matcher -> matcher.group(2))
                .orElse(file);
        return sha256Hex(stablePart).substring(0, KEY_HEX_LENGTH);
    }

    /**
     * When the signed URL stops working, if it carries an expiry.
     */
    public Optional<Instant> expiresAt() {
        return signedPath()
                .flatMap(matcher -> decodeBase64Url(matcher.group(1)))
                .map(EXPIRY::matcher)
                .filter(Matcher::find)
                .map(matcher -> Instant.ofEpochSecond(Long.parseLong(matcher.group(1))));
    }

    public boolean isExpired(Instant now) {
        return expiresAt().map(expiry -> !expiry.isAfter(now)).orElse(false);
    }

    private Optional<Matcher> signedPath() {
        try {
            URI uri = URI.create(file);
            if (uri.getRawPath() == null) {
                return Optional.empty();
            }
            Matcher matcher = SIGNED_PATH.matcher(uri.getRawPath());
            return matcher.matches() ? Optional.of(matcher) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> decodeBase64Url(String value) {
        try {
            return Optional.of(new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}

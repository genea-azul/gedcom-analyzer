package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.service.photo.ProfilePictureCache;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

/**
 * Cached profile photos (see {@link ProfilePictureCache}). Loaded by {@code <img>} tags, so no CORS needed.
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final ProfilePictureCache profilePictureCache;

    @GetMapping("/{key:[0-9a-f]{24}}.jpg")
    public ResponseEntity<Resource> getPhoto(@PathVariable String key) {
        return profilePictureCache.cachedFile(key)
                .<ResponseEntity<Resource>>map(path -> ResponseEntity.ok()
                        // The key identifies the photo's content, so it never changes under the same URL
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .eTag(key)
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}

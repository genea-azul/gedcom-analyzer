package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.service.photo.ProfilePictureCache;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PhotoControllerTest {

    private static final String KEY = "0123456789abcdef01234567";

    @TempDir
    Path tempDir;

    private ProfilePictureCache cache;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        cache = mock(ProfilePictureCache.class);
        when(cache.cachedFile(anyString())).thenReturn(Optional.empty());
        mvc = MockMvcBuilders.standaloneSetup(new PhotoController(cache)).build();
    }

    @Test
    void cachedPhotosAreServedWithAYearLongImmutableCache() throws Exception {
        Path photo = Files.write(tempDir.resolve(KEY + ".jpg"), new byte[] { (byte) 0xFF, (byte) 0xD8, 1, 2 });
        when(cache.cachedFile(KEY)).thenReturn(Optional.of(photo));

        mvc.perform(get("/api/photos/" + KEY + ".jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public, immutable"))
                .andExpect(content().bytes(Files.readAllBytes(photo)));
    }

    @Test
    void unknownPhotosAre404() throws Exception {
        mvc.perform(get("/api/photos/" + KEY + ".jpg")).andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void malformedKeysDoNotReachTheCache() throws Exception {
        mvc.perform(get("/api/photos/..%2F..%2Fetc%2Fpasswd.jpg")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/photos/ABC.jpg")).andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

}

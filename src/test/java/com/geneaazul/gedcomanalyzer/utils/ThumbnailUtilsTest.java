package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailUtilsTest {

    static byte[] image(int width, int height, String format, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x * 7 + y * 3) % 2 == 0 ? Color.RED.getRGB() : 0x00000000);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return bytes.toByteArray();
    }

    private static BufferedImage decode(byte[] jpeg) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(jpeg));
    }

    @Test
    void largePhotosAreShrunkToTheMaxSideKeepingTheAspectRatio() throws Exception {
        BufferedImage thumbnail = decode(ThumbnailUtils.toJpegThumbnail(image(2000, 1000, "jpg", BufferedImage.TYPE_INT_RGB), 256));

        assertThat(thumbnail.getWidth()).isEqualTo(256);
        assertThat(thumbnail.getHeight()).isEqualTo(128);
    }

    @Test
    void portraitPhotosAreLimitedByTheirHeight() throws Exception {
        BufferedImage thumbnail = decode(ThumbnailUtils.toJpegThumbnail(image(600, 1800, "jpg", BufferedImage.TYPE_INT_RGB), 256));

        assertThat(thumbnail.getHeight()).isEqualTo(256);
        assertThat(thumbnail.getWidth()).isBetween(84, 86);
    }

    @Test
    void smallPhotosAreNotUpscaled() throws Exception {
        BufferedImage thumbnail = decode(ThumbnailUtils.toJpegThumbnail(image(100, 80, "jpg", BufferedImage.TYPE_INT_RGB), 256));

        assertThat(thumbnail.getWidth()).isEqualTo(100);
        assertThat(thumbnail.getHeight()).isEqualTo(80);
    }

    @Test
    void transparentPngsBecomeJpegs() throws Exception {
        byte[] jpeg = ThumbnailUtils.toJpegThumbnail(image(300, 300, "png", BufferedImage.TYPE_INT_ARGB), 256);

        assertThat(jpeg[0] & 0xFF).isEqualTo(0xFF);
        assertThat(jpeg[1] & 0xFF).isEqualTo(0xD8);
        assertThat(decode(jpeg).getWidth()).isEqualTo(256);
    }

    @Test
    void somethingThatIsNotAnImageIsRejected() {
        assertThatThrownBy(() -> ThumbnailUtils.toJpegThumbnail("<html>403</html>".getBytes(StandardCharsets.UTF_8), 256))
                .isInstanceOf(IOException.class);
    }

}

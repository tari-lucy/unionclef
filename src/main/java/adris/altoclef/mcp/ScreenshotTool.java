package adris.altoclef.mcp;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * The game frame as a small JPEG for an agent: the Minecraft framebuffer only (never the desktop),
 * downscaled so the base64 text stays modest. The caller decides what to do with the picture;
 * an agent runtime should store it as a file instead of keeping the base64 in its context.
 */
final class ScreenshotTool {

    static final int DEFAULT_WIDTH = 960;
    static final int MIN_WIDTH = 160;
    static final int MAX_WIDTH = 1920;
    private static final float QUALITY = 0.82f;

    private ScreenshotTool() {}

    /** @param frame PNG bytes of the framebuffer, or null when the capture failed */
    static Map<String, Object> encode(byte[] frame, int requestedWidth) throws Exception {
        if (frame == null || frame.length == 0) {
            throw new IllegalStateException("no frame: the game is not rendering (minimized or loading)");
        }
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(frame));
        if (source == null) {
            throw new IllegalStateException("the frame could not be decoded");
        }
        int target = requestedWidth <= 0 ? DEFAULT_WIDTH : Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, requestedWidth));
        int width = Math.min(target, source.getWidth());
        int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));

        // JPEG has no alpha: draw on an RGB canvas (also does the downscale)
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(canvas, null, null), param);
        } finally {
            writer.dispose();
        }

        byte[] jpeg = out.toByteArray();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", "jpeg");
        result.put("width", width);
        result.put("height", height);
        result.put("bytes", jpeg.length);
        result.put("base64", Base64.getEncoder().encodeToString(jpeg));
        return result;
    }
}

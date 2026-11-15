package com.extraction.menu.client.bg;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Sequential GIF decoder. Composites frames (handles disposal methods), downscales them to at most
 * 960x540 and returns pixels already in the ABGR layout that NativeImage expects. Loops forever.
 */
final class GifDecoder {
    record Frame(int[] abgr, int w, int h, int delayMs) {}

    private static final int MAX_W = 960;
    private static final int MAX_H = 540;

    private final ImageReader reader;
    private final int count;
    private int lw;
    private int lh;
    private final int outW;
    private final int outH;
    private BufferedImage canvas;
    private int index;

    GifDecoder(byte[] data) throws IOException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
        if (!it.hasNext()) {
            throw new IOException("No GIF reader available in this Java runtime");
        }
        reader = it.next();
        reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(data)), false, false);
        count = reader.getNumImages(true);
        if (count <= 0) {
            throw new IOException("GIF has no frames");
        }
        readLogicalSize();
        float scale = Math.min(1.0F, Math.min((float) MAX_W / lw, (float) MAX_H / lh));
        outW = Math.max(1, Math.round(lw * scale));
        outH = Math.max(1, Math.round(lh * scale));
        canvas = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
    }

    private void readLogicalSize() throws IOException {
        lw = 0;
        lh = 0;
        try {
            IIOMetadata sm = reader.getStreamMetadata();
            if (sm != null) {
                Node root = sm.getAsTree("javax_imageio_gif_stream_1.0");
                Node d = child(root, "LogicalScreenDescriptor");
                if (d != null) {
                    lw = intAttr(d, "logicalScreenWidth", 0);
                    lh = intAttr(d, "logicalScreenHeight", 0);
                }
            }
        } catch (Exception ignored) {
            // fall back below
        }
        if (lw <= 0 || lh <= 0) {
            lw = reader.getWidth(0);
            lh = reader.getHeight(0);
        }
    }

    synchronized Frame next() throws IOException {
        if (index >= count) {
            index = 0;
            canvas = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage img = reader.read(index);

        int x = 0;
        int y = 0;
        int delay = 10;
        String disposal = "none";
        try {
            IIOMetadata md = reader.getImageMetadata(index);
            Node root = md.getAsTree("javax_imageio_gif_image_1.0");
            Node d = child(root, "ImageDescriptor");
            if (d != null) {
                x = intAttr(d, "imageLeftPosition", 0);
                y = intAttr(d, "imageTopPosition", 0);
            }
            Node g = child(root, "GraphicControlExtension");
            if (g != null) {
                delay = intAttr(g, "delayTime", 10);
                String dm = attr(g, "disposalMethod");
                if (dm != null) {
                    disposal = dm;
                }
            }
        } catch (Exception ignored) {
            // keep defaults
        }

        BufferedImage saved = null;
        if ("restoreToPrevious".equals(disposal)) {
            saved = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = saved.createGraphics();
            sg.setComposite(AlphaComposite.Src);
            sg.drawImage(canvas, 0, 0, null);
            sg.dispose();
        }

        Graphics2D cg = canvas.createGraphics();
        cg.setComposite(AlphaComposite.SrcOver);
        cg.drawImage(img, x, y, null);
        cg.dispose();

        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D og = out.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        og.drawImage(canvas, 0, 0, outW, outH, null);
        og.dispose();

        int[] px = out.getRGB(0, 0, outW, outH, null, 0, outW);
        for (int i = 0; i < px.length; i++) {
            int v = px[i]; // ARGB -> ABGR
            px[i] = (v & 0xFF00FF00) | ((v >> 16) & 0xFF) | ((v & 0xFF) << 16);
        }

        if ("restoreToBackgroundColor".equals(disposal)) {
            Graphics2D dg = canvas.createGraphics();
            dg.setComposite(AlphaComposite.Clear);
            dg.fillRect(x, y, img.getWidth(), img.getHeight());
            dg.dispose();
        } else if (saved != null) {
            canvas = saved;
        }

        index++;
        int ms = (delay <= 1 ? 10 : delay) * 10; // browsers treat 0/1 cs as 100 ms
        return new Frame(px, outW, outH, ms);
    }

    int width() {
        return outW;
    }

    int height() {
        return outH;
    }

    void close() {
        reader.dispose();
    }

    private static Node child(Node parent, String name) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (name.equals(list.item(i).getNodeName())) {
                return list.item(i);
            }
        }
        return null;
    }

    private static String attr(Node node, String name) {
        NamedNodeMap map = node.getAttributes();
        Node a = map == null ? null : map.getNamedItem(name);
        return a == null ? null : a.getNodeValue();
    }

    private static int intAttr(Node node, String name, int fallback) {
        try {
            String v = attr(node, name);
            return v == null ? fallback : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

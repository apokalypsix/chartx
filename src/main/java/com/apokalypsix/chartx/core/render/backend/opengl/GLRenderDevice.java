package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.*;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * OpenGL implementation of the RenderDevice interface.
 *
 * <p>Uses JOGL for OpenGL access and integrates with Swing via GLJPanel.
 */
public class GLRenderDevice implements RenderDevice, GLEventListener {

    private static final Logger log = LoggerFactory.getLogger(GLRenderDevice.class);

    private final Component targetComponent;
    private GLJPanel glPanel;
    private GL2ES2 gl;
    private boolean initialized = false;

    // Cached device info
    private float maxLineWidth = 1.0f;
    private int maxTextureSize = 1024;
    private String rendererInfo = "Unknown";

    /**
     * Creates a new GLRenderDevice.
     *
     * @param target the target Swing component (may be null for standalone use)
     */
    public GLRenderDevice(Component target) {
        this.targetComponent = target;
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        // GLJPanel will call init() via GLEventListener when ready
        log.info("GLRenderDevice initialization deferred to GL context creation");
    }

    /**
     * Called by JOGL when the GL context is created.
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL2ES2();

        // Query device capabilities
        float[] lineWidthRange = new float[2];
        gl.glGetFloatv(GL2ES2.GL_ALIASED_LINE_WIDTH_RANGE, lineWidthRange, 0);
        maxLineWidth = lineWidthRange[1];

        int[] maxTexSize = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
        maxTextureSize = maxTexSize[0];

        rendererInfo = gl.glGetString(GL.GL_RENDERER) + " / " + gl.glGetString(GL.GL_VERSION);

        log.info("OpenGL initialized: {}", rendererInfo);
        log.debug("Max line width: {}, max texture size: {}", maxLineWidth, maxTextureSize);

        initialized = true;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        log.info("GLRenderDevice disposing");
        initialized = false;
        gl = null;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // Rendering is handled externally via the render pipeline
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl = drawable.getGL().getGL2ES2();
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose() {
        initialized = false;
        gl = null;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public RenderBackend getBackendType() {
        return RenderBackend.OPENGL;
    }

    @Override
    public void beginFrame() {
        // No-op for OpenGL - frame management is handled by GLJPanel/JOGL
    }

    @Override
    public void endFrame() {
        // No-op for OpenGL - frame management is handled by GLJPanel/JOGL
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void setScissorEnabled(boolean enabled) {
        if (enabled) {
            gl.glEnable(GL.GL_SCISSOR_TEST);
        } else {
            gl.glDisable(GL.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        gl.glScissor(x, y, width, height);
    }

    @Override
    public void setBlendMode(BlendMode mode) {
        switch (mode) {
            case NONE -> gl.glDisable(GL.GL_BLEND);
            case ALPHA -> {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
            case ADDITIVE -> {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
            }
            case MULTIPLY -> {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_ZERO);
            }
            case PREMULTIPLIED_ALPHA -> {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    @Override
    public void setLineWidth(float width) {
        gl.glLineWidth(Math.min(width, maxLineWidth));
    }

    @Override
    public void setLineSmoothing(boolean enabled) {
        if (enabled) {
            gl.glEnable(GL.GL_LINE_SMOOTH);
            gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        } else {
            gl.glDisable(GL.GL_LINE_SMOOTH);
        }
    }

    @Override
    public void setDepthTestEnabled(boolean enabled) {
        if (enabled) {
            gl.glEnable(GL.GL_DEPTH_TEST);
        } else {
            gl.glDisable(GL.GL_DEPTH_TEST);
        }
    }

    @Override
    public void clearScreen(float r, float g, float b, float a) {
        gl.glClearColor(r, g, b, a);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void clearDepth() {
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public Shader createShader(ShaderSource source) {
        return new GLShader(this, source);
    }

    @Override
    public Buffer createBuffer(BufferDescriptor descriptor) {
        return new GLBuffer(this, descriptor);
    }

    @Override
    public Texture createTexture(TextureDescriptor descriptor) {
        return new GLTexture(this, descriptor);
    }

    @Override
    public float getMaxLineWidth() {
        return maxLineWidth;
    }

    @Override
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    @Override
    public String getRendererInfo() {
        return rendererInfo;
    }

    // -------------------------------------------------------------------------
    // OpenGL-specific methods
    // -------------------------------------------------------------------------

    /**
     * Returns the current GL context.
     *
     * <p>This is exposed for advanced usage and for the transition period
     * while existing renderers are being migrated.
     */
    public GL2ES2 getGL() {
        return gl;
    }

    /**
     * Sets the GL context.
     *
     * <p>Called internally when the context changes.
     */
    public void setGL(GL2ES2 gl) {
        this.gl = gl;
    }

    /**
     * Returns the GLJPanel if one was created.
     */
    public GLJPanel getGLPanel() {
        return glPanel;
    }

    /**
     * Sets the GLJPanel.
     */
    public void setGLPanel(GLJPanel glPanel) {
        this.glPanel = glPanel;
    }

    @Override
    public void readFramePixels(int[] pixels) {
        // OpenGL can read pixels using glReadPixels if needed
        // This is typically not needed when using GLJPanel since it renders directly
        if (gl == null) {
            return;
        }

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        int width = viewport[2];
        int height = viewport[3];

        if (pixels.length < width * height) {
            throw new IllegalArgumentException("Pixel buffer too small: " + pixels.length +
                    " < " + (width * height));
        }

        // Read pixels as RGBA bytes
        byte[] rgba = new byte[width * height * 4];
        gl.glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
                java.nio.ByteBuffer.wrap(rgba));

        // Convert RGBA to ARGB (Java's BufferedImage format)
        // Also flip vertically since OpenGL origin is bottom-left
        for (int y = 0; y < height; y++) {
            int srcRow = (height - 1 - y) * width;
            int dstRow = y * width;
            for (int x = 0; x < width; x++) {
                int srcIdx = (srcRow + x) * 4;
                int r = rgba[srcIdx] & 0xFF;
                int g = rgba[srcIdx + 1] & 0xFF;
                int b = rgba[srcIdx + 2] & 0xFF;
                int a = rgba[srcIdx + 3] & 0xFF;
                pixels[dstRow + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    @Override
    public boolean supportsPixelReadback() {
        // OpenGL supports pixel readback but it's typically not needed
        // when using GLJPanel since it renders directly to Swing
        return true;
    }
}

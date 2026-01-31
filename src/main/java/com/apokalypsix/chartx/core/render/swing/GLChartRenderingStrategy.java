package com.apokalypsix.chartx.core.render.swing;

import com.apokalypsix.chartx.core.render.service.RenderPipeline;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * OpenGL rendering strategy using JOGL's GLJPanel.
 *
 * <p>This strategy uses the existing JOGL integration where GLJPanel
 * handles GL context management and the RenderPipeline acts as the
 * GLEventListener.
 *
 * <p>This is the default and most mature rendering backend.
 */
public class GLChartRenderingStrategy implements ChartRenderingStrategy {

    private static final Logger log = LoggerFactory.getLogger(GLChartRenderingStrategy.class);

    private GLJPanel glPanel;
    private RenderPipeline pipeline;
    private boolean initialized = false;

    @Override
    public void initialize(RenderPipeline pipeline) {
        this.pipeline = pipeline;

        // Create GL panel with compatible profile
        GLProfile profile = selectGLProfile();
        GLCapabilities caps = createGLCapabilities(profile);

        glPanel = new GLJPanel(caps);
        glPanel.addGLEventListener(pipeline);

        initialized = true;
        log.debug("GLChartRenderingStrategy initialized with {} profile", profile.getName());
    }

    private GLProfile selectGLProfile() {
        if (GLProfile.isAvailable(GLProfile.GL3)) {
            log.debug("Using GL3 profile");
            return GLProfile.get(GLProfile.GL3);
        } else if (GLProfile.isAvailable(GLProfile.GL2)) {
            log.debug("Using GL2 profile");
            return GLProfile.get(GLProfile.GL2);
        } else {
            log.debug("Using default GL profile");
            return GLProfile.getDefault();
        }
    }

    private GLCapabilities createGLCapabilities(GLProfile profile) {
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4); // MSAA for smoother lines
        return caps;
    }

    @Override
    public JComponent getDisplayComponent() {
        return glPanel;
    }

    @Override
    public void requestRepaint() {
        if (glPanel != null) {
            glPanel.repaint();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void dispose() {
        if (glPanel != null) {
            glPanel.destroy();
            glPanel = null;
        }
        initialized = false;
        log.debug("GLChartRenderingStrategy disposed");
    }

    @Override
    public String getBackendName() {
        return "OpenGL";
    }

    /**
     * Returns the underlying GLJPanel for advanced usage.
     */
    public GLJPanel getGLPanel() {
        return glPanel;
    }
}

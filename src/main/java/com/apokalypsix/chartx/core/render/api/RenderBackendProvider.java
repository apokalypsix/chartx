package com.apokalypsix.chartx.core.render.api;

import java.awt.Component;

/**
 * Service Provider Interface for rendering backends.
 *
 * <p>Implementations of this interface are discovered at runtime using
 * Java's ServiceLoader mechanism. Each backend module (OpenGL, Vulkan, etc.)
 * provides its own implementation.
 *
 * <p>To register a provider, create a file named
 * {@code META-INF/services/com.apokalypsix.chartx.core.render.api.RenderBackendProvider}
 * containing the fully qualified name of your implementation class.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class OpenGLBackendProvider implements RenderBackendProvider {
 *     @Override
 *     public RenderBackend getBackendType() {
 *         return RenderBackend.OPENGL;
 *     }
 *
 *     @Override
 *     public boolean isAvailable() {
 *         try {
 *             Class.forName("com.jogamp.opengl.GL2ES2");
 *             return true;
 *         } catch (ClassNotFoundException e) {
 *             return false;
 *         }
 *     }
 *
 *     @Override
 *     public RenderDevice createDevice(Component target) {
 *         return new GLRenderDevice(target);
 *     }
 *
 *     @Override
 *     public ResourceManager createResourceManager() {
 *         return new GLBackendResourceManager();
 *     }
 * }
 * }</pre>
 */
public interface RenderBackendProvider {

    /**
     * Returns the backend type this provider implements.
     *
     * @return the backend type
     */
    RenderBackend getBackendType();

    /**
     * Checks if this backend is available on the current platform.
     *
     * <p>This method should perform lightweight checks such as verifying
     * that required native libraries or classes are present. It should not
     * perform expensive operations like creating GPU contexts.
     *
     * @return true if the backend is available
     */
    boolean isAvailable();

    /**
     * Returns the priority of this backend for auto-selection.
     *
     * <p>When {@link RenderBackend#AUTO} is requested, the backend with the
     * highest priority that is available will be selected. Higher values
     * indicate higher priority.
     *
     * <p>Recommended priority ranges:
     * <ul>
     *   <li>0-49: Fallback backends (software rendering)</li>
     *   <li>50-99: Standard backends (OpenGL)</li>
     *   <li>100-149: Modern backends (Vulkan)</li>
     *   <li>150-199: Platform-optimized backends (Metal on macOS, DX12 on Windows)</li>
     * </ul>
     *
     * @return the priority (higher = preferred)
     */
    default int getPriority() {
        return 50;
    }

    /**
     * Creates a render device for this backend.
     *
     * @param target the target Swing component (may be null for headless use)
     * @return the render device
     * @throws UnsupportedOperationException if the backend is not available
     */
    RenderDevice createDevice(Component target);

    /**
     * Creates a resource manager for this backend.
     *
     * @return the resource manager
     * @throws UnsupportedOperationException if the backend is not available
     */
    ResourceManager createResourceManager();
}

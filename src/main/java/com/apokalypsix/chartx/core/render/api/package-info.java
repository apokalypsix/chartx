/**
 * Backend-agnostic rendering API for ChartX.
 *
 * <p>This package defines the abstract interfaces for the rendering system,
 * allowing ChartX to support multiple graphics backends (OpenGL, Vulkan, etc.)
 * through a unified API.
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.RenderDevice} - GPU state and resource management</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.Shader} - Shader program abstraction</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.Buffer} - Vertex buffer abstraction</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.Texture} - Texture abstraction</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.ResourceManager} - Resource lifecycle management</li>
 * </ul>
 *
 * <h2>Service Provider Interface</h2>
 * <p>Backend implementations are discovered at runtime using Java's ServiceLoader:
 * <ul>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.RenderBackendProvider} - SPI for backends</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.api.RenderBackendFactory} - Factory using ServiceLoader</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a render device (auto-selects best backend)
 * RenderDevice device = RenderBackendFactory.createDevice(RenderBackend.AUTO, component);
 * ResourceManager resources = RenderBackendFactory.createResourceManager(device);
 *
 * // Initialize
 * device.initialize();
 * resources.initialize(device);
 *
 * // Create resources
 * Shader shader = resources.createShader("myShader", shaderSource);
 * Buffer buffer = resources.getOrCreateBuffer("vertices", BufferDescriptor.positionColor2D(1024));
 *
 * // Render
 * device.beginFrame();
 * device.clearScreen(0, 0, 0, 1);
 * shader.bind();
 * buffer.draw(DrawMode.TRIANGLES);
 * shader.unbind();
 * device.endFrame();
 * }</pre>
 *
 * @see com.apokalypsix.chartx.core.render.api.RenderBackendProvider
 */
package com.apokalypsix.chartx.core.render.api;

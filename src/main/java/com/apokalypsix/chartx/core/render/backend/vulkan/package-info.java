/**
 * Vulkan backend implementation for ChartX rendering.
 *
 * <p>This package provides a Vulkan-based implementation of the ChartX rendering
 * abstraction layer. It uses LWJGL for Vulkan bindings and the shaderc library
 * for runtime GLSL to SPIR-V compilation.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link VkRenderDevice} - Vulkan device, instance, and queue management</li>
 *   <li>{@link VkShader} - SPIR-V shader compilation and pipeline management</li>
 *   <li>{@link VkBuffer} - Vertex and index buffer management</li>
 *   <li>{@link VkTexture} - Image, image view, and sampler management</li>
 *   <li>{@link VkResourceManager} - Centralized resource lifecycle management</li>
 * </ul>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Vulkan 1.1+ capable GPU and drivers</li>
 *   <li>LWJGL 3.3+ with Vulkan, VMA, and shaderc bindings</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>To use the Vulkan backend:
 * <pre>{@code
 * // Select Vulkan backend
 * RenderDevice device = RenderBackendFactory.create(RenderBackend.VULKAN, null);
 * device.initialize();
 *
 * ResourceManager resources = new VkResourceManager();
 * resources.initialize(device);
 * }</pre>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Vulkan has higher initialization overhead than OpenGL</li>
 *   <li>Requires Vulkan-capable hardware (most modern GPUs)</li>
 *   <li>macOS requires MoltenVK (Vulkan over Metal)</li>
 * </ul>
 *
 * @see com.apokalypsix.chartx.core.render.api.RenderBackend
 * @see com.apokalypsix.chartx.core.render.api.RenderBackendFactory
 */
package com.apokalypsix.chartx.core.render.backend.vulkan;

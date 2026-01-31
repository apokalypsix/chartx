/**
 * OpenGL backend implementation for the ChartX rendering API.
 *
 * <p>This package provides JOGL-based implementations of the abstract
 * rendering interfaces defined in {@link com.apokalypsix.chartx.core.render.api}.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link com.apokalypsix.chartx.core.render.backend.opengl.GLRenderDevice} - OpenGL device implementation</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.backend.opengl.GLShader} - GLSL shader wrapper</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.backend.opengl.GLBuffer} - VBO wrapper</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.backend.opengl.GLTexture} - GL texture wrapper</li>
 *   <li>{@link com.apokalypsix.chartx.core.render.backend.opengl.GLBackendResourceManager} - Resource management</li>
 * </ul>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>JOGL 2.4+</li>
 *   <li>OpenGL 3.3+ core profile</li>
 * </ul>
 *
 * @see com.apokalypsix.chartx.core.render.api Abstract rendering API
 */
package com.apokalypsix.chartx.core.render.backend.opengl;

/**
 * V2 Renderers using the abstracted rendering API.
 *
 * <p>This package contains renderer implementations that use the backend-agnostic
 * rendering API defined in {@link com.apokalypsix.chartx.core.render.api}. These renderers
 * can work with any backend implementation (OpenGL, Vulkan, etc.) without modification.
 *
 * <h2>Available Renderers</h2>
 * <ul>
 *   <li>{@link LineRendererV2} - Renders LineSeries as connected line segments</li>
 *   <li>{@link CandlestickRendererV2} - Renders OHLC data as candlestick charts</li>
 *   <li>{@link AreaRendererV2} - Renders LineSeries as filled areas</li>
 *   <li>{@link HistogramRendererV2} - Renders histogram/bar data</li>
 *   <li>{@link ScatterRendererV2} - Renders scatter plots with various marker shapes</li>
 *   <li>{@link StepLineRendererV2} - Renders step lines (STEP_BEFORE, STEP_AFTER, STEP_MIDDLE)</li>
 *   <li>{@link BandRendererV2} - Renders band series (Bollinger Bands style)</li>
 *   <li>{@link DashedLineRendererV2} - Utility for drawing dashed lines</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>To use v2 renderers, enable the abstracted API on the RenderPipeline:
 * <pre>{@code
 * RenderPipeline pipeline = new RenderPipeline(viewport);
 * pipeline.setUseAbstractedAPI(true);
 *
 * // Create and initialize a v2 renderer
 * LineRendererV2 lineRenderer = new LineRendererV2();
 * // Renderer will be initialized when render() is first called with a valid RenderContext
 * }</pre>
 *
 * <h2>Migration from Legacy Renderers</h2>
 * <p>V2 renderers extend {@link BaseRenderer} and implement {@link com.apokalypsix.chartx.core.render.api.AbstractRenderer}.
 * The key differences from legacy renderers are:
 * <ul>
 *   <li>No direct GL2ES2 access - all GPU operations go through {@link com.apokalypsix.chartx.core.render.api.RenderDevice}</li>
 *   <li>Resources managed via {@link com.apokalypsix.chartx.core.render.api.ResourceManager}</li>
 *   <li>Shaders accessed by name (e.g., "default", "simple") from the resource manager</li>
 *   <li>Buffers created via resource manager with {@link com.apokalypsix.chartx.core.render.api.BufferDescriptor}</li>
 * </ul>
 *
 * <p>During the migration period, use {@link com.apokalypsix.chartx.core.render.api.LegacyRendererAdapter}
 * to wrap legacy renderers from {@link com.apokalypsix.chartx.core.render.service}.
 *
 * @see com.apokalypsix.chartx.core.render.api.AbstractRenderer
 * @see com.apokalypsix.chartx.core.render.api.LegacyRendererAdapter
 */
package com.apokalypsix.chartx.core.render.service.v2;

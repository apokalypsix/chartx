package com.apokalypsix.chartx.core.render.model;

import com.apokalypsix.chartx.chart.axis.CategoryAxis;
import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.api.RenderDevice;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.jogamp.opengl.GL2ES2;

/**
 * Per-frame rendering context containing all state needed for rendering.
 *
 * <p>A new RenderContext is created for each frame and passed to all render layers.
 * It provides access to the GL context, viewport, coordinate system, and shared resources.
 *
 * <p>This class supports both the legacy GL API and the new abstracted rendering API
 * to enable gradual migration. Use {@link #getDevice()} and {@link #getResourceManager()}
 * for the new API, or {@link #getGL()} and {@link #getResources()} for legacy code.
 */
public class RenderContext {

    // Legacy API (retained for backward compatibility during migration)
    private final GL2ES2 gl;
    private final GLResourceManager resources;

    // New abstracted API (optional during transition)
    private RenderDevice device;
    private ResourceManager resourceManager;

    private final Viewport viewport;
    private final MultiAxisCoordinateSystem coordinates;
    private final YAxisManager axisManager;

    // Visible data range (indices into the primary series)
    private int firstVisibleIndex = -1;
    private int lastVisibleIndex = -1;
    private int visibleCount = 0;

    // Bar duration for the primary series (in milliseconds)
    private long barDuration = 60000; // Default 1 minute

    // Projection matrix for shaders
    private final float[] projectionMatrix;

    // Frame timing
    private final long frameTimestamp;

    // Display scale factor for HiDPI displays
    private float scaleFactor = 1.0f;

    // Category axis for categorical charts
    private CategoryAxis categoryAxis;

    /**
     * Creates a render context for the current frame.
     *
     * @param gl the GL context
     * @param viewport the current viewport
     * @param coordinates the multi-axis coordinate system
     * @param axisManager the Y-axis manager
     * @param resources the resource manager
     */
    public RenderContext(GL2ES2 gl, Viewport viewport, MultiAxisCoordinateSystem coordinates,
                         YAxisManager axisManager, GLResourceManager resources) {
        this.gl = gl;
        this.viewport = viewport;
        this.coordinates = coordinates;
        this.axisManager = axisManager;
        this.resources = resources;
        this.frameTimestamp = System.currentTimeMillis();

        // Create orthographic projection matrix for 2D rendering
        this.projectionMatrix = GLResourceManager.createOrthoMatrix(
                0, viewport.getWidth(),
                viewport.getHeight(), 0  // Y flipped for screen coordinates
        );
    }

    // ========== Accessors ==========

    public GL2ES2 getGL() {
        return gl;
    }

    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Returns the multi-axis coordinate system.
     */
    public MultiAxisCoordinateSystem getCoordinates() {
        return coordinates;
    }

    /**
     * Returns a coordinate system for the specified axis.
     *
     * @param axisId the axis ID
     * @return axis-specific coordinate system
     */
    public CoordinateSystem getCoordinatesForAxis(String axisId) {
        return coordinates.forAxis(axisId);
    }

    /**
     * Returns a coordinate system for the given data using the specified axis.
     *
     * @param data the data
     * @param axisId the axis ID
     * @return axis-specific coordinate system for the data
     */
    public CoordinateSystem getCoordinatesForData(Data<?> data, String axisId) {
        return coordinates.forAxis(axisId);
    }

    /**
     * Returns a coordinate system for the given data using the default axis.
     *
     * @param data the data
     * @return coordinate system for the default axis
     */
    public CoordinateSystem getCoordinatesForData(Data<?> data) {
        return coordinates.forAxis("default");
    }

    /**
     * Returns the Y-axis manager.
     */
    public YAxisManager getAxisManager() {
        return axisManager;
    }

    /**
     * Returns the legacy GL resource manager.
     *
     * @deprecated Use {@link #getResourceManager()} for new code
     */
    public GLResourceManager getResources() {
        return resources;
    }

    // ========== New Abstracted API ==========

    /**
     * Sets the abstracted render device.
     *
     * <p>Call this to enable use of the new rendering API alongside the legacy GL API.
     *
     * @param device the render device
     */
    public void setDevice(RenderDevice device) {
        this.device = device;
    }

    /**
     * Returns the abstracted render device.
     *
     * @return the render device, or null if not set
     */
    public RenderDevice getDevice() {
        return device;
    }

    /**
     * Sets the abstracted resource manager.
     *
     * @param resourceManager the resource manager
     */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Returns the abstracted resource manager.
     *
     * @return the resource manager, or null if not set
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Returns true if the new abstracted API is available.
     */
    public boolean hasAbstractedAPI() {
        return device != null && resourceManager != null;
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix;
    }

    public long getFrameTimestamp() {
        return frameTimestamp;
    }

    // ========== Visible range ==========

    /**
     * Sets the visible data range (indices into the primary series).
     */
    public void setVisibleRange(int firstIndex, int lastIndex) {
        this.firstVisibleIndex = firstIndex;
        this.lastVisibleIndex = lastIndex;
        this.visibleCount = (firstIndex >= 0 && lastIndex >= firstIndex)
                ? lastIndex - firstIndex + 1 : 0;
    }

    public int getFirstVisibleIndex() {
        return firstVisibleIndex;
    }

    public int getLastVisibleIndex() {
        return lastVisibleIndex;
    }

    public int getVisibleCount() {
        return visibleCount;
    }

    public boolean hasVisibleData() {
        return visibleCount > 0;
    }

    // ========== Bar duration ==========

    public void setBarDuration(long barDuration) {
        this.barDuration = barDuration;
    }

    public long getBarDuration() {
        return barDuration;
    }

    // ========== Display scale factor ==========

    /**
     * Sets the display scale factor for HiDPI displays.
     *
     * @param scale the scale factor (1.0 for standard displays, 2.0 for Retina)
     */
    public void setScaleFactor(float scale) {
        this.scaleFactor = Math.max(1.0f, scale);
    }

    /**
     * Gets the display scale factor.
     *
     * @return the scale factor
     */
    public float getScaleFactor() {
        return scaleFactor;
    }

    // ========== Category axis ==========

    /**
     * Sets the category axis for categorical charts.
     *
     * @param axis the category axis
     */
    public void setCategoryAxis(CategoryAxis axis) {
        this.categoryAxis = axis;
    }

    /**
     * Gets the category axis.
     *
     * @return the category axis, or null if not set
     */
    public CategoryAxis getCategoryAxis() {
        return categoryAxis;
    }

    /**
     * Returns whether this context has a configured category axis with categories.
     *
     * @return true if a category axis is set with at least one category
     */
    public boolean hasCategoryAxis() {
        return categoryAxis != null && categoryAxis.getCategoryCount() > 0;
    }

    /**
     * Returns the width of a single bar in pixels.
     *
     * <p>For categorical charts, returns the width of a single category slot.
     * For time-based charts, returns the width based on bar duration.
     */
    public double getBarWidth() {
        if (hasCategoryAxis()) {
            // For categorical charts, bar width is based on category count
            int categoryCount = categoryAxis.getCategoryCount();
            if (categoryCount > 0) {
                return (double) viewport.getChartWidth() / categoryCount;
            }
        }
        return coordinates.getPixelWidth(barDuration);
    }

    /**
     * Returns the pixels per bar (useful for LOD calculations).
     */
    public double getPixelsPerBar() {
        return viewport.getPixelsPerBar(barDuration);
    }

    // ========== Convenience methods ==========

    /**
     * Returns the width of the chart area in pixels.
     */
    public int getChartWidth() {
        return viewport.getChartWidth();
    }

    /**
     * Returns the height of the chart area in pixels.
     */
    public int getChartHeight() {
        return viewport.getChartHeight();
    }

    /**
     * Returns the total viewport width in pixels.
     */
    public int getWidth() {
        return viewport.getWidth();
    }

    /**
     * Returns the total viewport height in pixels.
     */
    public int getHeight() {
        return viewport.getHeight();
    }
}

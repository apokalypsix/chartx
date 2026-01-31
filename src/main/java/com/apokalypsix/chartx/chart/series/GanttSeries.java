package com.apokalypsix.chartx.chart.series;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.chart.style.GanttSeriesOptions;
import com.apokalypsix.chartx.chart.data.GanttData;
import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.ResourceManager;
import com.apokalypsix.chartx.core.render.api.Shader;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

/**
 * Renderable Gantt chart series using GanttData.
 *
 * <p>Renders task bars, milestones, progress indicators,
 * and dependency lines for project management visualizations.
 */
public class GanttSeries {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final String id;
    private final GanttData data;
    private final GanttSeriesOptions options;

    // Rendering resources (abstracted API)
    private Buffer fillBuffer;
    private Buffer lineBuffer;
    private ResourceManager resourceManager;
    private boolean initialized;

    // Reusable arrays
    private float[] fillVertices;
    private float[] lineVertices;

    /**
     * Creates a Gantt series with default options.
     */
    public GanttSeries(GanttData data) {
        this(data, new GanttSeriesOptions());
    }

    /**
     * Creates a Gantt series with the given options.
     */
    public GanttSeries(GanttData data, GanttSeriesOptions options) {
        this(UUID.randomUUID().toString(), data, options);
    }

    /**
     * Creates a Gantt series with a custom ID.
     */
    public GanttSeries(String id, GanttData data, GanttSeriesOptions options) {
        this.id = id;
        this.data = data;
        this.options = options;
        this.initialized = false;
    }

    public String getId() {
        return id;
    }

    public GanttData getData() {
        return data;
    }

    public GanttSeriesOptions getOptions() {
        return options;
    }

    public SeriesType getType() {
        return SeriesType.GANTT;
    }

    /**
     * Initializes rendering resources.
     */
    public void initialize(ResourceManager resources) {
        if (initialized) {
            return;
        }

        this.resourceManager = resources;

        fillBuffer = resources.getOrCreateBuffer(id + "_fill",
                BufferDescriptor.positionColor2D(8192 * FLOATS_PER_VERTEX));

        lineBuffer = resources.getOrCreateBuffer(id + "_line",
                BufferDescriptor.positionColor2D(4096 * FLOATS_PER_VERTEX));

        fillVertices = new float[8192 * FLOATS_PER_VERTEX];
        lineVertices = new float[4096 * FLOATS_PER_VERTEX];

        initialized = true;
    }

    /**
     * Disposes rendering resources.
     */
    public void dispose() {
        if (resourceManager != null) {
            resourceManager.disposeBuffer(id + "_fill");
            resourceManager.disposeBuffer(id + "_line");
        }
        fillBuffer = null;
        lineBuffer = null;
        resourceManager = null;
        initialized = false;
    }

    /**
     * Renders the Gantt chart.
     */
    public void render(RenderContext ctx) {
        if (!initialized || !options.isVisible()) {
            return;
        }

        CoordinateSystem coords = ctx.getCoordinatesForAxis(options.getYAxisId());

        Shader shader = resourceManager.getShader(ResourceManager.SHADER_DEFAULT);
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.bind();
        shader.setUniformMatrix4("uProjection", ctx.getProjectionMatrix());

        // Get visible time range from viewport
        Viewport viewport = ctx.getViewport();
        long visibleStart = viewport.getStartTime();
        long visibleEnd = viewport.getEndTime();

        // Build and draw fills (row stripes, task bars, progress, milestones)
        int fillFloatCount = buildFillVertices(coords, viewport, visibleStart, visibleEnd);
        if (fillFloatCount > 0) {
            fillBuffer.upload(fillVertices, 0, fillFloatCount);
            fillBuffer.draw(DrawMode.TRIANGLES);
        }

        // Draw dependency lines and borders
        int lineFloatCount = buildLineVertices(coords, viewport, visibleStart, visibleEnd);
        if (lineFloatCount > 0) {
            ctx.getDevice().setLineWidth(options.getDependencyLineWidth());
            lineBuffer.upload(lineVertices, 0, lineFloatCount);
            lineBuffer.draw(DrawMode.LINES);
        }

        shader.unbind();
    }

    private int buildFillVertices(CoordinateSystem coords, Viewport viewport, long visibleStart, long visibleEnd) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        float rowHeight = options.getRowHeight();
        float taskHeight = options.getTaskHeight();
        float taskOffset = options.getTaskVerticalOffset();

        // Get chart bounds
        float chartTop = viewport.getTopInset();
        float chartLeft = (float) coords.xValueToScreenX(visibleStart);
        float chartRight = (float) coords.xValueToScreenX(visibleEnd);

        // Draw row stripes
        if (options.isShowRowStripes()) {
            Color stripeColor = options.getRowStripeColor();
            float sr = stripeColor.getRed() / 255f;
            float sg = stripeColor.getGreen() / 255f;
            float sb = stripeColor.getBlue() / 255f;
            float sa = (stripeColor.getAlpha() / 255f) * opacity;

            int rowCount = data.getRowCount();
            for (int row = 0; row < rowCount; row += 2) {
                float y = chartTop + row * rowHeight;
                floatIndex = addQuad(fillVertices, floatIndex,
                        chartLeft, y, chartRight - chartLeft, rowHeight,
                        sr, sg, sb, sa);
            }
        }

        // Get visible tasks
        List<GanttData.Task> visibleTasks = data.getTasksInRange(visibleStart, visibleEnd);

        // Draw task bars
        for (GanttData.Task task : visibleTasks) {
            float startX = (float) coords.xValueToScreenX(task.getStartTime());
            float endX = (float) coords.xValueToScreenX(task.getEndTime());
            float y = chartTop + task.getRow() * rowHeight + taskOffset;
            float width = endX - startX;

            // Task fill
            Color taskColor = task.getColor() != null ? task.getColor() : options.getTaskColor();
            float tr = taskColor.getRed() / 255f;
            float tg = taskColor.getGreen() / 255f;
            float tb = taskColor.getBlue() / 255f;
            float ta = (taskColor.getAlpha() / 255f) * opacity;

            floatIndex = addQuad(fillVertices, floatIndex, startX, y, width, taskHeight,
                    tr, tg, tb, ta);

            // Progress fill
            if (options.isShowProgress() && task.getProgress() > 0) {
                Color progressColor = options.getProgressColor();
                float pr = progressColor.getRed() / 255f;
                float pg = progressColor.getGreen() / 255f;
                float pb = progressColor.getBlue() / 255f;
                float pa = opacity;

                float progressWidth = width * task.getProgress();
                float progressHeight = taskHeight * 0.3f;
                float progressY = y + taskHeight - progressHeight - 2;

                floatIndex = addQuad(fillVertices, floatIndex, startX + 2, progressY,
                        progressWidth - 4, progressHeight, pr, pg, pb, pa);
            }
        }

        // Draw milestones
        if (options.isShowMilestones()) {
            List<GanttData.Milestone> milestones = data.getMilestones();
            float milestoneSize = options.getMilestoneSize();
            float halfSize = milestoneSize / 2;

            for (GanttData.Milestone milestone : milestones) {
                if (milestone.getTime() < visibleStart || milestone.getTime() > visibleEnd) {
                    continue;
                }

                float x = (float) coords.xValueToScreenX(milestone.getTime());
                float y = chartTop + milestone.getRow() * rowHeight + rowHeight / 2;

                Color milestoneColor = milestone.getColor() != null ?
                        milestone.getColor() : options.getMilestoneColor();
                float mr = milestoneColor.getRed() / 255f;
                float mg = milestoneColor.getGreen() / 255f;
                float mb = milestoneColor.getBlue() / 255f;
                float ma = opacity;

                // Draw diamond shape (two triangles)
                // Top triangle
                floatIndex = addVertex(fillVertices, floatIndex, x, y - halfSize, mr, mg, mb, ma);
                floatIndex = addVertex(fillVertices, floatIndex, x - halfSize, y, mr, mg, mb, ma);
                floatIndex = addVertex(fillVertices, floatIndex, x + halfSize, y, mr, mg, mb, ma);
                // Bottom triangle
                floatIndex = addVertex(fillVertices, floatIndex, x, y + halfSize, mr, mg, mb, ma);
                floatIndex = addVertex(fillVertices, floatIndex, x + halfSize, y, mr, mg, mb, ma);
                floatIndex = addVertex(fillVertices, floatIndex, x - halfSize, y, mr, mg, mb, ma);
            }
        }

        return floatIndex;
    }

    private int buildLineVertices(CoordinateSystem coords, Viewport viewport, long visibleStart, long visibleEnd) {
        int floatIndex = 0;
        float opacity = options.getOpacity();
        float rowHeight = options.getRowHeight();
        float taskHeight = options.getTaskHeight();
        float taskOffset = options.getTaskVerticalOffset();
        float chartTop = viewport.getTopInset();

        // Draw task borders
        if (options.getBorderWidth() > 0) {
            Color borderColor = options.getTaskBorderColor();
            float br = borderColor.getRed() / 255f;
            float bg = borderColor.getGreen() / 255f;
            float bb = borderColor.getBlue() / 255f;
            float ba = opacity;

            List<GanttData.Task> visibleTasks = data.getTasksInRange(visibleStart, visibleEnd);

            for (GanttData.Task task : visibleTasks) {
                float startX = (float) coords.xValueToScreenX(task.getStartTime());
                float endX = (float) coords.xValueToScreenX(task.getEndTime());
                float y = chartTop + task.getRow() * rowHeight + taskOffset;

                // Top edge
                floatIndex = addVertex(lineVertices, floatIndex, startX, y, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, endX, y, br, bg, bb, ba);
                // Right edge
                floatIndex = addVertex(lineVertices, floatIndex, endX, y, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, endX, y + taskHeight, br, bg, bb, ba);
                // Bottom edge
                floatIndex = addVertex(lineVertices, floatIndex, endX, y + taskHeight, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, startX, y + taskHeight, br, bg, bb, ba);
                // Left edge
                floatIndex = addVertex(lineVertices, floatIndex, startX, y + taskHeight, br, bg, bb, ba);
                floatIndex = addVertex(lineVertices, floatIndex, startX, y, br, bg, bb, ba);
            }
        }

        // Draw dependency lines
        if (options.isShowDependencies()) {
            Color depColor = options.getDependencyColor();
            float dr = depColor.getRed() / 255f;
            float dg = depColor.getGreen() / 255f;
            float db = depColor.getBlue() / 255f;
            float da = opacity;

            List<GanttData.Dependency> deps = data.getDependencies();

            for (GanttData.Dependency dep : deps) {
                GanttData.Task fromTask = data.getTask(dep.getFromTaskId());
                GanttData.Task toTask = data.getTask(dep.getToTaskId());

                if (fromTask == null || toTask == null) {
                    continue;
                }

                // Calculate connection points based on dependency type
                float fromX, fromY, toX, toY;

                switch (dep.getType()) {
                    case FINISH_TO_START:
                        fromX = (float) coords.xValueToScreenX(fromTask.getEndTime());
                        fromY = chartTop + fromTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        toX = (float) coords.xValueToScreenX(toTask.getStartTime());
                        toY = chartTop + toTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        break;
                    case START_TO_START:
                        fromX = (float) coords.xValueToScreenX(fromTask.getStartTime());
                        fromY = chartTop + fromTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        toX = (float) coords.xValueToScreenX(toTask.getStartTime());
                        toY = chartTop + toTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        break;
                    case FINISH_TO_FINISH:
                        fromX = (float) coords.xValueToScreenX(fromTask.getEndTime());
                        fromY = chartTop + fromTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        toX = (float) coords.xValueToScreenX(toTask.getEndTime());
                        toY = chartTop + toTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        break;
                    case START_TO_FINISH:
                        fromX = (float) coords.xValueToScreenX(fromTask.getStartTime());
                        fromY = chartTop + fromTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        toX = (float) coords.xValueToScreenX(toTask.getEndTime());
                        toY = chartTop + toTask.getRow() * rowHeight + taskOffset + taskHeight / 2;
                        break;
                    default:
                        continue;
                }

                // Draw connecting line with elbow
                float elbowOffset = 10f;

                // Horizontal segment from source
                floatIndex = addVertex(lineVertices, floatIndex, fromX, fromY, dr, dg, db, da);
                floatIndex = addVertex(lineVertices, floatIndex, fromX + elbowOffset, fromY, dr, dg, db, da);

                // Vertical segment
                floatIndex = addVertex(lineVertices, floatIndex, fromX + elbowOffset, fromY, dr, dg, db, da);
                floatIndex = addVertex(lineVertices, floatIndex, fromX + elbowOffset, toY, dr, dg, db, da);

                // Horizontal segment to target
                floatIndex = addVertex(lineVertices, floatIndex, fromX + elbowOffset, toY, dr, dg, db, da);
                floatIndex = addVertex(lineVertices, floatIndex, toX, toY, dr, dg, db, da);
            }
        }

        return floatIndex;
    }

    private int addQuad(float[] vertices, int index, float x, float y, float width, float height,
                        float r, float g, float b, float a) {
        // Triangle 1
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        // Triangle 2
        index = addVertex(vertices, index, x, y, r, g, b, a);
        index = addVertex(vertices, index, x + width, y + height, r, g, b, a);
        index = addVertex(vertices, index, x + width, y, r, g, b, a);
        return index;
    }

    private int addVertex(float[] vertices, int index, float x, float y,
                          float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;
        return index;
    }
}

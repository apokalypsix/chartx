package com.apokalypsix.chartx.chart.style;

import java.awt.Color;

/**
 * Rendering options for Gantt chart series.
 *
 * <p>Controls appearance of task bars, milestones,
 * dependency lines, and progress indicators.
 */
public class GanttSeriesOptions {

    /** Task bar fill color */
    private Color taskColor = new Color(65, 131, 196);

    /** Task bar border color */
    private Color taskBorderColor = new Color(45, 101, 166);

    /** Progress fill color */
    private Color progressColor = new Color(45, 101, 166);

    /** Milestone color */
    private Color milestoneColor = new Color(220, 180, 50);

    /** Dependency line color */
    private Color dependencyColor = new Color(120, 120, 120);

    /** Row height in pixels */
    private float rowHeight = 30f;

    /** Task bar height ratio (relative to row height) */
    private float taskHeightRatio = 0.7f;

    /** Task bar corner radius */
    private float cornerRadius = 3f;

    /** Border width */
    private float borderWidth = 1.0f;

    /** Milestone diamond size */
    private float milestoneSize = 12f;

    /** Dependency line width */
    private float dependencyLineWidth = 1.5f;

    /** Whether to show progress bars */
    private boolean showProgress = true;

    /** Whether to show task labels */
    private boolean showLabels = true;

    /** Label color */
    private Color labelColor = new Color(220, 220, 220);

    /** Label font size */
    private float labelFontSize = 11f;

    /** Whether to show dependency lines */
    private boolean showDependencies = true;

    /** Whether to show milestones */
    private boolean showMilestones = true;

    /** Row stripe color (alternating rows) */
    private Color rowStripeColor = new Color(35, 35, 35, 50);

    /** Whether to show row stripes */
    private boolean showRowStripes = true;

    /** Overall opacity */
    private float opacity = 1.0f;

    /** Whether visible */
    private boolean visible = true;

    /** Y-axis ID for coordinate system */
    private String yAxisId = "main";

    /**
     * Creates default Gantt options.
     */
    public GanttSeriesOptions() {
    }

    /**
     * Creates a copy of the given options.
     */
    public GanttSeriesOptions(GanttSeriesOptions other) {
        this.taskColor = other.taskColor;
        this.taskBorderColor = other.taskBorderColor;
        this.progressColor = other.progressColor;
        this.milestoneColor = other.milestoneColor;
        this.dependencyColor = other.dependencyColor;
        this.rowHeight = other.rowHeight;
        this.taskHeightRatio = other.taskHeightRatio;
        this.cornerRadius = other.cornerRadius;
        this.borderWidth = other.borderWidth;
        this.milestoneSize = other.milestoneSize;
        this.dependencyLineWidth = other.dependencyLineWidth;
        this.showProgress = other.showProgress;
        this.showLabels = other.showLabels;
        this.labelColor = other.labelColor;
        this.labelFontSize = other.labelFontSize;
        this.showDependencies = other.showDependencies;
        this.showMilestones = other.showMilestones;
        this.rowStripeColor = other.rowStripeColor;
        this.showRowStripes = other.showRowStripes;
        this.opacity = other.opacity;
        this.visible = other.visible;
        this.yAxisId = other.yAxisId;
    }

    // ========== Getters ==========

    public Color getTaskColor() {
        return taskColor;
    }

    public Color getTaskBorderColor() {
        return taskBorderColor;
    }

    public Color getProgressColor() {
        return progressColor;
    }

    public Color getMilestoneColor() {
        return milestoneColor;
    }

    public Color getDependencyColor() {
        return dependencyColor;
    }

    public float getRowHeight() {
        return rowHeight;
    }

    public float getTaskHeightRatio() {
        return taskHeightRatio;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getMilestoneSize() {
        return milestoneSize;
    }

    public float getDependencyLineWidth() {
        return dependencyLineWidth;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public float getLabelFontSize() {
        return labelFontSize;
    }

    public boolean isShowDependencies() {
        return showDependencies;
    }

    public boolean isShowMilestones() {
        return showMilestones;
    }

    public Color getRowStripeColor() {
        return rowStripeColor;
    }

    public boolean isShowRowStripes() {
        return showRowStripes;
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getYAxisId() {
        return yAxisId;
    }

    /**
     * Computes the task bar height.
     */
    public float getTaskHeight() {
        return rowHeight * taskHeightRatio;
    }

    /**
     * Computes the vertical offset of task bar within row.
     */
    public float getTaskVerticalOffset() {
        return (rowHeight - getTaskHeight()) / 2;
    }

    // ========== Fluent Setters ==========

    public GanttSeriesOptions taskColor(Color color) {
        this.taskColor = color;
        return this;
    }

    public GanttSeriesOptions taskBorderColor(Color color) {
        this.taskBorderColor = color;
        return this;
    }

    public GanttSeriesOptions progressColor(Color color) {
        this.progressColor = color;
        return this;
    }

    public GanttSeriesOptions milestoneColor(Color color) {
        this.milestoneColor = color;
        return this;
    }

    public GanttSeriesOptions dependencyColor(Color color) {
        this.dependencyColor = color;
        return this;
    }

    public GanttSeriesOptions rowHeight(float height) {
        this.rowHeight = Math.max(10, height);
        return this;
    }

    public GanttSeriesOptions taskHeightRatio(float ratio) {
        this.taskHeightRatio = Math.max(0.3f, Math.min(1.0f, ratio));
        return this;
    }

    public GanttSeriesOptions cornerRadius(float radius) {
        this.cornerRadius = Math.max(0, radius);
        return this;
    }

    public GanttSeriesOptions borderWidth(float width) {
        this.borderWidth = Math.max(0, width);
        return this;
    }

    public GanttSeriesOptions milestoneSize(float size) {
        this.milestoneSize = Math.max(4, size);
        return this;
    }

    public GanttSeriesOptions dependencyLineWidth(float width) {
        this.dependencyLineWidth = Math.max(0.5f, width);
        return this;
    }

    public GanttSeriesOptions showProgress(boolean show) {
        this.showProgress = show;
        return this;
    }

    public GanttSeriesOptions showLabels(boolean show) {
        this.showLabels = show;
        return this;
    }

    public GanttSeriesOptions labelColor(Color color) {
        this.labelColor = color;
        return this;
    }

    public GanttSeriesOptions showDependencies(boolean show) {
        this.showDependencies = show;
        return this;
    }

    public GanttSeriesOptions showMilestones(boolean show) {
        this.showMilestones = show;
        return this;
    }

    public GanttSeriesOptions showRowStripes(boolean show) {
        this.showRowStripes = show;
        return this;
    }

    public GanttSeriesOptions rowStripeColor(Color color) {
        this.rowStripeColor = color;
        return this;
    }

    public GanttSeriesOptions opacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
        return this;
    }

    public GanttSeriesOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public GanttSeriesOptions yAxisId(String id) {
        this.yAxisId = id;
        return this;
    }
}

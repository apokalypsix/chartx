package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.GanttData;
import com.apokalypsix.chartx.chart.data.VectorFieldData;
import com.apokalypsix.chartx.chart.series.GanttSeries;
import com.apokalypsix.chartx.chart.series.VectorFieldSeries;
import com.apokalypsix.chartx.chart.style.GanttSeriesOptions;
import com.apokalypsix.chartx.chart.style.VectorFieldSeriesOptions;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLBackendResourceManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLRenderDevice;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.model.RenderContext;
import com.apokalypsix.chartx.core.render.util.ColorMap;
import com.apokalypsix.chartx.examples.library.AbstractDemo;
import com.apokalypsix.chartx.examples.library.DemoUIHelper;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

/**
 * Demo showcasing Gantt and Vector Field chart types.
 *
 * <p>These are specialized visualizations that use their own
 * rendering approach rather than the standard chart pipeline.
 */
public class GanttVectorDemo extends AbstractDemo {

    private GLCanvas canvas;
    private GLResourceManager legacyResources;
    private GLBackendResourceManager resourceManager;
    private GLRenderDevice device;

    private GanttData ganttData;
    private VectorFieldData vectorFieldData;

    private GanttSeries ganttSeries;
    private VectorFieldSeries vectorFieldSeries;

    private int currentChart = 0; // 0=gantt, 1=vector field

    public GanttVectorDemo() {
        setWindowSize(1400, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Gantt & Vector Field Charts";
    }

    @Override
    protected Component createDemoContent() {
        // Create Gantt data (project schedule)
        ganttData = createGanttData();

        // Create vector field data (wind/flow simulation)
        vectorFieldData = createVectorFieldData();

        // Create series
        ganttSeries = new GanttSeries(ganttData,
                new GanttSeriesOptions()
                        .taskColor(new Color(65, 131, 196))
                        .taskHeightRatio(0.7f)
                        .showProgress(true)
                        .showDependencies(true)
                        .dependencyColor(new Color(150, 150, 150))
                        .milestoneColor(new Color(241, 196, 15)));

        vectorFieldSeries = new VectorFieldSeries(vectorFieldData,
                new VectorFieldSeriesOptions()
                        .arrowColor(new Color(65, 131, 196))
                        .lengthScale(1.0f)
                        .colorMap(ColorMap.viridis())
                        .arrowStyle(VectorFieldSeriesOptions.ArrowStyle.ARROW));

        // Create OpenGL canvas
        GLProfile profile = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        canvas = new GLCanvas(caps);
        canvas.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                GL2ES2 gl = drawable.getGL().getGL2ES2();
                legacyResources = new GLResourceManager();
                legacyResources.initialize(gl);

                // Initialize abstracted API
                device = new GLRenderDevice(null);
                device.setGL(gl);
                device.initialize();
                resourceManager = new GLBackendResourceManager();
                resourceManager.initialize(device);

                ganttSeries.initialize(resourceManager);
                vectorFieldSeries.initialize(resourceManager);
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                ganttSeries.dispose();
                vectorFieldSeries.dispose();
                if (resourceManager != null) {
                    resourceManager.dispose();
                }
                if (device != null) {
                    device.dispose();
                }
                GL2ES2 gl = drawable.getGL().getGL2ES2();
                legacyResources.dispose(gl);
            }

            @Override
            public void display(GLAutoDrawable drawable) {
                GL2ES2 gl = drawable.getGL().getGL2ES2();
                int width = drawable.getSurfaceWidth();
                int height = drawable.getSurfaceHeight();

                gl.glViewport(0, 0, width, height);
                gl.glClearColor(0.12f, 0.12f, 0.14f, 1.0f);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);

                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

                // Create render context
                Viewport viewport = new Viewport();
                viewport.setSize(width, height);
                viewport.setInsets(60, 150, 40, 40);

                YAxisManager axisManager = new YAxisManager();
                MultiAxisCoordinateSystem coords = new MultiAxisCoordinateSystem(viewport, axisManager);

                // Set viewport ranges based on current chart
                switch (currentChart) {
                    case 0:
                        // Gantt: X is time (project duration), Y is task row
                        viewport.setTimeRange(ganttData.getMinTime() - 3600000, ganttData.getMaxTime() + 3600000);
                        viewport.setPriceRange(-0.5f, ganttData.getRowCount() + 0.5f);
                        break;
                    case 1:
                        // Vector field: use data coordinate ranges
                        double[] xCoords = vectorFieldData.getXCoords();
                        double[] yCoords = vectorFieldData.getYCoords();
                        viewport.setTimeRange((long) xCoords[0], (long) xCoords[xCoords.length - 1]);
                        viewport.setPriceRange((float) yCoords[0], (float) yCoords[yCoords.length - 1]);
                        break;
                }

                RenderContext ctx = new RenderContext(gl, viewport, coords, axisManager, legacyResources);
                ctx.setDevice(device);
                ctx.setResourceManager(resourceManager);

                // Render current chart
                switch (currentChart) {
                    case 0 -> ganttSeries.render(ctx);
                    case 1 -> vectorFieldSeries.render(ctx);
                }
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
        });

        return canvas;
    }

    private GanttData createGanttData() {
        GanttData data = new GanttData("project", "Software Development Project");
        long startTime = System.currentTimeMillis();
        long hourMs = 3600000L;

        // Phase 1: Planning
        GanttData.Task planning = data.addTask("planning", "Planning & Design",
                startTime, startTime + 10 * hourMs);
        planning.setProgress(1.0f);
        planning.setRow(0);

        GanttData.Task requirements = data.addTask("requirements", "Requirements Analysis",
                startTime + 2 * hourMs, startTime + 8 * hourMs);
        requirements.setProgress(1.0f);
        requirements.setRow(1);
        data.addDependency("planning", "requirements");

        // Phase 2: Development
        GanttData.Milestone devStart = data.addMilestone("dev_start", "Development Start",
                startTime + 10 * hourMs);
        devStart.setRow(2);

        GanttData.Task backend = data.addTask("backend", "Backend Development",
                startTime + 10 * hourMs, startTime + 30 * hourMs);
        backend.setProgress(0.8f);
        backend.setRow(3);
        data.addDependency("requirements", "backend");

        GanttData.Task frontend = data.addTask("frontend", "Frontend Development",
                startTime + 12 * hourMs, startTime + 35 * hourMs);
        frontend.setProgress(0.6f);
        frontend.setRow(4);
        data.addDependency("requirements", "frontend");

        GanttData.Task database = data.addTask("database", "Database Design",
                startTime + 10 * hourMs, startTime + 20 * hourMs);
        database.setProgress(1.0f);
        database.setRow(5);
        data.addDependency("requirements", "database");

        GanttData.Task api = data.addTask("api", "API Implementation",
                startTime + 20 * hourMs, startTime + 40 * hourMs);
        api.setProgress(0.5f);
        api.setRow(6);
        data.addDependency("database", "api");

        // Phase 3: Testing
        GanttData.Task testing = data.addTask("testing", "Integration Testing",
                startTime + 35 * hourMs, startTime + 50 * hourMs);
        testing.setProgress(0.2f);
        testing.setRow(7);
        data.addDependency("backend", "testing");
        data.addDependency("frontend", "testing");

        GanttData.Task qa = data.addTask("qa", "QA & Bug Fixes",
                startTime + 45 * hourMs, startTime + 60 * hourMs);
        qa.setProgress(0.0f);
        qa.setRow(8);
        data.addDependency("testing", "qa");

        // Phase 4: Release
        GanttData.Milestone release = data.addMilestone("release", "Product Release",
                startTime + 60 * hourMs);
        release.setRow(9);

        return data;
    }

    private VectorFieldData createVectorFieldData() {
        int rows = 20;
        int cols = 30;

        VectorFieldData data = new VectorFieldData("wind", "Wind Flow", rows, cols,
                0, cols - 1, 0, rows - 1);

        // Create a flow pattern (circular vortex + background flow)
        float centerX = cols / 2f;
        float centerY = rows / 2f;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = col - centerX;
                float y = row - centerY;
                float dist = (float) Math.sqrt(x * x + y * y);

                // Vortex component (circular flow)
                float vortexStrength = 0.5f / (1 + dist * 0.1f);
                float vx = -y * vortexStrength;
                float vy = x * vortexStrength;

                // Background flow (right-to-left)
                vx += 0.3f;
                vy += 0.1f * (float) Math.sin(col * 0.3f);

                data.setVector(row, col, vx, vy);
            }
        }

        return data;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Chart Type:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        ButtonGroup group = new ButtonGroup();

        JRadioButton ganttBtn = new JRadioButton("Gantt Chart", true);
        ganttBtn.setForeground(Color.WHITE);
        ganttBtn.addActionListener(e -> {
            currentChart = 0;
            canvas.repaint();
        });
        group.add(ganttBtn);
        panel.add(ganttBtn);

        JRadioButton vectorBtn = new JRadioButton("Vector Field", false);
        vectorBtn.setForeground(Color.WHITE);
        vectorBtn.addActionListener(e -> {
            currentChart = 1;
            canvas.repaint();
        });
        group.add(vectorBtn);
        panel.add(vectorBtn);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Show dependencies toggle for Gantt
        JCheckBox depsBox = new JCheckBox("Show Dependencies");
        depsBox.setForeground(Color.WHITE);
        depsBox.setSelected(true);
        depsBox.addActionListener(e -> {
            ganttSeries.getOptions().showDependencies(depsBox.isSelected());
            canvas.repaint();
        });
        panel.add(depsBox);

        // Show progress toggle for Gantt
        JCheckBox progressBox = new JCheckBox("Show Progress");
        progressBox.setForeground(Color.WHITE);
        progressBox.setSelected(true);
        progressBox.addActionListener(e -> {
            ganttSeries.getOptions().showProgress(progressBox.isSelected());
            canvas.repaint();
        });
        panel.add(progressBox);

        // Arrow scale slider
        panel.add(DemoUIHelper.createControlLabel("Arrow Scale:"));
        JSlider scaleSlider = new JSlider(20, 200, 100);
        scaleSlider.addChangeListener(e -> {
            float scale = scaleSlider.getValue() / 100f;
            vectorFieldSeries.getOptions().lengthScale(scale);
            canvas.repaint();
        });
        panel.add(scaleSlider);

        return panel;
    }

    @Override
    protected void onDemoClosing() {
        if (canvas != null) {
            canvas.destroy();
        }
    }

    public static void main(String[] args) {
        launch(new GanttVectorDemo(), args);
    }
}

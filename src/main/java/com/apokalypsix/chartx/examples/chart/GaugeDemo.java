package com.apokalypsix.chartx.examples.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;

import com.apokalypsix.chartx.chart.axis.Viewport;
import com.apokalypsix.chartx.chart.data.GaugeData;
import com.apokalypsix.chartx.chart.series.LinearGaugeSeries;
import com.apokalypsix.chartx.chart.series.RadialGaugeSeries;
import com.apokalypsix.chartx.chart.style.LinearGaugeSeriesOptions;
import com.apokalypsix.chartx.chart.style.RadialGaugeSeriesOptions;
import com.apokalypsix.chartx.core.coordinate.MultiAxisCoordinateSystem;
import com.apokalypsix.chartx.core.coordinate.YAxisManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLBackendResourceManager;
import com.apokalypsix.chartx.core.render.backend.opengl.GLRenderDevice;
import com.apokalypsix.chartx.core.render.gl.GLResourceManager;
import com.apokalypsix.chartx.core.render.model.RenderContext;
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
 * Demo showcasing Linear and Radial gauge series.
 */
public class GaugeDemo extends AbstractDemo {

    private GLCanvas canvas;
    private GLResourceManager legacyResources;
    private GLBackendResourceManager resourceManager;
    private GLRenderDevice device;

    private GaugeData gaugeData1;
    private GaugeData gaugeData2;
    private GaugeData gaugeData3;

    private LinearGaugeSeries horizontalGauge;
    private LinearGaugeSeries verticalGauge;
    private RadialGaugeSeries radialGauge1;
    private RadialGaugeSeries radialGauge2;

    private Timer animationTimer;
    private float animationPhase = 0;

    public GaugeDemo() {
        setWindowSize(1200, 800);
    }

    @Override
    protected String getDemoTitle() {
        return "ChartX Demo - Gauge Charts";
    }

    @Override
    protected Component createDemoContent() {
        // Create gauge data with zones
        gaugeData1 = new GaugeData("gauge1", "Speed", 0, 200);
        gaugeData1.setTrafficLightZones(120, 160);
        gaugeData1.setValue(75);

        gaugeData2 = new GaugeData("gauge2", "Temperature", 0, 100);
        gaugeData2.addZone(0, 30, new Color(50, 150, 255), "Cold");
        gaugeData2.addZone(30, 60, new Color(50, 200, 100), "Normal");
        gaugeData2.addZone(60, 80, new Color(255, 180, 50), "Warm");
        gaugeData2.addZone(80, 100, new Color(220, 50, 50), "Hot");
        gaugeData2.setValue(45);

        gaugeData3 = new GaugeData("gauge3", "Fuel", 0, 100);
        gaugeData3.addZone(0, 20, new Color(220, 50, 50), "Low");
        gaugeData3.addZone(20, 100, new Color(50, 180, 100), "OK");
        gaugeData3.setValue(65);

        // Create gauge series
        horizontalGauge = new LinearGaugeSeries(gaugeData1,
                new LinearGaugeSeriesOptions()
                        .horizontal()
                        .trackThickness(0.4f)
                        .showTicks(true)
                        .majorTickCount(5));

        verticalGauge = new LinearGaugeSeries(gaugeData2,
                new LinearGaugeSeriesOptions()
                        .vertical()
                        .trackThickness(0.5f)
                        .indicatorStyle(LinearGaugeSeriesOptions.IndicatorStyle.BOTH));

        radialGauge1 = new RadialGaugeSeries(gaugeData1,
                new RadialGaugeSeriesOptions()
                        .threeQuarter()
                        .needleStyle(RadialGaugeSeriesOptions.NeedleStyle.ARROW)
                        .showValueArc(true));

        radialGauge2 = new RadialGaugeSeries(gaugeData3,
                new RadialGaugeSeriesOptions()
                        .halfCircle()
                        .needleStyle(RadialGaugeSeriesOptions.NeedleStyle.TRIANGLE)
                        .innerRadiusRatio(0.6f));

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

                horizontalGauge.initialize(resourceManager);
                verticalGauge.initialize(resourceManager);
                radialGauge1.initialize(resourceManager);
                radialGauge2.initialize(resourceManager);
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                horizontalGauge.dispose();
                verticalGauge.dispose();
                radialGauge1.dispose();
                radialGauge2.dispose();
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
                viewport.setInsets(0, 0, 0, 0);

                YAxisManager axisManager = new YAxisManager();
                MultiAxisCoordinateSystem coords = new MultiAxisCoordinateSystem(viewport, axisManager);

                RenderContext ctx = new RenderContext(gl, viewport, coords, axisManager, legacyResources);
                ctx.setDevice(device);
                ctx.setResourceManager(resourceManager);

                // Layout: 2x2 grid
                int halfW = width / 2;
                int halfH = height / 2;
                int padding = 40;

                // Top-left: Horizontal linear gauge
                horizontalGauge.render(ctx, padding, padding, halfW - 2 * padding, halfH - 2 * padding);

                // Top-right: Radial gauge 1
                float radius1 = Math.min(halfW, halfH) / 2 - padding;
                radialGauge1.render(ctx, halfW + halfW / 2, halfH / 2, radius1);

                // Bottom-left: Vertical linear gauge
                verticalGauge.render(ctx, padding, halfH + padding, halfW - 2 * padding, halfH - 2 * padding);

                // Bottom-right: Radial gauge 2
                float radius2 = Math.min(halfW, halfH) / 2 - padding;
                radialGauge2.render(ctx, halfW + halfW / 2, halfH + halfH / 2, radius2);
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
        });

        return canvas;
    }

    @Override
    protected JPanel createControlPanel() {
        JPanel panel = DemoUIHelper.createControlPanel();

        panel.add(DemoUIHelper.createInfoLabel("Gauge Values:"));
        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Speed slider
        panel.add(DemoUIHelper.createControlLabel("Speed:"));
        JSlider speedSlider = new JSlider(0, 200, 75);
        speedSlider.addChangeListener(e -> {
            gaugeData1.setValue(speedSlider.getValue());
            canvas.repaint();
        });
        panel.add(speedSlider);

        // Temperature slider
        panel.add(DemoUIHelper.createControlLabel("Temp:"));
        JSlider tempSlider = new JSlider(0, 100, 45);
        tempSlider.addChangeListener(e -> {
            gaugeData2.setValue(tempSlider.getValue());
            canvas.repaint();
        });
        panel.add(tempSlider);

        // Fuel slider
        panel.add(DemoUIHelper.createControlLabel("Fuel:"));
        JSlider fuelSlider = new JSlider(0, 100, 65);
        fuelSlider.addChangeListener(e -> {
            gaugeData3.setValue(fuelSlider.getValue());
            canvas.repaint();
        });
        panel.add(fuelSlider);

        panel.add(DemoUIHelper.createHorizontalSpacer());

        // Animation toggle
        JCheckBox animateBox = new JCheckBox("Animate");
        animateBox.setForeground(Color.WHITE);
        animateBox.addActionListener(e -> {
            if (animateBox.isSelected()) {
                startAnimation();
            } else {
                stopAnimation();
            }
        });
        panel.add(animateBox);

        return panel;
    }

    private void startAnimation() {
        if (animationTimer != null) return;
        animationTimer = new Timer(50, e -> {
            animationPhase += 0.05f;
            float sin = (float) Math.sin(animationPhase);
            gaugeData1.setValue(100 + sin * 80);
            gaugeData2.setValue(50 + sin * 40);
            gaugeData3.setValue(50 + (float) Math.sin(animationPhase * 0.7) * 45);
            canvas.repaint();
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    @Override
    protected void onDemoClosing() {
        stopAnimation();
        if (canvas != null) {
            canvas.destroy();
        }
    }

    public static void main(String[] args) {
        launch(new GaugeDemo(), args);
    }
}

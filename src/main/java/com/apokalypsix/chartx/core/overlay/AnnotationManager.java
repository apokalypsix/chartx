package com.apokalypsix.chartx.core.overlay;

import com.apokalypsix.chartx.core.render.model.DrawingLayerV2;
import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.chart.data.Data;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.overlay.Drawing;

import java.util.*;

/**
 * Manages automatic annotation generation and lifecycle.
 *
 * <p>The AnnotationManager:
 * <ul>
 *   <li>Registers annotation generators</li>
 *   <li>Tracks generated annotations per generator</li>
 *   <li>Updates annotations when data changes</li>
 *   <li>Adds/removes annotations from the drawing layer</li>
 * </ul>
 */
public class AnnotationManager {

    private final Map<String, AnnotationGenerator> generators = new LinkedHashMap<>();
    private final Map<String, List<Drawing>> generatedAnnotations = new HashMap<>();

    private OhlcData sourceData;
    private DrawingLayerV2 drawingLayer;
    private final DataListener dataListener;

    /**
     * Creates an annotation manager.
     */
    public AnnotationManager() {
        this.dataListener = new DataListener() {
            @Override
            public void onDataAppended(Data<?> data, int newIndex) {
                regenerateAll();
            }

            @Override
            public void onDataUpdated(Data<?> data, int index) {
                regenerateAll();
            }

            @Override
            public void onDataCleared(Data<?> data) {
                clearAllAnnotations();
            }
        };
    }

    // ========== Configuration ==========

    /**
     * Sets the source data for annotation generation.
     */
    public void setSourceData(OhlcData data) {
        if (this.sourceData != null) {
            this.sourceData.removeListener(dataListener);
        }

        this.sourceData = data;

        if (data != null) {
            data.addListener(dataListener);
            regenerateAll();
        } else {
            clearAllAnnotations();
        }
    }

    /**
     * Sets the drawing layer for annotation display.
     */
    public void setDrawingLayer(DrawingLayerV2 layer) {
        // Remove existing annotations from old layer
        if (this.drawingLayer != null) {
            removeAllFromLayer();
        }

        this.drawingLayer = layer;

        // Add annotations to new layer
        if (layer != null) {
            addAllToLayer();
        }
    }

    // ========== Generator Registration ==========

    /**
     * Registers an annotation generator.
     */
    public void registerGenerator(AnnotationGenerator generator) {
        generators.put(generator.getId(), generator);
        generatedAnnotations.put(generator.getId(), new ArrayList<>());

        if (sourceData != null && generator.isEnabled()) {
            regenerateForGenerator(generator.getId());
        }
    }

    /**
     * Unregisters an annotation generator.
     */
    public void unregisterGenerator(String generatorId) {
        AnnotationGenerator generator = generators.remove(generatorId);
        if (generator != null) {
            List<Drawing> annotations = generatedAnnotations.remove(generatorId);
            if (annotations != null && drawingLayer != null) {
                for (Drawing drawing : annotations) {
                    drawingLayer.removeDrawing(drawing);
                }
            }
        }
    }

    /**
     * Returns a registered generator.
     */
    public AnnotationGenerator getGenerator(String generatorId) {
        return generators.get(generatorId);
    }

    /**
     * Returns all registered generators.
     */
    public Collection<AnnotationGenerator> getGenerators() {
        return Collections.unmodifiableCollection(generators.values());
    }

    // ========== Enable/Disable ==========

    /**
     * Enables or disables a generator.
     */
    public void setGeneratorEnabled(String generatorId, boolean enabled) {
        AnnotationGenerator generator = generators.get(generatorId);
        if (generator != null) {
            boolean wasEnabled = generator.isEnabled();
            generator.setEnabled(enabled);

            if (enabled && !wasEnabled) {
                regenerateForGenerator(generatorId);
            } else if (!enabled && wasEnabled) {
                clearAnnotationsForGenerator(generatorId);
            }
        }
    }

    /**
     * Returns true if a generator is enabled.
     */
    public boolean isGeneratorEnabled(String generatorId) {
        AnnotationGenerator generator = generators.get(generatorId);
        return generator != null && generator.isEnabled();
    }

    // ========== Generation ==========

    /**
     * Regenerates annotations for all enabled generators.
     */
    public void regenerateAll() {
        for (String generatorId : generators.keySet()) {
            regenerateForGenerator(generatorId);
        }
    }

    /**
     * Regenerates annotations for a specific generator.
     */
    public void regenerateForGenerator(String generatorId) {
        AnnotationGenerator generator = generators.get(generatorId);
        if (generator == null || !generator.isEnabled() || sourceData == null) {
            return;
        }

        // Remove existing annotations
        clearAnnotationsForGenerator(generatorId);

        // Generate new annotations
        List<Drawing> newAnnotations = generator.generate(sourceData);
        if (newAnnotations != null && !newAnnotations.isEmpty()) {
            generatedAnnotations.get(generatorId).addAll(newAnnotations);

            // Add to drawing layer
            if (drawingLayer != null) {
                for (Drawing drawing : newAnnotations) {
                    drawingLayer.addDrawing(drawing);
                }
            }
        }
    }

    /**
     * Clears annotations for a specific generator.
     */
    private void clearAnnotationsForGenerator(String generatorId) {
        List<Drawing> annotations = generatedAnnotations.get(generatorId);
        if (annotations != null) {
            if (drawingLayer != null) {
                for (Drawing drawing : annotations) {
                    drawingLayer.removeDrawing(drawing);
                }
            }
            annotations.clear();
        }
    }

    /**
     * Clears all annotations.
     */
    private void clearAllAnnotations() {
        for (String generatorId : generatedAnnotations.keySet()) {
            clearAnnotationsForGenerator(generatorId);
        }
    }

    /**
     * Removes all annotations from the drawing layer.
     */
    private void removeAllFromLayer() {
        if (drawingLayer == null) return;

        for (List<Drawing> annotations : generatedAnnotations.values()) {
            for (Drawing drawing : annotations) {
                drawingLayer.removeDrawing(drawing);
            }
        }
    }

    /**
     * Adds all annotations to the drawing layer.
     */
    private void addAllToLayer() {
        if (drawingLayer == null) return;

        for (List<Drawing> annotations : generatedAnnotations.values()) {
            for (Drawing drawing : annotations) {
                drawingLayer.addDrawing(drawing);
            }
        }
    }

    // ========== Convenience Registration ==========

    /**
     * Registers built-in generators.
     */
    public void registerBuiltInGenerators() {
        registerGenerator(new DaySeparatorGenerator());
        registerGenerator(new SessionBoundaryGenerator());
    }
}

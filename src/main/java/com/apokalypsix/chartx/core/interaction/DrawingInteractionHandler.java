package com.apokalypsix.chartx.core.interaction;

import com.apokalypsix.chartx.chart.interaction.DrawingTool;
import com.apokalypsix.chartx.core.coordinate.CoordinateSystem;
import com.apokalypsix.chartx.core.render.model.DrawingLayerV2;
import com.apokalypsix.chartx.chart.overlay.*;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles mouse and keyboard interaction for drawing tools.
 *
 * <p>This handler manages:
 * <ul>
 *   <li>Drawing creation (click to place anchor points)</li>
 *   <li>Drawing selection (click to select)</li>
 *   <li>Drawing editing (drag handles to move anchor points)</li>
 *   <li>Drawing deletion (Delete key to remove selected drawing)</li>
 * </ul>
 *
 * <p>The handler uses a state machine approach for clean interaction handling.
 */
public class DrawingInteractionHandler {

    /** Listener interface for drawing events */
    public interface DrawingListener {
        /** Called when a drawing is created */
        void onDrawingCreated(Drawing drawing);

        /** Called when a drawing is modified */
        void onDrawingModified(Drawing drawing);

        /** Called when a drawing is deleted */
        void onDrawingDeleted(Drawing drawing);

        /** Called when selection changes */
        void onSelectionChanged(Drawing drawing);
    }

    private final DrawingLayerV2 drawingLayer;
    private final List<DrawingListener> listeners = new ArrayList<>();

    // Current state
    private DrawingTool activeTool = DrawingTool.NONE;
    private Drawing drawingInProgress;
    private int clickCount = 0;

    // Editing state
    private Drawing selectedDrawing;
    private Drawing.HandleType activeHandle = Drawing.HandleType.NONE;
    private boolean isDragging = false;

    // Drag tracking
    private int lastMouseX;
    private int lastMouseY;
    private long dragStartTime;
    private double dragStartPrice;

    // Coordinate system (set per interaction)
    private CoordinateSystem coords;

    /**
     * Creates a handler for the specified drawing layer.
     */
    public DrawingInteractionHandler(DrawingLayerV2 drawingLayer) {
        this.drawingLayer = drawingLayer;
    }

    // ========== Tool management ==========

    /**
     * Sets the active drawing tool.
     */
    public void setActiveTool(DrawingTool tool) {
        // Cancel any drawing in progress
        if (drawingInProgress != null) {
            drawingLayer.removeDrawing(drawingInProgress);
            drawingInProgress = null;
        }
        clickCount = 0;

        this.activeTool = tool;

        // If switching away from SELECT, deselect
        if (tool != DrawingTool.SELECT) {
            if (selectedDrawing != null) {
                selectedDrawing.setSelected(false);
                notifySelectionChanged(null);
                selectedDrawing = null;
            }
            drawingLayer.deselectAll();
        }
    }

    /**
     * Returns the active drawing tool.
     */
    public DrawingTool getActiveTool() {
        return activeTool;
    }

    /**
     * Returns true if a drawing tool is active (not NONE).
     */
    public boolean isToolActive() {
        return activeTool != DrawingTool.NONE;
    }

    // ========== Listener management ==========

    /**
     * Adds a drawing listener.
     */
    public void addDrawingListener(DrawingListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a drawing listener.
     */
    public void removeDrawingListener(DrawingListener listener) {
        listeners.remove(listener);
    }

    // ========== Mouse event handling ==========

    /**
     * Handles mouse pressed events.
     *
     * @param e the mouse event
     * @param coords the coordinate system for transformations
     * @return true if the event was consumed
     */
    public boolean mousePressed(MouseEvent e, CoordinateSystem coords) {
        this.coords = coords;
        int x = e.getX();
        int y = e.getY();

        if (activeTool == DrawingTool.NONE) {
            return false;
        }

        if (activeTool == DrawingTool.SELECT) {
            return handleSelectPress(x, y, coords);
        }

        if (activeTool.isCreationTool()) {
            return handleCreationPress(x, y, coords);
        }

        return false;
    }

    /**
     * Handles mouse released events.
     *
     * @param e the mouse event
     * @param coords the coordinate system for transformations
     * @return true if the event was consumed
     */
    public boolean mouseReleased(MouseEvent e, CoordinateSystem coords) {
        this.coords = coords;

        if (isDragging) {
            isDragging = false;
            activeHandle = Drawing.HandleType.NONE;

            if (selectedDrawing != null) {
                notifyDrawingModified(selectedDrawing);
            }
            return true;
        }

        return activeTool != DrawingTool.NONE;
    }

    /**
     * Handles mouse dragged events.
     *
     * @param e the mouse event
     * @param coords the coordinate system for transformations
     * @return true if the event was consumed
     */
    public boolean mouseDragged(MouseEvent e, CoordinateSystem coords) {
        this.coords = coords;
        int x = e.getX();
        int y = e.getY();

        if (isDragging && selectedDrawing != null && activeHandle != Drawing.HandleType.NONE) {
            handleDrag(x, y, coords);
            return true;
        }

        // Update drawing in progress for visual feedback
        if (drawingInProgress != null && activeTool.getRequiredClicks() == 2) {
            long time = coords.screenXToXValue(x);
            double price = coords.screenYToYValue(y);
            setSecondPoint(drawingInProgress, time, price);
            drawingLayer.markDirty();
            return true;
        }

        return activeTool != DrawingTool.NONE;
    }

    /**
     * Handles mouse moved events.
     *
     * @param e the mouse event
     * @param coords the coordinate system for transformations
     * @return the cursor to display, or null for default
     */
    public Cursor mouseMoved(MouseEvent e, CoordinateSystem coords) {
        this.coords = coords;
        int x = e.getX();
        int y = e.getY();

        // Update drawing in progress for visual feedback
        if (drawingInProgress != null && activeTool.getRequiredClicks() == 2) {
            long time = coords.screenXToXValue(x);
            double price = coords.screenYToYValue(y);
            setSecondPoint(drawingInProgress, time, price);
            drawingLayer.markDirty();
        }

        if (activeTool == DrawingTool.SELECT) {
            // Check if over a handle
            if (selectedDrawing != null) {
                Drawing.HandleType handle = drawingLayer.getHandleAt(selectedDrawing, x, y, coords);
                if (handle != Drawing.HandleType.NONE) {
                    return getCursorForHandle(handle);
                }
            }

            // Check if over a drawing
            Drawing drawing = drawingLayer.getDrawingAt(x, y, coords);
            if (drawing != null) {
                return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }

        if (activeTool.isCreationTool()) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }

        return null;
    }

    /**
     * Handles key pressed events.
     *
     * @param e the key event
     * @return true if the event was consumed
     */
    public boolean keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (selectedDrawing != null) {
                Drawing toDelete = selectedDrawing;
                selectedDrawing = null;
                drawingLayer.removeDrawing(toDelete);
                notifyDrawingDeleted(toDelete);
                notifySelectionChanged(null);
                return true;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // Cancel drawing in progress
            if (drawingInProgress != null) {
                drawingLayer.removeDrawing(drawingInProgress);
                drawingInProgress = null;
                clickCount = 0;
                return true;
            }

            // Deselect
            if (selectedDrawing != null) {
                selectedDrawing.setSelected(false);
                notifySelectionChanged(null);
                selectedDrawing = null;
                drawingLayer.markDirty();
                return true;
            }

            // Reset tool
            if (activeTool != DrawingTool.NONE) {
                setActiveTool(DrawingTool.NONE);
                return true;
            }
        }

        return false;
    }

    // ========== Selection handling ==========

    private boolean handleSelectPress(int x, int y, CoordinateSystem coords) {
        // First, check if clicking on a handle of the selected drawing
        if (selectedDrawing != null) {
            Drawing.HandleType handle = drawingLayer.getHandleAt(selectedDrawing, x, y, coords);
            if (handle != Drawing.HandleType.NONE) {
                activeHandle = handle;
                isDragging = true;
                lastMouseX = x;
                lastMouseY = y;
                dragStartTime = coords.screenXToXValue(x);
                dragStartPrice = coords.screenYToYValue(y);
                return true;
            }
        }

        // Check if clicking on a drawing
        Drawing drawing = drawingLayer.getDrawingAt(x, y, coords);

        if (drawing != null) {
            // Select this drawing
            if (selectedDrawing != null && selectedDrawing != drawing) {
                selectedDrawing.setSelected(false);
            }

            selectedDrawing = drawing;
            drawing.setSelected(true);
            drawingLayer.markDirty();
            notifySelectionChanged(drawing);

            // Start body drag
            activeHandle = Drawing.HandleType.BODY;
            isDragging = true;
            lastMouseX = x;
            lastMouseY = y;
            dragStartTime = coords.screenXToXValue(x);
            dragStartPrice = coords.screenYToYValue(y);
            return true;
        } else {
            // Click on empty space - deselect
            if (selectedDrawing != null) {
                selectedDrawing.setSelected(false);
                notifySelectionChanged(null);
                selectedDrawing = null;
                drawingLayer.markDirty();
            }
            return false; // Allow pan/zoom on empty space
        }
    }

    // ========== Creation handling ==========

    private boolean handleCreationPress(int x, int y, CoordinateSystem coords) {
        long time = coords.screenXToXValue(x);
        double price = coords.screenYToYValue(y);

        return switch (activeTool) {
            case TREND_LINE -> handleTwoPointCreation(time, price, "trendline",
                    () -> new TrendLine(generateId("trendline"), time, price));
            case HORIZONTAL_LINE -> handleSingleClickCreation(time, price, "hline",
                    () -> new HorizontalLine(generateId("hline"), time, price));
            case VERTICAL_LINE -> handleSingleClickCreation(time, price, "vline",
                    () -> new VerticalLine(generateId("vline"), time, price));
            case RAY -> handleTwoPointCreation(time, price, "ray",
                    () -> new Ray(generateId("ray"), time, price));
            case EXTENDED_LINE -> handleTwoPointCreation(time, price, "extline",
                    () -> new ExtendedLine(generateId("extline"), time, price));
            case RECTANGLE -> handleTwoPointCreation(time, price, "rect",
                    () -> new Rectangle(generateId("rect"), time, price));
            case ELLIPSE -> handleTwoPointCreation(time, price, "ellipse",
                    () -> new Ellipse(generateId("ellipse"), time, price));
            case FIBONACCI_RETRACEMENT -> handleTwoPointCreation(time, price, "fib",
                    () -> new FibonacciRetracement(generateId("fib"), time, price));
            case FIBONACCI_EXTENSION -> handleThreePointCreation(time, price, "fibext",
                    () -> new FibonacciExtension(generateId("fibext"), time, price));
            case PARALLEL_CHANNEL -> handleThreePointCreation(time, price, "channel",
                    () -> new ParallelChannel(generateId("channel"), time, price));
            case PITCHFORK -> handleThreePointCreation(time, price, "pitchfork",
                    () -> new Pitchfork(generateId("pitchfork"), time, price));
            case TRIANGLE -> handleThreePointCreation(time, price, "triangle",
                    () -> new Triangle(generateId("triangle"), time, price));
            case ARROW -> handleTwoPointCreation(time, price, "arrow",
                    () -> new Arrow(generateId("arrow"), time, price));
            case MEASURE_TOOL -> handleTwoPointCreation(time, price, "measure",
                    () -> new MeasureTool(generateId("measure"), time, price));
            case PRICE_RANGE -> handleTwoPointCreation(time, price, "pricerange",
                    () -> new PriceRange(generateId("pricerange"), price));
            case CALLOUT -> handleTwoPointCreation(time, price, "callout",
                    () -> new Callout(generateId("callout"), time, price));
            case PRICE_LABEL -> handleSingleClickCreation(time, price, "pricelabel",
                    () -> new PriceLabel(generateId("pricelabel"), time, price));
            case REGRESSION_CHANNEL -> handleTwoPointCreation(time, price, "regchannel",
                    () -> new RegressionChannel(generateId("regchannel"), time, price));
            case FIBONACCI_TIME_ZONES -> handleTwoPointCreation(time, price, "fibtimezones",
                    () -> new FibonacciTimeZones(generateId("fibtimezones"), time, price));
            case FIBONACCI_FAN -> handleTwoPointCreation(time, price, "fibfan",
                    () -> new FibonacciFan(generateId("fibfan"), time, price));
            case FIBONACCI_ARC -> handleTwoPointCreation(time, price, "fibarc",
                    () -> new FibonacciArc(generateId("fibarc"), time, price));
            case POLYLINE -> handleVariablePointCreation(time, price, "polyline",
                    () -> new Polyline(generateId("polyline"), time, price));
            case POLYGON -> handleVariablePointCreation(time, price, "polygon",
                    () -> new Polygon(generateId("polygon"), time, price));
            case ARC -> handleTwoPointCreation(time, price, "arc",
                    () -> new Arc(generateId("arc"), time, price));
            case GANN_FAN -> handleTwoPointCreation(time, price, "gannfan",
                    () -> new GannFan(generateId("gannfan"), time, price));
            case GANN_BOX -> handleTwoPointCreation(time, price, "gannbox",
                    () -> new GannBox(generateId("gannbox"), time, price));
            case NOTE -> handleSingleClickCreation(time, price, "note",
                    () -> new Note(generateId("note"), time, price));
            default -> false;
        };
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean handleSingleClickCreation(long time, double price, String prefix,
                                               java.util.function.Supplier<Drawing> creator) {
        Drawing drawing = creator.get();
        drawingLayer.addDrawing(drawing);
        notifyDrawingCreated(drawing);
        return true;
    }

    private boolean handleTwoPointCreation(long time, double price, String prefix,
                                            java.util.function.Supplier<Drawing> creator) {
        if (clickCount == 0) {
            // First click - create drawing with start point
            drawingInProgress = creator.get();

            // Set temporary second point at same location
            setSecondPoint(drawingInProgress, time, price);

            drawingLayer.addDrawing(drawingInProgress);
            clickCount = 1;
            return true;
        } else {
            // Second click - complete the drawing
            setSecondPoint(drawingInProgress, time, price);

            notifyDrawingCreated(drawingInProgress);
            drawingInProgress = null;
            clickCount = 0;
            return true;
        }
    }

    private boolean handleThreePointCreation(long time, double price, String prefix,
                                              java.util.function.Supplier<Drawing> creator) {
        if (clickCount == 0) {
            // First click - create drawing with first point
            drawingInProgress = creator.get();
            drawingLayer.addDrawing(drawingInProgress);
            clickCount = 1;
            return true;
        } else if (clickCount == 1) {
            // Second click - set second point
            setSecondPoint(drawingInProgress, time, price);
            clickCount = 2;
            return true;
        } else {
            // Third click - complete the drawing
            setThirdPoint(drawingInProgress, time, price);

            notifyDrawingCreated(drawingInProgress);
            drawingInProgress = null;
            clickCount = 0;
            return true;
        }
    }

    private boolean handleVariablePointCreation(long time, double price, String prefix,
                                                 java.util.function.Supplier<Drawing> creator) {
        AnchorPoint point = new AnchorPoint(time, price);

        if (drawingInProgress == null) {
            // First click - create drawing with first point
            drawingInProgress = creator.get();
            drawingLayer.addDrawing(drawingInProgress);
            clickCount = 1;
            return true;
        } else {
            // Additional clicks - add points
            if (drawingInProgress instanceof Polyline polyline) {
                polyline.addPoint(point);
            } else if (drawingInProgress instanceof Polygon polygon) {
                polygon.addPoint(point);
            }
            clickCount++;
            drawingLayer.markDirty();
            return true;
        }
    }

    /**
     * Completes a variable-point drawing (Polyline/Polygon).
     * Call this on double-click or when switching tools.
     */
    public void completeVariablePointDrawing() {
        if (drawingInProgress != null && activeTool.isVariablePointTool()) {
            if (drawingInProgress.isComplete()) {
                notifyDrawingCreated(drawingInProgress);
            } else {
                drawingLayer.removeDrawing(drawingInProgress);
            }
            drawingInProgress = null;
            clickCount = 0;
        }
    }

    private void setSecondPoint(Drawing drawing, long time, double price) {
        AnchorPoint point = new AnchorPoint(time, price);
        if (drawing instanceof TrendLine tl) {
            tl.setEnd(point);
        } else if (drawing instanceof Ray ray) {
            ray.setDirection(point);
        } else if (drawing instanceof ExtendedLine el) {
            el.setPoint2(point);
        } else if (drawing instanceof Rectangle rect) {
            rect.setCorner2(point);
        } else if (drawing instanceof Ellipse ellipse) {
            ellipse.setCorner2(point);
        } else if (drawing instanceof FibonacciRetracement fib) {
            fib.setLow(point);
        } else if (drawing instanceof FibonacciExtension fibExt) {
            fibExt.setPoint2(point);
        } else if (drawing instanceof ParallelChannel channel) {
            channel.setPoint2(point);
        } else if (drawing instanceof Pitchfork pitchfork) {
            pitchfork.setPoint2(point);
        } else if (drawing instanceof Triangle triangle) {
            triangle.setPoint2(point);
        } else if (drawing instanceof Arrow arrow) {
            arrow.setEnd(point);
        } else if (drawing instanceof MeasureTool measure) {
            measure.setEnd(point);
        } else if (drawing instanceof PriceRange priceRange) {
            priceRange.setBottomPrice(price);
        } else if (drawing instanceof Callout callout) {
            callout.setTextBox(point);
        } else if (drawing instanceof RegressionChannel regChannel) {
            regChannel.setEnd(point);
        } else if (drawing instanceof FibonacciTimeZones fibTZ) {
            fibTZ.setUnitEnd(point);
        } else if (drawing instanceof FibonacciFan fibFan) {
            fibFan.setEnd(point);
        } else if (drawing instanceof FibonacciArc fibArc) {
            fibArc.setEnd(point);
        } else if (drawing instanceof Arc arc) {
            arc.setRadiusPoint(point);
        } else if (drawing instanceof GannFan gannFan) {
            gannFan.setScalePoint(point);
        } else if (drawing instanceof GannBox gannBox) {
            gannBox.setCorner2(point);
        }
    }

    private void setThirdPoint(Drawing drawing, long time, double price) {
        AnchorPoint point = new AnchorPoint(time, price);
        if (drawing instanceof FibonacciExtension fibExt) {
            fibExt.setPoint3(point);
        } else if (drawing instanceof ParallelChannel channel) {
            channel.setPoint3(point);
        } else if (drawing instanceof Pitchfork pitchfork) {
            pitchfork.setPoint3(point);
        } else if (drawing instanceof Triangle triangle) {
            triangle.setPoint3(point);
        }
    }

    // ========== Drag handling ==========

    private void handleDrag(int x, int y, CoordinateSystem coords) {
        long time = coords.screenXToXValue(x);
        double price = coords.screenYToYValue(y);

        long deltaTime = time - coords.screenXToXValue(lastMouseX);
        double deltaPrice = price - coords.screenYToYValue(lastMouseY);

        AnchorPoint newPoint = new AnchorPoint(time, price);

        switch (activeHandle) {
            case ANCHOR_0 -> selectedDrawing.setAnchorPoint(0, newPoint);
            case ANCHOR_1 -> selectedDrawing.setAnchorPoint(1, newPoint);
            case ANCHOR_2 -> handleAnchor2Drag(selectedDrawing, newPoint, time, price);
            case ANCHOR_3 -> handleAnchor3Drag(selectedDrawing, newPoint, time, price);
            case BODY -> handleBodyDrag(selectedDrawing, deltaTime, deltaPrice);
        }

        lastMouseX = x;
        lastMouseY = y;
        drawingLayer.markDirty();
    }

    private void handleAnchor2Drag(Drawing drawing, AnchorPoint point, long time, double price) {
        // ANCHOR_2 is used for rectangles (opposite corner) and 3-point drawings
        if (drawing instanceof Rectangle rect) {
            // Adjust corner1 and corner2 to handle opposite corner drag
            AnchorPoint c1 = rect.getCorner1();
            AnchorPoint c2 = rect.getCorner2();
            // Keep time of corner2, price of corner1
            rect.setAnchorPoint(0, new AnchorPoint(c1.timestamp(), price));
            rect.setAnchorPoint(1, new AnchorPoint(time, c2.price()));
        } else if (drawing.getRequiredAnchorCount() >= 3) {
            // For 3-point drawings, directly set the third anchor point
            drawing.setAnchorPoint(2, point);
        }
    }

    private void handleAnchor3Drag(Drawing drawing, AnchorPoint point, long time, double price) {
        // ANCHOR_3 is used for rectangles (opposite corner)
        if (drawing instanceof Rectangle rect) {
            AnchorPoint c1 = rect.getCorner1();
            AnchorPoint c2 = rect.getCorner2();
            // Keep time of corner1, price of corner2
            rect.setAnchorPoint(0, new AnchorPoint(time, c1.price()));
            rect.setAnchorPoint(1, new AnchorPoint(c2.timestamp(), price));
        }
    }

    private void handleBodyDrag(Drawing drawing, long deltaTime, double deltaPrice) {
        if (drawing instanceof TrendLine tl) {
            tl.move(deltaTime, deltaPrice);
        } else if (drawing instanceof HorizontalLine hl) {
            hl.move(deltaPrice);
        } else if (drawing instanceof VerticalLine vl) {
            vl.move(deltaTime);
        } else if (drawing instanceof Ray ray) {
            ray.move(deltaTime, deltaPrice);
        } else if (drawing instanceof ExtendedLine el) {
            el.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Rectangle rect) {
            rect.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Ellipse ellipse) {
            ellipse.move(deltaTime, deltaPrice);
        } else if (drawing instanceof FibonacciRetracement fib) {
            fib.move(deltaTime, deltaPrice);
        } else if (drawing instanceof FibonacciExtension fibExt) {
            fibExt.move(deltaTime, deltaPrice);
        } else if (drawing instanceof ParallelChannel channel) {
            channel.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Pitchfork pitchfork) {
            pitchfork.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Triangle triangle) {
            triangle.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Arrow arrow) {
            arrow.move(deltaTime, deltaPrice);
        } else if (drawing instanceof MeasureTool measure) {
            measure.move(deltaTime, deltaPrice);
        } else if (drawing instanceof PriceRange priceRange) {
            priceRange.move(deltaPrice);
        } else if (drawing instanceof Callout callout) {
            callout.move(deltaTime, deltaPrice);
        } else if (drawing instanceof PriceLabel priceLabel) {
            priceLabel.move(deltaTime, deltaPrice);
        } else if (drawing instanceof RegressionChannel regChannel) {
            regChannel.move(deltaTime, deltaPrice);
        } else if (drawing instanceof FibonacciTimeZones fibTZ) {
            fibTZ.move(deltaTime, deltaPrice);
        } else if (drawing instanceof FibonacciFan fibFan) {
            fibFan.move(deltaTime, deltaPrice);
        } else if (drawing instanceof FibonacciArc fibArc) {
            fibArc.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Polyline polyline) {
            polyline.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Polygon polygon) {
            polygon.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Arc arc) {
            arc.move(deltaTime, deltaPrice);
        } else if (drawing instanceof GannFan gannFan) {
            gannFan.move(deltaTime, deltaPrice);
        } else if (drawing instanceof GannBox gannBox) {
            gannBox.move(deltaTime, deltaPrice);
        } else if (drawing instanceof Note note) {
            note.move(deltaTime, deltaPrice);
        }
    }

    // ========== Cursor helpers ==========

    private Cursor getCursorForHandle(Drawing.HandleType handle) {
        return switch (handle) {
            case ANCHOR_0, ANCHOR_1, ANCHOR_2, ANCHOR_3 -> Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            case BODY -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            default -> null;
        };
    }

    // ========== Notification helpers ==========

    private void notifyDrawingCreated(Drawing drawing) {
        for (DrawingListener listener : listeners) {
            listener.onDrawingCreated(drawing);
        }
    }

    private void notifyDrawingModified(Drawing drawing) {
        for (DrawingListener listener : listeners) {
            listener.onDrawingModified(drawing);
        }
    }

    private void notifyDrawingDeleted(Drawing drawing) {
        for (DrawingListener listener : listeners) {
            listener.onDrawingDeleted(drawing);
        }
    }

    private void notifySelectionChanged(Drawing drawing) {
        for (DrawingListener listener : listeners) {
            listener.onSelectionChanged(drawing);
        }
    }

    // ========== Additional API ==========

    /**
     * Returns the currently selected drawing.
     */
    public Drawing getSelectedDrawing() {
        return selectedDrawing;
    }

    /**
     * Returns the drawing layer.
     */
    public DrawingLayerV2 getDrawingLayer() {
        return drawingLayer;
    }

    /**
     * Returns true if currently dragging.
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Returns true if a drawing is in progress (being created).
     */
    public boolean isDrawingInProgress() {
        return drawingInProgress != null;
    }
}

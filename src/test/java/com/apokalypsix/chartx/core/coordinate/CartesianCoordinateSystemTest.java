package com.apokalypsix.chartx.core.coordinate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.apokalypsix.chartx.chart.axis.Viewport;

/**
 * Unit tests for CartesianCoordinateSystem.
 *
 * <p>Validates coordinate transformations between data space and screen space.
 */
class CartesianCoordinateSystemTest {

    private Viewport viewport;
    private CartesianCoordinateSystem coordSystem;

    @BeforeEach
    void setUp() {
        viewport = new Viewport();
        // Set up a simple 800x600 viewport showing X range [0, 1000] and Y range [0, 100]
        viewport.setSize(800, 600);
        viewport.setInsets(0, 60, 10, 30); // left, right, top, bottom
        viewport.setTimeRange(0, 1000);
        viewport.setPriceRange(0, 100);

        coordSystem = new CartesianCoordinateSystem(viewport);
    }

    @Test
    void xValueToScreenX_atStartOfRange_returnsLeftInset() {
        // X value at start of range should map to left inset
        double screenX = coordSystem.xValueToScreenX(0);
        assertEquals(0.0, screenX, 0.001, "X=0 should map to leftInset=0");
    }

    @Test
    void xValueToScreenX_atEndOfRange_returnsRightEdge() {
        // X value at end of range should map near right edge (minus right inset)
        // Chart width = 800 - 0 - 60 = 740
        double screenX = coordSystem.xValueToScreenX(1000);
        assertEquals(740.0, screenX, 0.001, "X=1000 should map to chart right edge");
    }

    @Test
    void xValueToScreenX_atMidpoint_returnsCenterOfChart() {
        double screenX = coordSystem.xValueToScreenX(500);
        assertEquals(370.0, screenX, 0.001, "X=500 should map to center of chart area");
    }

    @Test
    void yValueToScreenY_atMinValue_returnsBottomOfChart() {
        // Y value at minimum should map to bottom of chart area
        // Chart height = 600 - 10 - 30 = 560
        // Bottom = topInset + chartHeight = 10 + 560 = 570
        double screenY = coordSystem.yValueToScreenY(0);
        assertEquals(570.0, screenY, 0.001, "Y=0 should map to bottom of chart");
    }

    @Test
    void yValueToScreenY_atMaxValue_returnsTopOfChart() {
        // Y value at maximum should map to top of chart area (topInset)
        double screenY = coordSystem.yValueToScreenY(100);
        assertEquals(10.0, screenY, 0.001, "Y=100 should map to top of chart");
    }

    @Test
    void screenXToXValue_roundTrip() {
        // Test that converting to screen and back gives the original value
        long originalX = 333;
        double screenX = coordSystem.xValueToScreenX(originalX);
        long recoveredX = coordSystem.screenXToXValue(screenX);
        assertEquals(originalX, recoveredX, "Round-trip X conversion should preserve value");
    }

    @Test
    void screenYToYValue_roundTrip() {
        // Test that converting to screen and back gives the original value
        double originalY = 42.5;
        double screenY = coordSystem.yValueToScreenY(originalY);
        double recoveredY = coordSystem.screenYToYValue(screenY);
        assertEquals(originalY, recoveredY, 0.001, "Round-trip Y conversion should preserve value");
    }

    @Test
    void getPixelWidth_returnsCorrectWidth() {
        // For X span of 100 units, chart width 740, total range 1000
        // Pixel width = 100 * (740 / 1000) = 74
        double pixelWidth = coordSystem.getPixelWidth(100);
        assertEquals(74.0, pixelWidth, 0.001);
    }

    @Test
    void getPixelHeight_returnsCorrectHeight() {
        // For Y span of 10 units, chart height 560, total range 100
        // Pixel height = 10 * (560 / 100) = 56
        double pixelHeight = coordSystem.getPixelHeight(10);
        assertEquals(56.0, pixelHeight, 0.001);
    }

    @Test
    void batchXConversion_matchesSingleConversion() {
        long[] xValues = {0, 250, 500, 750, 1000};
        float[] screenX = new float[5];

        coordSystem.xValueToScreenX(xValues, screenX, 0, 5);

        for (int i = 0; i < xValues.length; i++) {
            double expected = coordSystem.xValueToScreenX(xValues[i]);
            assertEquals((float) expected, screenX[i], 0.001f,
                    "Batch conversion should match single conversion for index " + i);
        }
    }

    @Test
    void batchYConversion_matchesSingleConversion() {
        float[] yValues = {0f, 25f, 50f, 75f, 100f};
        float[] screenY = new float[5];

        coordSystem.yValueToScreenY(yValues, screenY, 0, 5);

        for (int i = 0; i < yValues.length; i++) {
            double expected = coordSystem.yValueToScreenY(yValues[i]);
            assertEquals((float) expected, screenY[i], 0.001f,
                    "Batch conversion should match single conversion for index " + i);
        }
    }

    @Test
    void cacheInvalidation_updatesTransformations() {
        // Initial conversion
        double initialScreenX = coordSystem.xValueToScreenX(500);

        // Change viewport and invalidate cache
        viewport.setTimeRange(0, 2000);  // Double the X range
        coordSystem.invalidateCache();

        // New conversion should reflect the change
        double newScreenX = coordSystem.xValueToScreenX(500);

        // X=500 in range [0,2000] with chart width 740 should be at 740 * (500/2000) = 185
        assertEquals(185.0, newScreenX, 0.001);
        assertNotEquals(initialScreenX, newScreenX, "Cache invalidation should update transformations");
    }
}

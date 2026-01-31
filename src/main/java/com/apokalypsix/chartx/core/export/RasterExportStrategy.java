package com.apokalypsix.chartx.core.export;

import com.apokalypsix.chartx.chart.export.ExportOptions;

import java.awt.image.BufferedImage;

/**
 * Strategy interface for raster (PNG/JPEG) export.
 */
public interface RasterExportStrategy {

    /**
     * Exports the chart to a BufferedImage.
     *
     * @param options export configuration
     * @return the rendered image at the specified scale
     */
    BufferedImage export(ExportOptions options);
}

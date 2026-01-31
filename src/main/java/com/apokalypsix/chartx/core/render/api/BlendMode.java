package com.apokalypsix.chartx.core.render.api;

/**
 * Blend modes for rendering operations.
 */
public enum BlendMode {
    /**
     * No blending - source overwrites destination.
     */
    NONE,

    /**
     * Standard alpha blending: src * srcAlpha + dst * (1 - srcAlpha).
     */
    ALPHA,

    /**
     * Additive blending: src + dst.
     */
    ADDITIVE,

    /**
     * Multiplicative blending: src * dst.
     */
    MULTIPLY,

    /**
     * Pre-multiplied alpha blending: src + dst * (1 - srcAlpha).
     */
    PREMULTIPLIED_ALPHA
}
